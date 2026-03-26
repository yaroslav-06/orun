package com.yarick.orunner

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yarick.orunner.data.RunDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        map.overlays.add(myLocationOverlay)
        map.overlays.add(DoubleTapZoomOverlay())

        val dp = resources.displayMetrics.density
        val size = (18 * dp).toInt()
        val dotBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dotCanvas = Canvas(dotBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f

        paint.color = Color.WHITE
        dotCanvas.drawCircle(cx, cx, cx, paint)

        paint.color = Color.parseColor("#4285F4")
        dotCanvas.drawCircle(cx, cx, cx * 0.65f, paint)

        myLocationOverlay.setPersonIcon(dotBitmap)
        myLocationOverlay.setPersonHotspot(cx, cx)
        myLocationOverlay.setDirectionArrow(dotBitmap, dotBitmap)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fabMyLocation = view.findViewById(R.id.fab_my_location)
        fabMyLocation.setOnClickListener { goToMyLocation() }
        return view
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        myLocationOverlay.enableMyLocation()
        scope.launch {
            val db = RunDatabase.getInstance(requireContext())
            val startPoint = withContext(Dispatchers.IO) {
                val lastRun = db.runDao().getAllFinishedRuns().first().firstOrNull()
                    ?: return@withContext null
                db.runDao().getPointsForRun(lastRun.id).firstOrNull()
            }
            if (startPoint != null) {
                map.controller.setZoom(15.0)
                map.controller.animateTo(GeoPoint(startPoint.latitude, startPoint.longitude))
            } else {
                // Default: San Francisco
                map.controller.setZoom(12.0)
                map.controller.setCenter(GeoPoint(37.7749, -122.4194))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        myLocationOverlay.disableMyLocation()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun goToMyLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
            }
        }
    }

    private inner class DoubleTapZoomOverlay : Overlay() {
        private val pixelsPerZoomLevel = 150f * resources.displayMetrics.density

        private var isDoubleTapDragging = false
        private var startY = 0f
        private var startZoomLevel = 0.0
        private var totalDragDistance = 0f

        private val gestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    isDoubleTapDragging = true
                    startY = e.y
                    startZoomLevel = map.zoomLevelDouble
                    totalDragDistance = 0f
                    return true
                }
            }
        ).also { it.setIsLongpressEnabled(false) }

        override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
            if (event.pointerCount > 1) {
                isDoubleTapDragging = false
                return false
            }
            gestureDetector.onTouchEvent(event)
            if (isDoubleTapDragging) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = event.y - startY
                        totalDragDistance += Math.abs(event.y - startY)
                        map.controller.setZoom(startZoomLevel + (deltaY / pixelsPerZoomLevel))
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (totalDragDistance < 10f) map.controller.zoomIn()
                        isDoubleTapDragging = false
                    }
                }
                return true
            }
            return false
        }
    }
}
