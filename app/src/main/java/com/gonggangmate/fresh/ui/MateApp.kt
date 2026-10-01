package com.gonggangmate.fresh.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gonggangmate.fresh.*
import kotlin.math.roundToInt

private val Rounded = RoundedCornerShape(24.dp)
private val tabs = listOf("홈", "추천", "약속", "채팅", "내 정보")
private val tabIcons = listOf(Icons.Outlined.Home, Icons.Outlined.AutoAwesome, Icons.Outlined.EventNote, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.PersonOutline)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateApp(vm: MateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }
    var roomId by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    val matches = DemoAgent.recommend(state.freeMinutes, state.interest)
    val details = demoMeetups.find { it.id == detailsId }
    val room = demoMeetups.find { it.id == roomId && it.id in state.joined }

    BackHandler(enabled = tab != 0 || roomId != null) {
        if (roomId != null) roomId = null else tab = 0
    }
    Scaffold(containerColor = Paper, bottomBar = {
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            tabs.forEachIndexed { index, label ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index; roomId = null },
                    icon = { Icon(tabIcons[index], contentDescription = null) }, label = { Text(label, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFECE4F7), selectedIconColor = Lilac))
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (tab) {
                0 -> Home(state, matches, onSettings = { settings = true }, onRecommendations = { tab = 1 }, onChat = { tab = 3 }, onScarf = vm::toggleScarf)
                1 -> Recommendations(state, matches, vm::setInterest, { settings = true }, { detailsId = it })
                2 -> Promises(state, { detailsId = it }, { roomId = it; tab = 3 }, { tab = 1 })
                3 -> Chat(state, room, onBack = { roomId = null }, onSend = { message ->
                    if (room != null) vm.sendRoom(room.id, message) else vm.sendAgent(message)
                }, onRecommendations = { tab = 1; roomId = null })
                4 -> Profile(state, vm::toggleScarf, { settings = true }, { resetConfirm = true })
            }
        }
    }
    if (settings) SettingsDialog(state, onDismiss = { settings = false }, onSave = { minutes, interest ->
        vm.setMinutes(minutes); vm.setInterest(interest); settings = false
    })
    if (details != null) ModalBottomSheet(onDismissRequest = { detailsId = null }, containerColor = Paper) {
        MeetupDetail(details, details.id in state.joined, details in matches,
            onJoin = { vm.join(details.id); detailsId = null; tab = 2 },
            onLeave = { vm.leave(details.id); detailsId = null },
            onChat = { roomId = details.id; detailsId = null; tab = 3 })
    }
    if (resetConfirm) AlertDialog(onDismissRequest = { resetConfirm = false }, title = { Text("데모를 처음부터 시작할까?") },
        text = { Text("이 기기에 저장한 대화, 데모 약속, 설정을 초기화해요.") },
        confirmButton = { TextButton(onClick = { vm.reset(); roomId = null; resetConfirm = false }) { Text("초기화") } },
        dismissButton = { TextButton(onClick = { resetConfirm = false }) { Text("취소") } })
}

@Composable private fun PageHeading(eyebrow: String, title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        trailing()
    }
}

@Composable private fun Home(state: MateState, matches: List<Meetup>, onSettings: () -> Unit, onRecommendations: () -> Unit, onChat: () -> Unit, onScarf: () -> Unit) {
    var greeting by rememberSaveable { mutableIntStateOf(0) }
    val greetings = listOf("공강이네! 오늘은 뭘 해볼까?", "네가 오니까 기분이 좋아졌어!", "커피? 산책? 같이 찾아보자.", "다음 수업 전 여유도 챙겨줄게.")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { PageHeading("너의 작은 캠퍼스 친구", "공강메이트") {
            Surface(color = Mint, shape = CircleShape) { Text("DEMO", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 10.sp, color = Color(0xFF536D48), fontWeight = FontWeight.Bold) }
        } }
        item {
            Row(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(Rounded).background(Color.White).clickable(onClick = onSettings).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = Lilac)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("지금 공강 ${state.freeMinutes}분", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${state.interest} · 왕복 이동 + 여유 5분 포함", fontSize = 11.sp, color = Muted)
                }
                Icon(Icons.Outlined.Tune, contentDescription = "공강 시간과 관심사 설정", tint = Muted)
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp).clip(Rounded).background(Color(0xFFF0EBF9))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("모아의 방", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("캐릭터를 눌러 인사해봐", fontSize = 11.sp, color = Muted)
                    }
                    IconButton(onClick = onScarf, modifier = Modifier.size(38.dp).background(Color.White.copy(alpha = .8f), CircleShape)) {
                        Icon(Icons.Outlined.Palette, "모아의 스카프 바꾸기", tint = Lilac, modifier = Modifier.size(20.dp))
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    MoaScene(state.scarf, Modifier.fillMaxWidth(), onTap = { greeting = (greeting + 1) % greetings.size })
                    Column(Modifier.align(Alignment.CenterStart).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoomButton(Icons.Outlined.AutoAwesome, "추천", onRecommendations)
                        RoomButton(Icons.Outlined.ChatBubbleOutline, "대화", onChat)
                    }
                }
                Surface(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .9f)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("모아", color = Lilac, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(greetings[greeting], fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("지금 가능한 작은 약속", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(if (matches.isEmpty()) "조건을 바꾸면 더 찾아볼 수 있어" else "모아가 ${matches.size}개를 골랐어", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onRecommendations) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "추천 모임 보기", tint = Lilac) }
            }
            Button(onClick = onRecommendations, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 10.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(16.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("내 공강에 맞는 모임 보기")
            }
        }
    }
}

