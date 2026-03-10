package com.yarick.orun

import android.content.Context

object VoiceoverPreference {
    private const val PREFS_NAME = "orun_prefs"
    private const val KEY_ENABLED = "voiceover_enabled"
    private const val KEY_INTERVAL_INDEX = "voiceover_interval_index"
    private const val KEY_STAT_TIME = "voiceover_stat_time"
    private const val KEY_STAT_SPLIT_PACE = "voiceover_stat_split_pace"
    private const val KEY_STAT_OVERALL_PACE = "voiceover_stat_overall_pace"
    private const val KEY_STAT_SPLIT_ELEVATION = "voiceover_stat_split_elevation"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, v: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, v).apply()

    fun getIntervalIndex(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_INDEX, 2)

    fun setIntervalIndex(context: Context, v: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_INDEX, v).apply()

    fun getStatTime(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STAT_TIME, true)

    fun setStatTime(context: Context, v: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STAT_TIME, v).apply()

    fun getStatSplitPace(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STAT_SPLIT_PACE, true)

    fun setStatSplitPace(context: Context, v: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STAT_SPLIT_PACE, v).apply()

    fun getStatOverallPace(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STAT_OVERALL_PACE, true)

    fun setStatOverallPace(context: Context, v: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STAT_OVERALL_PACE, v).apply()

    fun getStatSplitElevation(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_STAT_SPLIT_ELEVATION, false)

    fun setStatSplitElevation(context: Context, v: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STAT_SPLIT_ELEVATION, v).apply()
}
