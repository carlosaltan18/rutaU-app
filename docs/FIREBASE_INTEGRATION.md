# Integración Android, Firebase y RutaU

RutaU usa Firebase Authentication para identificar a cada estudiante. PostgreSQL,
a través de la API de NestJS, sigue siendo la fuente de verdad para perfiles,
trayectos, solicitudes, coordinaciones, plazas y notificaciones internas.

## Direcciones configuradas

- API: `https://rutau-back.onrender.com/api/v1/`
- Swagger: `https://rutau-back.onrender.com/docs`
- Proyecto Firebase: `ruta-u-ebf74`
- Realtime Database: `https://ruta-u-ebf74-default-rtdb.firebaseio.com/`

Las direcciones se declaran por variante en `app/build.gradle.kts`. No se
repiten en repositorios ni ViewModels.

## Autenticación

Los proveedores de Firebase son correo/contraseña y Google. La app nunca crea
un JWT propio ni guarda el Firebase ID token en DataStore.

Después de entrar, Android envía el token en cada llamada protegida:

```http
Authorization: Bearer <firebase-id-token>
```

Después llama `POST /auth/session`. Para una cuenta existente el cuerpo es
`{}`. Para una nueva envía `displayName`, `institutionalEmail`, `universityId`
y `campusId` obtenidos de los catálogos reales. Si la API responde 401, OkHttp
renueva el token una vez; si vuelve a fallar, se cierra la sesión local.

El formulario de registro permite crear la cuenta con correo/contraseña o con
Google. En ambos casos se solicitan los datos académicos necesarios para el
primer `POST /auth/session`.

## Respuestas de la API

Las respuestas correctas se leen como:

```json
{ "data": {}, "meta": { "requestId": "uuid", "nextCursor": null } }
```

Los errores se manejan por `error.code`, nunca por texto. Las listas siguen
`meta.nextCursor`; no usan números de página. Las cantidades monetarias son
centavos enteros de GTQ.

Las creaciones de trayectos, solicitudes y dispositivos mandan
`Idempotency-Key`. La misma clave se conserva mientras se reintenta la misma
operación.

## Catálogos y trayectos

Registro y editor de trayectos leen universidades, campus y zonas desde la
API. La pantalla conserva los UUID y muestra el nombre de cada opción. Los
roles pertenecen a un trayecto:

- `DRIVER`: capacidad de 1 a 3.
- `PASSENGER`: `seatCapacity: null`.

## FCM

El canal Android es `rutau_updates`. Al crear o renovar una sesión, Android
registra el token mediante `POST /devices`; el identificador devuelto se guarda
localmente y se elimina con `DELETE /devices/{id}` al cerrar sesión.

El backend debe enviar estos datos FCM:

```json
{
  "notificationId": "uuid",
  "type": "REQUEST_RECEIVED",
  "resourceId": "uuid",
  "destination": "RIDE_REQUEST_DETAIL"
}
```

Los valores de `type` son `REQUEST_RECEIVED`, `REQUEST_ACCEPTED`,
`REQUEST_REJECTED`, `REQUEST_CANCELLED`, `REQUEST_EXPIRED` y
`COORDINATION_CANCELLED`. Los destinos permitidos son `RIDE_REQUEST_DETAIL` y
`COORDINATION_DETAIL`. Antes de mostrar el recurso, la app vuelve a consultarlo
al servidor.

## Firestore y DataStore

Firestore se usa solo para `appMessages/{messageId}`. Android lee `title` y
`body`; utiliza el ID del documento como identificador estable y guarda en
DataStore los mensajes vistos o descartados. El cliente nunca escribe en
Firestore. No se duplican trayectos, solicitudes, coordinaciones ni plazas.

## Ubicación en vivo

Solo el conductor puede llamar:

```text
POST /coordinations/{id}/live-location/start
POST /coordinations/{id}/live-location/end
```

Después de iniciar, publica la última ubicación aproximadamente cada diez
segundos en:

```text
liveTrips/{occurrenceId}/location
```

El registro contiene `latitude`, `longitude`, `accuracy`, `bearing`, `speed` y
`updatedAt` con timestamp de servidor. Los pasajeros confirmados solo observan
esa ubicación. No se guarda historial. El servicio se detiene al finalizar,
cancelar o cerrar sesión.

## App Check

La aplicación ya está registrada en Firebase App Check. Debug instala el
proveedor Debug y release instala Play Integrity. Todavía no se debe activar
Enforce. Para un teléfono de desarrollo, registra en Firebase Console el token
de depuración que aparece en Logcat antes de probar Auth, Firestore o Realtime
Database.

## Crashlytics y distribución

Crashlytics está desactivado en debug y activado en release. Solo se registran
categorías técnicas: nunca token, correo, teléfono, contraseña, coordenadas ni
cuerpo HTTP. Para comprobarlo, instala una release interna y fuerza un cierre
de prueba controlado; el plugin adjunta el mapping al construir una release
minificada.

App Distribution entrega APK y tiene configurado el tester autorizado. No hay
publicación automática. Para distribuir una release firmada:

```text
./gradlew :app:appDistributionUploadRelease
```

No se incluyen cuentas de servicio, claves privadas, tokens ni contraseñas en
el repositorio.

## Pendiente antes de producción

- Configurar firma release y registrar sus SHA-1 y SHA-256 en Firebase.
- Probar un usuario Firebase de prueba, Google Sign-In y el arranque en frío de
  Render.
- Confirmar en dispositivos reales el token de Debug App Check y, antes de
  distribuir, Play Integrity.
- Ejecutar la matriz manual de trayectos, solicitudes, FCM y ubicación con dos
  cuentas reales.
