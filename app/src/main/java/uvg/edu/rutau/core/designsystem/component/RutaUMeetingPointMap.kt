package uvg.edu.rutau.core.designsystem.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/** Permite elegir un punto de encuentro tocando una ubicación en el mapa. */
@Composable
fun RutaUMeetingPointMap(
    latitude: Double?,
    longitude: Double?,
    onPointSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 440.dp,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPointSelected by rememberUpdatedState(onPointSelected)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }

    AndroidView(
        factory = { context ->
            MeetingPointMapView(context).also { view ->
                val tapDetector = GestureDetector(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                            val point = view.projection.fromPixels(event.x.toInt(), event.y.toInt())
                            latestOnPointSelected(point.latitude, point.longitude)
                            return true
                        }
                    },
                )
                view.setOnTouchListener { _, event ->
                    tapDetector.onTouchEvent(event)
                    if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    false
                }
                view.setTileSource(TileSourceFactory.MAPNIK)
                view.overlayManager.tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT)
                view.overlayManager.tilesOverlay.setLoadingLineColor(Color.TRANSPARENT)
                view.setMultiTouchControls(true)
                view.controller.setZoom(InitialZoom)
                view.controller.setCenter(DefaultMeetingPointCenter)
                mapView = view
            }
        },
        update = { view ->
            view.updateMeetingPointMarker(latitude, longitude)
            if (latitude != null && longitude != null) {
                view.controller.setCenter(GeoPoint(latitude, longitude))
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight),
    )
}

/** Hace que el mapa anuncie los toques a los servicios de accesibilidad. */
private class MeetingPointMapView(context: Context) : MapView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

/** Actualiza el marcador que representa el punto que se propondrá. */
private fun MapView.updateMeetingPointMarker(latitude: Double?, longitude: Double?) {
    val existing = overlays.filterIsInstance<MeetingPointMarkerOverlay>().firstOrNull()
    if (latitude == null || longitude == null) {
        existing?.let(overlays::remove)
        invalidate()
        return
    }
    val marker = existing ?: MeetingPointMarkerOverlay().also(overlays::add)
    marker.position = GeoPoint(latitude, longitude)
    invalidate()
}

/** Dibuja el lugar propuesto sin depender de una ventana emergente del mapa. */
private class MeetingPointMarkerOverlay : Overlay() {
    var position: GeoPoint? = null
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(195, 49, 49) }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val point = position ?: return
        val screenPoint = mapView.projection.toPixels(point, null) ?: return
        val x = screenPoint.x.toFloat()
        val y = screenPoint.y.toFloat()
        canvas.drawCircle(x, y, MarkerRadius + 4f, borderPaint)
        canvas.drawCircle(x, y, MarkerRadius, markerPaint)
    }

    private companion object {
        const val MarkerRadius = 18f
    }
}

private val DefaultMeetingPointCenter = GeoPoint(14.6349, -90.5069)
private const val InitialZoom = 12.0
