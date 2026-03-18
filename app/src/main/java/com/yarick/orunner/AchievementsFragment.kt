package com.yarick.orunner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.yarick.orunner.data.RunDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AchievementsFragment : Fragment() {
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val view = view ?: return
        val tvNoRecords = view.findViewById<TextView>(R.id.tvNoRecords)
        val recordsContainer = view.findViewById<LinearLayout>(R.id.recordsContainer)
        val metric = UnitPreference.isMetric(requireContext())

        scope.launch {
            recordsContainer.removeAllViews()
            val beDao = RunDatabase.getInstance(requireContext()).bestEffortDao()
            val efforts = withContext(Dispatchers.IO) { beDao.getAllBestPerDistance() }

            if (efforts.isEmpty()) {
                tvNoRecords.visibility = View.VISIBLE
            } else {
                tvNoRecords.visibility = View.GONE
                val inflater = LayoutInflater.from(requireContext())
                for (effort in efforts) {
                    val row = inflater.inflate(R.layout.item_record, recordsContainer, false)
                    row.findViewById<TextView>(R.id.tvRecordDistance).text = effort.distanceKey
                    row.findViewById<TextView>(R.id.tvRecordStats).text =
                        "${formatDuration(effort.durationMs)}  ${formatPace(effort.distanceMeters.toFloat(), effort.durationMs, metric)}"
                    row.setOnClickListener {
                        startActivity(
                            Intent(requireContext(), DistanceEffortsActivity::class.java)
                                .putExtra("distance_key", effort.distanceKey)
                                .putExtra("distance_label", effort.distanceKey)
                        )
                    }
                    recordsContainer.addView(row)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
