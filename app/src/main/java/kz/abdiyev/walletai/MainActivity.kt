package kz.abdiyev.walletai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.ui.screens.AdvisorScreen
import kz.abdiyev.walletai.ui.screens.AnalyticsScreen
import kz.abdiyev.walletai.ui.screens.DashboardScreen
import kz.abdiyev.walletai.ui.screens.ProfileScreen
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainAppScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val profileViewModel: ProfileViewModel = koinViewModel()
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val analyticsViewModel: AnalyticsViewModel = koinViewModel()
    val advisorViewModel: AdvisorViewModel = koinViewModel()

    val themeMode by profileViewModel.themeMode.collectAsStateWithLifecycle()
    val snackbarMessage by dashboardViewModel.snackbarMessage.collectAsStateWithLifecycle()
    val profileSaveMessage by profileViewModel.saveMessage.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        when (snackbarMessage) {
            DashboardViewModel.SNACKBAR_DELETED -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Транзакция удалена",
                    actionLabel = "Отменить"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    dashboardViewModel.undoDelete()
                }
                dashboardViewModel.clearSnackbar()
            }
            DashboardViewModel.SNACKBAR_SAVE_ERROR -> {
                snackbarHostState.showSnackbar("Не удалось сохранить операцию")
                dashboardViewModel.clearSnackbar()
            }
        }
    }

    LaunchedEffect(profileSaveMessage) {
        if (profileSaveMessage == "saved") {
            snackbarHostState.showSnackbar("Сохранено")
            profileViewModel.clearSaveMessage()
        }
    }

    WalletAiTheme(themeMode = themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "WalletAI — Учёт"
                                1 -> "WalletAI — Аналитика"
                                2 -> "WalletAI — Советник"
                                else -> "WalletAI — Профиль"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(Icons.Default.Dashboard, contentDescription = "Учёт", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("Учёт", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(Icons.Default.Analytics, contentDescription = "Аналитика", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("Аналитика", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Советник", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("ИИ", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(Icons.Default.Person, contentDescription = "Профиль", modifier = Modifier.size(22.dp))
                        },
                        label = { Text("Профиль", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        onLoadDemo = profileViewModel::resetToDemoData,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> AnalyticsScreen(viewModel = analyticsViewModel, modifier = Modifier.fillMaxSize())
                    2 -> AdvisorScreen(viewModel = advisorViewModel, modifier = Modifier.fillMaxSize())
                    3 -> ProfileScreen(viewModel = profileViewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
