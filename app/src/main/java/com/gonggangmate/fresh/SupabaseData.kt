package com.gonggangmate.fresh

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Course(val id: Long, val name: String, val code: String, val department: String = "", val professor: String = "")
data class CourseSession(
    val courseId: Long, val day: Int, val start: String, val end: String, val place: String
)
data class PlanStep(
    val kind: String, val title: String, val start: String, val end: String,
    val location: String?, val minutes: Int
)
data class AgentResult(
    val mode: String, val evaluatedAt: String, val action: String, val reason: String,
    val freeMinutes: Int, val usableMinutes: Int, val nextClass: String?,
    val steps: List<PlanStep>, val factors: List<String>, val source: String,
    val dataSources: Map<String, String> = emptyMap()
) {
    fun chatText() = buildString {
        append(action).append("\n").append(reason)
        steps.forEach { append("\n").append(it.start).append("–").append(it.end)
            .append(" ").append(it.title)
            if (it.location != null) append(" · ").append(it.location)
        }
    }
}

fun timeHour(value: String): Float {
    val parts = value.split(":")
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    require(hour in 0..23 && minute in 0..59)
    return hour + minute / 60f
}

fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

private fun JSONObject.nullableText(key: String): String? =
    if (isNull(key)) null else getString(key).takeIf { it.isNotBlank() }

fun parseCourses(json: String): List<Course> = JSONArray(json).let { rows ->
    List(rows.length()) { index -> rows.getJSONObject(index).let {
        Course(it.getLong("id"), it.getString("name"), it.getString("course_code"), it.optString("department").takeUnless { v -> v == "null" }.orEmpty(),
            it.optString("professor").takeUnless { v -> v == "null" }.orEmpty())
    } }
}
fun parseSessions(json: String): List<CourseSession> = JSONArray(json).let { rows ->
    List(rows.length()) { index -> rows.getJSONObject(index).let {
        CourseSession(it.getLong("course_id"), it.getInt("day_of_week"),
            it.getString("start_time"), it.getString("end_time"),
            listOfNotNull(it.nullableText("building_name"), it.nullableText("room")).joinToString(" "))
    } }.also { sessions ->
        require(sessions.all { it.day in 1..7 && timeHour(it.start) < timeHour(it.end) })
    }
}
fun parseAgentResult(json: String): AgentResult {
    val value = JSONObject(json)
    val plan = value.getJSONArray("plan")
    val steps = List(plan.length()) { index -> plan.getJSONObject(index).let {
        PlanStep(it.getString("kind"), it.getString("title"), it.getString("start_time"),
            it.getString("end_time"), it.nullableText("location"), it.getInt("minutes"))
    } }
    val factors = value.optJSONArray("reasoning_factors") ?: JSONArray()
    val next = value.optJSONObject("next_class")
    return AgentResult(value.getString("mode"), value.getString("current_time"),
        value.getString("action"), value.optString("reason", ""),
        value.getInt("free_minutes"), value.getInt("usable_minutes"),
        next?.let { "${it.optString("start_time")} · ${it.optString("name")} " +
            listOfNotNull(it.nullableText("building"), it.nullableText("room")).joinToString(" ") },
        steps, List(factors.length()) { factors.getString(it) }, value.optString("source", "rule"),
        value.optJSONObject("data_sources")?.let { sources ->
            sources.keys().asSequence().mapNotNull { rawKey ->
                val key = rawKey as? String ?: return@mapNotNull null
                sources.nullableText(key)?.let { key to it }
            }.toMap() } ?: emptyMap())
}

fun validateCredentials(email: String, password: String, register: Boolean = false) {
    require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())) {
        "올바른 이메일 주소를 입력해주세요. 예: name@naver.com"
    }
    require(password.isNotEmpty()) { "비밀번호를 입력해주세요." }
    require(!register || password.length >= 6) { "회원가입 비밀번호는 6자 이상 입력해주세요." }
}

