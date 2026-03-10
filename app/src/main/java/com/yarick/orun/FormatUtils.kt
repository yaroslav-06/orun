package com.yarick.orun

private const val M_PER_MI = 1609.344f
private const val M_PER_FT = 0.3048f

fun formatDistance(meters: Float, metric: Boolean): String =
    if (metric) "%.2f km".format(meters / 1000f)
    else        "%.2f mi".format(meters / M_PER_MI)

fun formatPace(distanceMeters: Float, durationMs: Long, metric: Boolean): String {
    if (distanceMeters <= 0f) return if (metric) "– /km" else "– /mi"
    val secPerM = durationMs / 1000f / distanceMeters
    val secPerUnit = if (metric) secPerM * 1000f else secPerM * M_PER_MI
    val m = (secPerUnit / 60).toInt()
    val s = (secPerUnit % 60).toInt()
    val unit = if (metric) "/km" else "/mi"
    return "%d:%02d %s".format(m, s, unit)
}

fun formatPaceFromSecPerKm(secPerKm: Float, metric: Boolean): String {
    val secPerUnit = if (metric) secPerKm else secPerKm * 1000f / M_PER_MI
    val m = (secPerUnit / 60).toInt()
    val s = (secPerUnit % 60).toInt()
    val unit = if (metric) "/km" else "/mi"
    return "%d:%02d %s".format(m, s, unit)
}

fun formatElevation(meters: Float, metric: Boolean): String =
    if (metric) "%.0f m".format(meters)
    else        "%.0f ft".format(meters / M_PER_FT)

fun formatDuration(durationMs: Long): String {
    val s = durationMs / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun formatDurationSec(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
