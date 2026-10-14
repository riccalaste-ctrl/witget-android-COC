package com.zacatac.clashboard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zacatac.clashboard.ui.boosts.BoostsScreen
import com.zacatac.clashboard.ui.dashboard.DashboardScreen
import com.zacatac.clashboard.ui.importdata.ImportScreen
import com.zacatac.clashboard.ui.notifications.NotificationsScreen
import com.zacatac.clashboard.ui.profiles.ProfileDetailScreen
import com.zacatac.clashboard.ui.profiles.ProfilesScreen
import com.zacatac.clashboard.ui.settings.SettingsScreen
import com.zacatac.clashboard.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                onProfileClick = { profileId ->
                                    navController.navigate("profile/$profileId")
                                },
                                onImportClick = {
                                    navController.navigate("import")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("profiles") {
                            ProfilesScreen(
                                onProfileClick = { profileId ->
                                    navController.navigate("profile/$profileId")
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("import") {
                            ImportScreen(
                                onImportComplete = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToImport = { navController.navigate("import") },
                                onNavigateToNotifications = { navController.navigate("notifications") },
                                onNavigateToProfiles = { navController.navigate("profiles") },
                                onNavigateToBoosts = { navController.navigate("boosts") }
                            )
                        }
                        composable("boosts") {
                            BoostsScreen(onBackClick = { navController.popBackStack() })
                        }
                        composable("notifications") {
                            NotificationsScreen(onBackClick = { navController.popBackStack() })
                        }
                        composable(
                            route = "profile/{profileId}",
                            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val profileId = backStackEntry.arguments?.getString("profileId").orEmpty()
                            ProfileDetailScreen(
                                profileId = profileId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
