package uvg.edu.rutau.core.designsystem.component

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
    meetingLatitude: Double?,
    meetingLongitude: Double?,
    centerRequest: Long = 0L,
    modifier: Modifier = Modifier,
    mapHeight: Dp = Dp.Unspecified,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasCenteredOnLocation by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val markerTitle = if (currentUserIsDriver) "Tú" else "Conductor"

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

    LaunchedEffect(
        mapView,
        liveLocation?.latitude,
        liveLocation?.longitude,
        liveLocation?.updatedAtMillis,
        meetingLatitude,
        meetingLongitude,
    ) {
        liveLocation?.let { location ->
            if (!hasCenteredOnLocation) mapView?.let { view ->
                view.centerOnTravelPoints(location, meetingLatitude, meetingLongitude)
                hasCenteredOnLocation = true
            }
        }
        if (liveLocation == null && meetingLatitude != null && meetingLongitude != null) {
            mapView?.centerOnTravelPoints(null, meetingLatitude, meetingLongitude)
        }
    }

    LaunchedEffect(centerRequest) {
        if (centerRequest > 0L) {
            mapView?.centerOnTravelPoints(liveLocation, meetingLatitude, meetingLongitude)
        }
    }

    val mapModifier = modifier
        .fillMaxWidth()
        .let { currentModifier ->
            if (mapHeight == Dp.Unspecified) currentModifier else currentModifier.height(mapHeight)
        }
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                val initialLocation = liveLocation
                setTileSource(TileSourceFactory.MAPNIK)
                overlayManager.tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT)
                overlayManager.tilesOverlay.setLoadingLineColor(Color.TRANSPARENT)
                setMultiTouchControls(true)
                controller.setZoom(if (initialLocation == null) DefaultZoom else LiveLocationZoom)
                controller.setCenter(
                    initialLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: DefaultCenter,
                )
                mapView = this
            }
        },
        update = { view ->
            view.updateTravelMarkers(
                liveLocation = liveLocation,
                liveLocationTitle = markerTitle,
                meetingLatitude = meetingLatitude,
                meetingLongitude = meetingLongitude,
            )
        },
        modifier = mapModifier,
    )
}

/** Centra el mapa en el conductor y el punto de encuentro disponible. */
private fun MapView.centerOnTravelPoints(
    liveLocation: LiveTripLocation?,
    meetingLatitude: Double?,
    meetingLongitude: Double?,
) {
    val livePoint = liveLocation?.let { GeoPoint(it.latitude, it.longitude) }
    val meetingPoint = if (meetingLatitude != null && meetingLongitude != null) {
        GeoPoint(meetingLatitude, meetingLongitude)
    } else {
        null
    }
    val center = when {
        livePoint != null && meetingPoint != null -> GeoPoint(
            (livePoint.latitude + meetingPoint.latitude) / 2,
            (livePoint.longitude + meetingPoint.longitude) / 2,
        )
        livePoint != null -> livePoint
        meetingPoint != null -> meetingPoint
        else -> DefaultCenter
    }
    controller.setCenter(center)
    controller.setZoom(if (livePoint != null && meetingPoint != null) CombinedPointsZoom else LiveLocationZoom)
}

/** Actualiza los marcadores que explican el viaje activo. */
private fun MapView.updateTravelMarkers(
    liveLocation: LiveTripLocation?,
    liveLocationTitle: String,
    meetingLatitude: Double?,
    meetingLongitude: Double?,
) {
    updateLocationMarker(liveLocation, liveLocationTitle)
    updateMeetingPointMarker(meetingLatitude, meetingLongitude)
}

/** Actualiza el marcador del conductor sin descargar zonas adicionales. */
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

/** Actualiza el punto donde los estudiantes acordaron reunirse. */
private fun MapView.updateMeetingPointMarker(latitude: Double?, longitude: Double?) {
    val existing = overlays.filterIsInstance<LiveMeetingPointOverlay>().firstOrNull()
    if (latitude == null || longitude == null) {
        existing?.let(overlays::remove)
        invalidate()
        return
    }
    val marker = existing ?: LiveMeetingPointOverlay().also(overlays::add)
    marker.position = GeoPoint(latitude, longitude)
    invalidate()
}

/** Dibuja un marcador verde para diferenciar el punto de encuentro. */
private class LiveMeetingPointOverlay : Overlay() {
    var position: GeoPoint? = null

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(27, 127, 75) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(24, 33, 38)
        textSize = 28f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val point = position ?: return
        val screenPoint = mapView.projection.toPixels(point, null) ?: return
        val x = screenPoint.x.toFloat()
        val y = screenPoint.y.toFloat()
        canvas.drawCircle(x, y, MarkerRadius + 4f, borderPaint)
        canvas.drawCircle(x, y, MarkerRadius, markerPaint)
        canvas.drawText("Encuentro", x + 20f, y - 16f, textPaint)
    }

    private companion object {
        const val MarkerRadius = 16f
    }
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
private const val DefaultZoom = 11.0
private const val LiveLocationZoom = 15.0
private const val CombinedPointsZoom = 13.0
