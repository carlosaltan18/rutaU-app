package uvg.edu.rutau.core.designsystem.component

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    DisposableEffect(lifecycleOwner, mapView) {
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Elige el punto de encuentro", style = MaterialTheme.typography.titleMedium)
            Text(
                "Toca el mapa para colocar el marcador. Ambas personas deberán confirmarlo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        overlayManager.tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT)
                        overlayManager.tilesOverlay.setLoadingLineColor(Color.TRANSPARENT)
                        setMultiTouchControls(true)
                        controller.setZoom(InitialZoom)
                        controller.setCenter(DefaultMeetingPointCenter)
                        overlays.add(MeetingPointTapOverlay { selectedLatitude, selectedLongitude ->
                            latestOnPointSelected(selectedLatitude, selectedLongitude)
                        })
                        mapView = this
                    }
                },
                update = { view ->
                    view.updateMeetingPointMarker(latitude, longitude)
                    if (latitude != null && longitude != null) {
                        view.controller.setCenter(GeoPoint(latitude, longitude))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
            )
            Text(
                text = if (latitude == null || longitude == null) {
                    "Aún no has seleccionado un punto."
                } else {
                    "Punto seleccionado: ${latitude.formatCoordinate()}, ${longitude.formatCoordinate()}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "© OpenStreetMap contributors",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
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

/** Recibe un toque sencillo sobre el mapa y devuelve sus coordenadas. */
private class MeetingPointTapOverlay(
    private val onPointSelected: (latitude: Double, longitude: Double) -> Unit,
) : Overlay() {
    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView): Boolean {
        val point = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt())
        onPointSelected(point.latitude, point.longitude)
        return true
    }
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

/** Da un formato corto a una coordenada sin exponer precisión innecesaria. */
private fun Double.formatCoordinate(): String = "%.5f".format(java.util.Locale.US, this)

private val DefaultMeetingPointCenter = GeoPoint(14.6349, -90.5069)
private const val InitialZoom = 12.0