@Composable private fun RoomButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = .94f), shadowElevation = 2.dp) {
        Column(Modifier.width(52.dp).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Lilac, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(3.dp)); Text(label, fontSize = 10.sp, color = Ink)
        }
    }
}

@Composable private fun Recommendations(state: MateState, matches: List<Meetup>, onInterest: (String) -> Unit, onSettings: () -> Unit, onDetail: (String) -> Unit) {
    Column {
        PageHeading("사람보다, 같이 할 일부터", "지금 뭐 할까?") { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, "추천 조건 바꾸기", tint = Lilac) } }
        Text("공강 ${state.freeMinutes}분 · 이동과 수업 준비까지 계산했어", Modifier.padding(horizontal = 24.dp), color = Muted, fontSize = 12.sp)
        InterestChips(state.interest, onInterest)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp, 4.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("예시 모임 · 실제 참가자는 연결되지 않아요", color = Muted, fontSize = 11.sp) }
            if (matches.isEmpty()) item { EmptyCard("맞는 모임이 아직 없어", "시간을 늘리거나 다른 관심사를 골라봐.", "시간 바꾸기", onSettings) }
            items(matches, key = { it.id }) { meetup -> MeetupCard(meetup, meetup.id in state.joined) { onDetail(meetup.id) } }
        }
    }
}

@Composable private fun InterestChips(selected: String, onSelected: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        interests.forEach { label -> FilterChip(selected = selected == label, onClick = { onSelected(label) }, label = { Text(label) }, shape = CircleShape) }
    }
}

