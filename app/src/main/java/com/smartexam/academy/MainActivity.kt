package com.smartexam.academy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartexam.academy.data.Config
import com.smartexam.academy.data.Repo
import com.smartexam.academy.ui.*

class MainActivity : ComponentActivity() {
    private lateinit var repo: Repo
    /** Set when the app is opened from a local notification. */
    private val openNotifications = mutableStateOf(false)

    private val askNotifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_notifications", false)) openNotifications.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = Repo(applicationContext)
        if (savedInstanceState == null && intent?.getBooleanExtra("open_notifications", false) == true) openNotifications.value = true
        Notifier.createChannel(this)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Brand.Navy.toArgb()))
        setContent {
            var theme by remember { mutableIntStateOf(repo.prefs.theme) }
            AppTheme(theme) {
                CompositionLocalProvider(LocalRepo provides repo) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Root(
                            openNotifications.value, { openNotifications.value = false },
                            onSignedIn = { onSignedIn() },
                            themeMode = theme, onTheme = { theme = it; repo.prefs.theme = it }
                        )
                    }
                }
            }
        }
    }

    /** After sign-in: background notification checks + Android 13 permission. */
    private fun onSignedIn() {
        Notifier.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            try { askNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) } catch (_: Exception) {}
        }
    }
}

private enum class Gate { CHECKING, LOGIN, PROFILE, MAIN }

@Composable
private fun Root(openNotifications: Boolean, consumeOpen: () -> Unit, onSignedIn: () -> Unit, themeMode: Int, onTheme: (Int) -> Unit) {
    if (!Config.isConfigured) { NotConfiguredScreen(); return }
    val repo = LocalRepo.current
    var gate by remember { mutableStateOf(if (repo.supa.session.isLoggedIn) Gate.CHECKING else Gate.LOGIN) }
    LaunchedEffect(gate) { if (gate == Gate.MAIN) onSignedIn() }

    LaunchedEffect(gate) {
        if (gate == Gate.CHECKING) {
            gate = try {
                val p = repo.profile()
                if (p == null || p.fullName.isBlank()) Gate.PROFILE else Gate.MAIN
            } catch (e: Exception) {
                when {
                    !repo.supa.session.isLoggedIn -> Gate.LOGIN
                    // offline: let the student in if we already know their name
                    repo.prefs.cachedName.isNotBlank() -> Gate.MAIN
                    else -> Gate.PROFILE
                }
            }
            if (gate == Gate.MAIN) try { repo.syncPending() } catch (_: Exception) {}
        }
    }

    when (gate) {
        Gate.CHECKING -> SplashScreen()
        Gate.LOGIN -> AuthFlow(onDone = { gate = Gate.CHECKING })
        Gate.PROFILE -> ProfileFormScreen(first = true, onDone = { gate = Gate.MAIN }, onBack = null)
        Gate.MAIN -> MainShell(themeMode, onTheme, openNotifications, consumeOpen, onLogout = { gate = Gate.LOGIN })
    }
}

@Composable
private fun MainShell(themeMode: Int, onTheme: (Int) -> Unit, openNotifications: Boolean, consumeOpen: () -> Unit, onLogout: () -> Unit) {
    val nav = remember { Nav() }
    LaunchedEffect(openNotifications) { if (openNotifications) { consumeOpen(); nav.push(Route.Notifications) } }
    CompositionLocalProvider(LocalNav provides nav) {
        BackHandler(enabled = nav.stack.size > 1 || nav.top !is Route.Home) {
            if (!nav.pop()) nav.tab(Route.Home)
        }
        val top = nav.top
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            bottomBar = { if (top.isRoot) BottomBar(top, nav) }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().let { if (top.isRoot) it else it.navigationBarsPadding() }) {
                when (top) {
                    Route.Home -> HomeScreen()
                    is Route.Exams -> ExamsScreen(top.categoryId)
                    Route.Offline -> OfflineScreen()
                    Route.Profile -> ProfileScreen(themeMode, onTheme, onLogout)
                    is Route.ExamDetail -> ExamDetailScreen(top.examId)
                    is Route.PackTests -> PackTestsScreen(top.packId, top.title, top.isFree)
                    is Route.Instructions -> InstructionsScreen(top.testId)
                    is Route.TakeTest -> TestScreen(top.bundle, top.hindi)
                    is Route.Result -> ResultScreen(top.bundle, top.attempt)
                    is Route.Analysis -> AnalysisScreen(top.bundle, top.attempt)
                    is Route.Review -> ReviewScreen(top.bundle, top.attempt)
                    is Route.Leaderboard -> LeaderboardScreen(top.testId, top.title)
                    Route.History -> HistoryScreen()
                    Route.EditProfile -> ProfileFormScreen(first = false, onDone = { nav.pop() }, onBack = { nav.pop() })
                    Route.Notifications -> NotificationsScreen()
                    Route.AiGuru -> AiGuruScreen()
                }
            }
        }
    }
}

@Composable
private fun BottomBar(top: Route, nav: Nav) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf(
            Triple("होम", Icons.Default.Home, Route.Home),
            Triple("परीक्षाएँ", Icons.Default.List, Route.Exams()),
            Triple("AI गुरु", Icons.Default.Star, Route.AiGuru),
            Triple("ऑफ़लाइन", Icons.Default.CheckCircle, Route.Offline),
            Triple("प्रोफ़ाइल", Icons.Default.Person, Route.Profile),
        )
        items.forEach { (label, icon, route) ->
            val selected = top::class == route::class
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected || top is Route.Exams) nav.tab(route) },
                icon = { Icon(icon, label) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
