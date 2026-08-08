package com.mohamed.miqaat.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batoulapps.adhan2.Madhab
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLanguage
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.PrayerTimeAdjustments
import com.mohamed.miqaat.domain.model.ReminderSettings
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.labelRes
import com.mohamed.miqaat.notifications.NotificationChannels
import com.mohamed.miqaat.notifications.PrayerAlarmScheduler
import com.mohamed.miqaat.widget.NextPrayerWidget
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsContent(
        settings = settings,
        reminder = reminder,
        autoMethod = viewModel.autoMethod,
        language = AppLocale.current(context),
        onBack = onBack,
        onMethodSelected = viewModel::setMethod,
        onAutoSelected = viewModel::setMethodAuto,
        onMadhabSelected = viewModel::setMadhab,
        onHijriOffsetChanged = viewModel::setHijriOffset,
        onAdjustmentChanged = viewModel::setPrayerAdjustment,
        onAdjustmentsReset = viewModel::clearPrayerAdjustments,
        onReminderEnabledChanged = viewModel::setReminderEnabled,
        onReminderLeadSelected = viewModel::setReminderLead,
        onLanguageSelected = { language -> applyLanguage(context, language) },
        modifier = modifier,
    )
}

/**
 * La langue s'applique en habillant le contexte de l'activité
 * (`MainActivity.attachBaseContext`) : il faut donc recréer l'activité pour que
 * tout l'écran — textes et sens d'écriture — reparte dans la nouvelle langue.
 * Le canal de notification et le widget vivent hors activité : on les rafraîchit
 * dans la foulée.
 */
