package com.prog2.frontend.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prog2.frontend.reservation.data.ReservationSelection
import com.prog2.frontend.ui.availability.AvailabilityScreen
import com.prog2.frontend.ui.availability.AvailabilityViewModel
import com.prog2.frontend.ui.components.AppBottomBar
import com.prog2.frontend.ui.reservation.BannerAction
import com.prog2.frontend.ui.reservation.MyReservationsScreen
import com.prog2.frontend.ui.reservation.MyReservationsViewModel
import com.prog2.frontend.ui.reservation.ProcessScreen
import com.prog2.frontend.ui.reservation.ReservationBanner
import com.prog2.frontend.ui.reservation.ReservationViewModel
import com.prog2.frontend.ui.reservation.StartStatus
import com.prog2.frontend.ui.search.SearchScreen
import com.prog2.frontend.ui.search.SearchViewModel

/**
 * Contenedor del área autenticada: único punto que conoce el `NavController`.
 *
 * `Scaffold` reserva el espacio de la BottomBar, que solo se muestra en las
 * pestañas [AppDestination]. Disponibilidad y proceso son destinos secundarios y
 * se abren mediante navegación explícita. Las pantallas se crean una vez con
 * `remember` y los ViewModels recibidos conservan sus datos entre destinos.
 */
class MainScreen {
    @Composable
    fun Content(
        searchViewModel: SearchViewModel,
        availabilityViewModel: AvailabilityViewModel,
        reservationViewModel: ReservationViewModel,
        myReservationsViewModel: MyReservationsViewModel,
        onSignOut: () -> Unit,
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val destinations = AppDestination.entries
        val bottomBar = remember { AppBottomBar() }
        val searchScreen = remember { SearchScreen() }
        val availabilityScreen = remember { AvailabilityScreen() }
        val processScreen = remember { ProcessScreen() }
        val myReservationsScreen = remember { MyReservationsScreen() }
        val reservationBanner = remember { ReservationBanner() }
        var openedProcessId by rememberSaveable { mutableStateOf<String?>(null) }
        val processId = reservationViewModel.state.value.process?.id

        // Abre una sola vez cada proceso informado por Turnos, sea por inicio o
        // por el pendiente descubierto. La guarda evita reabrirlo al rotar o al
        // volver de la pantalla de proceso.
        LaunchedEffect(processId) {
            if (processId != null && processId != openedProcessId) {
                openedProcessId = processId
                navController.navigate(PROCESS_ROUTE) { launchSingleTop = true }
            }
        }

        Scaffold(
            modifier = Modifier.safeDrawingPadding().imePadding(),
            bottomBar = {
                if (destinations.any { it.route == currentRoute }) {
                    bottomBar.Content(
                        destinations = destinations,
                        currentRoute = currentRoute,
                        onSelect = { destination -> navigateToTab(navController, destination) },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Search.route,
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxSize(),
                ) {
                    composable(AppDestination.Search.route) {
                        val search = searchViewModel.state.value
                        val reservation = reservationViewModel.state.value
                        ScreenLayout {
                            reservation.banner?.let { banner ->
                                reservationBanner.Content(
                                    banner.message,
                                    banner.actionLabel,
                                ) {
                                    when (banner.action) {
                                        BannerAction.RETRY_DISCOVERY ->
                                            reservationViewModel.retryDiscovery()
                                        BannerAction.SHOW_PROCESS -> {
                                            if (reservation.process != null) {
                                                reservationViewModel.refresh()
                                            } else {
                                                reservationViewModel.verify()
                                            }
                                            navController.navigate(PROCESS_ROUTE) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            }
                            searchScreen.Content(
                                search,
                                searchViewModel::changeName,
                                searchViewModel::changeCategory,
                                searchViewModel::changeEnabled,
                                searchViewModel::changeAvailableOn,
                                searchViewModel::search,
                                searchViewModel::retry,
                                searchViewModel::previousPage,
                                searchViewModel::nextPage,
                                onSelect = { professional ->
                                    searchViewModel.select(professional)
                                    availabilityViewModel.open(
                                        professional,
                                        searchViewModel.state.value.availableOn,
                                    )
                                    navController.navigate(AVAILABILITY_ROUTE) {
                                        launchSingleTop = true
                                    }
                                },
                                searchViewModel::ensureLoaded,
                            )
                        }
                    }
                    composable(AppDestination.Reservations.route) {
                        val myReservations = myReservationsViewModel.state.value
                        // Cada entrada a la pestaña relee la primera página; el
                        // `LaunchedEffect` depende del destino, no de recomposiciones.
                        LaunchedEffect(Unit) {
                            myReservationsViewModel.load()
                        }
                        ScreenLayout {
                            myReservationsScreen.Content(
                                myReservations,
                                onRefresh = myReservationsViewModel::refresh,
                                onPrevious = myReservationsViewModel::previous,
                                onNext = myReservationsViewModel::next,
                                onOpenProcess = { process ->
                                    reservationViewModel.present(process)
                                    openedProcessId = process.id
                                    navController.navigate(PROCESS_ROUTE) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenCancellation = myReservationsViewModel::openCancellation,
                                onDismissCancellation = myReservationsViewModel::dismissCancellation,
                                onConfirmCancellation = myReservationsViewModel::confirmCancellation,
                                onSignOut = onSignOut,
                            )
                        }
                    }
                    composable(AVAILABILITY_ROUTE) {
                        val availability = availabilityViewModel.state.value
                        val reservation = reservationViewModel.state.value
                        ScreenLayout {
                            availabilityScreen.Content(
                                availability,
                                availabilityViewModel::changeDate,
                                availabilityViewModel::consult,
                                availabilityViewModel::selectTime,
                                availabilityViewModel::retry,
                                onReserve = {
                                    val current = availabilityViewModel.state.value
                                    val professionalId = current.professionalId
                                    val time = current.selectedTime
                                    if (professionalId != null && time != null) {
                                        reservationViewModel.start(
                                            ReservationSelection(
                                                professionalId = professionalId,
                                                date = current.date.trim(),
                                                startTime = time,
                                                professionalName = current.professionalName,
                                            ),
                                        )
                                        navController.navigate(PROCESS_ROUTE) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                reservationBlocked = when {
                                    reservation.process?.isFinal == false ||
                                        reservation.start == StartStatus.SENDING ||
                                        reservation.start == StartStatus.UNCERTAIN ->
                                        "Tenés una solicitud de turno pendiente. " +
                                            "Consultá su estado antes de iniciar otra."
                                    else -> null
                                },
                                onBack = {
                                    availabilityViewModel.reset()
                                    searchViewModel.backToSearch()
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                    composable(PROCESS_ROUTE) {
                        val reservation = reservationViewModel.state.value
                        val backToSearch = {
                            reservationViewModel.dismiss()
                            if (navController.previousBackStackEntry?.destination?.route ==
                                AppDestination.Reservations.route
                            ) {
                                navController.popBackStack()
                            } else {
                                availabilityViewModel.reset()
                                searchViewModel.backToSearch()
                                navController.popBackStack(AppDestination.Search.route, inclusive = false)
                            }
                            Unit
                        }
                        val backToAvailability = {
                            reservationViewModel.dismiss()
                            availabilityViewModel.consult()
                            navController.popBackStack(AVAILABILITY_ROUTE, inclusive = false)
                            Unit
                        }
                        BackHandler {
                            if (reservation.process == null &&
                                (reservation.start == StartStatus.REJECTED ||
                                    reservation.start == StartStatus.NOT_REGISTERED)
                            ) {
                                backToAvailability()
                            } else {
                                backToSearch()
                            }
                        }
                        ScreenLayout {
                            processScreen.Content(
                                reservation,
                                reservationViewModel::refresh,
                                reservationViewModel::verify,
                                reservationViewModel::retryStart,
                                reservationViewModel::changePhone,
                                reservationViewModel::submitPhone,
                                reservationViewModel::verifyPhone,
                                onBackToSearch = backToSearch,
                                onBackToAvailability = backToAvailability,
                            )
                        }
                    }
                }
            }
        }
    }

    /** Contenedor desplazable que conserva el ancho, el espaciado y los márgenes actuales. */
    @Composable
    private fun ScreenLayout(content: @Composable ColumnScope.() -> Unit) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }

    /**
     * Navega a una pestaña con el comportamiento estándar de una BottomBar:
     * una sola entrada por destino, sin duplicados y con su estado guardado.
     */
    private fun navigateToTab(
        navController: NavHostController,
        destination: AppDestination,
    ) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    private companion object {
        const val AVAILABILITY_ROUTE = "availability"
        const val PROCESS_ROUTE = "process"
    }
}
