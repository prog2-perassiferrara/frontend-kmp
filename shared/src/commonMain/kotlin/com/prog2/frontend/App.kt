package com.prog2.frontend

import androidx.compose.material.MaterialTheme
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog2.frontend.availability.data.AvailabilityApi
import com.prog2.frontend.catalog.data.CatalogApi
import com.prog2.frontend.registration.data.RegistrationApi
import com.prog2.frontend.reservation.data.ReservationSelection
import com.prog2.frontend.reservation.data.ReservationsApi
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.ui.availability.AvailabilityScreen
import com.prog2.frontend.ui.availability.AvailabilityViewModel
import com.prog2.frontend.ui.registration.RegistrationScreen
import com.prog2.frontend.ui.registration.RegistrationViewModel
import com.prog2.frontend.ui.reservation.BannerAction
import com.prog2.frontend.ui.reservation.ProcessScreen
import com.prog2.frontend.ui.reservation.ReservationBanner
import com.prog2.frontend.ui.reservation.ReservationViewModel
import com.prog2.frontend.ui.reservation.StartStatus
import com.prog2.frontend.ui.search.SearchScreen
import com.prog2.frontend.ui.search.SearchViewModel
import com.prog2.frontend.ui.session.LoginScreen
import com.prog2.frontend.ui.session.SessionViewModel

/**
 * Conecta el estado de presentación con la pantalla, dentro del tema de la app.
 *
 * @param sessions Coordinador compartido durante la vida del proceso.
 * @param registrationApi Alta pública usada por el ViewModel de registro.
 * @param catalogApi Búsqueda protegida de Catálogo usada por el ViewModel de búsqueda.
 * @param availabilityApi Consulta protegida de disponibilidad usada por su ViewModel.
 * @param reservationsApi Contratos protegidos de inicio y consulta de reservas.
 */
