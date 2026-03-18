package com.yarick.orunner.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yarick.orunner.RunStatsActivity
import com.yarick.orunner.UnitPreference
import com.yarick.orunner.VoiceoverPreference
import com.yarick.orunner.data.LocationPoint
import com.yarick.orunner.data.Run
import com.yarick.orunner.data.RunDatabase
import com.yarick.orunner.data.RunStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.yarick.orunner.ACTION_START"
        const val ACTION_STOP = "com.yarick.orunner.ACTION_STOP"
        const val EXTRA_RUN_ID = "extra_run_id"
        const val EXTRA_START_TIME = "extra_start_time"
        const val CHANNEL_ID = "orun_tracking"
        private const val NOTIFICATION_ID = 1

        val stats = MutableStateFlow(RunStats())
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var runId: Long = 0
    private var lastSavedLocation: Location? = null
    private var lastAltitude: Double? = null
    private var distanceMeters: Float = 0f
    private var elevationGainMeters: Float = 0f
    private var elevationLossMeters: Float = 0f
    private val paceBuffer = ArrayDeque<Pair<Long, Float>>() // (timestamp ms, cumulative distance m)
    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var voiceoverEnabled: Boolean = false
    private var announceIntervalMeters: Float = 0f
    private var nextAnnouncementMeters: Float = 0f
    private var splitBoundaryMeters: Float = 0f
    private var splitBoundaryTimeMs: Long = 0L
    private var isMetric: Boolean = true

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tts = TextToSpeech(this) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) tts?.language = Locale.getDefault()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                runId = intent.getLongExtra(EXTRA_RUN_ID, 0)
                val startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                stats.value = RunStats(startTime = startTime)

                // Reset accumulation for new run
                distanceMeters = 0f; elevationGainMeters = 0f; elevationLossMeters = 0f
                paceBuffer.clear(); lastSavedLocation = null; lastAltitude = null

                // Voiceover
                voiceoverEnabled = VoiceoverPreference.isEnabled(this)
                isMetric = UnitPreference.isMetric(this)
                announceIntervalMeters = intervalIndexToMeters(VoiceoverPreference.getIntervalIndex(this), isMetric)
                nextAnnouncementMeters = announceIntervalMeters
                splitBoundaryMeters = 0f
                splitBoundaryTimeMs = startTime

                startForeground(NOTIFICATION_ID, buildNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                val currentStats = stats.value
                scope.launch {
                    RunDatabase.getInstance(applicationContext).runDao().updateRun(
                        Run(
                            id = runId,
                            startTime = currentStats.startTime,
                            endTime = System.currentTimeMillis(),
                            totalDistanceMeters = currentStats.distanceMeters,
                            elevationGainMeters = currentStats.elevationGainMeters,
                            elevationLossMeters = currentStats.elevationLossMeters
                        )
                    )
                    stats.value = currentStats.copy(isFinished = true)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { processLocation(it) }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("orun", "Location permission not granted", e)
        }
    }

    private fun processLocation(location: Location) {
        val last = lastSavedLocation
        val dist = last?.distanceTo(location) ?: Float.MAX_VALUE

        if (last != null && dist < 2f) {
            return
        }

        // Elevation tracking
        val prevAlt = lastAltitude
        lastAltitude = location.altitude
        if (prevAlt != null) {
            val delta = location.altitude - prevAlt
            if (delta > 2.0) elevationGainMeters += delta.toFloat()
            else if (delta < -2.0) elevationLossMeters += (-delta).toFloat()
        }

        // Distance tracking
        if (last != null) {
            distanceMeters += dist
        }

        lastSavedLocation = location

        // Update ring buffer (keep last 200 m of points)
        paceBuffer.addLast(System.currentTimeMillis() to distanceMeters)
        while (paceBuffer.size > 1 && distanceMeters - paceBuffer.first().second > 200f) {
            paceBuffer.removeFirst()
        }

        stats.value = stats.value.copy(
            distanceMeters = distanceMeters,
            elevationGainMeters = elevationGainMeters,
            elevationLossMeters = elevationLossMeters,
            currentPaceSecPerKm = calculateCurrentPace()
        )

        if (voiceoverEnabled && announceIntervalMeters > 0f) {
            while (distanceMeters >= nextAnnouncementMeters) {
                announceStats()
                nextAnnouncementMeters += announceIntervalMeters
            }
        }

        // Update whole-unit split boundary after any announcements
        if (voiceoverEnabled) {
            val unitMeters = if (isMetric) 1000f else 1609.344f
            val newBoundary = (distanceMeters / unitMeters).toInt().toFloat() * unitMeters
            if (newBoundary > splitBoundaryMeters) {
                splitBoundaryMeters = newBoundary
                splitBoundaryTimeMs = System.currentTimeMillis()
            }
        }

        Log.d("orun", "GPS fix: dist=${distanceMeters}m elev+${elevationGainMeters}m -${elevationLossMeters}m")

        scope.launch {
            RunDatabase.getInstance(applicationContext).runDao().insertPoint(
                LocationPoint(
                    runId = runId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    timestamp = location.time
                )
            )
        }
    }

    private fun calculateCurrentPace(): Float {
        if (paceBuffer.size < 2) return 0f
        val oldest = paceBuffer.first()
        val newest = paceBuffer.last()
        val distCovered = newest.second - oldest.second
        if (distCovered <= 0f) return 0f
        val timeSec = (newest.first - oldest.first) / 1000f
        return timeSec * 1000f / distCovered // sec per km
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, RunStatsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orun")
            .setContentText("Orun is tracking your run")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun announceStats() {
        if (!ttsReady) return
        val snapshot = stats.value
        val parts = mutableListOf<String>()

        if (VoiceoverPreference.getStatDistance(this)) {
            parts.add("Distance ${formatDistanceSpoken(snapshot.distanceMeters, isMetric)}")
        }
        if (VoiceoverPreference.getStatTime(this)) {
            val elapsedSec = (System.currentTimeMillis() - snapshot.startTime) / 1000L
            parts.add("Time ${formatDurationSpoken(elapsedSec)}")
        }
        if (VoiceoverPreference.getStatSplitPace(this)) {
            val splitDist = distanceMeters - splitBoundaryMeters
            val splitTimeSec = (System.currentTimeMillis() - splitBoundaryTimeMs) / 1000f
            if (splitDist > 0f && splitTimeSec > 0f) {
                val splitPaceSecPerKm = splitTimeSec * 1000f / splitDist
                parts.add("Pace ${formatPaceSpoken(splitPaceSecPerKm, isMetric)}")
            }
        }
        if (VoiceoverPreference.getStatOverallPace(this)) {
            val elapsedMs = System.currentTimeMillis() - snapshot.startTime
            val dist = snapshot.distanceMeters
            if (dist > 0f) {
                val overallSecPerKm = (elapsedMs / 1000f) / (dist / 1000f)
                parts.add("Overall pace ${formatPaceSpoken(overallSecPerKm, isMetric)}")
            }
        }
        if (VoiceoverPreference.getStatSplitElevation(this)) {
            parts.add("Elevation gain ${formatElevationSpoken(snapshot.elevationGainMeters, isMetric)}")
        }

        if (parts.isNotEmpty()) {
            tts?.speak(parts.joinToString(". "), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun intervalIndexToMeters(index: Int, metric: Boolean): Float {
        val unitMeters = if (metric) 1000f else 1609.344f
        return when (index) {
            0 -> unitMeters / 8f
            1 -> unitMeters / 4f
            2 -> unitMeters / 2f
            else -> unitMeters
        }
    }

    private fun formatPaceSpoken(secPerKm: Float, metric: Boolean): String {
        val secPerUnit = if (metric) secPerKm else secPerKm * 1000f / 1609.344f
        val m = (secPerUnit / 60).toInt()
        val s = (secPerUnit % 60).toInt()
        val unit = if (metric) "per kilometre" else "per mile"
        return if (s == 0) "$m minutes $unit" else "$m minutes $s seconds $unit"
    }

    private fun formatDurationSpoken(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val parts = mutableListOf<String>()
        if (h > 0) parts.add("$h ${if (h == 1L) "hour" else "hours"}")
        if (m > 0) parts.add("$m ${if (m == 1L) "minute" else "minutes"}")
        if (s > 0 || parts.isEmpty()) parts.add("$s ${if (s == 1L) "second" else "seconds"}")
        return parts.joinToString(" ")
    }

    private fun formatElevationSpoken(meters: Float, metric: Boolean): String =
        if (metric) "${meters.toInt()} metres"
        else "${(meters / 0.3048f).toInt()} feet"

    private fun formatDistanceSpoken(meters: Float, metric: Boolean): String =
        if (metric) "%.2f kilometres".format(meters / 1000f)
        else "%.2f miles".format(meters / 1609.344f)

    override fun onDestroy() {
        stopLocationUpdates()
        tts?.stop()
        tts?.shutdown()
        tts = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
