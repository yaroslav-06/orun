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
import com.yarick.orun.MainActivity
import com.yarick.orun.data.LocationPoint
import com.yarick.orun.data.RunDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.yarick.orun.ACTION_START"
        const val ACTION_STOP = "com.yarick.orun.ACTION_STOP"
        const val EXTRA_RUN_ID = "extra_run_id"
        const val CHANNEL_ID = "orun_tracking"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var runId: Long = 0
    private var lastSavedLocation: Location? = null
    private var pointCount = 0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                runId = intent.getLongExtra(EXTRA_RUN_ID, 0)
                startForeground(NOTIFICATION_ID, buildNotification())
                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .build()

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
            Log.d("orun", "GPS fix: lat=${location.latitude} lng=${location.longitude} alt=${location.altitude} (skipped, dist=${dist}m < 2m)")
            return
        }

        Log.d("orun", "GPS fix: lat=${location.latitude} lng=${location.longitude} alt=${location.altitude}")
        lastSavedLocation = location

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
            pointCount++
            Log.d("orun", "Point saved: lat=${location.latitude} lng=${location.longitude} alt=${location.altitude} dist=${dist}m total=$pointCount")
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
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