class App(
    private val sessions: SessionManager,
    private val registrationApi: RegistrationApi,
    private val catalogApi: CatalogApi,
    private val availabilityApi: AvailabilityApi,
    private val reservationsApi: ReservationsApi,
    private val loginScreen: LoginScreen,
    private val registrationScreen: RegistrationScreen,
    private val searchScreen: SearchScreen,
    private val availabilityScreen: AvailabilityScreen,
    private val processScreen: ProcessScreen,
    private val reservationBanner: ReservationBanner,
) {
    /** Obtiene los ViewModels del owner Android, conservándolos durante la rotación. */
    @Composable
    fun Content() {
        val sessionViewModel = viewModel { SessionViewModel(sessions) }
        val registrationViewModel = viewModel { RegistrationViewModel(registrationApi, sessions) }
        val searchViewModel = viewModel { SearchViewModel(catalogApi) }
        val availabilityViewModel = viewModel { AvailabilityViewModel(availabilityApi) }
        val reservationViewModel = viewModel { ReservationViewModel(reservationsApi) }
        val state by sessionViewModel.state
        val reservation = reservationViewModel.state.value
        var showRegistration by rememberSaveable { mutableStateOf(false) }
        var processVisible by rememberSaveable { mutableStateOf(false) }
        var openedProcessId by rememberSaveable { mutableStateOf<String?>(null) }

        // Al autenticarse se consulta una vez el pendiente propio sin bloquear la
        // búsqueda. Al cerrar sesión se descartan filtros, disponibilidad y proceso.
        LaunchedEffect(state.session.status) {
            when (state.session.status) {
                SessionStatus.SIGNED_IN -> {
                    showRegistration = false
                    reservationViewModel.discover()
                }
                SessionStatus.SIGNED_OUT -> {
                    processVisible = false
                    openedProcessId = null
                    availabilityViewModel.reset()
                    searchViewModel.reset()
                    reservationViewModel.reset()
                }
                SessionStatus.RESTORING -> Unit
            }
        }

        // Abre el proceso cuando Turnos informa uno nuevo, sea por inicio o por el
        // pendiente descubierto. El identificador guardado evita reabrirlo al rotar
        // o después de que la persona lo dejó.
        LaunchedEffect(reservation.process?.id) {
            val processId = reservation.process?.id
            if (processId != null && processId != openedProcessId) {
                openedProcessId = processId
                processVisible = true
            }
        }

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (state.session.status) {
                            SessionStatus.RESTORING -> {
                                CircularProgressIndicator()
                                Text("Restaurando sesión…")
                            }
                            SessionStatus.SIGNED_IN -> {
                                if (processVisible) {
                                    processScreen.Content(
                                        reservation,
                                        reservationViewModel::refresh,
                                        reservationViewModel::verify,
                                        reservationViewModel::retryStart,
                                        reservationViewModel::changePhone,
                                        reservationViewModel::submitPhone,
                                        reservationViewModel::verifyPhone,
                                        onBackToSearch = {
                                            reservationViewModel.dismiss()
                                            processVisible = false
                                            availabilityViewModel.reset()
                                            searchViewModel.backToSearch()
                                        },
                                        onBackToAvailability = {
                                            reservationViewModel.dismiss()
                                            processVisible = false
                                            availabilityViewModel.consult()
                                        },
                                    )
                                } else {
                                    val selected = searchViewModel.state.value.selected
                                    if (selected == null) {
                                        reservation.banner?.let { banner ->
                                            reservationBanner.Content(
                                                banner.message,
                                                banner.actionLabel,
                                            ) {
                                                when (banner.action) {
                                                    BannerAction.RETRY_DISCOVERY ->
                                                        reservationViewModel.retryDiscovery()
                                                    BannerAction.SHOW_PROCESS -> {
                                                        processVisible = true
                                                        if (reservation.process != null) {
                                                            reservationViewModel.refresh()
                                                        } else {
                                                            reservationViewModel.verify()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        searchScreen.Content(
                                            searchViewModel.state.value,
                                            searchViewModel::changeName,
                                            searchViewModel::changeCategory,
                                            searchViewModel::changeEnabled,
                                            searchViewModel::changeAvailableOn,
                                            searchViewModel::search,
                                            searchViewModel::retry,
                                            searchViewModel::previousPage,
                                            searchViewModel::nextPage,
                                            searchViewModel::select,
                                            sessionViewModel::signOut,
                                            searchViewModel::ensureLoaded,
                                        )
                                    } else {
                                        LaunchedEffect(selected.id) {
                                            availabilityViewModel.open(
                                                selected,
                                                searchViewModel.state.value.availableOn,
                                            )
                                        }
                                        availabilityScreen.Content(
                                            availabilityViewModel.state.value,
                                            availabilityViewModel::changeDate,
                                            availabilityViewModel::consult,
                                            availabilityViewModel::selectTime,
                                            availabilityViewModel::retry,
                                            onReserve = {
                                                val current = availabilityViewModel.state.value
                                                val professionalId = current.professionalId
                                                val time = current.selectedTime
                                                if (professionalId != null && time != null) {
                                                    processVisible = true
                                                    reservationViewModel.start(
                                                        ReservationSelection(
                                                            professionalId = professionalId,
                                                            date = current.date.trim(),
                                                            startTime = time,
                                                            professionalName = current.professionalName,
                                                        ),
                                                    )
                                                }
                                            },
                                            reservationBlocked = when {
                                                reservation.process?.isFinal == false ||
                                                    reservation.start == StartStatus.SENDING ||
                                                    reservation.start == StartStatus.UNCERTAIN ->
                                                    "Tenés un proceso de reserva en curso o sin verificar. " +
                                                        "Volvé a la búsqueda para consultar su estado antes de iniciar otro."
                                                else -> null
                                            },
                                            onBack = {
                                                availabilityViewModel.reset()
                                                searchViewModel.backToSearch()
                                            },
                                        )
                                    }
                                }
                            }
                            SessionStatus.SIGNED_OUT -> if (showRegistration) {
                                registrationScreen.Content(
                                    registrationViewModel.state.value,
                                    registrationViewModel::changeLogin,
                                    registrationViewModel::changePassword,
                                    registrationViewModel::changeFirstName,
                                    registrationViewModel::changeLastName,
                                    registrationViewModel::changeEmail,
                                    registrationViewModel::submit,
                                    onBackToLogin = {
                                        registrationViewModel.reset()
                                        showRegistration = false
                                    },
                                )
                            } else {
                                loginScreen.Content(
                                    state, sessionViewModel::changeUsername, sessionViewModel::changePassword,
                                    sessionViewModel::changeRememberMe, sessionViewModel::signIn,
                                    onRegister = {
                                        registrationViewModel.reset()
                                        showRegistration = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
