package com.cristobalcariqueo.defensadedeudores.ui.nav

sealed class Destination(val route: String) {
    data object Starting : Destination("starting")
    data object Main : Destination("main")
    data object People : Destination("people")
    data object Sources : Destination("sources")
    data object Config : Destination("config")

    data object Track : Destination("track/{trackId}") {
        const val ARG_TRACK_ID = "trackId"
        fun route(trackId: String) = "track/$trackId"
    }
}
