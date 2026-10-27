package uvg.edu.rutau.core.designsystem.component

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import uvg.edu.rutau.core.location.LiveTripLocation

/** Muestra un mapa con la última ubicación compartida durante un viaje. */
@Composable
fun RutaULiveMap(
    liveLocation: LiveTripLocation?,
    currentUserIsDriver: Boolean,
    modifier: Modifier = Modifier,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val markerTitle = if (currentUserIsDriver) "Tú" else "Conductor"

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

    LaunchedEffect(mapView, liveLocation?.latitude, liveLocation?.longitude, liveLocation?.updatedAtMillis) {
        liveLocation?.let { location ->
            mapView?.let { view ->
                view.controller.setZoom(LiveLocationZoom)
                view.controller.animateTo(GeoPoint(location.latitude, location.longitude))
            }
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
            Text("Ubicación en vivo", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (currentUserIsDriver) "Marcador: Tú" else "Marcador: Conductor",
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
                        controller.setZoom(DefaultZoom)
                        controller.setCenter(DefaultCenter)
                        mapView = this
                    }
                },
                update = { view ->
                    view.updateLocationMarker(liveLocation, markerTitle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
            RutaUOutlinedButton(
                text = "Centrar ubicación",
                onClick = {
                    liveLocation?.let { location ->
                        mapView?.controller?.animateTo(GeoPoint(location.latitude, location.longitude))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = liveLocation != null,
            )
            Text(
                text = "© OpenStreetMap contributors",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Actualiza el único marcador del mapa sin descargar zonas adicionales. */
private fun MapView.updateLocationMarker(
    liveLocation: LiveTripLocation?,
    title: String,
) {
    val existing = overlays.filterIsInstance<LiveLocationOverlay>().firstOrNull()
    if (liveLocation == null) {
        existing?.let(overlays::remove)
        invalidate()
        return
    }
    val marker = existing ?: LiveLocationOverlay().also(overlays::add)
    marker.position = GeoPoint(liveLocation.latitude, liveLocation.longitude)
    marker.title = title
    invalidate()
}

/** Dibuja un marcador sencillo para no depender de la ventana emergente de osmdroid. */
private class LiveLocationOverlay : Overlay() {
    var position: GeoPoint? = null
    var title: String = ""

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 97, 112) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(24, 33, 38)
        textSize = 30f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val location = position ?: return
        val screenPoint = mapView.projection.toPixels(location, null) ?: return
        val x = screenPoint.x.toFloat()
        val y = screenPoint.y.toFloat()
        canvas.drawCircle(x, y, MarkerRadius + 4f, borderPaint)
        canvas.drawCircle(x, y, MarkerRadius, markerPaint)
        if (title.isNotBlank()) canvas.drawText(title, x + 22f, y - 18f, textPaint)
    }

    private companion object {
        const val MarkerRadius = 18f
    }
}

private val DefaultCenter = GeoPoint(14.6349, -90.5069)
private const val DefaultZoom = 12.0
private const val LiveLocationZoom = 16.0
