package safety

import android.util.Log
import com.example.eldercaremonitor.data.location.WatchLocationHelper
import com.example.eldercaremonitor.presentation.EmergencyContact
import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import data.network.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class SafetyEvent {
    // ----SENSORS----
    data class HeartRate(val bpm: Int) : SafetyEvent()
    object FallDetected : SafetyEvent()
    object WatchRemoved : SafetyEvent()
    object WatchWornAgain : SafetyEvent()

    // ----USER ACTIONS----
    object UserIsOk : SafetyEvent()
    object UserNeedsHelp : SafetyEvent()
    object PanicButtonPressed : SafetyEvent()

    data class FallNoResponse(val elapsedMs: Long) : SafetyEvent()
}

// THRESHOLDS AND CONSTANTS
const val HIGH_BPM_THRESHOLD = 120
const val LOW_BPM_THRESHOLD = 45

private const val ALERT_COOLDOWN_WINDOW = 10000
private const val WATCH_REMOVED_CONFIRMATION_WINDOW = 5000
private const val FALL_DETECTED_CONFIRMATION_WINDOW = 15_000L
private const val HIGH_HR_CONFIRMATION_WINDOW = 10_000L
private const val LOW_HR_CONFIRMATION_WINDOW = 5_000L
private const val HIGH_HR_RESET_HYSTERESIS = 5
private const val LOW_HR_RESET_HYSTERESIS = 5

