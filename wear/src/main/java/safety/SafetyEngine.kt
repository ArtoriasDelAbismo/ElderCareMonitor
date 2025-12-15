package safety

import com.example.eldercaremonitor.presentation.utils.NotificationHelper
import com.example.eldercaremonitor.presentation.utils.VibrationHelper
import data.network.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel


sealed class SafetyEvent {

    // ----SENSORS----
    data class HeartRate(val bpm: Int) : SafetyEvent()
    data class DangerousHeartRate(val bpm: Int) : SafetyEvent()
    object FallDetected : SafetyEvent()
    object WatchRemoved : SafetyEvent()

    object WatchWornAgain : SafetyEvent()

    // ----USER ACTIONS----
    object UserIsOk : SafetyEvent()
    object UserNeedsHelp : SafetyEvent()

}

// THRESHOLDS AND CONSTANTS

const val HIGH_BPM_THRESHOLD = 120
const val LOW_BPM_THRESHOLD = 45

private const val ALERT_COOLDOWN_WINDOW = 10000
private const val WATCH_REMOVED_CONFIRMATION_WINDOW = 5000
private const val FALL_DETECTED_CONFIRMATION_WINDOW = 5000

class SafetyEngine(
    //----RECEIVES PARAMETERS FROM MAIN ACTIVITY----
    private val vibrateWarning: VibrationHelper,
    private val showWatchRemovedNotification: NotificationHelper,
    private val sendWatchRemovedAlert: AlertService,
    private val showFallDetectedNotification: NotificationHelper,
    private val sendFallDetectedAlert: AlertService,
) {
    private var lastAlertTimestamp: Long = 0L
    private var pendingAlertJob: Job? = null

    // Coroutine scope for safety operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    //----PUBLIC ENTRY POINT----

    fun onEvent(event: SafetyEvent) {
        when (event) {

            is SafetyEvent.DangerousHeartRate -> {
                triggerAlert(
                    AlertType.DANGEROUS_HR,
                    "⚠️\uFE0F Dangerous heart rate detected: ${event.bpm} bpm"
                )
            }

            is SafetyEvent.FallDetected -> {
                triggerAlert(
                    AlertType.FALL,
                    "⚠️\uFE0F Fall detected"
                )
            }

            is SafetyEvent.WatchRemoved -> {
                triggerAlert(
                    AlertType.WATCH_REMOVED,
                    "⚠️\uFE0F Watch removed"
                )
            }

            is SafetyEvent.UserIsOk -> {
                cancelPendingAlerts()
            }

            is SafetyEvent.UserNeedsHelp -> {
                triggerAlert(
                    AlertType.FALL,
                    "⚠️\uFE0F Fall detected, user needs immediate attention"
                )
            }

            else -> Unit
        }

    }

    fun clear() {
        scope.cancel()
    }

    // ----COOLDOWN LOGIC----

    private fun canAlert(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastAlertTimestamp > ALERT_COOLDOWN_WINDOW
    }

    // ----CREATE ALERT TYPES SO TRIGGER ALERT DECIDES WHICH ONE TO CALL----

    enum class AlertType {
        FALL, WATCH_REMOVED, DANGEROUS_HR
    }

    private fun triggerAlert(
        type: AlertType,
        message: String
    ) {
        if (!canAlert()) return
        lastAlertTimestamp = System.currentTimeMillis()
        pendingAlertJob?.cancel()
        vibrateWarning.vibrate()
        when (type) {
            AlertType.FALL -> {
                showFallDetectedNotification.showFallDetectedNotification()
                sendFallDetectedAlert.sendFallDetectedAlert(message)
            }

            AlertType.WATCH_REMOVED -> {
                showWatchRemovedNotification.showWatchRemovedNotification()
                sendWatchRemovedAlert.sendWatchRemovedAlert(message)
            }

            AlertType.DANGEROUS_HR -> {
                vibrateWarning.vibrate()

            }

        }
    }

    private fun cancelPendingAlerts() {
        pendingAlertJob?.cancel()
    }


}








