package com.etherion.network.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.etherion.network.auth.AuthManager
import com.etherion.network.miner.MiningPersistence
import com.etherion.network.ui.ads.AdBanner
import com.etherion.network.ui.auth.AuthScreen
import com.etherion.network.ui.home.GuiHomeScreen
import com.etherion.network.ui.leaderboard.LeaderboardScreen
import com.etherion.network.ui.legal.LegalConsensusScreen
import com.etherion.network.ui.settings.SettingsScreen
import com.etherion.network.ui.splash.SplashScreen
import com.etherion.network.ui.store.StoreScreen
import com.etherion.network.ui.wallet.WalletScreen
import com.etherion.network.ui.profile.ProfileScreen
import com.example.etherion3.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String) {
    object Splash : Screen("splash", "Splash")
    object Legal : Screen("legal", "Terms")
    object Auth : Screen("auth", "Login")
    object Home : Screen("home", "Home")
    object Store : Screen("store", "Store")
    object Wallet : Screen("wallet", "Wallet")
    object Leaderboard : Screen("leaderboard", "Ranking")
    object Settings : Screen("settings", "Settings")
    object Profile : Screen("profile", "Profile")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager() }
    val persistence = remember { MiningPersistence(context) }
    val currentUser by authManager.userFlow.collectAsState(initial = authManager.currentUser)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != Screen.Auth.route && currentRoute != Screen.Splash.route && currentRoute != Screen.Legal.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                scope.launch {
                    val legalAccepted = persistence.isLegalAccepted.first()
                    val nextRoute = if (!legalAccepted) {
                        Screen.Legal.route
                    } else if (authManager.isUserSignedIn()) {
                        Screen.Home.route
                    } else {
                        Screen.Auth.route
                    }
                    navController.navigate(nextRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.Legal.route) {
            LegalConsensusScreen(onAccepted = {
                val nextRoute = if (authManager.isUserSignedIn()) Screen.Home.route else Screen.Auth.route
                navController.navigate(nextRoute) {
                    popUpTo(Screen.Legal.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Auth.route) {
            AuthScreen(onAuthSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) { MainLayout(navController) { GuiHomeScreen() } }
        composable(Screen.Store.route) { MainLayout(navController) { StoreScreen() } }
        composable(Screen.Wallet.route) { MainLayout(navController) { WalletScreen() } }
        composable(Screen.Leaderboard.route) { MainLayout(navController) { LeaderboardScreen() } }
        composable(Screen.Settings.route) { 
            MainLayout(navController) { 
                SettingsScreen(onViewProfile = { navController.navigate(Screen.Profile.route) }) 
            } 
        }
        composable(Screen.Profile.route) { 
            ProfileScreen(onBack = { navController.popBackStack() }) 
        }
    }
}

@Composable
fun MainLayout(navController: NavHostController, content: @Composable () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF050510),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFF050510))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.etr),
                    contentDescription = "Etherion Logo",
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ETHERION NETWORK",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A15))
                    .navigationBarsPadding()
            ) {
                AdBanner(modifier = Modifier.fillMaxWidth())
                
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val items = listOf(
                        Triple(Screen.Home, Icons.Default.Home, "Home"),
                        Triple(Screen.Store, Icons.Default.Store, "Store"),
                        Triple(Screen.Wallet, Icons.Default.AttachMoney, "Wallet"),
                        Triple(Screen.Leaderboard, Icons.Default.EmojiEvents, "Ranking"),
                        Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                    )

                    items.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            label = { Text(screen.label, fontSize = 10.sp) },
                            icon = { Icon(icon, contentDescription = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00FF00),
                                unselectedIconColor = Color.Gray,
                                selectedTextColor = Color(0xFF00FF00),
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color(0xFF00FF00).copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            content()
        }
    }
}
