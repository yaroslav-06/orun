package com.yarick.orun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yarick.orun.data.Run
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
        holder.tvDate.text = formatDate(run.startTime)
        holder.tvDistance.text = "%.2f km".format(run.totalDistanceMeters / 1000f)
        val endTime = run.endTime ?: run.startTime
        holder.tvDuration.text = formatDuration(run.startTime, endTime)
        holder.tvPace.text = formatPace(run.totalDistanceMeters, endTime - run.startTime)
    }

    override fun getItemCount() = runs.size

    private fun formatDate(ms: Long): String {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
        return sdf.format(ms)
    }

    private fun formatDuration(startMs: Long, endMs: Long): String {
        val totalSec = (endMs - startMs) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun formatPace(distanceM: Float, durationMs: Long): String {
        if (distanceM <= 0f) return "– /km"
        val secPerKm = (durationMs / 1000f) / (distanceM / 1000f)
        val m = (secPerKm / 60).toInt()
        val s = (secPerKm % 60).toInt()
        return "%d:%02d /km".format(m, s)
    }
}
