package com.shp.evernote.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shp.evernote.ui.home.HomeRoute

/** Navigation graph. Screens are added here as they land. */
object Routes {
    const val HOME = "home"
}

@Composable
fun EvernoteApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute()
        }
    }
}
