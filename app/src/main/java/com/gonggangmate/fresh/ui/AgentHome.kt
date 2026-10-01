package com.gonggangmate.fresh.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonggangmate.fresh.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun AgentHome(state: MateState, onProfile: () -> Unit, onChat: () -> Unit, onRefresh: () -> Unit,
              onPlans: () -> Unit, onOpportunity: (Opportunity) -> Unit, onAccept: (Opportunity) -> Unit, onStopFocus: () -> Unit) {
    val moment = state.moment
    val local = Instant.ofEpochMilli(state.now).atZone(Seoul)
    val date = local.format(DateTimeFormatter.ofPattern("M월 d일")) + " ${weekdays[local.dayOfWeek.value - 1]}요일"
    val time = local.format(DateTimeFormatter.ofPattern("HH:mm"))
    val fresh = state.resultUpdatedAt > 0 && state.now - state.resultUpdatedAt < 20 * 60_000
    val currentTitle = if (moment.inClass) "${state.courseName(moment.current!!.courseId)} 수업 중" else if (moment.freeMinutes > 0) "지금 ${moment.freeMinutes}분의 여유" else "오늘도 수고했어"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 22.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KURU  /  KONKUK GLOCAL", color = Green, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                    Text("$date · $time", color = Muted, fontSize = 12.sp)
                }
                Surface(onClick = onProfile, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, Line)) {
                    Icon(Icons.Outlined.PersonOutline, "내 계정과 시간표", Modifier.padding(12.dp).size(20.dp), tint = Green)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                StatusPill(if (moment.inClass) "수업에 집중할 시간" else "네 하루를 살피는 중", if (moment.inClass) Sun else Green)
                Spacer(Modifier.height(14.dp))
                Text(currentTitle, style = MaterialTheme.typography.headlineMedium)
                Text(when {
                    moment.inClass -> "${moment.current!!.end.take(5)}에 다시 만날까?"
                    moment.next != null -> "다음 수업 ${moment.next.start.take(5)} · ${state.courseName(moment.next.courseId)}"
                    else -> "오늘 남은 수업은 없어. 네 속도로 보내자."
                }, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, start = 24.dp, end = 24.dp))
                KuruScene(Modifier.fillMaxWidth().widthIn(max = 430.dp), thinking = state.agentBusy, onTap = onChat)
            }
        }
        item {
            Surface(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = Color.White,
                border = BorderStroke(1.dp, Line.copy(alpha = .65f))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("쿠루의 제안", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (state.agentBusy) "살펴보고 있어" else if (!fresh) "업데이트 필요" else if (state.aiResponse) "AI가 정리했어" else "기본 시간표 계획", color = Muted, fontSize = 10.sp)
                    }
                    Text(if (state.agentBusy && state.serverResult == null) "네 수업 시간과 학교 소식을 함께 살펴보고 있어. 잠깐만 기다려줄래?"
                        else if (moment.inClass) "지금은 ${state.courseName(moment.current!!.courseId)}에 집중하자. 수업이 끝나면 남은 공강에 맞춰 다시 제안할게."
                        else if (!fresh) "현재 시간을 기준으로 계획을 확인해볼까? 이동 여유까지 챙겨줄게."
                        else state.agentMessage, fontSize = 16.sp, lineHeight = 26.sp)
                    state.agentError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    if (state.agentBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val opportunity = state.primaryOpportunity
                    if (opportunity != null && fresh && !moment.inClass) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAccept(opportunity) }, enabled = !state.agentBusy, shape = RoundedCornerShape(14.dp)) { Text("이걸로 준비할래") }
                            TextButton(onClick = { onOpportunity(opportunity) }) { Text("먼저 살펴볼게") }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPlans, shape = RoundedCornerShape(14.dp)) { Text("내 계획 살펴보기") }
                        TextButton(onClick = onRefresh, enabled = !state.agentBusy) { Text("다시 살펴봐줘") }
                    }
                }
            }
        }
        if (state.focusEndsAt != null && state.activeTask != null) item {
            FocusCard(state.activeTask!!.title, state.focusEndsAt, onStopFocus)
        }
        item {
            Surface(onClick = onChat, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), shape = RoundedCornerShape(22.dp), color = Mint.copy(alpha = .7f)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Green, modifier = Modifier.size(20.dp))
                    Text("쿠루에게 하고 싶은 말…", Modifier.weight(1f).padding(horizontal = 12.dp), color = Green, fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "쿠루와 대화하기", tint = Green, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (state.suggestions.isNotEmpty()) item {
            val item = state.primaryOpportunity ?: state.suggestions.first()
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).clickable { onOpportunity(item) }, verticalAlignment = Alignment.CenterVertically) {
                Text(item.badge(state.now), color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(item.title, Modifier.weight(1f).padding(start = 10.dp), color = Muted, maxLines = 1, fontSize = 12.sp)
                Icon(Icons.Outlined.ChevronRight, "학교 소식 자세히 보기", tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable fun StatusPill(text: String, color: Color = Green) {
    Surface(shape = CircleShape, color = color.copy(alpha = .09f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(color, CircleShape)); Spacer(Modifier.width(6.dp)); Text(text, fontSize = 10.sp, color = color)
        }
    }
}
@Composable fun QuietEmpty(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Spa, null, Modifier.size(28.dp), tint = Green.copy(alpha = .6f))
        Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.SemiBold)
        Text(body, Modifier.padding(top = 6.dp), color = Muted, fontSize = 12.sp)
    }
}
@Composable private fun FocusCard(title: String, endsAt: Long, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) { while (now < endsAt) { now = System.currentTimeMillis(); delay(1000) } }
    val seconds = ((endsAt - now) / 1000).coerceAtLeast(0)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), color = Green, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("함께 집중하는 중", color = Mint, fontSize = 11.sp); Text(title, color = Color.White, maxLines = 2) }
            Text("%02d:%02d".format(seconds / 60, seconds % 60), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onStop) { Icon(Icons.Outlined.StopCircle, "집중 타이머 종료", tint = Color.White) }
        }
    }
}
