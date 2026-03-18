package com.yarick.orunner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yarick.orunner.data.BestEffort
import java.text.SimpleDateFormat
import java.util.Locale

class EffortAdapter(
    private val items: List<Pair<BestEffort, Long>>,
    private val metric: Boolean,
    private val onClick: (Long) -> Unit
) : RecyclerView.Adapter<EffortAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvRunDate)
        val tvDistance: TextView = view.findViewById(R.id.tvRunDistance)
        val tvDuration: TextView = view.findViewById(R.id.tvRunDuration)
        val tvPace: TextView = view.findViewById(R.id.tvRunPace)
        init { view.setOnClickListener { onClick(items[adapterPosition].first.runId) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (effort, startTime) = items[position]
        val sdf = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(startTime)
        holder.tvDistance.text = effort.distanceKey
        holder.tvDuration.text = formatDuration(effort.durationMs)
        holder.tvPace.text = formatPace(effort.distanceMeters.toFloat(), effort.durationMs, metric)
    }

    override fun getItemCount() = items.size
}
