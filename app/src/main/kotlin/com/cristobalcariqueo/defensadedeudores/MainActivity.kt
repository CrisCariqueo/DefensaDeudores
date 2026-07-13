package com.cristobalcariqueo.defensadedeudores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cristobalcariqueo.defensadedeudores.ui.nav.DefensaNavGraph
import com.cristobalcariqueo.defensadedeudores.ui.theme.DefensaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DefensaTheme {
                DefensaNavGraph()
            }
        }
    }
}
