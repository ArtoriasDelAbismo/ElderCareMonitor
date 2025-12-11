# ElderCareMonitor

Wear OS companion app that tracks an elder user's heart rate, detects falls, and alerts caregivers when the watch is removed or a fall is suspected.

## Modules
- `wear`: Wear OS app built with Jetpack Compose. Contains sensors, UI, and alert network client.
- `mobile`: Placeholder Android app module (no app logic yet).

## Runtime Flow
- **Heart rate**: `HeartRateManager` streams BPM via Health Services. Values of `0` are ignored until seen 3 times in a row to avoid transient zeros. Dangerous BPM if `>120` or `<45` triggers the warning callback.
- **Wearing detection**: `WearingStateManager` monitors `Sensor.TYPE_HEART_RATE` activity. If no heart rate arrives for 18 seconds (after a 5-second startup delay), the watch is treated as removed, vibration is triggered, HR streaming stops, and a removal alert is sent.
- **Fall detection**: `FallDetectionManager` watches accelerometer magnitude. A strong impact (`>30 m/s²`) followed by ≥2s of near-stillness raises a fall, with a 5-second cooldown between events. The user is prompted to confirm or request help.
- **User actions**: From the fall prompt, "I'm ok" closes the prompt and resets detection; "Need help" sends a fall alert to the emergency contact, vibrates, and shows a notification.

## Data Formats
Alerts are posted as JSON to the backend via `AlertService` using `OkHttp`.

Common fields:
- `userId` (string): Identifier for the watch wearer. Set in `MainActivity` (`elder_001` by default); passed to `AlertService`.
- `timestamp` (number): Epoch milliseconds generated on the watch at send time.

Endpoints and payloads:
```json
POST /api/alert/watch-removed
{
  "userId": "elder_001",
  "timestamp": 1700000000000
}

POST /api/alert/fall-detected
{
  "userId": "elder_001",
  "timestamp": 1700000000000
}
```
Responses are not processed beyond logging the HTTP status code.

## Build & Run (Wear)
1. Open the project in Android Studio.
2. Select the `wear` run configuration or module.
3. Deploy to a Wear OS device/emulator with body sensor and notification permissions granted when prompted.

## Key Thresholds
- Heart rate danger: `<45` or `>120` BPM.
- Fall impact: `>30 m/s²` followed by ≥2s stillness; 5s cooldown between detections.
- Watch removal: no HR readings for 18s (after a 5s startup window).

## Notifications & Vibration
- Vibration: `VibrationHelper` issues a 600 ms one-shot vibration for warnings.
- Notifications (API 26+ channels):
  - `wearing_alerts` for watch removal (currently not shown in UI flow).
  - `fall_alerts` for fall confirmations when help is requested.