fun serverErrorMessage(status: Int, response: String): String {
    val error = runCatching { JSONObject(response) }.getOrNull()
    return when (error?.optString("error_code")?.takeIf { it.isNotBlank() } ?: error?.optString("code")) {
        "invalid_credentials" -> "이메일 또는 비밀번호가 올바르지 않습니다."
        "email_not_confirmed" -> "가입 확인 메일을 승인한 뒤 로그인해주세요."
        "email_address_invalid" -> "사용 가능한 실제 이메일 주소를 입력해주세요."
        "validation_failed" -> "이메일 형식과 비밀번호를 확인해주세요."
        "weak_password" -> "비밀번호가 보안 조건을 충족하지 않습니다. 더 긴 비밀번호에 영문·숫자·특수문자를 포함해주세요."
        "email_exists", "user_already_exists" -> "이미 가입된 이메일입니다. 로그인해주세요."
        "email_address_not_authorized" -> "현재 서버에서 이 주소로 인증 메일을 보낼 수 없습니다. 메일 발송 설정 확인이 필요합니다."
        "signup_disabled", "email_provider_disabled" -> "현재 서버에서 이메일 회원가입이 비활성화되어 있습니다."
        "over_email_send_rate_limit", "over_request_rate_limit" -> "요청이 많습니다. 잠시 후 다시 시도해주세요."
        else -> when (status) {
            400, 422 -> "입력한 이메일과 비밀번호를 확인해주세요."
            401, 403 -> "인증에 실패했습니다. 로그인과 서버 연결 설정을 확인해주세요."
            429 -> "요청이 많습니다. 잠시 후 다시 시도해주세요."
            else -> "서버 요청에 실패했습니다 (HTTP $status). 잠시 후 다시 시도해주세요."
        }
    }
}

class SupabaseHttpError(val status: Int, message: String) : Exception(message)

// Blocking HTTP is called only on Dispatchers.IO. Passwords are never stored.
interface SessionStorage {
    fun read(): String?
    fun save(value: String)
    fun clear()
}

