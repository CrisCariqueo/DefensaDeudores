package com.cristobalcariqueo.defensadedeudores.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.ui.screens.config.ConfigScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.main.MainScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.people.PeopleScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.sources.SourcesScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.starting.StartingScreen
import com.cristobalcariqueo.defensadedeudores.ui.screens.track.TrackScreen
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun DefensaNavGraph(navController: NavHostController = rememberNavController()) {
    // Starting screen only for accounts that haven't onboarded (>= 1 person and
    // >= 1 source created). Hold rendering until the flag is known so the
    // wrong start destination never flashes.
    val settingsRepository = koinInject<SettingsRepository>()
    val onboarded by remember(settingsRepository) {
        settingsRepository.observeSettings().map { it.onboarded }
    }.collectAsState(initial = null)

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> DefensaNavHost(
            navController = navController,
            startDestination = if (onboarded == true) Destination.Main.route else Destination.Starting.route,
        )
    }
}

@Composable
private fun DefensaNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
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
        composable(Destination.People.route) {
            PeopleScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.Sources.route) {
            SourcesScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.Config.route) { ConfigScreen() }
    }
}

private fun NavHostController.navigateToMainClearingBackStack() {
    navigate(Destination.Main.route) {
        popUpTo(Destination.Starting.route) { inclusive = true }
    }
}
