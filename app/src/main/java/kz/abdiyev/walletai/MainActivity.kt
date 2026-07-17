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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.abdiyev.walletai.ui.components.TiltGlowProvider
import kz.abdiyev.walletai.ui.components.rememberDynamicTiltOffset
import kz.abdiyev.walletai.ui.components.tiltGlow
import kz.abdiyev.walletai.ui.navigation.WalletAiRoutes
import kz.abdiyev.walletai.ui.screens.AdvisorScreen
import kz.abdiyev.walletai.ui.screens.AnalyticsScreen
import kz.abdiyev.walletai.ui.screens.DashboardScreen
import kz.abdiyev.walletai.ui.screens.HistoryScreen
import kz.abdiyev.walletai.ui.screens.ImportScreen
import kz.abdiyev.walletai.ui.screens.ProfileScreen
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.ui.viewmodel.DashboardEvent
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.HistoryViewModel
import kz.abdiyev.walletai.ui.viewmodel.ImportViewModel
import kz.abdiyev.walletai.ui.viewmodel.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import kz.abdiyev.walletai.R

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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WalletAiRoutes.DASHBOARD

    val profileViewModel: ProfileViewModel = koinViewModel()
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val analyticsViewModel: AnalyticsViewModel = koinViewModel()
    val advisorViewModel: AdvisorViewModel = koinViewModel()
    val importViewModel: ImportViewModel = koinViewModel()
    val historyViewModel: HistoryViewModel = koinViewModel()

    val settings by profileViewModel.settings.collectAsStateWithLifecycle()
    val profileSaveMessage by profileViewModel.saveMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val deletedMessage = stringResource(R.string.snackbar_transaction_deleted)
    val undoLabel = stringResource(R.string.snackbar_undo)
    val saveFailedMessage = stringResource(R.string.snackbar_save_failed)
    val voiceAmountMissingMessage = stringResource(R.string.snackbar_voice_amount_missing)
    val editLabel = stringResource(R.string.snackbar_edit)
    val savedMessage = stringResource(R.string.snackbar_saved)

    val showBottomBar = currentRoute in WalletAiRoutes.mainTabs

    LaunchedEffect(Unit) {
        dashboardViewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dashboardViewModel.undoDelete(event.transaction)
                    }
                }
                DashboardEvent.SaveError -> snackbarHostState.showSnackbar(saveFailedMessage)
                DashboardEvent.VoiceAmountMissing -> snackbarHostState.showSnackbar(
                    message = voiceAmountMissingMessage,
                    actionLabel = editLabel
                )
                is DashboardEvent.Info -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(profileSaveMessage) {
        if (profileSaveMessage == "saved") {
            snackbarHostState.showSnackbar(savedMessage)
            profileViewModel.clearSaveMessage()
        }
    }

    WalletAiTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
        val tiltOffset = rememberDynamicTiltOffset(settings.dynamicEffects)
        TiltGlowProvider(tiltOffset) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        modifier = Modifier.tiltGlow(intensity = 0.15f, maxShiftDp = 4.dp),
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = currentRoute == WalletAiRoutes.DASHBOARD,
                            onClick = {
                                navController.navigate(WalletAiRoutes.DASHBOARD) {
                                    popUpTo(WalletAiRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(Icons.Default.Dashboard, contentDescription = stringResource(R.string.nav_dashboard), modifier = Modifier.size(22.dp))
                            },
                            label = { Text(stringResource(R.string.nav_dashboard)) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == WalletAiRoutes.ANALYTICS,
                            onClick = {
                                navController.navigate(WalletAiRoutes.ANALYTICS) {
                                    popUpTo(WalletAiRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(Icons.Default.Analytics, contentDescription = stringResource(R.string.nav_analytics), modifier = Modifier.size(22.dp))
                            },
                            label = { Text(stringResource(R.string.nav_analytics)) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == WalletAiRoutes.ADVISOR,
                            onClick = {
                                navController.navigate(WalletAiRoutes.ADVISOR) {
                                    popUpTo(WalletAiRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.nav_advisor), modifier = Modifier.size(22.dp))
                            },
                            label = { Text(stringResource(R.string.nav_advisor)) },
                            colors = itemColors
                        )
                        NavigationBarItem(
                            selected = currentRoute == WalletAiRoutes.PROFILE,
                            onClick = {
                                navController.navigate(WalletAiRoutes.PROFILE) {
                                    popUpTo(WalletAiRoutes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile), modifier = Modifier.size(22.dp))
                            },
                            label = { Text(stringResource(R.string.nav_profile)) },
                            colors = itemColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = WalletAiRoutes.DASHBOARD,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(WalletAiRoutes.DASHBOARD) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            userName = settings.userName,
                            onLoadDemo = profileViewModel::resetToDemoData,
                            onNavigateToHistory = { navController.navigate(WalletAiRoutes.HISTORY) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable(WalletAiRoutes.ANALYTICS) {
                        AnalyticsScreen(viewModel = analyticsViewModel, modifier = Modifier.fillMaxSize())
                    }
                    composable(WalletAiRoutes.ADVISOR) {
                        AdvisorScreen(viewModel = advisorViewModel, modifier = Modifier.fillMaxSize())
                    }
                    composable(WalletAiRoutes.PROFILE) {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onNavigateToHistory = { navController.navigate(WalletAiRoutes.HISTORY) },
                            onNavigateToImport = { navController.navigate(WalletAiRoutes.IMPORT) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable(WalletAiRoutes.HISTORY) {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable(WalletAiRoutes.IMPORT) {
                        ImportScreen(
                            viewModel = importViewModel,
                            onBack = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        }
    }
}