private fun applyLanguage(context: Context, language: AppLanguage) {
    if (AppLocale.current(context) == language) return
    AppLocale.set(context, language)
    NotificationChannels.createAll(context)
    NextPrayerWidget.refresh(context)
    context.findActivity()?.recreate()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun settingsViewModel(): SettingsViewModel {
    val app = LocalContext.current.miqaatApp
    return viewModel {
        SettingsViewModel(
            repository = app.settingsRepository,
            locationRepository = app.locationRepository,
            onSettingsChanged = { PrayerAlarmScheduler(app).scheduleNext() },
        )
    }
}

@Composable
private fun SettingsContent(
    settings: CalculationSettings,
    reminder: ReminderSettings,
    autoMethod: MethodOption,
    language: AppLanguage,
    onBack: () -> Unit,
    onMethodSelected: (MethodOption) -> Unit,
    onAutoSelected: () -> Unit,
    onMadhabSelected: (Madhab) -> Unit,
    onHijriOffsetChanged: (Int) -> Unit,
    onAdjustmentChanged: (PrayerName, Int) -> Unit,
    onAdjustmentsReset: () -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderLeadSelected: (Int) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var openDialog by rememberSaveable { mutableStateOf(SettingsDialog.NONE) }

    // Les insets s'appliquent *avant* verticalScroll : posés après, ils
    // feraient partie du contenu qui défile et l'en-tête passerait sous la
    // barre de statut dès le premier glissement.
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                SettingRow(
                    title = stringResource(R.string.settings_calc_method),
                    value = if (settings.methodAuto) {
                        stringResource(R.string.method_auto_resolved, stringResource(autoMethod.labelRes))
                    } else {
                        stringResource(settings.method.labelRes)
                    },
                    onClick = { openDialog = SettingsDialog.METHOD },
                )
                SettingDivider()
                SettingRow(
                    title = stringResource(R.string.settings_madhab),
                    value = stringResource(settings.madhab.labelRes),
                    onClick = { openDialog = SettingsDialog.MADHAB },
                )
                SettingDivider()
                SettingRow(
                    title = stringResource(R.string.settings_adjustments),
                    value = adjustmentsSummary(settings.adjustments),
                    onClick = { openDialog = SettingsDialog.ADJUSTMENTS },
                )
                SettingDivider()
                HijriOffsetRow(
                    offsetDays = settings.hijriOffsetDays,
                    onChanged = onHijriOffsetChanged,
                )
                SettingDivider()
                SettingSwitchRow(
                    title = stringResource(R.string.settings_reminder),
                    subtitle = stringResource(R.string.settings_reminder_hint),
                    checked = reminder.enabled,
                    onCheckedChange = onReminderEnabledChanged,
                )
                // Le délai n'a de sens que si le rappel est actif : on le masque
                // plutôt que de le griser, l'interrupteur dit déjà tout.
                if (reminder.enabled) {
                    SettingDivider()
                    SettingRow(
                        title = stringResource(R.string.settings_reminder_lead),
                        value = pluralStringResource(
                            R.plurals.duration_minutes,
                            reminder.leadMinutes,
                            reminder.leadMinutes,
                        ),
                        onClick = { openDialog = SettingsDialog.REMINDER_LEAD },
                    )
                }
                SettingDivider()
                SettingRow(
                    title = stringResource(R.string.settings_language),
                    value = stringResource(language.labelRes),
                    onClick = { openDialog = SettingsDialog.LANGUAGE },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    when (openDialog) {
        // null = sélection automatique, en tête de liste
        SettingsDialog.METHOD -> RadioDialog(
            title = stringResource(R.string.settings_calc_method),
            options = buildList<Pair<MethodOption?, String>> {
                add(
                    null to stringResource(
                        R.string.method_auto_resolved,
                        stringResource(autoMethod.labelRes),
                    ),
                )
                selectableMethods.forEach { add(it to stringResource(it.labelRes)) }
            },
            selected = if (settings.methodAuto) null else settings.method,
            onSelect = { method ->
                if (method == null) onAutoSelected() else onMethodSelected(method)
                openDialog = SettingsDialog.NONE
            },
            onDismiss = { openDialog = SettingsDialog.NONE },
        )

        SettingsDialog.MADHAB -> RadioDialog(
            title = stringResource(R.string.settings_madhab),
            options = Madhab.entries.map { it to stringResource(it.labelRes) },
            selected = settings.madhab,
            onSelect = { onMadhabSelected(it); openDialog = SettingsDialog.NONE },
            onDismiss = { openDialog = SettingsDialog.NONE },
        )

        SettingsDialog.REMINDER_LEAD -> RadioDialog(
            title = stringResource(R.string.settings_reminder_lead),
            options = ReminderSettings.LEAD_CHOICES.map { minutes ->
                minutes to pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
            },
            selected = reminder.leadMinutes,
            onSelect = { onReminderLeadSelected(it); openDialog = SettingsDialog.NONE },
            onDismiss = { openDialog = SettingsDialog.NONE },
        )

        SettingsDialog.LANGUAGE -> RadioDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { it to stringResource(it.labelRes) },
            selected = language,
            onSelect = { openDialog = SettingsDialog.NONE; onLanguageSelected(it) },
            onDismiss = { openDialog = SettingsDialog.NONE },
        )

        SettingsDialog.ADJUSTMENTS -> AdjustmentsDialog(
            adjustments = settings.adjustments,
            onChanged = onAdjustmentChanged,
            onReset = onAdjustmentsReset,
            onDismiss = { openDialog = SettingsDialog.NONE },
        )

        SettingsDialog.NONE -> Unit
    }
}

private enum class SettingsDialog { NONE, METHOD, MADHAB, REMINDER_LEAD, LANGUAGE, ADJUSTMENTS }

/** « العصر +1 · المغرب +3 », ou « aucun ajustement » si tout est à zéro. */
@Composable
private fun adjustmentsSummary(adjustments: PrayerTimeAdjustments): String {
    if (adjustments.isEmpty) return stringResource(R.string.settings_adjustments_none)
    // `joinToString` n'est pas inline : sa lambda perdrait le contexte @Composable.
    // On résout donc les libellés dans un `map` (inline, lui), puis on assemble.
    val parts = adjustments.adjustedPrayers.map { prayer ->
        "${stringResource(prayer.labelRes)} ${signedMinutes(adjustments[prayer])}"
    }
    return parts.joinToString(" · ")
}

/** Chiffres occidentaux même en arabe, comme partout ailleurs dans l'app. */
private fun signedMinutes(minutes: Int): String = String.format(Locale.ROOT, "%+d", minutes)

/**
 * Les six moments avec un pas de ±1 minute. Les valeurs s'appliquent aussitôt
 * (chaque pas replanifie l'alarme), donc le dialogue n'a qu'un bouton de
 * fermeture — et une remise à zéro tant qu'il y a quelque chose à effacer.
 */
@Composable
private fun AdjustmentsDialog(
    adjustments: PrayerTimeAdjustments,
    onChanged: (PrayerName, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_adjustments)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.settings_adjustments_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                PrayerName.entries.forEach { prayer ->
                    AdjustmentRow(
                        label = stringResource(prayer.labelRes),
                        minutes = adjustments[prayer],
                        onChanged = { onChanged(prayer, it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        // Le bouton reste un composable inconditionnel : un `if` autour du
        // paramètre lui ferait perdre son contexte @Composable.
        dismissButton = {
            if (!adjustments.isEmpty) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.settings_adjustments_reset))
                }
            }
        },
    )
}

