package com.yarick.orunner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yarick.orunner.data.Run
import com.yarick.orunner.data.RunDatabase
import com.yarick.orunner.data.RunStats
import com.yarick.orunner.service.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: RunAdapter
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null
    private var listJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                requireContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            startRun()
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* background permission result; run already started */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RunAdapter { run ->
            startActivity(Intent(requireContext(), RunDetailActivity::class.java).putExtra("run_id", run.id))
        }
        val rvRuns = view.findViewById<RecyclerView>(R.id.rvRuns)
        rvRuns.layoutManager = LinearLayoutManager(requireContext())
        rvRuns.adapter = adapter

        fab = view.findViewById(R.id.fab)
        fab.setOnClickListener {
            val currentStats = LocationTrackingService.stats.value
            if (currentStats.startTime > 0 && !currentStats.isFinished) {
                startActivity(Intent(requireContext(), RunStatsActivity::class.java))
            } else {
                if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        requireContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
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
            RunDatabase.getInstance(requireContext()).runDao().getAllFinishedRuns().collect { runs ->
                adapter.submitList(runs)
            }
        }
    }

    override fun onPause() {
        observeJob?.cancel()
        listJob?.cancel()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateFab(stats: RunStats) {
        fab.setImageResource(
            if (stats.startTime > 0 && !stats.isFinished) android.R.drawable.ic_media_play
            else android.R.drawable.ic_input_add
        )
    }

    private fun startRun() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val runId = RunDatabase.getInstance(requireContext())
                .runDao()
                .insertRun(Run(startTime = startTime))

            val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_RUN_ID, runId)
                putExtra(LocationTrackingService.EXTRA_START_TIME, startTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }

            requireActivity().runOnUiThread {
                startActivity(Intent(requireContext(), RunStatsActivity::class.java))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mainScope.cancel()
    }
}
