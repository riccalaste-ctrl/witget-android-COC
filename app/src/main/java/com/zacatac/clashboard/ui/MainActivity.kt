package com.zacatac.clashboard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme {
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
                        ProfilesScreen(onProfileClick = { profileId ->
                            navController.navigate("profile/$profileId")
                        })
                    }
                    composable("import") {
                        ImportScreen(onImportComplete = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        })
                    }
                    composable("settings") {
                        SettingsScreen()
                    }
                    composable("boosts") {
                        BoostsScreen()
                    }
                    composable("notifications") {
                        NotificationsScreen()
                    }
                    composable(
                        route = "profile/{profileId}",
                        arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val profileId = backStackEntry.arguments?.getString("profileId").orEmpty()
                        ProfileDetailScreen(profileId = profileId)
                    }
                }
            }
        }
    }
}