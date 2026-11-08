package com.prog2.frontend

import android.app.Application
import com.prog2.frontend.config.AppContainer
import com.prog2.frontend.network.ApiConfiguration

/** Crea el cableado una vez por proceso; no conserva una referencia a la Activity. */
class FrontendApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            applicationContext,
            ApiConfiguration(BuildConfig.TURNOS_BASE_URL, BuildConfig.CATALOG_BASE_URL, BuildConfig.DEBUG),
        )
    }
}
