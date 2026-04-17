package com.yarick.orunner

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

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

        val metric = UnitPreference.isMetric(this)
        val unitLabel = if (metric) "km" else "mi"

        // Switch
        val switch = findViewById<MaterialSwitch>(R.id.switchVoiceover)
        val optionsGroup = findViewById<View>(R.id.llVoiceoverOptions)
        switch.isChecked = VoiceoverPreference.isEnabled(this)
        optionsGroup.visibility = if (switch.isChecked) View.VISIBLE else View.GONE
        switch.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setEnabled(this, checked)
            optionsGroup.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Slider — 4 steps: 1/8, 1/4, 1/2, 1
        val INTERVAL_LABELS = arrayOf("1/8", "1/4", "1/2", "1")
        val slider = findViewById<Slider>(R.id.sliderInterval)
        val tvInterval = findViewById<TextView>(R.id.tvIntervalValue)
        slider.value = VoiceoverPreference.getIntervalIndex(this).toFloat()
        slider.setLabelFormatter { v -> "${INTERVAL_LABELS[v.toInt()]} $unitLabel" }
        fun updateIntervalLabel(v: Float) {
            tvInterval.text = "Every ${INTERVAL_LABELS[v.toInt()]} $unitLabel"
        }
        updateIntervalLabel(slider.value)
        slider.addOnChangeListener { _, value, _ ->
            updateIntervalLabel(value)
            VoiceoverPreference.setIntervalIndex(this, value.toInt())
        }

        // Checkboxes
        val cbStatDistance = findViewById<CheckBox>(R.id.cbStatDistance)
        cbStatDistance.isChecked = VoiceoverPreference.getStatDistance(this)
        cbStatDistance.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatDistance(this, checked)
        }

        val cbStatTime = findViewById<CheckBox>(R.id.cbStatTime)
        val cbStatSplitPace = findViewById<CheckBox>(R.id.cbStatSplitPace)
        val cbStatOverallPace = findViewById<CheckBox>(R.id.cbStatOverallPace)
        val cbStatSplitElev = findViewById<CheckBox>(R.id.cbStatSplitElev)

        cbStatSplitPace.text = "Current $unitLabel pace"
        cbStatSplitElev.text = "Current $unitLabel elevation"

        cbStatTime.isChecked = VoiceoverPreference.getStatTime(this)
        cbStatSplitPace.isChecked = VoiceoverPreference.getStatSplitPace(this)
        cbStatOverallPace.isChecked = VoiceoverPreference.getStatOverallPace(this)
        cbStatSplitElev.isChecked = VoiceoverPreference.getStatSplitElevation(this)

        cbStatTime.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatTime(this, checked)
        }
        cbStatSplitPace.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatSplitPace(this, checked)
        }
        cbStatOverallPace.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatOverallPace(this, checked)
        }
        cbStatSplitElev.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatSplitElevation(this, checked)
        }

        val cbStatGoalPercent = findViewById<CheckBox>(R.id.cbStatGoalPercent)
        cbStatGoalPercent.isChecked = VoiceoverPreference.getStatGoalPercent(this)
        cbStatGoalPercent.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatGoalPercent(this, checked)
        }

        val cbStatGoalEta = findViewById<CheckBox>(R.id.cbStatGoalEta)
        cbStatGoalEta.isChecked = VoiceoverPreference.getStatGoalEta(this)
        cbStatGoalEta.setOnCheckedChangeListener { _, checked ->
            VoiceoverPreference.setStatGoalEta(this, checked)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
