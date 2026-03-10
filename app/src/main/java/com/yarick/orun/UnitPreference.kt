package com.yarick.orun

import android.content.Context

object UnitPreference {
    private const val PREFS_NAME = "orun_prefs"
    private const val KEY_METRIC = "use_metric"

    fun isMetric(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_METRIC, true)

    fun setMetric(context: Context, metric: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_METRIC, metric).apply()
}
