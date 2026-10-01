package com.gonggangmate.fresh.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonggangmate.fresh.*
import java.net.URI
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun PlansScreen(state: MateState, onAdd: () -> Unit, onTask: (WorkItem) -> Unit, onComplete: (String) -> Unit, onTimetable: () -> Unit, onChat: () -> Unit) {
    var day by rememberSaveable { mutableIntStateOf(Instant.ofEpochMilli(state.now).atZone(Seoul).dayOfWeek.value) }
    val sessions = state.sessions.filter { it.courseId in state.selectedCourseIds && it.day == day }.sortedBy { it.start }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp, 24.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("작은 시작이 쌓이는 곳", "내 계획") {
            IconButton(onClick = onAdd, enabled = !state.agentBusy) { Icon(Icons.Outlined.Add, "과제나 할 일 추가", tint = Green) }
        } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("내 시간표", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onTimetable, enabled = !state.agentBusy) { Text("수정하기") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                weekdays.forEachIndexed { index, label -> FilterChip(day == index + 1, { day = index + 1 }, label = { Text(label) }, shape = CircleShape) }
            }
        }
        if (sessions.isEmpty()) item { Text("${weekdays[day - 1]}요일에는 선택한 수업이 없어요.", color = Muted, fontSize = 12.sp) }
        items(sessions) { session ->
            val active = day == Instant.ofEpochMilli(state.now).atZone(Seoul).dayOfWeek.value && state.moment.current == session
            Surface(shape = RoundedCornerShape(18.dp), color = if (active) Mint else Color.White) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(60.dp)) { Text(session.start.take(5), color = Green, fontWeight = FontWeight.SemiBold); Text(session.end.take(5), color = Muted, fontSize = 11.sp) }
                    Box(Modifier.width(2.dp).height(34.dp).background(if (active) Green else Line))
                    Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(state.courseName(session.courseId), fontWeight = FontWeight.Medium); Text(session.place.ifBlank { "강의실 미등록" }, color = Muted, fontSize = 11.sp) }
                    if (active) Text("수업 중", color = Green, fontSize = 10.sp)
                }
            }
        }
        state.serverResult?.let { plan -> item {
            Column(Modifier.fillMaxWidth().background(Mint.copy(alpha = .6f), RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("지금 공강의 흐름", color = Green, fontWeight = FontWeight.SemiBold)
                if (state.now - state.resultUpdatedAt >= 20 * 60_000) Text("이전 추천이에요. 쿠루 화면에서 현재 계획을 다시 확인해주세요.", color = Muted, fontSize = 11.sp)
                plan.steps.forEach { step -> Row {
                    Text("${step.start}–${step.end}", color = Green, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Column { Text(step.title, fontSize = 13.sp); step.location?.let { Text(it, color = Muted, fontSize = 11.sp) } }
                } }
            }
        } }
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("쿠루와 할 일", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${state.tasks.count { !it.completed }}개 준비 중", color = Muted, fontSize = 11.sp)
            }
        }
        if (state.tasks.isEmpty()) item {
            QuietEmpty("아직 정해둔 계획이 없어요", "쿠루의 제안을 선택하거나, 지금 할 과제를 추가해보세요.")
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(), enabled = !state.agentBusy) { Text("첫 번째 할 일 추가") }
        }
        items(state.tasks.sortedBy { it.completed }, key = { it.id }) { task ->
            Surface(onClick = { onTask(task) }, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Line)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, { onComplete(task.id) }, enabled = !state.agentBusy)
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Medium, color = if (task.completed) Muted else Ink)
                        Text(listOf(task.kind, task.courseId?.let { state.courseName(it) }, task.deadline?.let { "$it 마감" }).filterNotNull().joinToString(" · "), fontSize = 11.sp, color = Muted)
                        if (task.notes.isNotBlank()) Text("준비안이 도착했어요", color = Green, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, "계획 상세 보기", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item { Text("계획과 준비안은 이 기기에서 계정별로 보관돼요.", color = Muted, fontSize = 10.sp) }
        item { TextButton(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Text("쿠루와 다음 단계를 이야기하기") } }
    }
}

