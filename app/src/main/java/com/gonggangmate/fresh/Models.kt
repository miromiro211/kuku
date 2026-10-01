package com.gonggangmate.fresh

import java.time.*
import java.time.temporal.ChronoUnit

val Seoul: ZoneId = ZoneId.of("Asia/Seoul")
val weekdays = listOf("월", "화", "수", "목", "금", "토", "일")
data class Message(val body: String, val mine: Boolean)
data class Opportunity(
    val id: String, val kind: String, val title: String, val detail: String,
    val deadline: String?, val url: String?, val eligibility: String = "",
    val startsOn: String? = null
) {
    fun daysLeft(now: Long): Long? = deadline?.let {
        runCatching { ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(Seoul).toLocalDate(), LocalDate.parse(it.take(10))) }.getOrNull()
    }
    fun badge(now: Long): String = daysLeft(now)?.let { if (it == 0L) "오늘 마감" else "D-$it" } ?: "일정 확인"
}
data class WorkItem(
    val id: String, val title: String, val kind: String, val courseId: Long? = null,
    val deadline: String? = null, val notes: String = "", val completed: Boolean = false
)
data class CompanionResult(
    val plan: AgentResult, val message: String, val opportunities: List<Opportunity>,
    val suggestedId: String?, val workTitle: String?, val workContent: String?, val ai: Boolean
)
data class CampusMoment(
    val current: CourseSession?, val next: CourseSession?, val freeMinutes: Int,
    val usableMinutes: Int, val today: List<CourseSession>
) {
    val inClass get() = current != null
}
fun campusMoment(sessions: List<CourseSession>, ids: Set<Long>, now: Long): CampusMoment {
    val local = Instant.ofEpochMilli(now).atZone(Seoul)
    val minute = local.hour * 60 + local.minute
    val today = sessions.filter { it.courseId in ids && it.day == local.dayOfWeek.value }.sortedBy { it.start }
    val current = today.firstOrNull { minute >= minuteOfDay(it.start) && minute < minuteOfDay(it.end) }
    val next = today.firstOrNull { minuteOfDay(it.start) > minute }
    val free = if (current != null) 0 else ((next?.let { minuteOfDay(it.start) } ?: 18 * 60) - minute).coerceAtLeast(0)
    return CampusMoment(current, next, free, (free - if (next != null) 15 else 0).coerceAtLeast(0), today)
}
fun minuteOfDay(time: String): Int = LocalTime.parse(time).let { it.hour * 60 + it.minute }
fun hasCourseConflict(sessions: List<CourseSession>, ids: Set<Long>): Boolean {
    val chosen = sessions.filter { it.courseId in ids }
    return chosen.any { first -> chosen.any { second -> first.courseId != second.courseId && first.day == second.day &&
        minuteOfDay(first.start) < minuteOfDay(second.end) && minuteOfDay(second.start) < minuteOfDay(first.end) } }
}
fun canFinishTimetable(courses: List<Course>, sessions: List<CourseSession>, ids: Set<Long>): Boolean =
    ids.isNotEmpty() && ids.all { id -> courses.any { it.id == id } && sessions.any { it.courseId == id } } && !hasCourseConflict(sessions, ids)

data class MateState(
    val booting: Boolean = true,
    val signedIn: Boolean = false,
    val accountEmail: String? = null,
    val authBusy: Boolean = false,
    val authMessage: String? = null,
    val timetableReady: Boolean = false,
    val timetableSaving: Boolean = false,
    val catalogBusy: Boolean = false,
    val catalogError: String? = null,
    val courses: List<Course> = emptyList(),
    val sessions: List<CourseSession> = emptyList(),
    val selectedCourseIds: Set<Long> = emptySet(),
    val agentBusy: Boolean = false,
    val agentError: String? = null,
    val serverResult: AgentResult? = null,
    val agentMessage: String = "시간표를 알려주면 네 하루에 맞춰 움직일게.",
    val suggestions: List<Opportunity> = emptyList(),
    val suggestedId: String? = null,
    val feedBusy: Boolean = false,
    val feedError: String? = null,
    val feedUpdatedAt: Long = 0,
    val resultUpdatedAt: Long = 0,
    val aiResponse: Boolean = false,
    val tasks: List<WorkItem> = emptyList(),
    val activeTaskId: String? = null,
    val focusEndsAt: Long? = null,
    val messages: List<Message> = emptyList(),
    val now: Long = System.currentTimeMillis()
) {
    val moment get() = campusMoment(sessions, selectedCourseIds, now)
    val primaryOpportunity get() = suggestions.firstOrNull { it.id == suggestedId }
    val activeTask get() = tasks.firstOrNull { it.id == activeTaskId && !it.completed }
    val chosenCourses get() = courses.filter { it.id in selectedCourseIds }
    fun courseName(id: Long) = courses.firstOrNull { it.id == id }?.name ?: "수업"
}