@Composable private fun MeetupCard(meetup: Meetup, joined: Boolean, onClick: () -> Unit) {
    val tint = when (meetup.category) { "밥" -> Color(0xFFF7E8D4); "산책" -> Mint; "공부" -> Color(0xFFE5EAF8); else -> Color(0xFFF0E5F5) }
    Card(onClick = onClick, shape = Rounded, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().background(tint).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(meetup.emoji, fontSize = 42.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(meetup.category.uppercase(), color = Muted, fontSize = 11.sp)
                Text("${meetup.minutes}분이면 충분해", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(18.dp)) {
            Text(meetup.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("${meetup.place} · 도보 ${meetup.walkMinutes}분", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("예시 인원 ${meetup.people}/${meetup.capacity}명", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(if (joined) "참여한 데모 약속 ✓" else "살펴보기 →", color = Lilac, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable private fun MeetupDetail(meetup: Meetup, joined: Boolean, eligible: Boolean, onJoin: () -> Unit, onLeave: () -> Unit, onChat: () -> Unit) {
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp, 4.dp, 24.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(meetup.emoji, fontSize = 52.sp); Text(meetup.title, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        item { Text("${meetup.place} · 편도 ${meetup.walkMinutes}분 · 활동 ${meetup.minutes}분", color = Muted, fontSize = 13.sp) }
        item { Text(meetup.description, lineHeight = 25.sp) }
        item { Surface(shape = RoundedCornerShape(16.dp), color = Mint) {
            Text("활동 + 왕복 이동 + 수업 전 여유 5분\n총 ${meetup.minutes + meetup.walkMinutes * 2 + 5}분이 필요해요.", Modifier.padding(16.dp), color = Ink, fontSize = 13.sp)
        } }
        item { Text("데모 모임이에요. 참여 정보는 이 기기에만 저장되며 실제 예약이나 참가자 연결은 일어나지 않아요.", color = Muted, fontSize = 12.sp) }
        if (joined) {
            item { Button(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Text("데모 모임 채팅 열기") }; TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text("데모 약속 취소") } }
        } else {
            item {
                if (!eligible) Text("현재 추천 조건과 맞지 않아요. 시간이나 관심사를 바꿔줘.", color = Lilac, fontSize = 12.sp)
                Button(onClick = onJoin, enabled = eligible, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) { Text("데모로 참여하기") }
            }
        }
    }
}

@Composable private fun Promises(state: MateState, onDetail: (String) -> Unit, onChat: (String) -> Unit, onFind: () -> Unit) {
    val joined = demoMeetups.filter { it.id in state.joined }
    Column {
        PageHeading("작은 약속이 하루를 바꿔", "내 약속 ${joined.size}")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp, 4.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (joined.isEmpty()) item { EmptyCard("아직 잡은 약속이 없어", "마음에 드는 모임을 데모로 참여해봐.", "모임 찾아보기", onFind) }
            items(joined, key = { it.id }) { meetup ->
                MeetupCard(meetup, true) { onDetail(meetup.id) }
                OutlinedButton(onClick = { onChat(meetup.id) }, modifier = Modifier.fillMaxWidth()) { Text("${meetup.category} 모임 채팅") }
            }
        }
    }
}

@Composable private fun EmptyCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Surface(shape = Rounded, color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", color = Lilac, fontSize = 40.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp)); Text(body, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp)); TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable private fun Chat(state: MateState, room: Meetup?, onBack: () -> Unit, onSend: (String) -> Unit, onRecommendations: () -> Unit) {
    var input by rememberSaveable(room?.id) { mutableStateOf("") }
    val messages = if (room == null) state.messages else listOf(Message("${room.title} 데모 채팅이에요. 실제 참가자 대신 자동 안내가 응답해요.", false)) + state.roomMessages[room.id].orEmpty()
    val scroll = rememberLazyListState()
    LaunchedEffect(messages.size, room?.id) { if (messages.isNotEmpty()) scroll.animateScrollToItem(messages.lastIndex) }
    val submit = { if (input.isNotBlank()) { onSend(input); input = "" } }
    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeading(if (room == null) "시간과 하고 싶은 일을 말해줘" else "기기에만 저장되는 예시 대화", if (room == null) "모아와 대화" else room.title) {
            if (room != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "모아 채팅으로 돌아가기") }
        }
        Surface(color = Color(0xFFEEE8F6), modifier = Modifier.fillMaxWidth()) {
            Text(if (room == null) "오프라인 규칙 기반 데모 · AI 서버 연결 전" else "데모 모임 채팅 · 외부 전송 없음", Modifier.padding(horizontal = 24.dp, vertical = 9.dp), color = Muted, fontSize = 11.sp)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(18.dp), color = if (message.mine) Lilac else Color.White, modifier = Modifier.widthIn(max = 290.dp)) {
                        Text(message.body, Modifier.padding(15.dp), color = if (message.mine) Color.White else Ink, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
        }
        if (room == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("60분 있어, 커피 마실래", "30분 산책할래", "90분 공부하고 싶어").forEach { suggestion ->
                    SuggestionChip(onClick = { onSend(suggestion) }, label = { Text(suggestion, fontSize = 11.sp) })
                }
            }
            TextButton(onClick = onRecommendations, modifier = Modifier.align(Alignment.End).padding(end = 12.dp)) { Text("추천 모임 보기 →", fontSize = 12.sp) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it.take(1000) }, modifier = Modifier.weight(1f), placeholder = { Text("모아에게 말해줘…", fontSize = 13.sp) }, shape = RoundedCornerShape(24.dp), maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { submit() }))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = submit, enabled = input.isNotBlank()) { Icon(Icons.AutoMirrored.Outlined.Send, "메시지 보내기") }
        }
    }
}

@Composable private fun Profile(state: MateState, onScarf: () -> Unit, onSettings: () -> Unit, onReset: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageHeading("모아와 함께하는 캠퍼스", "내 공간") }
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = Rounded, color = Color.White) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("방문자", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("로그인 없이 둘러보는 오프라인 데모", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("공강 ${state.freeMinutes}분  ·  관심사 ${state.interest}")
                        TextButton(onClick = onSettings) { Text("공강 조건 바꾸기") }
                    }
                }
                Surface(shape = Rounded, color = Color(0xFFEDE5F6)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("모아의 스카프", fontWeight = FontWeight.Bold); Text("연두색 스카프로 작은 변화를", fontSize = 12.sp, color = Muted) }
                        Switch(checked = state.scarf, onCheckedChange = { onScarf() })
                    }
                }
                Text("이 프로젝트에는 로그인, Supabase, 실제 AI 모델, 실시간 참가자 연결이 아직 포함되어 있지 않아요. 추천·참여·대화 흐름을 시연하는 첫 버전이에요.", color = Muted, fontSize = 13.sp, lineHeight = 22.sp)
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("데모 초기화") }
            }
        }
    }
}

@Composable private fun SettingsDialog(state: MateState, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    var minutes by rememberSaveable { mutableFloatStateOf(state.freeMinutes.coerceIn(10, 240).toFloat()) }
    var interest by rememberSaveable { mutableStateOf(state.interest) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("공강, 얼마나 있어?") }, text = {
        Column {
            Text("${minutes.roundToInt()}분", fontSize = 32.sp, color = Lilac, fontWeight = FontWeight.Bold)
            Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 10f..240f, steps = 45)
            Text("이동 시간과 수업 전 5분 여유도 챙겨줄게.", fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(14.dp))
            Text("하고 싶은 일", fontWeight = FontWeight.SemiBold)
            InterestChips(interest) { interest = it }
        }
    }, confirmButton = { TextButton(onClick = { onSave(minutes.roundToInt(), interest) }) { Text("저장") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 393, heightDp = 851)
@Composable private fun HomePreview() {
    MateTheme {
        Surface(color = Paper) {
            Home(MateState(), DemoAgent.recommend(60, "전체"), {}, {}, {}, {})
        }
    }
}
