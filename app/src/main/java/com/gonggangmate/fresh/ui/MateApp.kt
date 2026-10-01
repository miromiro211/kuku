package com.gonggangmate.fresh.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.gonggangmate.fresh.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateApp(vm: MateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var chat by rememberSaveable { mutableStateOf(false) }
    var profile by remember { mutableStateOf(false) }
    var selectedNews by remember { mutableStateOf<Opportunity?>(null) }
    var taskSheet by remember { mutableStateOf<String?>(null) }
    var newTask by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { vm.tick(); delay(30_000) }
        }
    }
    LaunchedEffect(state.signedIn) { if (!state.signedIn) { tab = 0; chat = false; profile = false; selectedNews = null; taskSheet = null; newTask = false } }
    when {
        state.booting -> Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KuruScene(Modifier.width(180.dp)); Text("쿠루가 하루를 준비하고 있어요", color = Muted)
                Spacer(Modifier.height(20.dp)); CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
        !state.signedIn -> AuthScreen(state, vm::authenticate, vm::resumeSession)
        !state.timetableReady -> TimetableScreen(state, vm::selectCourse, { vm.loadCourses() }, vm::finishTimetable, vm::signOut)
        else -> {
            BackHandler(enabled = tab != 0) { tab = 0 }
            Scaffold(containerColor = Paper, bottomBar = {
                Surface(color = Paper) {
                    NavigationBar(Modifier.padding(horizontal = 28.dp, vertical = 6.dp), containerColor = Color.White,
                        tonalElevation = 0.dp) {
                        listOf("쿠루", "내 계획", "학교 소식").forEachIndexed { i, label ->
                            NavigationBarItem(selected = tab == i, onClick = { tab = i },
                                icon = { Icon(listOf(Icons.Outlined.Spa, Icons.Outlined.Checklist, Icons.Outlined.NotificationsNone)[i], null) },
                                label = { Text(label) }, colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Mint, selectedIconColor = Green, selectedTextColor = Green, unselectedIconColor = Muted))
                        }
                    }
                }
            }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    when (tab) {
                        0 -> AgentHome(state, onProfile = { profile = true }, onChat = { chat = true },
                            onRefresh = { vm.requestAgent() }, onPlans = { tab = 1 }, onOpportunity = { selectedNews = it },
                            onAccept = { vm.acceptOpportunity(it); tab = 1; taskSheet = it.id }, onStopFocus = vm::stopFocus)
                        1 -> PlansScreen(state, onAdd = { newTask = true }, onTask = { taskSheet = it.id }, onComplete = vm::completeTask,
                            onTimetable = vm::editTimetable, onChat = { chat = true })
                        2 -> NewsScreen(state, vm::refreshFeed) { selectedNews = it }
                    }
                }
            }
            if (profile) ModalBottomSheet(onDismissRequest = { profile = false }, containerColor = Paper) {
                ProfileSheet(state, onEdit = { profile = false; vm.editTimetable() }, onSignOut = { profile = false; vm.signOut() })
            }
            if (chat) ModalBottomSheet(onDismissRequest = { chat = false }, containerColor = Paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                AgentChat(state, onSend = { vm.requestAgent(it) })
            }
            selectedNews?.let { item -> ModalBottomSheet(onDismissRequest = { selectedNews = null }, containerColor = Paper) {
                NewsDetail(item, state, onAccept = { selectedNews = null; vm.acceptOpportunity(item); tab = 1; taskSheet = item.id })
            } }
            taskSheet?.let { id -> state.tasks.firstOrNull { it.id == id }?.let { task ->
                ModalBottomSheet(onDismissRequest = { taskSheet = null }, containerColor = Paper) {
                    TaskDetail(task, state, onPrepare = { vm.prepareTask(task) }, onFocus = { vm.startFocus(task.id); taskSheet = null; tab = 0 },
                        onComplete = { vm.completeTask(task.id) })
                }
            } }
            if (newTask) NewTaskDialog(state, onDismiss = { newTask = false }, onAdd = { title, course, deadline ->
                newTask = false; vm.addTask(title, course, deadline)
            })
        }
    }
}
