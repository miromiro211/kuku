package com.gonggangmate.fresh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mate_demo_v1", 0)
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    private fun restore(): MateState = runCatching {
        val json = JSONObject(prefs.getString("state", "{}") ?: "{}")
        val rooms = json.optJSONObject("rooms") ?: JSONObject()
        MateState(
            freeMinutes = json.optInt("minutes", 60).coerceIn(10, 240),
            interest = json.optString("interest", "전체").takeIf { it in interests } ?: "전체",
            joined = (json.optJSONArray("joined") ?: JSONArray()).let { array ->
                (0 until array.length()).map { array.getString(it) }.filter { id -> demoMeetups.any { it.id == id } }.toSet()
            },
            messages = decodeMessages(json.optJSONArray("messages")).ifEmpty { MateState().messages },
            roomMessages = rooms.keys().asSequence().associateWith { decodeMessages(rooms.optJSONArray(it)) },
            scarf = json.optBoolean("scarf", true)
        )
    }.getOrDefault(MateState())

    private fun decodeMessages(array: JSONArray?): List<Message> = if (array == null) emptyList() else
        (0 until array.length()).map { i -> array.getJSONObject(i).let { Message(it.getString("body"), it.getBoolean("mine")) } }

    private fun encodeMessages(messages: List<Message>): JSONArray = JSONArray().apply {
        messages.takeLast(100).forEach { put(JSONObject().put("body", it.body).put("mine", it.mine)) }
    }

    private fun update(transform: (MateState) -> MateState) {
        val next = transform(mutableState.value)
        mutableState.value = next
        val rooms = JSONObject().apply { next.roomMessages.forEach { (id, messages) -> put(id, encodeMessages(messages)) } }
        val json = JSONObject().put("minutes", next.freeMinutes).put("interest", next.interest)
            .put("joined", JSONArray(next.joined.toList())).put("scarf", next.scarf)
            .put("messages", encodeMessages(next.messages)).put("rooms", rooms)
        prefs.edit().putString("state", json.toString()).apply()
    }

    fun setMinutes(minutes: Int) = update { it.copy(freeMinutes = minutes.coerceIn(10, 240)) }
    fun setInterest(interest: String) = update { it.copy(interest = interest.takeIf { item -> item in interests } ?: "전체") }
    fun toggleScarf() = update { it.copy(scarf = !it.scarf) }
    fun join(id: String) {
        if (demoMeetups.none { it.id == id }) return
        update { it.copy(joined = it.joined + id) }
    }
    fun leave(id: String) = update { it.copy(joined = it.joined - id, roomMessages = it.roomMessages - id) }
    fun sendAgent(raw: String) {
        val text = raw.trim().take(1000)
        if (text.isEmpty()) return
        update { before ->
            val next = before.copy(freeMinutes = DemoAgent.parseMinutes(text) ?: before.freeMinutes,
                interest = DemoAgent.parseInterest(text) ?: before.interest)
            next.copy(messages = (next.messages + Message(text, true) + Message(DemoAgent.reply(text, next), false)).takeLast(100))
        }
    }
    fun sendRoom(id: String, raw: String) {
        val text = raw.trim().take(1000)
        if (text.isEmpty() || id !in state.value.joined) return
        update { before ->
            val old = before.roomMessages[id].orEmpty()
            val reply = Message("[데모 자동 응답] 메시지를 저장했어요. 실제 참가자에게 전송되지는 않아요.", false)
            before.copy(roomMessages = before.roomMessages + (id to (old + Message(text, true) + reply).takeLast(100)))
        }
    }
    fun reset() = update { MateState() }
}