@Composable
private fun AdjustmentRow(label: String, minutes: Int, onChanged: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            label = "−",
            contentDescription = stringResource(R.string.settings_adjustments_decrease, label),
            enabled = minutes > PrayerTimeAdjustments.MIN_MINUTES,
            onClick = { onChanged(minutes - 1) },
        )
        Text(
            text = signedMinutes(minutes),
            style = MaterialTheme.typography.titleMedium,
            color = if (minutes == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            label = "+",
            contentDescription = stringResource(R.string.settings_adjustments_increase, label),
            enabled = minutes < PrayerTimeAdjustments.MAX_MINUTES,
            onClick = { onChanged(minutes + 1) },
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // Toute la ligne bascule ; l'interrupteur n'est qu'un témoin (onCheckedChange = null),
            // sinon la zone tactile serait comptée deux fois par l'accessibilité.
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun HijriOffsetRow(offsetDays: Int, onChanged: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_hijri_offset),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_hijri_offset_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StepperButton(
            label = "−",
            contentDescription = stringResource(R.string.settings_hijri_decrease),
            enabled = offsetDays > CalculationSettings.HIJRI_OFFSET_MIN,
            onClick = { onChanged(offsetDays - 1) },
        )
        Text(
            text = if (offsetDays == 0) {
                stringResource(R.string.settings_hijri_no_offset)
            } else {
                "%+d".format(offsetDays)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            label = "+",
            contentDescription = stringResource(R.string.settings_hijri_increase),
            enabled = offsetDays < CalculationSettings.HIJRI_OFFSET_MAX,
            onClick = { onChanged(offsetDays + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (option, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
    )
}

/** Ordre curé : les pays du public cible d'abord (Maghreb), puis par région. */
private val selectableMethods: List<MethodOption> = listOf(
    MethodOption.ALGERIA,
    MethodOption.TUNISIA,
    MethodOption.MOROCCO,
    MethodOption.EGYPTIAN,
    MethodOption.MUSLIM_WORLD_LEAGUE,
    MethodOption.UMM_AL_QURA,
    MethodOption.DUBAI,
    MethodOption.KUWAIT,
    MethodOption.QATAR,
    MethodOption.GULF,
    MethodOption.JORDAN,
    MethodOption.TURKEY,
    MethodOption.RUSSIA,
    MethodOption.FRANCE,
    MethodOption.PORTUGAL,
    MethodOption.KARACHI,
    MethodOption.INDONESIA,
    MethodOption.MALAYSIA,
    MethodOption.SINGAPORE,
    MethodOption.NORTH_AMERICA,
    MethodOption.MOON_SIGHTING_COMMITTEE,
)

private val MethodOption.labelRes: Int
    @StringRes get() = when (this) {
        MethodOption.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
        MethodOption.EGYPTIAN -> R.string.method_egyptian
        MethodOption.KARACHI -> R.string.method_karachi
        MethodOption.UMM_AL_QURA -> R.string.method_umm_al_qura
        MethodOption.DUBAI -> R.string.method_dubai
        MethodOption.MOON_SIGHTING_COMMITTEE -> R.string.method_moonsighting
        MethodOption.NORTH_AMERICA -> R.string.method_north_america
        MethodOption.KUWAIT -> R.string.method_kuwait
        MethodOption.QATAR -> R.string.method_qatar
        MethodOption.SINGAPORE -> R.string.method_singapore
        MethodOption.TURKEY -> R.string.method_turkey
        MethodOption.ALGERIA -> R.string.method_algeria
        MethodOption.TUNISIA -> R.string.method_tunisia
        MethodOption.MOROCCO -> R.string.method_morocco
        MethodOption.JORDAN -> R.string.method_jordan
        MethodOption.FRANCE -> R.string.method_france
        MethodOption.RUSSIA -> R.string.method_russia
        MethodOption.INDONESIA -> R.string.method_indonesia
        MethodOption.MALAYSIA -> R.string.method_malaysia
        MethodOption.PORTUGAL -> R.string.method_portugal
        MethodOption.GULF -> R.string.method_gulf
    }

private val Madhab.labelRes: Int
    @StringRes get() = when (this) {
        Madhab.SHAFI -> R.string.madhab_shafi
        Madhab.HANAFI -> R.string.madhab_hanafi
    }

/** Chaque langue est nommée dans sa propre langue — même valeur dans les trois `strings.xml`. */
private val AppLanguage.labelRes: Int
    @StringRes get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ARABIC -> R.string.language_arabic
        AppLanguage.FRENCH -> R.string.language_french
        AppLanguage.ENGLISH -> R.string.language_english
    }
