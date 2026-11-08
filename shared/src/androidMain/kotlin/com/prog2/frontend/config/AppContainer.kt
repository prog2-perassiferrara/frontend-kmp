package com.prog2.frontend.config

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.prog2.frontend.network.ApiConfiguration
import com.prog2.frontend.network.ApiHttpClient
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.registration.data.KtorRegistrationApi
import com.prog2.frontend.session.JwtExpiry
import com.prog2.frontend.session.SessionLifecycleObserver
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.data.AndroidEncryptedSessionStore
import com.prog2.frontend.session.data.KtorAuthApi
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Cableado Android por proceso; los consumidores reciben sus colaboradores por constructor. */
class AppContainer(context: Context, configuration: ApiConfiguration) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = ApiHttpClient().create(
        OkHttp.create {
            config {
                retryOnConnectionFailure(false)
                followRedirects(false)
                followSslRedirects(false)
            }
        },
    )
    val sessions = SessionManager(
        KtorAuthApi(client, configuration),
        AndroidEncryptedSessionStore(context.applicationContext),
        JwtExpiry(),
        Clock.System,
    )
    val authenticatedApi = AuthenticatedApiClient(client, configuration, sessions)
    val registrationApi = KtorRegistrationApi(client, configuration)

    init {
        scope.launch { sessions.restore() }
        ProcessLifecycleOwner.get().lifecycle.addObserver(SessionLifecycleObserver(sessions, scope))
    }
}
