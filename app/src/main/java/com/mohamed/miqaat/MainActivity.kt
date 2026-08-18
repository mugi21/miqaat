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
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.notifications.PrayerAlarmScheduler
import com.mohamed.miqaat.ui.calendar.CalendarScreen
import com.mohamed.miqaat.ui.home.HomeScreen
import com.mohamed.miqaat.ui.invocations.InvocationsScreen
import com.mohamed.miqaat.ui.qibla.QiblaScreen
import com.mohamed.miqaat.ui.quran.QuranPlayerBar
import com.mohamed.miqaat.ui.quran.QuranScreen
import com.mohamed.miqaat.ui.reliability.ReliabilityScreen
import com.mohamed.miqaat.ui.settings.SettingsScreen
import com.mohamed.miqaat.ui.splash.SplashScreen
import com.mohamed.miqaat.ui.update.UpdateScreen
import com.mohamed.miqaat.ui.theme.MiqaatTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Les écrans de premier niveau ; enum = Serializable, donc rememberSaveable le garde. */
private enum class Screen { HOME, SETTINGS, QIBLA, CALENDAR, INVOCATIONS, RELIABILITY, QURAN, UPDATE }

class MainActivity : ComponentActivity() {

    /**
     * L'invocation à ouvrir quand on arrive depuis sa notification. `singleTop`
     * dans le manifeste : l'activité déjà à l'écran est réutilisée, et c'est
     * [onNewIntent] qui apporte la demande.
     */
    private val openInvocationId = mutableLongStateOf(NO_INVOCATION)

    /** Posé par la notification du lecteur : rouvrir l'app sur l'écran du Coran. */
    private val openQuran = mutableStateOf(false)

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
        openQuran.value = intent.requestedQuran()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openInvocationId.longValue = intent.requestedInvocationId()
        openQuran.value = intent.requestedQuran()
        requestMissingPermissions()
        // Chaque ouverture resynchronise la chaîne d'alarmes (filet de sécurité),
        // puis rafraîchit la position si on y a droit.
        PrayerAlarmScheduler(this).scheduleNext()
        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            refreshLocationThenReschedule()
        }
        checkForUpdate()

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
                    // Le mini-lecteur vit dans la barre du bas et non dans un
                    // écran : c'est ce qui lui permet de survivre au retour à
                    // l'accueil, donc d'écouter une sourate en consultant les
                    // horaires. Il ne compose rien quand rien ne joue.
                    val playback by miqaatApp.quranPlayer.state.collectAsStateWithLifecycle()

                    // Des écrans à plat depuis l'accueil : un état suffit, pas
                    // besoin de librairie de navigation pour l'instant. Déclaré
                    // **au-dessus** du Scaffold : la barre du bas doit pouvoir
                    // ouvrir l'écran du Coran, elle aussi.
                    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                    val requestedInvocation by openInvocationId
                    val requestedQuran by openQuran

                    // Arrivée depuis la notification d'une invocation : on ouvre
                    // l'écran directement sur elle, prête à être lue.
                    LaunchedEffect(requestedInvocation) {
                        if (requestedInvocation != NO_INVOCATION) screen = Screen.INVOCATIONS
                    }
                    // Arrivée depuis la notification du lecteur.
                    LaunchedEffect(requestedQuran) {
                        if (requestedQuran) screen = Screen.QURAN
                    }

                    val backToHome = {
                        openInvocationId.longValue = NO_INVOCATION
                        openQuran.value = false
                        screen = Screen.HOME
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0),
                        bottomBar = {
                            QuranPlayerBar(
                                state = playback,
                                onOpen = { screen = Screen.QURAN },
                                onTogglePlayPause = miqaatApp.quranPlayer::togglePlayPause,
                                onStop = miqaatApp.quranPlayer::stop,
                            )
                        },
                    ) { innerPadding ->
                        // `consumeWindowInsets` en plus du `padding` : la barre
                        // du bas porte déjà `navigationBarsPadding`, et sans
                        // cette consommation les écrans la rajouteraient
                        // par-dessus — un blanc de la hauteur de la barre de
                        // navigation apparaîtrait dès qu'une sourate joue.
                        val screenModifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)

                        if (screen != Screen.HOME) {
                            BackHandler(onBack = backToHome)
                        }
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onOpenQibla = { screen = Screen.QIBLA },
                                onOpenCalendar = { screen = Screen.CALENDAR },
                                onOpenInvocations = { screen = Screen.INVOCATIONS },
                                onOpenQuran = { screen = Screen.QURAN },
                                onOpenReliability = { screen = Screen.RELIABILITY },
                                onOpenUpdate = { screen = Screen.UPDATE },
                                modifier = screenModifier,
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                onBack = { screen = Screen.HOME },
                                onOpenReliability = { screen = Screen.RELIABILITY },
                                onOpenUpdate = { screen = Screen.UPDATE },
                                modifier = screenModifier,
                            )

                            Screen.RELIABILITY -> ReliabilityScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = screenModifier,
                            )

                            Screen.UPDATE -> UpdateScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = screenModifier,
                            )

                            Screen.QIBLA -> QiblaScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = screenModifier,
                            )

                            Screen.CALENDAR -> CalendarScreen(
                                onBack = { screen = Screen.HOME },
                                modifier = screenModifier,
                            )

                            Screen.INVOCATIONS -> InvocationsScreen(
                                openInvocationId = requestedInvocation.takeIf { it != NO_INVOCATION },
                                onBack = backToHome,
                                modifier = screenModifier,
                            )

                            Screen.QURAN -> QuranScreen(
                                onBack = backToHome,
                                modifier = screenModifier,
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

    /**
     * La veille des releases GitHub (D44) : le **seul** point de départ d'une
     * vérification, et il est ici plutôt que dans la chaîne d'alarmes ou un
     * receiver. Au plus une fois par jour, coupable depuis l'écran de mise à jour,
     * et sans conséquence si elle échoue.
     */
    private fun checkForUpdate() {
        lifecycleScope.launch {
            miqaatApp.updateRepository.cleanUpIfInstalled()
            miqaatApp.updateRepository.refreshIfDue()
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

    private fun Intent.requestedQuran(): Boolean = getBooleanExtra(EXTRA_OPEN_QURAN, false)

    companion object {
        /** Posé par la notification d'une invocation : son identifiant. */
        const val EXTRA_OPEN_INVOCATION = "open_invocation"

        /** Posé par la notification du lecteur de Coran. */
        const val EXTRA_OPEN_QURAN = "open_quran"

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
