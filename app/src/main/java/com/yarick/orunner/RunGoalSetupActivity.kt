package com.yarick.orunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip

class RunGoalSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GOAL_DISTANCE_METERS = "extra_goal_distance_meters"
        const val EXTRA_GOAL_DURATION_MS = "extra_goal_duration_ms"

        private val NO_GOAL_TEMPLATES = listOf(
            "Just run" to "No distance or time target — enjoy the run",
            "Run free" to "Leave the numbers behind today",
            "Explore" to "Go wherever your legs take you",
            "Clear your head" to "A run with no expectations",
            "Enjoy the moment" to "No finish line — just movement",
            "Run for fun" to "No pressure, just pleasure",
            "Follow the path" to "See where the road leads you",
            "Break loose" to "Today is a day without limits",
            "Move your body" to "No goal needed — just go",
            "Feel the run" to "Let your pace be your guide",
            "Run your way" to "Your rules, your pace, your run",
            "Wander freely" to "No route, no target, no problem",
            "Unplug" to "Just you, your shoes, and the open road",
            "Run wild" to "Let instinct be your pacer",
            "Go anywhere" to "The best runs have no plan",
            "Pure running" to "Strip it back to the basics",
            "Breathe and run" to "That's all you need today",
            "No clock watching" to "Time isn't your concern right now",
            "Your body knows best" to "Trust the pace that feels right",
            "Make it yours" to "A run entirely on your own terms"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_goal_setup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val metric = UnitPreference.isMetric(this)

        val toggleGoalType = findViewById<MaterialButtonToggleGroup>(R.id.toggleGoalType)
        val llNoGoalContainer = findViewById<LinearLayout>(R.id.llNoGoalContainer)
        val llDistanceContainer = findViewById<LinearLayout>(R.id.llDistanceContainer)
        val llDurationContainer = findViewById<LinearLayout>(R.id.llDurationContainer)
        val llChipsContainer = findViewById<LinearLayout>(R.id.llChipsContainer)
        val hsvDistanceChips = findViewById<View>(R.id.hsvDistanceChips)
        val hsvDurationChips = findViewById<View>(R.id.hsvDurationChips)
        val vSpacer = findViewById<View>(R.id.vSpacer)

        val etDistanceGoal = findViewById<EditText>(R.id.etDistanceGoal)
        val tvDistanceUnit = findViewById<TextView>(R.id.tvDistanceUnit)
        val tvHours = findViewById<TextView>(R.id.tvHours)
        val tvMinutes = findViewById<TextView>(R.id.tvMinutes)
        val tvSeconds = findViewById<TextView>(R.id.tvSeconds)
        val etDurationInput = findViewById<EditText>(R.id.etDurationInput)

        // Set unit label
        tvDistanceUnit.text = if (metric) "km" else "mi"

        // Randomise no-goal text
        val (title, subtitle) = NO_GOAL_TEMPLATES.random()
        findViewById<TextView>(R.id.tvNoGoalTitle).text = title
        findViewById<TextView>(R.id.tvNoGoalSubtitle).text = subtitle

        // ── Distance chips ─────────────────────────────────────────────────────────
        // chip → preset value string (in user's unit)
        val distanceChipList: List<Pair<Chip, String>> = if (metric)
            listOf(
                findViewById<Chip>(R.id.chipDist5k) to "5",
                findViewById<Chip>(R.id.chipDist10k) to "10",
                findViewById<Chip>(R.id.chipDistHalf) to "21.1",
                findViewById<Chip>(R.id.chipDistMarathon) to "42.2"
            )
        else
            listOf(
                findViewById<Chip>(R.id.chipDist5k) to "3.11",
                findViewById<Chip>(R.id.chipDist10k) to "6.21",
                findViewById<Chip>(R.id.chipDistHalf) to "13.11",
                findViewById<Chip>(R.id.chipDistMarathon) to "26.22"
            )

        // Chip tap → set value, cursor at end; isChecked driven by TextWatcher below
        distanceChipList.forEach { (chip, value) ->
            chip.setOnClickListener {
                etDistanceGoal.setText(value)
                etDistanceGoal.setSelection(value.length)
            }
        }

        // TextWatcher: highlight chip whose float value matches current input
        etDistanceGoal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val inputFloat = s?.toString()?.toFloatOrNull()
                distanceChipList.forEach { (chip, value) ->
                    chip.isChecked = inputFloat != null && inputFloat == value.toFloat()
                }
            }
        })

        // ── Duration chips ─────────────────────────────────────────────────────────
        // chip → digit string (right-aligned HHMMSS encoding)
        val durationChipList: List<Pair<Chip, String>> = listOf(
            findViewById<Chip>(R.id.chipDur10m) to "1000",   // 10 min = 00:10:00
            findViewById<Chip>(R.id.chipDur30m) to "3000",   // 30 min = 00:30:00
            findViewById<Chip>(R.id.chipDur1h) to "10000"    // 1 hr  = 01:00:00
        )

        // Chip tap → set digits; TextWatcher handles display + isChecked + cursor
        durationChipList.forEach { (chip, digits) ->
            chip.setOnClickListener {
                etDurationInput.setText(digits)
            }
        }

        // TextWatcher: update clock display, sync chip selection, pin cursor to end
        etDurationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString() ?: ""
                val d = raw.padStart(6, '0')
                tvHours.text = d.substring(0, 2)
                tvMinutes.text = d.substring(2, 4)
                tvSeconds.text = d.substring(4, 6)
                // Highlight chip whose preset ms exactly matches current ms
                val totalMs = totalMsFromDigits(raw)
                durationChipList.forEach { (chip, digits) ->
                    chip.isChecked = raw.isNotEmpty() && totalMs == totalMsFromDigits(digits)
                }
                // Pin cursor to end — enforces right-to-left digit-stack behaviour
                if (etDurationInput.selectionEnd != raw.length) {
                    etDurationInput.setSelection(raw.length)
                }
            }
        })

        // ── Goal type toggle ────────────────────────────────────────────────────────
        toggleGoalType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnDistance -> {
                    llNoGoalContainer.visibility = View.GONE
                    llDistanceContainer.visibility = View.VISIBLE
                    llDurationContainer.visibility = View.GONE
                    llChipsContainer.visibility = View.VISIBLE
                    hsvDistanceChips.visibility = View.VISIBLE
                    hsvDurationChips.visibility = View.GONE
                    vSpacer.visibility = View.VISIBLE
                    etDistanceGoal.requestFocus()
                    showKeyboard(etDistanceGoal)
                }
                R.id.btnDuration -> {
                    llNoGoalContainer.visibility = View.GONE
                    llDistanceContainer.visibility = View.GONE
                    llDurationContainer.visibility = View.VISIBLE
                    llChipsContainer.visibility = View.VISIBLE
                    hsvDistanceChips.visibility = View.GONE
                    hsvDurationChips.visibility = View.VISIBLE
                    vSpacer.visibility = View.VISIBLE
                    etDurationInput.requestFocus()
                    showKeyboard(etDurationInput)
                }
                else -> { // No goal
                    llNoGoalContainer.visibility = View.VISIBLE
                    llDistanceContainer.visibility = View.GONE
                    llDurationContainer.visibility = View.GONE
                    llChipsContainer.visibility = View.GONE
                    vSpacer.visibility = View.GONE
                    hideKeyboard()
                }
            }
        }

        // Tap clock display to re-open keyboard
        llDurationContainer.setOnClickListener {
            etDurationInput.requestFocus()
            showKeyboard(etDurationInput)
        }

        // ── Start Run ───────────────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnStartRun).setOnClickListener {
            val result = Intent()
            when (toggleGoalType.checkedButtonId) {
                R.id.btnDistance -> {
                    val distUserUnit = etDistanceGoal.text.toString().trim().toFloatOrNull()
                    if (distUserUnit != null && distUserUnit > 0f) {
                        val meters = if (metric) distUserUnit * 1000f else distUserUnit * 1609.344f
                        result.putExtra(EXTRA_GOAL_DISTANCE_METERS, meters)
                    }
                }
                R.id.btnDuration -> {
                    val ms = totalMsFromDigits(etDurationInput.text.toString())
                    if (ms > 0) result.putExtra(EXTRA_GOAL_DURATION_MS, ms)
                }
                // btnNoGoal → no extras
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun totalMsFromDigits(raw: String): Long {
        val d = raw.padStart(6, '0')
        val h = d.substring(0, 2).toLong()
        val m = d.substring(2, 4).toLong()
        val s = d.substring(4, 6).toLong()
        return (h * 3600 + m * 60 + s) * 1000L
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