class SafetyEngine(
    //----RECEIVES PARAMETERS FROM MAIN ACTIVITY----
    private val vibrateWarning: VibrationHelper,
    private val showWatchRemovedNotification: NotificationHelper,
    private val showFallDetectedNotification: NotificationHelper,
    private val showDangerousHeartRateNotification: NotificationHelper,
    private val showPanicButtonPressedNotification: NotificationHelper,
    private val alertService: AlertService,
    private val userId: String,
    private val _isWearing: MutableStateFlow<Boolean> = MutableStateFlow(false),
    private val locationHelper: WatchLocationHelper,
    private val getPrimaryContact: () -> EmergencyContact?,
    private val hasLocationPermission: () -> Boolean,
    private val onHighHrSuggestion: () -> Unit = {}
) {
    private val lastAlertByType: MutableMap<AlertType, Long> = mutableMapOf()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastBpm: Int? = null
    private var highHrJob: Job? = null
    private var highHrArmed: Boolean = true

    private var lowHrJob: Job? = null
    private var lowHrArmed: Boolean = true

    fun onEvent(event: SafetyEvent) {
        when (event) {
            is SafetyEvent.HeartRate -> {
                val bpm = event.bpm
                lastBpm = bpm

                if (bpm <= (HIGH_BPM_THRESHOLD - HIGH_HR_RESET_HYSTERESIS) &&
                    bpm >= (LOW_BPM_THRESHOLD + LOW_HR_RESET_HYSTERESIS)
                ) {
                    lowHrArmed = true
                    lowHrJob?.cancel()
                    lowHrJob = null
                    highHrArmed = true
                    highHrJob?.cancel()
                    highHrJob = null
                }

                if (bpm <= LOW_BPM_THRESHOLD && lowHrArmed) {
                    if (lowHrJob == null) {
                        lowHrJob = scope.launch {
                            delay(LOW_HR_CONFIRMATION_WINDOW)
                            val current = lastBpm ?: return@launch
                            if (current <= LOW_BPM_THRESHOLD && lowHrArmed) {
                                lowHrArmed = false
                                showDangerousHeartRateNotification.showDangerousHeartRateNotification()
                                triggerAlert(
                                    AlertType.DANGEROUS_HR,
                                    "WARNING: Sustained low heart rate: " +
                                        "$current bpm for ${LOW_HR_CONFIRMATION_WINDOW / 1000}s"
                                )
                            }
                            lowHrJob = null
                        }
                    }
                } else {
                    lowHrJob?.cancel()
                    lowHrJob = null
                }

                if (bpm >= HIGH_BPM_THRESHOLD && highHrArmed) {
                    if (highHrJob == null) {
                        highHrJob = scope.launch {
                            delay(HIGH_HR_CONFIRMATION_WINDOW)
                            val current = lastBpm ?: return@launch
                            if (current >= HIGH_BPM_THRESHOLD && highHrArmed) {
                                highHrArmed = false
                                showDangerousHeartRateNotification.showDangerousHeartRateNotification()
                                onHighHrSuggestion()
                                triggerAlert(
                                    AlertType.DANGEROUS_HR,
                                    "WARNING: Sustained high heart rate: " +
                                        "$current bpm for ${HIGH_HR_CONFIRMATION_WINDOW / 1000}s"
                                )
                            }
                            highHrJob = null
                        }
                    }
                } else {
                    highHrJob?.cancel()
                    highHrJob = null
                }
            }

            is SafetyEvent.FallDetected -> {
                Log.d("ALERT", "Fall detected, waiting for user confirmation")
                showFallDetectedNotification.showFallDetectedNotification()

                alertService.sendFallDetectedAlert(
                    userId = userId,
                    message = "Fall detected, awaiting user confirmation",
                    severity = "MEDIUM",
                    requiresUserConfirmation = true,
                    userResponded = false,
                    confirmationWindowSec = (FALL_DETECTED_CONFIRMATION_WINDOW / 1000).toInt(),
                    wearingStatus = wearingStatus()
                )
            }

            is SafetyEvent.WatchRemoved -> {
                showWatchRemovedNotification.showWatchRemovedNotification()
                _isWearing.value = false
                triggerAlert(
                    AlertType.WATCH_REMOVED,
                    "WARNING: Watch removed"
                )
            }

            is SafetyEvent.WatchWornAgain -> {
                _isWearing.value = true
            }

            is SafetyEvent.UserIsOk -> {
                cancelPendingAlerts()
            }

            is SafetyEvent.UserNeedsHelp -> {
                triggerAlert(
                    AlertType.FALL_CONFIRMED_HELP,
                    "WARNING: Fall detected, user needs immediate attention"
                )
            }

            is SafetyEvent.FallNoResponse -> {
                val seconds = event.elapsedMs / 1000
                triggerAlert(
                    AlertType.FALL_NO_RESPONSE,
                    "High severity fall: no response after ${seconds}s",
                    event.elapsedMs
                )
            }

            is SafetyEvent.PanicButtonPressed -> {
                showPanicButtonPressedNotification.showPanicButtonPressedNotification()
                triggerAlert(
                    AlertType.PANIC_BUTTON,
                    "WARNING: Panic button pressed"
                )
            }
        }
    }

    fun clear() {
        scope.cancel()
    }

    private fun wearingStatus(): String =
        if (_isWearing.value) "ON_WRIST" else "OFF_WRIST"

    private fun canAlert(type: AlertType): Boolean {
        val now = System.currentTimeMillis()
        val last = lastAlertByType[type] ?: 0L
        return now - last > ALERT_COOLDOWN_WINDOW
    }

    enum class AlertType {
        FALL_CONFIRMED_HELP,
        FALL_NO_RESPONSE,
        WATCH_REMOVED,
        DANGEROUS_HR,
        PANIC_BUTTON
    }

    private fun triggerAlert(
        type: AlertType,
        message: String,
        elapsedMs: Long? = null
    ) {
        val bypassCooldown = type == AlertType.WATCH_REMOVED

        Log.d("ALERT", "Triggering alert: $type")
        if (!bypassCooldown && !canAlert(type)) {
            Log.d("ALERT", "Alert suppressed due to cooldown")
            return
        }
        lastAlertByType[type] = System.currentTimeMillis()
        vibrateWarning.vibrate()

        when (type) {
            AlertType.FALL_CONFIRMED_HELP -> {
                Log.d("LOCATION", "Permission available: ${hasLocationPermission()}")
                scope.launch(Dispatchers.IO) {
                    val locJson = if (hasLocationPermission()) {
                        locationHelper.getLastKnownLocationJson()
                    } else {
                        null
                    }
                    Log.d("LOCATION", "locJson = ${locJson?.toString() ?: "null"}")

                    val contact = getPrimaryContact()
                    alertService.sendFallConfirmedHelpAlert(
                        userId = userId,
                        confirmationWindowSec = (FALL_DETECTED_CONFIRMATION_WINDOW / 1000).toInt(),
                        wearingStatus = wearingStatus(),
                        location = locJson,
                        contactName = contact?.name,
                        contactPhone = contact?.phoneNumber,
                        heartRateBpm = lastBpm
                    )
                }
            }

            AlertType.FALL_NO_RESPONSE -> {
                Log.d("LOCATION", "NO_RESPONSE permission=${hasLocationPermission()}")
                scope.launch(Dispatchers.IO) {
                    val locJson = if (hasLocationPermission()) {
                        locationHelper.getLastKnownLocationJson()
                    } else null
                    Log.d("LOCATION", "locJson = ${locJson?.toString() ?: "null"}")

                    val contact = getPrimaryContact()
                    alertService.sendFallNoResponseAlert(
                        userId = userId,
                        elapsedMs = elapsedMs ?: 0L,
                        wearingStatus = wearingStatus(),
                        location = locJson,
                        contactName = contact?.name,
                        contactPhone = contact?.phoneNumber,
                        heartRateBpm = lastBpm
                    )
                }
            }

            AlertType.WATCH_REMOVED -> {
                alertService.sendWatchRemovedAlert(userId, message, wearingStatus())
            }

            AlertType.DANGEROUS_HR -> {
                alertService.sendDangerousHeartRateAlert(userId, message)
            }

            AlertType.PANIC_BUTTON -> {
                Log.d("LOCATION", "PANIC permission=${hasLocationPermission()}")
                scope.launch(Dispatchers.IO) {
                    val locJson = if (hasLocationPermission()) {
                        locationHelper.getLastKnownLocationJson()
                    } else null
                    Log.d("LOCATION", "locJson = ${locJson?.toString() ?: "null"}")

                    val contact = getPrimaryContact()
                    alertService.panicButtonPressed(
                        userId = userId,
                        message = message,
                        contactName = contact?.name,
                        contactPhone = contact?.phoneNumber,
                        location = locJson
                    )
                }
            }
        }
    }

    private fun cancelPendingAlerts() {
        lastAlertByType.clear()
    }
}
