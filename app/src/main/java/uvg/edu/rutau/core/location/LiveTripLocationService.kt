package uvg.edu.rutau.core.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uvg.edu.rutau.R
import uvg.edu.rutau.app.RutaUAppDependencies

/** Comparte ubicación cada diez segundos durante un viaje activo del conductor. */
class LiveTripLocationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var tripOccurrenceId: String? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val occurrenceId = tripOccurrenceId ?: return
            scope.launch {
                RutaUAppDependencies.liveTripLocationRepository.publish(
                    occurrenceId,
                    LiveTripLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        bearing = location.bearing,
                        speed = location.speed,
                    ),
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val occurrenceId = intent?.getStringExtra(TripOccurrenceIdKey)
        if (occurrenceId.isNullOrBlank() || FirebaseAuth.getInstance().currentUser == null || !hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        tripOccurrenceId = occurrenceId
        startForeground(NotificationId, createNotification())
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UpdateIntervalMillis)
            .setMinUpdateIntervalMillis(UpdateIntervalMillis)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (_: SecurityException) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationClient.removeLocationUpdates(locationCallback)
        val occurrenceId = tripOccurrenceId
        if (!occurrenceId.isNullOrBlank()) {
            scope.launch { RutaUAppDependencies.liveTripLocationRepository.stopSharing(occurrenceId) }
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun createNotification(): android.app.Notification {
        val channel = NotificationChannel(
            LocationChannelId,
            "Ubicación durante el viaje",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "RutaU comparte tu ubicación mientras el viaje está activo." }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, LocationChannelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Ubicación compartida")
            .setContentText("Se comparte solo durante este viaje activo.")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TripOccurrenceIdKey = "trip_occurrence_id"
        private const val LocationChannelId = "live_trip_location"
        private const val NotificationId = 2002
        private const val UpdateIntervalMillis = 10_000L

        /** Inicia el servicio solo después de confirmar el viaje y los permisos. */
        fun start(context: Context, tripOccurrenceId: String) {
            val intent = Intent(context, LiveTripLocationService::class.java)
                .putExtra(TripOccurrenceIdKey, tripOccurrenceId)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Detiene la ubicación al cancelar o terminar el viaje. */
        fun stop(context: Context) {
            context.stopService(Intent(context, LiveTripLocationService::class.java))
        }
    }
}
