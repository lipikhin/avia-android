package com.aviatechnik.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aviatechnik.android.ui.screens.home.HomeScreen
import com.aviatechnik.android.ui.screens.login.LoginScreen
import com.aviatechnik.android.ui.screens.splash.SplashScreen
import com.aviatechnik.android.ui.theme.AviaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AviaTheme {
                Surface(Modifier.fillMaxSize()) { AviaNavHost() }
            }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun AviaNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onReady = { hasSession ->
                    nav.navigate(if (hasSession) Routes.HOME else Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onOpenWorkorder = { id -> nav.navigate("wo/$id") },
            )
        }
        composable(
            route = "wo/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: 0
            com.aviatechnik.android.ui.screens.workorders.WorkorderDetailScreen(
                onBack = { nav.popBackStack() },
                onOpenTasks = { nav.navigate("wo/$id/tasks") },
                onOpenProcesses = { nav.navigate("wo/$id/processes") },
                onOpenComponents = { nav.navigate("wo/$id/components") },
            )
        }
        composable(
            route = "wo/{id}/components",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            com.aviatechnik.android.ui.screens.components.ComponentsScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = "wo/{id}/tasks",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            com.aviatechnik.android.ui.screens.workorders.TasksScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = "wo/{id}/processes",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            com.aviatechnik.android.ui.screens.workorders.ProcessesScreen(onBack = { nav.popBackStack() })
        }
    }
}
