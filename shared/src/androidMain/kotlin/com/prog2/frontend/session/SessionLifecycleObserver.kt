package com.prog2.frontend.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Controla vigencia al entrar en foreground, sin temporizador ni efectos remotos. */
class SessionLifecycleObserver(
    private val sessions: SessionManager,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        scope.launch { sessions.checkValidity() }
    }
}