class SupabaseRepository(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    private val storage: SessionStorage? = null
) {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt = 0L
    var userId: String? = null
        private set
    var email: String? = null
        private set
    val signedIn: Boolean get() = accessToken != null

    private fun request(path: String, body: JSONObject? = null, token: String? = null,
                        method: String = if (body == null) "GET" else "POST", payload: JSONArray? = null): String {
        require(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
            "Supabase 연결 설정을 확인해주세요."
        }
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 90000
            connection.instanceFollowRedirects = false
            connection.requestMethod = method
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null || payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write((payload?.toString() ?: body.toString()).toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = serverErrorMessage(status, response)
                throw SupabaseHttpError(status, message)
            }
            return response
        } finally { connection.disconnect() }
    }

    fun courses() = parseCourses(request("/rest/v1/course_catalog?select=id,name,course_code,department,professor&order=name&limit=1000"))
    fun sessions() = parseSessions(request("/rest/v1/course_sessions?select=course_id,day_of_week,start_time,end_time,building_name,room&order=day_of_week,start_time&limit=1000"))

    @Synchronized
    fun savedCourseIds(): Set<Long> {
        check(signedIn) { "로그인해주세요." }
        refreshIfNeeded()
        val owner = checkNotNull(userId) { "로그인 사용자 정보를 확인할 수 없어요. 다시 로그인해주세요." }
        val rows = JSONArray(request("/rest/v1/user_courses?select=course_id&user_id=eq.$owner", token = accessToken))
        return (0 until rows.length()).map { rows.getJSONObject(it).getLong("course_id") }.toSet()
    }

    @Synchronized
    fun saveCourses(ids: Set<Long>) {
        require(ids.isNotEmpty() && ids.all { it > 0 }) { "시간표에서 과목을 선택해주세요." }
        val existing = savedCourseIds()
        val owner = checkNotNull(userId)
        // Insert first so a failed insert never erases the previous timetable.
        val added = ids - existing
        if (added.isNotEmpty()) {
            val rows = JSONArray().apply { added.sorted().forEach {
                put(JSONObject().put("user_id", owner).put("course_id", it))
            } }
            request("/rest/v1/user_courses", token = accessToken, method = "POST", payload = rows)
        }
        val removed = existing - ids
        if (removed.isNotEmpty()) request(
            "/rest/v1/user_courses?user_id=eq.$owner&course_id=in.(${removed.sorted().joinToString(",")})",
            token = accessToken, method = "DELETE")
        check(savedCourseIds() == ids) { "시간표 저장 결과를 확인할 수 없어요. 다시 저장해주세요." }
    }

    private fun acceptSession(json: String) {
        val session = JSONObject(json)
        val nextAccess = session.getString("access_token")
        val nextRefresh = session.getString("refresh_token")
        val nextExpiry = System.currentTimeMillis() + session.getLong("expires_in") * 1000
        val user = session.optJSONObject("user")
        val nextUserId = user?.nullableText("id") ?: userId
        val nextEmail = user?.nullableText("email") ?: email
        val stored = JSONObject().put("access_token", nextAccess).put("refresh_token", nextRefresh)
            .put("expires_at", nextExpiry).put("user_id", nextUserId).put("email", nextEmail)
        runCatching { storage?.save(stored.toString()) }
        userId = nextUserId
        email = nextEmail
        accessToken = session.getString("access_token")
        refreshToken = session.getString("refresh_token")
        expiresAt = System.currentTimeMillis() + session.getLong("expires_in") * 1000
    }
    @Synchronized
    fun signIn(email: String, password: String) {
        validateCredentials(email, password)
        acceptSession(request("/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password)))
    }
    @Synchronized
    fun signUp(email: String, password: String): Boolean {
        validateCredentials(email, password, register = true)
        val response = request("/auth/v1/signup", JSONObject().put("email", email.trim()).put("password", password))
        val active = !JSONObject(response).isNull("access_token")
        if (active) acceptSession(response)
        return active
    }
    @Synchronized
    fun clearSession() {
        accessToken = null; refreshToken = null; expiresAt = 0; userId = null; email = null
        runCatching { storage?.clear() }
    }
    @Synchronized
    fun resumeSession(): Boolean {
        val saved = storage?.read() ?: return false
        try {
            val session = JSONObject(saved)
            userId = session.getString("user_id").also { require(it.isNotBlank() && it != "null") }
            email = session.nullableText("email")
            accessToken = session.getString("access_token")
            refreshToken = session.getString("refresh_token")
            expiresAt = session.getLong("expires_at")
            refreshIfNeeded()
            return true
        } catch (error: SupabaseHttpError) {
            if (error.status == 400 || error.status == 401 || error.status == 403) { clearSession(); return false }
            throw error
        } catch (error: java.io.IOException) { throw error
        } catch (_: Exception) { clearSession(); return false }
    }
    private fun refreshIfNeeded() {
        if (System.currentTimeMillis() >= expiresAt - 60000) {
            acceptSession(request("/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refreshToken)))
        }
    }
    @Synchronized
    fun signOut() {
        val token = accessToken
        clearSession()
        if (token != null) request("/auth/v1/logout?scope=local", JSONObject(), token)
    }
    @Synchronized
    fun recommend(ids: Set<Long>, useAi: Boolean): AgentResult {
        require(ids.all { it > 0 }) { "과목 정보를 확인해주세요." }
        check(signedIn) { "공강 추천을 받으려면 로그인해주세요." }
        try {
            refreshIfNeeded()
            return parseAgentResult(request("/functions/v1/free-time-agent",
                JSONObject().put("current_time", utcNow()).put("use_ai", useAi).apply {
                    if (ids.isNotEmpty()) put("selected_course_ids", JSONArray(ids.sorted()))
                }, accessToken))
        } catch (error: SupabaseHttpError) {
            if (error.status == 401 || error.status == 403) {
                clearSession()
                throw IllegalStateException("로그인이 만료됐습니다. 다시 로그인해주세요.")
            }
            throw error
        }
    }
    fun opportunities(): List<Opportunity> {
        val today = java.time.LocalDate.now(Seoul).toString()
        val contests = request("/rest/v1/contests?select=id,title,description,deadline,start_date,target,application_url,source_url&or=(deadline.gte.$today,deadline.is.null)&order=deadline.asc.nullslast&limit=40")
        val scholarships = request("/rest/v1/scholarships?select=id,title,description,deadline,application_start,eligibility,application_url,source_url&or=(deadline.gte.$today,deadline.is.null)&order=deadline.asc.nullslast&limit=40")
        val academic = request("/rest/v1/academic_events?select=id,title,description,start_date,end_date,source_url&or=(end_date.gte.$today,and(end_date.is.null,start_date.gte.$today))&order=start_date&limit=40")
        return parseOpportunities(contests, "공모전") + parseOpportunities(scholarships, "장학금") + parseOpportunities(academic, "학사공지")
    }
    @Synchronized
    fun companion(ids: Set<Long>, tasks: List<WorkItem>, question: String? = null, opportunityId: String? = null, messages: List<Message> = emptyList()): CompanionResult {
        require(ids.isNotEmpty() && ids.all { it > 0 }) { "시간표에서 과목을 선택해주세요." }
        check(signedIn) { "로그인해주세요." }
        try {
            refreshIfNeeded()
            val pending = JSONArray().apply { tasks.filter { !it.completed }.take(12).forEach {
                put(JSONObject().put("title", it.title.take(200)).put("course_id", it.courseId)
                    .put("deadline", it.deadline).put("kind", it.kind).put("notes", it.notes.take(1600)))
            } }
            val conversation = JSONArray().apply { messages.takeLast(8).forEach {
                put(JSONObject().put("mine", it.mine).put("body", it.body.take(2500)))
            } }
            val body = JSONObject().put("selected_course_ids", JSONArray(ids.sorted()))
                .put("conversation", conversation).put("tasks", pending).put("question", question?.take(1500)).put("opportunity_id", opportunityId)
            return parseCompanionResult(request("/functions/v1/campus-companion", body, accessToken))
        } catch (error: SupabaseHttpError) {
            if (error.status == 401 || error.status == 403) {
                clearSession()
                throw IllegalStateException("로그인이 만료됐습니다. 다시 로그인해주세요.")
            }
            throw error
        }
    }

}

fun parseOpportunities(json: String, kind: String): List<Opportunity> = JSONArray(json).let { rows ->
    List(rows.length()) { i -> rows.getJSONObject(i).let {
        Opportunity("${if (kind == "공모전") "contest" else if (kind == "장학금") "scholarship" else "academic"}:${it.getLong("id")}",
            kind, it.getString("title"), it.nullableText("description").orEmpty(),
            it.nullableText(if (kind == "학사공지") "end_date" else "deadline") ?: if (kind == "학사공지") it.nullableText("start_date") else null,
            it.nullableText("application_url") ?: it.nullableText("source_url"),
            it.nullableText("eligibility") ?: it.nullableText("target").orEmpty(),
            it.nullableText("start_date") ?: it.nullableText("application_start"))
    } }
}
fun parseCompanionResult(json: String): CompanionResult {
    val result = JSONObject(json)
    val opportunities = result.optJSONArray("opportunities") ?: JSONArray()
    val items = List(opportunities.length()) { i -> opportunities.getJSONObject(i).let {
        Opportunity(it.getString("id"), it.getString("kind"), it.getString("title"), it.optString("detail", ""),
            it.nullableText("deadline"), it.nullableText("url"), it.optString("eligibility", ""), it.nullableText("starts_on"))
    } }
    val work = result.optJSONObject("work")
    return CompanionResult(parseAgentResult(json), result.getString("companion_message"), items,
        result.nullableText("suggested_opportunity_id"), work?.nullableText("title"), work?.nullableText("content"), result.optBoolean("companion_ai", false))
}
