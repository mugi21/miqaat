package com.mohamed.miqaat.data.reliability

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Les écrans de « démarrage automatique » des surcouches constructeur.
 *
 * Ces réglages n'ont **aucune API** : ni pour les lire, ni pour les demander. Une
 * application gelée par MIUI ne reçoit tout simplement pas ses alarmes, sans le
 * moindre signal. Le seul remède est d'y emmener l'utilisateur, écran par écran.
 *
 * ⚠ Trois précautions indispensables, chacune pour une raison différente :
 * 1. un bloc `<queries>` dans le manifeste — sans lui, [Intent.resolveActivity]
 *    renvoie `null` sur Android 11+ même pour ces paquets système (filtrage de
 *    visibilité des paquets) ;
 * 2. `resolveActivity` malgré tout, parce que les noms de composants changent
 *    d'une version de surcouche à l'autre : on essaie dans l'ordre ;
 * 3. un `try/catch` autour de `startActivity`, parce qu'un composant résolu peut
 *    encore refuser d'être lancé depuis une application tierce. On retombe alors
 *    sur les instructions écrites.
 */
object OemAutostart {

    private const val TAG = "OemAutostart"

    /** Le fabricant a-t-il un écran connu ? `null` = rien à proposer, on n'invente pas. */
    fun forDevice(context: Context): List<ComponentName> {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val candidates = COMPONENTS.entries
            .firstOrNull { (brands, _) -> brands.any { manufacturer.contains(it) } }
            ?.value
            ?: return emptyList()
        return candidates.filter { it.isResolvable(context) }
    }

    fun hasKnownScreen(context: Context): Boolean = forDevice(context).isNotEmpty()

    /**
     * Ouvre le premier écran disponible. `false` = échec, l'appelant doit afficher
     * les instructions manuelles.
     */
    fun open(context: Context): Boolean {
        forDevice(context).forEach { component ->
            val intent = Intent().setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val started = runCatching { context.startActivity(intent) }
                .onFailure { Log.w(TAG, "Écran constructeur injoignable : $component", it) }
                .isSuccess
            if (started) return true
        }
        return false
    }

    private fun ComponentName.isResolvable(context: Context): Boolean =
        runCatching {
            Intent().setComponent(this).resolveActivity(context.packageManager) != null
        }.getOrDefault(false)

    /**
     * Ordre d'essai par famille de marques. Pour Xiaomi, les **deux** écrans
     * comptent : le démarrage automatique et l'économiseur de batterie sont deux
     * verrous distincts, et l'un sans l'autre ne suffit pas.
     */
    private val COMPONENTS: Map<List<String>, List<ComponentName>> = mapOf(
        listOf("xiaomi", "redmi", "poco") to listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
        ),
        listOf("huawei", "honor") to listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            ),
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity",
            ),
        ),
        listOf("oppo", "realme") to listOf(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            ),
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartUpAppListActivity",
            ),
        ),
        listOf("vivo", "iqoo") to listOf(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            ),
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            ),
        ),
        listOf("samsung") to listOf(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity",
            ),
        ),
        listOf("oneplus") to listOf(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            ),
        ),
    )
}
