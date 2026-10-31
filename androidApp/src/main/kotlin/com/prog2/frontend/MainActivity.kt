package com.prog2.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prog2.frontend.ui.setup.SetupScreen

/** Punto de entrada Android y composición de los componentes de presentación. */
class MainActivity : ComponentActivity() {
    private val app = App(SetupScreen())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            app.Content()
        }
    }
}

