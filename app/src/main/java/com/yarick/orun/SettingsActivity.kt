package com.yarick.orun

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        val rgUnits = findViewById<RadioGroup>(R.id.rgUnits)
        val rbMetric = findViewById<RadioButton>(R.id.rbMetric)
        val rbImperial = findViewById<RadioButton>(R.id.rbImperial)

        if (UnitPreference.isMetric(this)) rbMetric.isChecked = true
        else rbImperial.isChecked = true

        rgUnits.setOnCheckedChangeListener { _, id ->
            UnitPreference.setMetric(this, id == R.id.rbMetric)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