@Composable
fun NewsScreen(state: MateState, onRefresh: () -> Unit, onOpen: (Opportunity) -> Unit) {
    var kind by rememberSaveable { mutableStateOf("전체") }
    val items = state.suggestions.filter { (kind == "전체" || it.kind == kind) && (it.daysLeft(state.now)?.let { days -> days >= 0 } ?: true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("놓치지 않았으면 하는 기회", "학교 소식") {
            IconButton(onClick = onRefresh, enabled = !state.feedBusy) { Icon(Icons.Outlined.Refresh, "학교 소식 새로고침", tint = Green) }
        } }
        item {
            Text("쿠루가 학교의 새 소식을 주기적으로 살펴봐요.", color = Muted, fontSize = 12.sp)
            if (state.feedUpdatedAt > 0) Text("최근 확인 ${Instant.ofEpochMilli(state.feedUpdatedAt).atZone(Seoul).format(DateTimeFormatter.ofPattern("M.d HH:mm"))}", color = Muted, fontSize = 10.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("전체", "공모전", "장학금", "학사공지").forEach { label -> FilterChip(kind == label, { kind = label }, label = { Text(label) }, shape = CircleShape) }
            }
            if (state.feedBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.feedError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
        if (items.isEmpty() && !state.feedBusy) item { QuietEmpty("지금 확인된 예정 소식이 없어요", "새 일정이 들어오면 여기에 알려줄게요.") }
        items(items, key = { it.id }) { item ->
            Surface(onClick = { onOpen(item) }, color = Color.White, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(item.kind, color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); StatusPill(item.badge(state.now)) }
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    if (item.detail.isNotBlank()) Text(item.detail, color = Muted, fontSize = 12.sp, maxLines = 2)
                    Row { Text("쿠루와 준비해보기", color = Green, fontSize = 12.sp, modifier = Modifier.weight(1f)); Icon(Icons.Outlined.ChevronRight, null, tint = Green, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable fun PageTitle(eyebrow: String, title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(eyebrow, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(5.dp)); Text(title, style = MaterialTheme.typography.headlineLarge) }
        trailing()
    }
}

@Composable
fun NewsDetail(item: Opportunity, state: MateState, onAccept: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    val validUrl = item.url?.let { runCatching { URI(it) }.getOrNull() }?.takeIf { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row { StatusPill(item.kind); Spacer(Modifier.width(8.dp)); StatusPill(item.badge(state.now)) }
        Text(item.title, style = MaterialTheme.typography.headlineMedium)
        item.startsOn?.let { Text("시작 $it", color = Muted, fontSize = 12.sp) }
        item.deadline?.let { Text("마감 $it · 정확한 마감 시각은 원문 확인", color = Green, fontSize = 12.sp) }
        if (item.detail.isNotBlank()) Text(item.detail)
        if (item.eligibility.isNotBlank()) { Text("신청 조건", fontWeight = FontWeight.Bold); Text(item.eligibility, color = Muted) }
        Text("신청 자격과 제출 형식은 원문에서 확인해주세요. 쿠루는 준비를 도와줘요.", fontSize = 12.sp, color = Muted)
        if (validUrl != null) OutlinedButton(onClick = { runCatching { uriHandler.openUri(validUrl.toString()) }.onFailure { linkError = true } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("원문 확인하기")
        }
        if (linkError) Text("원문을 열지 못했어요. 연결을 확인해주세요.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Button(onClick = onAccept, enabled = !state.agentBusy && (item.daysLeft(state.now)?.let { it >= 0 } ?: true),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) { Text("쿠루와 이 기회 준비하기") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun TaskDetail(task: WorkItem, state: MateState, onPrepare: () -> Unit, onFocus: () -> Unit, onComplete: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val available = state.moment.usableMinutes.coerceAtMost(state.serverResult?.usableMinutes ?: state.moment.usableMinutes).coerceAtMost(25)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatusPill(if (task.completed) "완료한 계획" else task.kind)
        Text(task.title, style = MaterialTheme.typography.headlineMedium)
        task.deadline?.let { Text("$it 마감", color = Muted, fontSize = 12.sp) }
        if (state.agentBusy && state.activeTaskId == task.id) {
            Text("쿠루가 시작할 수 있는 준비안을 만들고 있어요.", color = Green)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        state.agentError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        if (task.notes.isNotBlank()) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) { MarkdownText(task.notes, Modifier.padding(18.dp), lineHeight = 25.sp) }
            TextButton(onClick = { clipboard.setText(AnnotatedString(task.notes)); copied = true }) {
                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(if (copied) "복사했어요" else "준비안 복사")
            }
        } else if (!state.agentBusy) Text("쿠루와 준비하면 작은 단계와 활용할 초안을 여기에서 볼 수 있어요.", color = Muted)
        if (!task.completed) {
            OutlinedButton(onClick = onPrepare, enabled = !state.agentBusy, modifier = Modifier.fillMaxWidth()) { Text(if (task.notes.isBlank()) "쿠루와 준비하기" else "준비안 다시 만들기") }
            Button(onClick = onFocus, enabled = available >= 5 && !state.moment.inClass && !state.agentBusy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) { Text(if (available >= 5) "${available}분 같이 집중하기" else "다음 공강에 시작하기") }
            Text("이동 여유를 남겨두고 시작해요. 수업이 시작되면 집중 타이머가 멈춰요.", color = Muted, fontSize = 11.sp)
        }
        TextButton(onClick = onComplete, enabled = !state.agentBusy, modifier = Modifier.fillMaxWidth()) { Text(if (task.completed) "다시 준비할 계획으로" else "이 계획 완료했어") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ProfileSheet(state: MateState, onEdit: () -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("쿠루와 너의 하루", style = MaterialTheme.typography.headlineMedium)
        Text(state.accountEmail.orEmpty(), color = Muted)
        Text("선택한 수강 과목 ${state.chosenCourses.size}개", fontWeight = FontWeight.SemiBold)
        state.chosenCourses.forEach { Text(it.name, color = Green, fontSize = 13.sp) }
        OutlinedButton(onClick = onEdit, enabled = !state.agentBusy, modifier = Modifier.fillMaxWidth()) { Text("시간표 다시 고르기") }
        Text("건국대학교 GLOCAL 학생을 위한 독립 프로젝트.\n쿠루는 시간표와 학교 소식으로 계획과 준비를 도와줘요.", color = Muted, fontSize = 12.sp)
        TextButton(onClick = onSignOut, enabled = !state.authBusy && !state.agentBusy, modifier = Modifier.fillMaxWidth()) { Text("로그아웃") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NewTaskDialog(state: MateState, onDismiss: () -> Unit, onAdd: (String, Long?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var course by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Paper, title = { Text("무엇부터 해볼까?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("과제나 할 일") }, placeholder = { Text("예: 데이터베이스 발표 준비") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            OutlinedTextField(deadline, { deadline = it }, label = { Text("마감일 · 선택") }, placeholder = { Text("2026-10-20") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("관련 과목 · 선택", color = Muted, fontSize = 12.sp)
            Column { state.chosenCourses.forEach { item -> FilterChip(selected = course == item.id, onClick = { course = if (course == item.id) null else item.id }, label = { Text(item.name) }) } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        } },
        confirmButton = { TextButton(onClick = {
            val date = deadline.trim().takeIf { it.isNotEmpty() }
            if (date != null && runCatching { LocalDate.parse(date) }.isFailure) error = "마감일을 YYYY-MM-DD 형식으로 입력해주세요."
            else onAdd(title, course, date)
        }, enabled = title.isNotBlank() && !state.agentBusy) { Text("쿠루와 시작하기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
fun AgentChat(state: MateState, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val scroll = rememberLazyListState()
    val submit = { if (input.isNotBlank() && !state.agentBusy) { onSend(input.trim()); input = "" } }
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) scroll.animateScrollToItem(state.messages.lastIndex) }
    Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Spa, null, tint = Green); Spacer(Modifier.width(10.dp))
            Column { Text("쿠루와 이야기", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("시간표를 기억하는 너의 캠퍼스 친구", color = Muted, fontSize = 11.sp) }
        }
        LazyColumn(Modifier.weight(1f), state = scroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.messages.isEmpty()) item { QuietEmpty("무엇이든 작은 시작부터", "공강 계획, 과제 개요, 공모전 준비를 같이 생각해봐요.") }
            items(state.messages) { message -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start) {
                Surface(color = if (message.mine) Green else Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 300.dp)) {
                    if (message.mine) Text(message.body, Modifier.padding(16.dp), color = Color.White, fontSize = 14.sp, lineHeight = 24.sp)
                    else MarkdownText(message.body, Modifier.padding(16.dp), color = Ink, fontSize = 14.sp, lineHeight = 24.sp)
                }
            } }
            if (state.agentBusy) item { Text("쿠루가 생각하고 있어…", color = Green, fontSize = 12.sp) }
            state.agentError?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("이 공강에 뭐 할까?", "다가오는 일정 알려줘").forEach { text ->
                SuggestionChip(onClick = { onSend(text) }, enabled = !state.agentBusy, label = { Text(text, fontSize = 11.sp) })
            }
            state.tasks.filterNot { it.completed }.take(6).forEach { task ->
                SuggestionChip(onClick = { onSend("등록된 과제 '${task.title}' 준비를 도와줘. 과제 목록에서 이 항목을 기준으로 준비 순서와 체크리스트를 만들어줘.") },
                    enabled = !state.agentBusy, label = { Text("${task.title} 준비", fontSize = 11.sp) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("쿠루, 나 지금…") },
                maxLines = 4, shape = RoundedCornerShape(22.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { submit() }))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = submit, enabled = input.isNotBlank() && !state.agentBusy) { Icon(Icons.AutoMirrored.Outlined.Send, "메시지 보내기") }
        }
    }
}
