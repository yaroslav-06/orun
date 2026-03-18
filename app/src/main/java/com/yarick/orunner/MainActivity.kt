package com.yarick.orunner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yarick.orunner.service.LocationTrackingService
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (savedInstanceState == null) {
            val homeFragment = HomeFragment()
            val mapFragment = MapFragment()
            val achievementsFragment = AchievementsFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, achievementsFragment, "achievements")
                .add(R.id.fragment_container, mapFragment, "map")
                .add(R.id.fragment_container, homeFragment, "home")
                .hide(achievementsFragment)
                .hide(mapFragment)
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val fm = supportFragmentManager
            val home = fm.findFragmentByTag("home")!!
            val map = fm.findFragmentByTag("map")!!
            val achievements = fm.findFragmentByTag("achievements")!!
            when (item.itemId) {
                R.id.nav_home -> {
                    fm.beginTransaction().show(home).hide(map).hide(achievements).commit()
                    supportActionBar?.show()
                    true
                }
                R.id.nav_map -> {
                    fm.beginTransaction().hide(home).show(map).hide(achievements).commit()
                    supportActionBar?.hide()
                    true
                }
                R.id.nav_achievements -> {
                    fm.beginTransaction().hide(home).hide(map).show(achievements).commit()
                    supportActionBar?.hide()
                    true
                }
                else -> false
            }
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
}
