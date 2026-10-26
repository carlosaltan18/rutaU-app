package uvg.edu.rutau.core.location

import android.Manifest

/** Reúne el permiso y el texto que se muestran antes de compartir ubicación. */
object LiveTripLocationPermissions {
    val foregroundPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    const val Explanation = "Tu ubicación se comparte cada 10 segundos solo mientras el conductor mantiene este viaje activo."
}
