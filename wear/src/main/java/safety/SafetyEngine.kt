package safety

import java.sql.Timestamp

sealed class Sandwich {
    data class HeartRate(val bpm: Int) : Sandwich()
    data class FallDetected(val timestamp: Long = System.currentTimeMillis()) : Sandwich()
    data class WatchRemoved(val timestamp: Long = System.currentTimeMillis()) : Sandwich()
    object WatchWornAgain : Sandwich()
    object UserOk : Sandwich()
    object UserNeedsHelp : Sandwich()
}

// THRESHOLDS AND CONSTANTS

const val HIGH_BPM_THRESHOLD = 140
const val LOW_BPM_THRESHOLD = 45

private const val COOLDOWN_TIME = 60000 // 1 minute
private const val FALL_CONFIRMATION_WINDOW = 10000 // 10 seconds
private const val WATCH_REMOVED_WINDOW = 4000 // 4 SECONDS

// SAFETY ENGINE

class SafetyEngine {

}
