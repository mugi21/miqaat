package com.mohamed.miqaat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.notifications.PrayerAlarmScheduler
import com.mohamed.miqaat.ui.calendar.CalendarScreen
import com.mohamed.miqaat.ui.home.HomeScreen
import com.mohamed.miqaat.ui.invocations.InvocationsScreen
import com.mohamed.miqaat.ui.qibla.QiblaScreen
import com.mohamed.miqaat.ui.settings.SettingsScreen
import com.mohamed.miqaat.ui.splash.SplashScreen
import com.mohamed.miqaat.ui.theme.MiqaatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Les écrans de premier niveau ; enum = Serializable, donc rememberSaveable le garde. */
private enum class Screen { HOME, SETTINGS, QIBLA, CALENDAR, INVOCATIONS }

class MainActivity : ComponentActivity() {

    /**
     * L'invocation à ouvrir quand on arrive depuis sa notification. `singleTop`
     * dans le manifeste : l'activité déjà à l'écran est réutilisée, et c'est
     * [onNewIntent] qui apporte la demande.
     */
    private val openInvocationId = mutableLongStateOf(NO_INVOCATION)

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            // Refus toléré : l'app reste utilisable (position par défaut, pas de notification).
            if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                refreshLocationThenReschedule()
            }
        }

    /** Applique la langue choisie dans les réglages avant que l'écran ne se construise. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openInvocationId.longValue = intent.requestedInvocationId()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openInvocationId.longValue = intent.requestedInvocationId()
        requestMissingPermissions()
        // Chaque ouverture resynchronise la chaîne d'alarmes (filet de sécurité),
        // puis rafraîchit la position si on y a droit.
        PrayerAlarmScheduler(this).scheduleNext()
        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            refreshLocationThenReschedule()
        }

        setContent {
            MiqaatTheme {
                // Écran de démarrage : superposé à l'accueil plutôt que joué à sa
                // place, pour que celui-ci se compose (et charge ses horaires)
                // pendant qu'il est affiché. `rememberSaveable` : un changement de
                // langue recrée l'activité, il ne doit pas rejouer le démarrage.
                var splashVisible by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(SPLASH_DURATION_MS)
                    splashVisible = false
                }

                // Sur le vert du démarrage, les icônes système doivent rester
                // claires quel que soit le mode ; ensuite seulement elles suivent
                // le thème, comme le fait enableEdgeToEdge().
                val darkTheme = isSystemInDarkTheme()
                LaunchedEffect(splashVisible, darkTheme) {
                    val light = !splashVisible && !darkTheme
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = light
                        isAppearanceLightNavigationBars = light
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    // Insets à zéro : l'écran gère lui-même les barres système
                    // (le héros peint son dégradé derrière la barre de statut).
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0),
                    ) { innerPadding ->
                        // Des écrans à plat depuis l'accueil : un état suffit,
                        // pas besoin de librairie de navigation pour l'instant.
                        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                        val requestedInvocation by openInvocationId

                        // Arrivée depuis la notification d'une invocation : on ouvre
                        // l'écran directement sur elle, prête à être lue.
                        LaunchedEffect(requestedInvocation) {
                            if (requestedInvocation != NO_INVOCATION) screen = Screen.INVOCATIONS
                        }

                        val backToHome = {
                            openInvocationId.longValue = NO_INVOCATION
                            screen = Screen.HOME
                        }
                        if (screen != Screen.HOME) {
                            BackHandler(onBack = backToHome)
                        }
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onOpenQibla = { screen = Screen.QIBLA },
                                onOpenCalendar = { screen = Screen.CALENDAR },
                                onOpenInvocations = { screen = Screen.INVOCATIONS },
                                modifier = Modifier.padding(innerPadding),
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = Modifier.padding(innerPadding),
                            )

                            Screen.QIBLA -> QiblaScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = Modifier.padding(innerPadding),
                            )

                            Screen.CALENDAR -> CalendarScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = Modifier.padding(innerPadding),
                            )

                            Screen.INVOCATIONS -> InvocationsScreen(
                                openInvocationId = requestedInvocation.takeIf { it != NO_INVOCATION },
                                onBack = backToHome,
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }

                    // Pas d'entrée en fondu : l'écran système d'Android 12+ (ou le
                    // `windowBackground`) affiche déjà ce vert-là, le raccord doit
                    // être invisible. Seule la sortie s'estompe, sur l'accueil déjà prêt.
                    AnimatedVisibility(
                        visible = splashVisible,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(SPLASH_FADE_MS)),
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }

    /** Fix appareil → cache Room, puis replanification sur la position à jour. */
    private fun refreshLocationThenReschedule() {
        lifecycleScope.launch {
            if (miqaatApp.locationRepository.refresh()) {
                PrayerAlarmScheduler(this@MainActivity).scheduleNext()
            }
        }
    }

    private fun requestMissingPermissions() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun Intent.requestedInvocationId(): Long =
        getLongExtra(EXTRA_OPEN_INVOCATION, NO_INVOCATION)

    companion object {
        /** Posé par la notification d'une invocation : son identifiant. */
        const val EXTRA_OPEN_INVOCATION = "open_invocation"

        private const val NO_INVOCATION = -1L

        /**
         * Durée d'affichage de l'écran de démarrage, fondu de sortie compris.
         * Assez pour lire la baseline, assez court pour ne pas retarder qui vient
         * juste voir l'heure du prochain adhan.
         */
        private const val SPLASH_DURATION_MS = 1400L
        private const val SPLASH_FADE_MS = 450
    }
}
