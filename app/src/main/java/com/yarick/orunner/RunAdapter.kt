package com.yarick.orunner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yarick.orunner.data.Run
import java.text.SimpleDateFormat
import java.util.Locale

class RunAdapter(private val onClick: (Run) -> Unit) :
    RecyclerView.Adapter<RunAdapter.ViewHolder>() {

    private var runs: List<Run> = emptyList()

    fun submitList(newRuns: List<Run>) {
        runs = newRuns
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvRunDate)
        val tvDistance: TextView = view.findViewById(R.id.tvRunDistance)
        val tvDuration: TextView = view.findViewById(R.id.tvRunDuration)
        val tvPace: TextView = view.findViewById(R.id.tvRunPace)
        init { view.setOnClickListener { onClick(runs[adapterPosition]) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val run = runs[position]
        val context = holder.itemView.context
        val metric = UnitPreference.isMetric(context)
        val endTime = run.endTime ?: run.startTime
        val durationMs = endTime - run.startTime

        holder.tvDate.text = formatDate(run.startTime)
        holder.tvDistance.text = formatDistance(run.totalDistanceMeters, metric)
        holder.tvDuration.text = formatDuration(durationMs)
        holder.tvPace.text = formatPace(run.totalDistanceMeters, durationMs, metric)
    }

    override fun getItemCount() = runs.size

    private fun formatDate(ms: Long): String {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
        return sdf.format(ms)
    }
}
