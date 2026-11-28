package com.prog2.frontend.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.prog2.frontend.ui.components.availabilityDays
import com.prog2.frontend.ui.reservation.MyReservationsScreen
import com.prog2.frontend.ui.reservation.MyReservationsViewModel
import com.prog2.frontend.ui.reservation.ProcessScreen
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
        Scaffold(
            modifier = Modifier.safeDrawingPadding().imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            if (reservation.start == StartStatus.UNCERTAIN) {
                                TextButton(onClick = {
                                    reservationViewModel.refresh()
                                    navController.navigate(PROCESS_ROUTE) { launchSingleTop = true }
                                }) { Text("Verificar solicitud pendiente") }
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
                                    availabilityViewModel.open(
                                        professional,
                                        availabilityDays(searchViewModel.state.value.availableOn).first().date,
                                    )
                                    availabilityViewModel.consult()
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
                                    navController.navigate(PROCESS_ROUTE) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenCancellation = myReservationsViewModel::openCancellation,
                                onDismissCancellation = myReservationsViewModel::dismissCancellation,
                                onConfirmCancellation = myReservationsViewModel::confirmCancellation,
                                onVerifyCancellation = myReservationsViewModel::verifyCancellation,
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
                                availabilityViewModel::selectDate,
                                availabilityViewModel::selectTime,
                                availabilityViewModel::retry,
                                onReserve = {
                                    val current = availabilityViewModel.state.value
                                    val professionalId = current.professionalId
                                    val time = current.selectedTime
                                    if (professionalId != null && time != null) {
                                        val started = reservationViewModel.start(
                                            ReservationSelection(
                                                professionalId = professionalId,
                                                date = current.date.trim(),
                                                startTime = time,
                                                professionalName = current.professionalName,
                                            ),
                                        )
                                        if (started) navController.navigate(PROCESS_ROUTE) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                reservationBlocked = when {
                                    !reservation.canStart ->
                                        "Tenés una solicitud de turno pendiente. " +
                                            "Consultá su estado en Cuenta antes de iniciar otra."
                                    else -> null
                                },
                                onBack = {
                                    availabilityViewModel.reset()
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                    composable(PROCESS_ROUTE) {
                        val reservation = reservationViewModel.state.value
                        val origin = navController.previousBackStackEntry?.destination?.route
                        val refreshAvailability = origin == AVAILABILITY_ROUTE && reservation.process == null &&
                            (reservation.start == StartStatus.REJECTED || reservation.start == StartStatus.NOT_REGISTERED)
                        val goBack: () -> Unit = {
                            reservationViewModel.dismiss()
                            when {
                                refreshAvailability -> {
                                    navController.popBackStack()
                                    availabilityViewModel.consult()
                                }
                                origin == AppDestination.Reservations.route -> navController.popBackStack()
                                else -> {
                                    availabilityViewModel.reset()
                                    navController.popBackStack(AppDestination.Search.route, inclusive = false)
                                }
                            }
                        }
                        BackHandler(onBack = goBack)
                        ScreenLayout {
                            processScreen.Content(
                                reservation,
                                reservationViewModel::refresh,
                                reservationViewModel::retryStart,
                                reservationViewModel::changePhone,
                                reservationViewModel::submitPhone,
                                onBack = goBack,
                                backLabel = if (refreshAvailability) "Volver a disponibilidad" else "Volver",
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
