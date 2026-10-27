package uvg.edu.rutau.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Describe las llamadas publicadas por el servidor de RutaU. */
interface BackendApi {
    @POST("auth/session")
    suspend fun createSession(@Body body: SessionRequestDto): Response<ApiEnvelopeDto<BackendSessionDto>>

    @GET("universities")
    suspend fun getUniversities(@Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null): Response<ApiEnvelopeDto<List<UniversityDto>>>

    @GET("universities/{id}/campuses")
    suspend fun getCampuses(@Path("id") universityId: String, @Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null): Response<ApiEnvelopeDto<List<CampusDto>>>

    @GET("zones")
    suspend fun getZones(@Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null, @Query("municipality") municipality: String? = null, @Query("query") query: String? = null): Response<ApiEnvelopeDto<List<ZoneDto>>>

    @GET("me") suspend fun getMe(): Response<ApiEnvelopeDto<PrivateProfileDto>>
    @PATCH("me") suspend fun updateMe(@Body body: UpdateMeRequestDto): Response<ApiEnvelopeDto<PrivateProfileDto>>
    @DELETE("me") suspend fun deleteMe(): Response<ApiEnvelopeDto<DeletedDto>>

    @POST("trips") suspend fun createTrip(@Header("Idempotency-Key") idempotencyKey: String, @Body body: CreateTripRequestDto): Response<ApiEnvelopeDto<BackendTripDto>>
    @GET("trips") suspend fun getTrips(@Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null, @Query("status") status: String? = null): Response<ApiEnvelopeDto<List<BackendTripDto>>>
    @GET("trips/{id}") suspend fun getTrip(@Path("id") tripId: String): Response<ApiEnvelopeDto<BackendTripDto>>
    @PATCH("trips/{id}") suspend fun updateTrip(@Path("id") tripId: String, @Body body: UpdateTripRequestDto): Response<ApiEnvelopeDto<BackendTripDto>>
    @DELETE("trips/{id}") suspend fun deleteTrip(@Path("id") tripId: String): Response<ApiEnvelopeDto<DeletedDto>>
    @POST("trips/{id}/deactivate") suspend fun deactivateTrip(@Path("id") tripId: String): Response<ApiEnvelopeDto<BackendTripDto>>
    @GET("trips/{id}/matches") suspend fun getMatches(@Path("id") tripId: String, @Query("travelDate") travelDate: String, @Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null): Response<ApiEnvelopeDto<List<BackendMatchDto>>>

    @POST("ride-requests") suspend fun createRideRequest(@Header("Idempotency-Key") idempotencyKey: String, @Body body: CreateRideRequestRequestDto): Response<ApiEnvelopeDto<BackendRideRequestDto>>
    @GET("ride-requests") suspend fun getRideRequests(@Query("box") box: String, @Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null, @Query("status") status: String? = null): Response<ApiEnvelopeDto<List<BackendRideRequestDto>>>
    @GET("ride-requests/{id}") suspend fun getRideRequest(@Path("id") requestId: String): Response<ApiEnvelopeDto<BackendRideRequestDto>>
    @POST("ride-requests/{id}/accept") suspend fun acceptRideRequest(@Path("id") requestId: String): Response<ApiEnvelopeDto<BackendRideRequestDto>>
    @POST("ride-requests/{id}/reject") suspend fun rejectRideRequest(@Path("id") requestId: String): Response<ApiEnvelopeDto<BackendRideRequestDto>>
    @POST("ride-requests/{id}/cancel") suspend fun cancelRideRequest(@Path("id") requestId: String): Response<ApiEnvelopeDto<BackendRideRequestDto>>

    @GET("coordinations") suspend fun getCoordinations(@Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null, @Query("status") status: String? = null): Response<ApiEnvelopeDto<List<BackendCoordinationDto>>>
    @GET("coordinations/{id}") suspend fun getCoordination(@Path("id") coordinationId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @GET("ride-requests/{id}/coordination") suspend fun getCoordinationForRequest(@Path("id") requestId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @POST("coordinations/{id}/cancel") suspend fun cancelCoordination(@Path("id") coordinationId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @PATCH("coordinations/{id}/meeting-point") suspend fun updateMeetingPoint(@Path("id") coordinationId: String, @Body body: UpdateMeetingPointRequestDto): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @POST("coordinations/{id}/meeting-point/confirm") suspend fun confirmMeetingPoint(@Path("id") coordinationId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @POST("coordinations/{id}/live-location/start") suspend fun startLiveLocation(@Path("id") coordinationId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>
    @POST("coordinations/{id}/live-location/end") suspend fun endLiveLocation(@Path("id") coordinationId: String): Response<ApiEnvelopeDto<BackendCoordinationDto>>

    @GET("notifications") suspend fun getNotifications(@Query("limit") limit: Int = 100, @Query("cursor") cursor: String? = null): Response<ApiEnvelopeDto<List<BackendNotificationDto>>>
    @POST("notifications/{id}/read") suspend fun markNotificationRead(@Path("id") notificationId: String): Response<ApiEnvelopeDto<BackendNotificationDto>>
    @POST("devices") suspend fun registerDevice(@Header("Idempotency-Key") idempotencyKey: String, @Body body: RegisterDeviceRequestDto): Response<ApiEnvelopeDto<RegisteredDeviceDto>>
    @DELETE("devices/{id}") suspend fun deleteDevice(@Path("id") deviceId: String): Response<ApiEnvelopeDto<DeletedDto>>
}

/** Representa una respuesta que confirma una eliminación. */
data class DeletedDto(val deleted: Boolean)
