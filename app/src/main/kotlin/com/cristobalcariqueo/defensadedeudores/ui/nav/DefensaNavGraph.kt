package com.cristobalcariqueo.defensadedeudores.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cristobalcariqueo.defensadedeudores.ui.screens.config.ConfigScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.main.MainScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.people.PeopleScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.sources.SourcesScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.starting.StartingScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.track.TrackScreen

@Composable
fun DefensaNavGraph(navController: NavHostController = rememberNavController()) {
    // TODO(#5): start destination should branch on settings.onboarded once the
    // settings repository exists -- Starting screen only for fresh accounts.
    NavHost(navController = navController, startDestination = Destination.Starting.route) {
        composable(Destination.Starting.route) {
            StartingScreen(onDone = { navController.navigateToMainClearingBackStack() })
        }
        composable(Destination.Main.route) {
            MainScreen(
                onOpenTrack = { trackId -> navController.navigate(Destination.Track.route(trackId)) },
                onOpenPeople = { navController.navigate(Destination.People.route) },
                onOpenSources = { navController.navigate(Destination.Sources.route) },
                onOpenConfig = { navController.navigate(Destination.Config.route) },
            )
        }
        composable(Destination.Track.route) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString(Destination.Track.ARG_TRACK_ID).orEmpty()
            TrackScreen(trackId = trackId)
        }
        composable(Destination.People.route) { PeopleScreen() }
        composable(Destination.Sources.route) { SourcesScreen() }
        composable(Destination.Config.route) { ConfigScreen() }
    }
}

private fun NavHostController.navigateToMainClearingBackStack() {
    navigate(Destination.Main.route) {
        popUpTo(Destination.Starting.route) { inclusive = true }
    }
}
