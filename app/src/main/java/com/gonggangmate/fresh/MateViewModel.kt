package com.gonggangmate.fresh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SupabaseRepository(storage = AndroidSessionStorage(application))
    private val prefs = application.getSharedPreferences("kuru_profiles_v1", 0)
    private val mutableState = MutableStateFlow(MateState())
    val state = mutableState.asStateFlow()
    private var lastAttempt = 0L
    private var lastFingerprint = ""
    private var feedAttempt = 0L

    init { resumeSession() }

    fun resumeSession() {
        mutableState.update { it.copy(booting = true, authMessage = null) }
        viewModelScope.launch {
            try {
                val resumed = withContext(Dispatchers.IO) { repository.resumeSession() }
                if (resumed) enterAccount() else mutableState.update { it.copy(booting = false) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(booting = false, authMessage = "이전 로그인 확인에 실패했어요. 연결을 확인하고 다시 시도해주세요.") }
            }
        }
    }

    fun authenticate(email: String, password: String, register: Boolean = false) {
        if (state.value.authBusy) return
        try { validateCredentials(email, password, register) }
        catch (error: IllegalArgumentException) { mutableState.update { it.copy(authMessage = error.message) }; return }
        mutableState.update { it.copy(authBusy = true, authMessage = null) }
        viewModelScope.launch {
            try {
                val active = withContext(Dispatchers.IO) {
                    if (register) repository.signUp(email, password) else { repository.signIn(email, password); true }
                }
                if (active) enterAccount() else mutableState.update { it.copy(authMessage = "확인 메일을 보냈어요. 메일의 링크를 누른 후 로그인해주세요.") }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { mutableState.update { it.copy(authMessage = readable(error)) }
            } finally { mutableState.update { it.copy(authBusy = false) } }
        }
    }

    private fun enterAccount() {
        lastAttempt = 0; lastFingerprint = ""; feedAttempt = 0
        val profile = readProfile()
        val selected = profile.optJSONArray("courses") ?: JSONArray()
        val tasks = profile.optJSONArray("tasks") ?: JSONArray()
        val messages = profile.optJSONArray("messages") ?: JSONArray()
        mutableState.value = MateState(booting = false, signedIn = true, accountEmail = repository.email,
            selectedCourseIds = (0 until selected.length()).mapNotNull { selected.optLong(it).takeIf { id -> id > 0 } }.toSet(),
            tasks = (0 until tasks.length()).mapNotNull { i -> runCatching { tasks.getJSONObject(i).let {
                WorkItem(it.getString("id"), it.getString("title"), it.getString("kind"),
                    if (it.isNull("course_id")) null else it.getLong("course_id"),
                    if (it.isNull("deadline")) null else it.getString("deadline"), it.optString("notes", ""), it.optBoolean("completed"))
            } }.getOrNull() },
            messages = (0 until messages.length()).mapNotNull { i -> runCatching { messages.getJSONObject(i).let { Message(it.getString("body"), it.getBoolean("mine")) } }.getOrNull() })
        loadCourses(restoreReady = profile.optBoolean("ready"))
        refreshFeed()
    }

    fun loadCourses(restoreReady: Boolean = false) {
        if (state.value.catalogBusy || !state.value.signedIn) return
        mutableState.update { it.copy(catalogBusy = true, catalogError = null) }
        val account = repository.userId
        viewModelScope.launch {
            try {
                val (courses, sessions, saved) = withContext(Dispatchers.IO) {
                    Triple(repository.courses(), repository.sessions(), repository.savedCourseIds())
                }
                if (account != repository.userId) return@launch
                mutableState.update { before ->
                    val selected = (if (saved.isNotEmpty()) saved else before.selectedCourseIds).intersect(courses.map { it.id }.toSet())
                    before.copy(courses = courses, sessions = sessions, selectedCourseIds = selected,
                        timetableReady = saved.isNotEmpty() && canFinishTimetable(courses, sessions, selected))
                }
                if (state.value.timetableReady) requestAgent()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { mutableState.update { it.copy(catalogError = readable(error)) }
            } finally { if (account == repository.userId) mutableState.update { it.copy(catalogBusy = false) } }
        }
    }

    fun selectCourse(id: Long) {
        if (state.value.timetableSaving || state.value.catalogBusy || state.value.agentBusy || state.value.courses.none { it.id == id } || state.value.sessions.none { it.courseId == id }) return
        mutableState.update { it.copy(selectedCourseIds = if (id in it.selectedCourseIds) it.selectedCourseIds - id else it.selectedCourseIds + id) }
    }
    fun finishTimetable() {
        val before = state.value
        if (before.timetableSaving || before.catalogBusy || !canFinishTimetable(before.courses, before.sessions, before.selectedCourseIds)) return
        val account = repository.userId
        mutableState.update { it.copy(timetableSaving = true, catalogError = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.saveCourses(before.selectedCourseIds) }
                if (account != repository.userId) return@launch
                mutableState.update { it.copy(timetableReady = true, serverResult = null, resultUpdatedAt = 0, agentError = null) }
                saveProfile()
                requestAgent()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (account == repository.userId) mutableState.update { it.copy(catalogError = "시간표 저장에 실패했어요. " + readable(error)) }
            } finally { if (account == repository.userId) mutableState.update { it.copy(timetableSaving = false) } }
        }
    }
    fun editTimetable() {
        if (state.value.agentBusy) return
        mutableState.update { it.copy(timetableReady = false, focusEndsAt = null) }
    }

    fun refreshFeed() {
        if (state.value.feedBusy || !state.value.signedIn) return
        val account = repository.userId
        feedAttempt = System.currentTimeMillis()
        mutableState.update { it.copy(feedBusy = true, feedError = null) }
        viewModelScope.launch {
            try {
                val opportunities = withContext(Dispatchers.IO) { repository.opportunities() }
                if (account == repository.userId) mutableState.update { it.copy(suggestions = opportunities, feedUpdatedAt = System.currentTimeMillis()) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { if (account == repository.userId) mutableState.update { it.copy(feedError = readable(error)) }
            } finally { if (account == repository.userId) mutableState.update { it.copy(feedBusy = false) } }
        }
    }

    /** Called only while the app is in the foreground. Server data collection has its own cron. */
    fun tick() {
        val now = System.currentTimeMillis()
        mutableState.update { before ->
            val moment = campusMoment(before.sessions, before.selectedCourseIds, now)
            val stopped = before.focusEndsAt != null && (now >= before.focusEndsAt || moment.inClass)
            before.copy(now = now, focusEndsAt = if (stopped) null else before.focusEndsAt)
        }
        val current = state.value
        if (!current.signedIn || !current.timetableReady) return
        if (now - feedAttempt >= 15 * 60_000) refreshFeed()
        val fingerprint = "${java.time.Instant.ofEpochMilli(now).atZone(Seoul).toLocalDate()}:${current.moment.current?.courseId}:${current.moment.next?.courseId}"
        if (now - lastAttempt >= 15 * 60_000 || (fingerprint != lastFingerprint && now - lastAttempt >= 60_000)) requestAgent()
    }

    fun requestAgent(question: String? = null, opportunityId: String? = null, taskId: String? = null) {
        val before = state.value
        if (before.agentBusy || !before.signedIn || !before.timetableReady) return
        lastAttempt = System.currentTimeMillis()
        lastFingerprint = "${java.time.Instant.ofEpochMilli(lastAttempt).atZone(Seoul).toLocalDate()}:${before.moment.current?.courseId}:${before.moment.next?.courseId}"
        mutableState.update { it.copy(agentBusy = true, agentError = null,
            messages = if (question.isNullOrBlank()) it.messages else (it.messages + Message(question, true)).takeLast(60)) }
        viewModelScope.launch {
            try {
                val focused = before.tasks.firstOrNull { it.id == taskId }
                val agentTasks = if (focused == null) before.tasks else listOf(focused) + before.tasks.filterNot { it.id == taskId }
                val response = withContext(Dispatchers.IO) { repository.companion(before.selectedCourseIds, agentTasks, question, opportunityId, before.messages) }
                mutableState.update { current ->
                    val updatedTasks = if (taskId != null && !response.workContent.isNullOrBlank()) current.tasks.map {
                        if (it.id == taskId) it.copy(notes = response.workContent) else it
                    } else current.tasks
                    current.copy(serverResult = response.plan, agentMessage = response.message,
                        suggestions = response.opportunities, suggestedId = response.suggestedId,
                        resultUpdatedAt = System.currentTimeMillis(), aiResponse = response.ai,
                        tasks = updatedTasks, messages = (current.messages + Message(response.message +
                            (response.workContent?.let { "\n\n$it" } ?: ""), false)).takeLast(60))
                }
                saveProfile()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (!repository.signedIn) mutableState.value = MateState(booting = false, authMessage = "로그인이 만료됐어요. 다시 로그인해주세요.")
                else mutableState.update { it.copy(agentError = readable(error)) }
            } finally { mutableState.update { it.copy(agentBusy = false) } }
        }
    }

    fun acceptOpportunity(item: Opportunity) {
        if (state.value.agentBusy) return
        val existing = state.value.tasks.firstOrNull { it.id == item.id && !it.completed }
        if (existing == null) mutableState.update { it.copy(tasks = (it.tasks.filterNot { task -> task.id == item.id } +
            WorkItem(item.id, item.title, item.kind, deadline = item.deadline)), activeTaskId = item.id) }
        else mutableState.update { it.copy(activeTaskId = existing.id) }
        saveProfile()
        requestAgent("‘${item.title}’ 준비를 시작할게. 신청 조건에서 확인할 것과 이번 공강에 할 준비 계획·초안을 만들어줘.", item.id, item.id)
    }
    fun addTask(title: String, courseId: Long?, deadline: String?) {
        if (title.isBlank() || state.value.agentBusy) return
        val id = UUID.randomUUID().toString()
        mutableState.update { it.copy(tasks = it.tasks + WorkItem(id, title.trim().take(200), "과제", courseId, deadline), activeTaskId = id) }
        saveProfile()
        requestAgent("‘${title.trim().take(200)}’ 과제를 작은 단계로 나누고, 바로 시작할 수 있는 개요와 체크리스트를 만들어줘.", taskId = id)
    }
    fun prepareTask(task: WorkItem) {
        mutableState.update { it.copy(activeTaskId = task.id) }
        requestAgent("‘${task.title}’ 준비를 도와줘. 현재 공강에 할 단계와 활용할 수 있는 초안을 만들어줘.", opportunityId = task.id.takeIf { it.contains(':') }, taskId = task.id)
    }
    fun completeTask(id: String) {
        if (state.value.agentBusy) return
        mutableState.update { it.copy(tasks = it.tasks.map { task -> if (task.id == id) task.copy(completed = !task.completed) else task },
            focusEndsAt = if (it.activeTaskId == id) null else it.focusEndsAt) }
        saveProfile()
    }
    fun startFocus(id: String) {
        val current = state.value
        val freshMoment = campusMoment(current.sessions, current.selectedCourseIds, System.currentTimeMillis())
        val minutes = freshMoment.usableMinutes.coerceAtMost(current.serverResult?.usableMinutes ?: freshMoment.usableMinutes).coerceAtMost(25)
        if (minutes < 5 || freshMoment.inClass || current.tasks.none { it.id == id && !it.completed }) return
        mutableState.update { it.copy(activeTaskId = id, focusEndsAt = System.currentTimeMillis() + minutes * 60_000, now = System.currentTimeMillis()) }
    }
    fun stopFocus() { mutableState.update { it.copy(focusEndsAt = null) } }
    fun signOut() {
        if (state.value.agentBusy || state.value.authBusy || state.value.timetableSaving) return
        mutableState.update { it.copy(authBusy = true) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { repository.signOut() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Local session has already been cleared. */ }
            finally { mutableState.value = MateState(booting = false); lastAttempt = 0; feedAttempt = 0 }
        }
    }

    private fun readProfile(): JSONObject = runCatching { JSONObject(prefs.getString(repository.userId ?: "signed_out", "{}") ?: "{}") }.getOrDefault(JSONObject())
    private fun saveProfile() {
        val key = repository.userId ?: return
        val value = state.value
        val tasks = JSONArray().apply { value.tasks.forEach { put(JSONObject().put("id", it.id).put("title", it.title)
            .put("kind", it.kind).put("course_id", it.courseId).put("deadline", it.deadline).put("notes", it.notes).put("completed", it.completed)) } }
        val messages = JSONArray().apply { value.messages.takeLast(60).forEach { put(JSONObject().put("body", it.body).put("mine", it.mine)) } }
        val json = JSONObject().put("courses", JSONArray(value.selectedCourseIds.toList())).put("ready", value.timetableReady)
            .put("tasks", tasks).put("messages", messages)
        prefs.edit().putString(key, json.toString()).apply()
    }
    private fun readable(error: Exception) = if (error is SupabaseHttpError || error is IllegalArgumentException || error is IllegalStateException)
        error.message ?: "요청에 실패했어요. 다시 시도해주세요." else "서버에 연결하지 못했어요. 인터넷 연결을 확인해주세요."
}
