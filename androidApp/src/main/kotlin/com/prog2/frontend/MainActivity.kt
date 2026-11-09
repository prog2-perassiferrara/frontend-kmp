package com.prog2.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prog2.frontend.ui.registration.RegistrationScreen
import com.prog2.frontend.ui.session.LoginScreen

/** Punto de entrada Android y composición de los componentes de presentación. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // El manifest fija esta Application; es una frontera Android, no selección de negocio.
        val container = (application as FrontendApplication).container
        val app = App(
            container.sessions,
            container.registrationApi,
            container.catalogApi,
            container.availabilityApi,
            container.reservationsApi,
            LoginScreen(),
            RegistrationScreen(),
        )

        setContent {
            app.Content()
        }
    }
}
