package app.tendwell

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.tendwell.apps.AppEntry
import app.tendwell.home.HomeScreen
import app.tendwell.home.LauncherViewModel
import app.tendwell.search.SearchScreen
import app.tendwell.settings.SettingsScreen
import app.tendwell.ui.components.AppActionsSheet
import app.tendwell.ui.theme.TendwellTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

private enum class Screen { Home, Search, Settings }

/** The launcher activity. It handles the HOME intent, so pressing Home always lands here. */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Fires when the Home button is pressed while Tendwell is already showing. */
    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TendwellTheme {
                LauncherRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressed.tryEmit(Unit)
        }
    }

    @Composable
    private fun LauncherRoot() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var screen by rememberSaveable { mutableStateOf(Screen.Home) }
        var query by rememberSaveable { mutableStateOf("") }
        var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
        var sheetOnHome by rememberSaveable { mutableStateOf(false) }
        var isDefaultHome by rememberSaveable { mutableStateOf(true) }

        fun goHome() {
            screen = Screen.Home
            query = ""
            sheetFor = null
        }

        fun launch(app: AppEntry) {
            if (viewModel.launch(app)) {
                goHome()
            } else {
                Toast.makeText(this, "That app could not be opened.", Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) { homePressed.collect { goHome() } }
        LifecycleResumeEffect(Unit) {
            isDefaultHome = viewModel.isDefaultHome()
            onPauseOrDispose { }
        }
        // On Home, Back does nothing: there is nowhere further back to go.
        BackHandler(enabled = true) { if (screen != Screen.Home) goHome() }

        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    essentials = state.essentials,
                    loaded = state.loaded,
                    onLaunch = ::launch,
                    onLongPress = { sheetFor = it.key; sheetOnHome = true },
                    onOpenSearch = { screen = Screen.Search },
                    onOpenSettings = { screen = Screen.Settings },
                )
                Screen.Search -> SearchScreen(
                    apps = state.allApps,
                    query = query,
                    onQueryChange = { query = it },
                    onLaunch = ::launch,
                    onLongPress = { sheetFor = it.key; sheetOnHome = it.key in state.essentialKeys },
                    onClose = ::goHome,
                )
                Screen.Settings -> SettingsScreen(
                    isDefaultHome = isDefaultHome,
                    onOpenHomeSettings = { startSafely(viewModel.homeSettingsIntent()) },
                    onBack = ::goHome,
                )
            }

            val sheetApp = sheetFor?.let { key -> state.allApps.firstOrNull { it.key == key } }
            if (sheetApp != null) {
                val index = state.essentials.indexOfFirst { it.key == sheetApp.key }
                AppActionsSheet(
                    app = sheetApp,
                    onHome = sheetOnHome && index >= 0,
                    homeIsFull = state.homeIsFull,
                    canMoveUp = index > 0,
                    canMoveDown = index in 0 until state.essentials.lastIndex,
                    onAdd = { viewModel.addToHome(sheetApp) },
                    onRemove = { viewModel.removeFromHome(sheetApp) },
                    onMoveUp = { viewModel.moveOnHome(sheetApp, -1) },
                    onMoveDown = { viewModel.moveOnHome(sheetApp, 1) },
                    onAppInfo = { viewModel.openAppInfo(sheetApp) },
                    onDismiss = { sheetFor = null },
                )
            }
        }
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "This setting is not available on your phone.", Toast.LENGTH_SHORT).show()
        }
    }
}
