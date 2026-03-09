package com.yarick.orun.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yarick.orun.RunStatsActivity
import com.yarick.orun.data.LocationPoint
import com.yarick.orun.data.Run
import com.yarick.orun.data.RunDatabase
import com.yarick.orun.data.RunStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.yarick.orun.ACTION_START"
        const val ACTION_STOP = "com.yarick.orun.ACTION_STOP"
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

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                runId = intent.getLongExtra(EXTRA_RUN_ID, 0)
                val startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                stats.value = RunStats(startTime = startTime)
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

    override fun onDestroy() {
        stopLocationUpdates()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
