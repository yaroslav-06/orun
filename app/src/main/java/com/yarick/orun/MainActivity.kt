package com.yarick.orun

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yarick.orun.data.Run
import com.yarick.orun.data.RunDatabase
import com.yarick.orun.data.RunStats
import com.yarick.orun.service.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var fab: FloatingActionButton
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null
    private var listJob: Job? = null
    private lateinit var adapter: RunAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            startRun()
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* background permission result; run already started */ }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        adapter = RunAdapter { run ->
            startActivity(Intent(this, RunDetailActivity::class.java).putExtra("run_id", run.id))
        }
        val rvRuns = findViewById<RecyclerView>(R.id.rvRuns)
        rvRuns.layoutManager = LinearLayoutManager(this)
        rvRuns.adapter = adapter

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            val currentStats = LocationTrackingService.stats.value
            if (currentStats.startTime > 0 && !currentStats.isFinished) {
                startActivity(Intent(this, RunStatsActivity::class.java))
            } else {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    startRun()
                } else {
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        observeJob = mainScope.launch {
            LocationTrackingService.stats.collect { stats ->
                updateFab(stats)
            }
        }
        listJob = mainScope.launch {
            RunDatabase.getInstance(this@MainActivity).runDao().getAllFinishedRuns().collect { runs ->
                adapter.submitList(runs)
            }
        }
    }

    override fun onPause() {
        observeJob?.cancel()
        listJob?.cancel()
        super.onPause()
    }

    private fun updateFab(stats: RunStats) {
        if (stats.startTime > 0 && !stats.isFinished) {
            fab.setImageResource(android.R.drawable.ic_media_play)
        } else {
            fab.setImageResource(android.R.drawable.ic_input_add)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LocationTrackingService.CHANNEL_ID,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while a run is being tracked"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startRun() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val runId = RunDatabase.getInstance(this@MainActivity)
                .runDao()
                .insertRun(Run(startTime = startTime))

            val intent = Intent(this@MainActivity, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_RUN_ID, runId)
                putExtra(LocationTrackingService.EXTRA_START_TIME, startTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            runOnUiThread {
                startActivity(Intent(this@MainActivity, RunStatsActivity::class.java))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mainScope.cancel()
    }
}
