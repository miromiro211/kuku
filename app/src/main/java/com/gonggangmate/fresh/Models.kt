package com.gonggangmate.fresh

data class Meetup(
    val id: String, val title: String, val place: String, val category: String,
    val minutes: Int, val walkMinutes: Int, val people: Int, val capacity: Int,
    val description: String, val emoji: String
)
data class Message(val body: String, val mine: Boolean)
data class MateState(
    val freeMinutes: Int = 60,
    val interest: String = "전체",
    val joined: Set<String> = emptySet(),
    val messages: List<Message> = listOf(Message("안녕! 나는 네 공강을 챙겨주는 모아야.\n얼마나 시간이 있어? 하고 싶은 것도 알려줘.", false)),
    val roomMessages: Map<String, List<Message>> = emptyMap(),
    val scarf: Boolean = true
)
val interests = listOf("전체", "카페", "밥", "산책", "공부")
val demoMeetups = listOf(
    Meetup("coffee", "수업 전, 커피 한 잔", "캠퍼스 카페", "카페", 25, 3, 2, 4, "잠깐 쉬어 갈 사람? 커피 한 잔 하며 가볍게 이야기해요. 처음 만나는 사람도 환영!", "☕"),
    Meetup("lunch", "혼밥 대신 같이 점심", "학생회관 식당", "밥", 35, 5, 1, 3, "점심 메뉴를 같이 고르고 식사하는 작은 모임이에요. 다음 수업 전에는 마무리해요.", "🍙"),
    Meetup("walk", "캠퍼스 한 바퀴", "정문 앞", "산책", 15, 4, 2, 5, "햇볕 쐬며 가볍게 걷는 짧은 산책. 운동복이나 준비물은 필요 없어요.", "🌿"),
    Meetup("study", "조용히 50분 집중", "도서관 스터디존", "공부", 50, 5, 1, 4, "각자 할 일을 가져와 함께 집중해요. 끝나면 오늘 한 일을 한마디씩 나눠요.", "📚"),
    Meetup("coffee2", "작은 아이디어 수다", "도서관 라운지", "카페", 45, 4, 2, 4, "재밌는 앱이나 게임 아이디어를 부담 없이 나누는 모임. 전공은 상관없어요.", "💡")
)

/** Offline demo policy. This is a deterministic recommender, not a remote AI model. */
object DemoAgent {
    fun recommend(minutes: Int, interest: String): List<Meetup> = demoMeetups
        .filter { it.minutes + it.walkMinutes * 2 + 5 <= minutes }
        .filter { interest == "전체" || it.category == interest }
        .sortedBy { it.walkMinutes }

    fun parseMinutes(text: String): Int? = when {
        "한 시간 반" in text || "1시간 반" in text -> 90
        "두 시간" in text -> 120
        "한 시간" in text -> 60
        Regex("(\\d+)\\s*시간").containsMatchIn(text) -> {
            val hours = Regex("(\\d+)\\s*시간").find(text)?.groupValues?.get(1)?.toLongOrNull()
            val minutes = Regex("(\\d+)\\s*분").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            hours?.let { (it.coerceAtMost(4) * 60 + minutes.coerceAtMost(240)).coerceAtMost(240).toInt() }
        }
        else -> Regex("(\\d+)\\s*분").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }?.coerceIn(10, 240)

    fun parseInterest(text: String): String? = when {
        listOf("커피", "카페", "수다").any { it in text } -> "카페"
        listOf("밥", "점심", "배고", "식사").any { it in text } -> "밥"
        listOf("산책", "걷", "바람").any { it in text } -> "산책"
        listOf("공부", "과제", "집중").any { it in text } -> "공부"
        listOf("아무거나", "전체", "상관없", "다 보여").any { it in text } -> "전체"
        else -> null
    }

    fun reply(text: String, state: MateState): String {
        if (listOf("안녕", "반가워").any { it in text }) return "반가워! ‘60분 있어, 커피 마시고 싶어’처럼 말해줘. 시간과 관심사에 맞춰 찾아볼게."
        val matches = recommend(state.freeMinutes, state.interest)
        if (matches.isEmpty()) return "${state.freeMinutes}분 안에 다녀올 ${state.interest} 모임이 아직 없어. 공강 시간을 늘리거나 ‘전체’로 바꿔볼까? 왕복 이동과 5분 여유도 포함해서 계산했어."
        val top = matches.first()
        return "${state.freeMinutes}분 공강이면 ‘${top.title}’ 어때? ${top.place}까지 편도 ${top.walkMinutes}분, 활동 ${top.minutes}분이야. 왕복 이동과 5분 여유를 포함해도 가능해. 추천 탭에서 데모로 참여해볼 수 있어!"
    }
}
