package com.cristobalcariqueo.defensadedeudores

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.ui.nav.DefensaNavGraph
import com.cristobalcariqueo.defensadedeudores.ui.theme.DefensaTheme
import com.cristobalcariqueo.defensadedeudores.ui.theme.fontFamilyFor
import org.koin.android.ext.android.inject

/**
 * AppCompatActivity (not ComponentActivity) so per-app locales work below
 * API 33 -- the Config screen's language switch relies on AppCompatDelegate.
 */
class MainActivity : AppCompatActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observeSettings().collectAsState(initial = null)

            LaunchedEffect(settings?.language) {
                settings?.language?.let { tag ->
                    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != tag) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                }
            }

            DefensaTheme(
                darkTheme = settings?.darkTheme ?: isSystemInDarkTheme(),
                fontFamily = fontFamilyFor(settings?.font ?: "default"),
            ) {
                DefensaNavGraph()
            }
        }
    }
}
