package com.gonggangmate.fresh

import org.junit.Test
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

class SupabaseContractTest {
    @Test fun credentialsAndServerErrors() {
        validateCredentials(" student@naver.com ", "abc123!", true)
        check(runCatching { validateCredentials("123456", "abcdef", true) }.isFailure)
        check(runCatching { validateCredentials("student@naver.com", "123", true) }.isFailure)
        check(serverErrorMessage(400, """{"error_code":"invalid_credentials"}""").contains("비밀번호"))
        check(serverErrorMessage(422, """{"code":"email_not_confirmed"}""").contains("확인 메일"))
        check(serverErrorMessage(400, """{"error_code":"validation_failed"}""").contains("이메일 형식"))
        check(serverErrorMessage(500, "not json").contains("HTTP 500"))
    }

    @Test fun responseAndTimeContract() {
        val courses = parseCourses("""[{"id":2,"name":"데이터베이스","course_code":"CSE302"}]""")
        check(courses.single().id == 2L)
        val sessions = parseSessions("""[{"course_id":2,"day_of_week":1,"start_time":"09:00:00","end_time":"10:30:00","building_name":null,"room":null}]""")
        check(sessions.single().day - 1 == 0)
        check(sessions.single().place == "")
        check(timeHour(sessions.single().end) == 10.5f)
        check(runCatching { timeHour("25:30") }.isFailure)
        val classResult = parseAgentResult(classResponse)
        check(classResult.nextClass == null && classResult.freeMinutes == 0)
        check(classResult.steps.single().location == null)
        check(classResult.chatText().contains("09:00–10:30"))
        val free = JSONObject(classResponse).put("mode", "free_period")
            .put("free_minutes", 120).put("usable_minutes", 105)
            .put("next_class", JSONObject().put("name", "운영체제").put("start_time", "13:00")
                .put("building", "인문사회관").put("room", "204"))
        check(parseAgentResult(free.toString()).nextClass!!.contains("13:00 · 운영체제"))
    }

    @Test fun savedTimetableAndV17() {
        FakeHttp.install()
        FakeHttp.connections.clear(); FakeHttp.responses.clear()
        val repo = SupabaseRepository("https://contract.invalid", "sb_publishable_test")
        FakeHttp.responses.add(200 to JSONObject(session("jwt-user", 3600)).put("user", JSONObject().put("id", "user-test")).toString())
        repo.signIn("student@example.com", "password")
        FakeHttp.responses.add(200 to "[{\"course_id\":1}]")
        FakeHttp.responses.add(201 to "")
        FakeHttp.responses.add(204 to "")
        FakeHttp.responses.add(200 to "[{\"course_id\":2}]")
        repo.saveCourses(setOf(2L))
        val insert = FakeHttp.connections[2]
        check(insert.requestMethod == "POST")
        check(org.json.JSONArray(insert.body.toString("UTF-8")).getJSONObject(0).getString("user_id") == "user-test")
        check(FakeHttp.connections[3].requestMethod == "DELETE")
        check(FakeHttp.connections[3].url.query.contains("course_id=in.(1)"))
        check(FakeHttp.connections.drop(1).all { it.getRequestProperty("Authorization") == "Bearer jwt-user" })
        FakeHttp.responses.add(200 to JSONObject(classResponse).put("data_sources", JSONObject().put("timetable", "실제 시간표")).toString())
        val result = repo.recommend(emptySet(), false)
        check(result.dataSources["timetable"] == "실제 시간표")
        check(!JSONObject(FakeHttp.connections.last().body.toString("UTF-8")).has("selected_course_ids"))
        check(parseAgentResult(classResponse).dataSources.isEmpty())
        FakeHttp.responses.add(200 to "[{\"course_id\":2}]")
        FakeHttp.responses.add(403 to "{}")
        check(runCatching { repo.saveCourses(setOf(3L)) }.isFailure)
        check(FakeHttp.connections.last().requestMethod == "POST")
    }

    @Test fun headersRefreshAndAuthFailure() {
        FakeHttp.install()
        FakeHttp.connections.clear()
        FakeHttp.responses.clear()
        val repo = SupabaseRepository("https://contract.invalid", "sb_publishable_test")
        FakeHttp.responses.add(200 to "[]")
        repo.courses()
        check(FakeHttp.connections.last().getRequestProperty("Authorization") == null)
        check(FakeHttp.connections.last().getRequestProperty("apikey") == "sb_publishable_test")
        FakeHttp.responses.add(200 to session("jwt-before-refresh", 0))
        val beforeInvalid = FakeHttp.connections.size
        check(runCatching { repo.signUp("123456", "password") }.isFailure)
        check(FakeHttp.connections.size == beforeInvalid)
        repo.signIn("student@example.com", "password")
        FakeHttp.responses.add(200 to session("jwt-after-refresh", 3600))
        FakeHttp.responses.add(200 to classResponse)
        repo.recommend(setOf(2L, 1L), false)
        val refresh = FakeHttp.connections[2]
        check(refresh.url.query == "grant_type=refresh_token")
        val call = FakeHttp.connections.last()
        check(call.url.path == "/functions/v1/free-time-agent")
        check(call.getRequestProperty("Authorization") == "Bearer jwt-after-refresh")
        val body = JSONObject(call.body.toString("UTF-8"))
        check(body.getJSONArray("selected_course_ids").getLong(0) == 1L)
        check(!body.getBoolean("use_ai") && body.getString("current_time").endsWith("Z"))
        FakeHttp.responses.add(401 to "{}")
        check(runCatching { repo.recommend(setOf(1L), true) }.isFailure)
        check(!repo.signedIn)
        val count = FakeHttp.connections.size
        check(runCatching { repo.recommend(emptySet(), true) }.isFailure)
        check(FakeHttp.connections.size == count)
        FakeHttp.responses.add(200 to """{"user":{"id":"example"}}""")
        check(!repo.signUp("student@example.com", "password"))
    }
}

private fun session(token: String, seconds: Int) = JSONObject().put("access_token", token)
    .put("refresh_token", "refresh-test").put("expires_in", seconds).toString()

private val classResponse = """{"mode":"class_in_progress","current_time":"2026-10-02T00:00:00.000Z","free_minutes":0,"usable_minutes":0,"action":"현재 수업에 집중하세요","reason":"수업 중입니다","source":"rule","plan":[{"kind":"class","title":"데이터베이스","start_time":"09:00","end_time":"10:30","location":null,"minutes":90}],"reasoning_factors":["현재 수업 시간과 겹침"]}"""

private object FakeHttp {
    val responses = ArrayDeque<Pair<Int, String>>()
    val connections = mutableListOf<FakeConnection>()
    private var installed = false
    fun install() {
        if (!installed) {
            URL.setURLStreamHandlerFactory(URLStreamHandlerFactory { protocol ->
                if (protocol != "https") null else object : URLStreamHandler() {
                    override fun openConnection(url: URL) = FakeConnection(url, responses.removeFirst())
                        .also { connections.add(it) }
                }
            })
            installed = true
        }
    }
}
private class FakeConnection(url: URL, private val reply: Pair<Int, String>) : HttpURLConnection(url) {
    val body = ByteArrayOutputStream()
    override fun connect() {}
    override fun disconnect() {}
    override fun usingProxy() = false
    override fun getResponseCode() = reply.first
    override fun getOutputStream() = body
    override fun getInputStream() = ByteArrayInputStream(reply.second.toByteArray())
    override fun getErrorStream() = ByteArrayInputStream(reply.second.toByteArray())
}
