package com.gonggangmate.fresh.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonggangmate.fresh.*

@Composable
fun AuthScreen(state: MateState, onAuth: (String, String, Boolean) -> Unit, onRetry: () -> Unit) {
    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("KONKUK GLOCAL  /  CAMPUS COMPANION", color = Green, fontSize = 10.sp, letterSpacing = 1.4.sp)
        KuruScene(Modifier.widthIn(max = 250.dp).fillMaxWidth())
        Text("공강에도,\n너의 편이 있어.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(10.dp))
        Text("시간표를 기억하고, 기회를 찾아주는\n너의 작은 캠퍼스 에이전트 쿠루.", color = Muted, lineHeight = 23.sp)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth().background(Mint, RoundedCornerShape(20.dp)).padding(5.dp)) {
            listOf("로그인", "회원가입").forEachIndexed { index, title ->
                val selected = register == (index == 1)
                Surface(onClick = { register = index == 1; localError = null }, color = if (selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) { Text(title, color = if (selected) Ink else Muted, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(email, { email = it; localError = null }, label = { Text("이메일 주소") }, placeholder = { Text("name@naver.com") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), enabled = !state.authBusy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it; localError = null }, label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), enabled = !state.authBusy,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (register) ImeAction.Next else ImeAction.Done),
            trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "비밀번호 숨기기" else "비밀번호 보기") } })
        if (register) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text("비밀번호 확인") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), enabled = !state.authBusy,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Text("6자 이상으로 입력해주세요. 가입 후 확인 메일을 승인하면 로그인할 수 있어요.", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        (localError ?: state.authMessage)?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = Green, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (register && password != confirmation) localError = "비밀번호가 서로 달라요. 다시 확인해주세요."
            else onAuth(email, password, register)
        }, enabled = !state.authBusy && email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
            if (state.authBusy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(if (register) "쿠루와 함께 시작하기" else "로그인하고 쿠루 만나기", fontWeight = FontWeight.SemiBold)
        }
        if (state.authMessage?.contains("이전 로그인") == true) TextButton(onClick = onRetry) { Text("이전 로그인 다시 확인") }
        Spacer(Modifier.height(20.dp))
        Text("건국대학교 GLOCAL 학생을 위한 독립 프로젝트", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun TimetableScreen(state: MateState, onSelect: (Long) -> Unit, onReload: () -> Unit, onFinish: () -> Unit, onLogout: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = state.courses.filter { course ->
        (!selectedOnly || course.id in state.selectedCourseIds) && listOf(course.name, course.code, course.department, course.professor).any { it.contains(query.trim(), ignoreCase = true) }
    }
    val conflict = hasCourseConflict(state.sessions, state.selectedCourseIds)
    Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("01  /  너를 알아가는 중", color = Green, fontSize = 11.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onLogout, enabled = !state.agentBusy) { Text("로그아웃", fontSize = 12.sp) }
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("네 시간표부터\n알려줄래?", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("수업이 끝나는 순간부터,\n쿠루가 빈 시간을 챙겨줄게.", color = Muted)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("과목명 · 학과 · 교수 · 과목코드") }, shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, null) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selectedOnly, { selectedOnly = !selectedOnly }, label = { Text("선택한 과목 ${state.selectedCourseIds.size}") })
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReload, enabled = !state.catalogBusy && !state.timetableSaving) { Text("다시 불러오기") }
            }
            if (conflict) Text("수업 시간이 겹치는 과목이 있어요. 분반과 시간을 확인해주세요.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (state.catalogBusy || state.timetableSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.catalogError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp, 10.dp, 24.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (filtered.isEmpty() && !state.catalogBusy && !state.timetableSaving) item { QuietEmpty("과목이 보이지 않아요", "검색어를 바꾸거나 시간표를 다시 불러와주세요.") }
            items(filtered, key = { it.id }) { course ->
                val selected = course.id in state.selectedCourseIds
                val sessions = state.sessions.filter { it.courseId == course.id }
                Surface(onClick = { onSelect(course.id) }, enabled = sessions.isNotEmpty(), shape = RoundedCornerShape(20.dp),
                    color = if (selected) Mint else Color.White, border = BorderStroke(1.dp, if (selected) Green.copy(alpha = .4f) else Line)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(course.name, fontWeight = FontWeight.SemiBold)
                            Text(listOf(course.department, course.professor, course.code).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.height(5.dp))
                            if (sessions.isEmpty()) Text("서버에 수업시간이 아직 등록되지 않았어요", fontSize = 11.sp, color = Muted)
                            sessions.forEach { Text("${weekdays[it.day - 1]} ${it.start.take(5)}–${it.end.take(5)}  ${it.place}", fontSize = 12.sp, color = Green) }
                        }
                        Checkbox(selected, onCheckedChange = { onSelect(course.id) }, enabled = sessions.isNotEmpty())
                    }
                }
            }
        }
        Surface(color = Paper, shadowElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                Text("시간표는 나중에 수정할 수 있어요.", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                Button(onFinish, enabled = canFinishTimetable(state.courses, state.sessions, state.selectedCourseIds) && !state.catalogBusy && !state.timetableSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("${state.selectedCourseIds.size}개 과목으로 쿠루 만나기")
                }
            }
        }
    }
}
