# RutaU Android

Aplicación Android nativa de RutaU. Permite a estudiantes publicar sus trayectos habituales, encontrar compatibilidades, enviar solicitudes o invitaciones y coordinar viajes. Se conecta a la API NestJS de RutaU, usa Firebase para identidad, avisos y datos en tiempo real, y conserva únicamente preferencias locales con DataStore.

> La fuente de verdad de perfiles, trayectos, plazas, solicitudes, coordinaciones y notificaciones es PostgreSQL a través de la API. Firestore no duplica esos datos.

## Arquitectura

RutaU se mantiene en un único módulo Android (`app`) organizado por funcionalidad. Cada pantalla usa un `ViewModel`; este llama a una interfaz de repositorio; y el repositorio usa Firebase, la API o DataStore según corresponda. Las pantallas no conocen Retrofit ni las clases de red directamente.

```text
Pantalla Compose
      |
ViewModel y estado de pantalla
      |
Repositorio (contrato de dominio)
      |
+------------------+--------------------+-------------------+
| API NestJS       | Firebase            | DataStore         |
| Retrofit/OkHttp  | Auth, FCM,          | tema y estado     |
| PostgreSQL       | Firestore, RTDB     | local de avisos   |
+------------------+--------------------+-------------------+
```

Las pantallas y componentes visuales muestran texto en español. Los identificadores de Kotlin, rutas y modelos se mantienen en inglés para que el código sea consistente. Cada pantalla con lógica recibe un estado inmutable desde un `ViewModel` y emite acciones mediante callbacks; un composable no consulta Firebase, Retrofit, DataStore ni Room directamente.

Las listas principales usan estados visibles para evitar pantallas bloqueadas cuando falla la red:

- `Loading`: la consulta todavía está en progreso.
- `Content`: existen datos para mostrar.
- `Empty`: la consulta terminó sin datos para la sección seleccionada.
- `Error`: se informa el problema y se ofrece un botón **Reintentar**.

### Flujo principal

1. La persona inicia sesión con correo/contraseña o Google usando Firebase Authentication.
2. `FirebaseSessionRepository` obtiene el ID token de Firebase y llama a `POST /auth/session` para crear o recuperar el perfil de RutaU.
3. Las pantallas autenticadas leen y escriben información mediante repositorios remotos, que consumen la API NestJS.
4. Cuando hay una coordinación aceptada, conductor y pasajero ven el detalle del viaje. Solo el conductor puede iniciar el viaje y compartir ubicación.
5. Al cerrar sesión se elimina el registro FCM del dispositivo, se detiene el servicio de ubicación y se limpia el historial de navegación.

## Estructura del proyecto

```text
app/src/main/java/uvg/edu/rutau/
├── app/                         Shell y navegación global
├── core/                        Código compartido entre funcionalidades
├── feature/                     Pantallas y ViewModels por funcionalidad
├── ui/theme/                    Tema visual de RutaU
├── MainActivity.kt              Punto de entrada de la interfaz
└── RutaUApplication.kt          Inicializa Firebase y servicios compartidos
```

### `app/`: el contenedor de la aplicación

| Archivo | Responsabilidad |
| --- | --- |
| `RutaUApp.kt` | Une tema, `Scaffold`, navegación, diálogos de avisos y permiso de notificaciones. También consulta avisos internos mientras la app está visible. |
| `RutaUAppState.kt` | Conserva el `NavController`, cambia entre las tres secciones principales y abre el recurso indicado por una notificación. |
| `RutaUNavHost.kt` | Declara todas las rutas tipadas de Navigation Compose. |
| `RutaUScaffold.kt` | Aplica de forma consistente barra superior y barra inferior. |
| `RutaUBottomNavigation.kt` | Navega a **Trayectos**, **Solicitudes** y **Cuenta** conservando el estado de cada sección. |
| `RutaUAppDependencies.kt` | Crea las implementaciones reales de repositorios. Es el punto temporal de composición de dependencias; evita crear clientes de red dentro de pantallas. |

### `feature/`: funcionalidades de interfaz

| Paquete | Contenido |
| --- | --- |
| `feature/auth/` | Inicio de sesión, registro, Google Sign-In, recuperación y restablecimiento de contraseña. `LoginViewModel`, `SignUpViewModel`, `PasswordRecoveryViewModel` y `ResetPasswordViewModel` contienen la lógica de cada recorrido. |
| `feature/account/` | Perfil, cambio de foto, catálogos de universidad/campus, credenciales, preferencias, tema y documentos legales. |
| `feature/trips/` | Lista y edición de trayectos base, coincidencias, perfil compatible, explicación de compatibilidad y confirmación de solicitud/invitación. |
| `feature/requests/` | Bandeja de recibidas/enviadas e historial, detalle, decisiones sobre solicitudes y detalle del viaje coordinado. |

Cada funcionalidad usa normalmente cuatro archivos: `*Screens.kt` para la UI, `*ViewModels.kt` para estado y acciones, `*Navigation.kt` para enlazar rutas, y `*Previews.kt` para verificar los composables sin abrir toda la app.

### `core/`: código reutilizable

| Paquete | Para qué sirve |
| --- | --- |
| `core/model/` | Modelos de dominio compartidos: estudiante, trayecto, solicitud, coordinación, roles y estados. No contiene DTO de Retrofit. |
| `core/data/repository/` | Contratos de repositorio e implementaciones reales. `RemoteTripRepository`, `RemoteRideRequestRepository` y `RemoteCoordinationRepository` hablan con la API; `FirebaseSessionRepository` y `FirebaseUserRepository` coordinan Auth y perfil. Las clases `Fake*` y `MockRutaUStore` se conservan solo para pruebas y previews. |
| `core/data/catalog/` | Lee universidades, campus y zonas de la API y transforma identificadores en nombres visibles. |
| `core/network/` | `BackendApi` define Retrofit; DTOs, mapeadores, envolturas de respuesta, token Firebase e idempotencia viven aquí. |
| `core/auth/` | `GoogleSignInCoordinator` abre Credential Manager y devuelve el ID token de Google que consume Firebase Auth. |
| `core/notifications/` | Registro de dispositivos FCM, recepción de push, canal Android, aviso del sistema, popup dentro de la app y lectura de notificaciones internas del backend. |
| `core/preferences/` | Repositorios DataStore para tema y anuncios Firestore ya vistos. `PopupMessageViewModel` decide qué anuncio mostrar, guarda la interacción y pide la navegación; `PopupMessageHost` solo dibuja el diálogo. |
| `core/location/` | Permisos, servicio foreground y repositorio de Firebase Realtime Database para ubicación durante un viaje activo. |
| `core/firebase/` | Configura Firebase App Check según el tipo de compilación. |
| `core/diagnostics/` | Reporta errores técnicos no fatales a Crashlytics sin enviar datos personales. |
| `core/designsystem/component/` | Botones, campos, listas, barra superior/inferior, mapa y demás composables reutilizables de RutaU. |
| `core/navigation/` | Rutas serializables. Las rutas llevan IDs, no objetos enteros. |

### Tema visual

`ui/theme/` define `Color.kt`, `SemanticColors.kt`, `Type.kt`, `Shapes.kt` y `Dimensions.kt`. `RutaUTheme` aplica el tema claro, oscuro o del sistema. La preferencia se guarda localmente con `ThemePreferenceRepository`.

## Servicios Firebase

El proyecto Firebase configurado es `ruta-u-ebf74`. El archivo `app/google-services.json` debe corresponder a la aplicación Android con `applicationId` `com.apprutau.rutau`.

### Firebase Authentication

La app admite correo y contraseña, además de Google mediante Credential Manager y Firebase Auth. El recorrido está detrás de `SessionRepository` y su implementación real es `FirebaseSessionRepository`:

```text
Correo/Google -> FirebaseAuth -> Firebase ID token
                         |             |
                         +----> POST /auth/session
                                         |
                                  perfil de RutaU listo
```

No se generan JWT propios ni se guarda el ID token en DataStore. `FirebaseAuth` conserva la sesión de forma segura. `FirebaseIdTokenProvider` obtiene el token cuando OkHttp necesita una llamada protegida.

Si la API responde `401`, `FirebaseTokenAuthenticator` solicita una renovación del token una sola vez y repite la llamada. Si el token renovado también es rechazado, se cierra la sesión local para evitar una cuenta inválida.

Después de autenticar con Firebase, siempre se llama a `POST /auth/session`:

- una cuenta existente puede enviar datos vacíos o de perfil ya conocidos;
- una cuenta nueva aporta nombre, correo institucional, universidad y campus;
- el servidor identifica a la persona por el `uid` validado, no por un ID que mande Android.

El registro valida nombre, universidad, campus, correo, contraseña, confirmación y aceptación de términos en cada cambio. **Crear cuenta** solo se habilita cuando todo el formulario es válido; el botón de Google valida únicamente los datos académicos y términos necesarios para ese proveedor.

### Cloud Firestore: anuncios generales dentro de la app

Firestore se utiliza solamente para avisos generales en la colección `appMessages`; no contiene trayectos, solicitudes, plazas ni coordinaciones. La implementación está en `FirestorePopupMessageFeedRepository`.

Cada documento válido tiene este formato:

| Campo | Tipo | Uso |
| --- | --- | --- |
| `title` | texto | Título del popup. |
| `body` | texto | Mensaje visible. |
| `active` | booleano | Debe ser `true` para mostrarlo. |
| `audience` | texto | `ALL`, `UNIVERSITY:<uuid>` o `CAMPUS:<uuid>`. |
| `priority` | texto | `LOW`, `NORMAL` o `HIGH`. |
| `actionType` | texto | `NONE`, `OPEN_PROFILE`, `OPEN_TRIPS` u `OPEN_REQUESTS`. |
| `actionValue` | texto o `null` | Valor adicional reservado para acciones futuras. |
| `minAppVersionCode` | número | Versión mínima que puede mostrarlo. |
| `startsAt` / `endsAt` | Timestamp o ausente | Ventana opcional de publicación. |
| `createdAt` | Timestamp | Ordena avisos de igual prioridad. |

La app observa cambios en tiempo real, filtra por audiencia, versión y fechas, y muestra un solo anuncio a la vez. Un usuario autenticado solo necesita permiso de lectura para `appMessages`. `PopupMessageViewModel` recibe esta lista desde el repositorio y mantiene la lógica de mostrar, descartar y abrir un anuncio fuera de Compose.

Al mostrar, cerrar o abrir un anuncio, Android crea un evento inmutable en `appMessageEvents`. El evento contiene únicamente el UID de Firebase, ID del anuncio, tipo (`SHOWN`, `DISMISSED` u `OPENED`), versión de la app y timestamp de servidor. Después de escribirlo, el repositorio lee ese mismo documento y lo convierte a `AppPopupMessageEvent` para confirmar la operación sin exponer tipos de Firebase al dominio. Cada cliente solo puede leer por ID sus propios eventos; no puede listar, editar ni borrar eventos. La operación no bloquea la interfaz si no hay red y no contiene correo, ubicación, solicitudes ni trayectos. Las reglas versionadas están en [`firebase/firestore.rules`](firebase/firestore.rules).

### Firebase Cloud Messaging (FCM)

FCM se usa para eventos personales generados por el backend: solicitud recibida, aceptación, rechazo, cancelación, expiración y cancelación de una coordinación.

1. `BackendDeviceRegistrationRepository` obtiene el token de FCM y lo registra mediante `POST /devices` después de crear la sesión del backend.
2. `RutaUFirebaseMessagingService.onNewToken` vuelve a registrarlo al cambiar.
3. `RutaUFirebaseMessagingService.onMessageReceived` procesa `notificationId`, `type`, `resourceId` y `destination`.
4. `RutaUNotificationManager` publica un aviso del sistema en el canal `rutau_updates` cuando Android entrega el mensaje a la app.
5. Al tocarlo, se abre el detalle de solicitud o coordinación y la pantalla vuelve a consultar la API; el push no se toma como información definitiva.

Android 13 o superior exige aceptar el permiso de notificaciones. La app lo solicita después de iniciar sesión. Cuando llega una notificación con la app abierta también se muestra un popup dentro de RutaU. Como respaldo, la app consulta las notificaciones internas del backend cada 30 segundos mientras está en primer plano.

> Los anuncios de Firestore y los push FCM son distintos: Firestore muestra información general dentro de la app; FCM entrega eventos personales desde NestJS al teléfono.

### Realtime Database: ubicación en vivo

Solo se guarda la última ubicación durante un viaje activo, en:

```text
liveTrips/{occurrenceId}/location
```

El conductor inicia la sesión con la API, ejecuta `LiveTripLocationService` como servicio foreground y publica aproximadamente cada 10 segundos. Los pasajeros confirmados solamente escuchan los cambios. Al terminar, cancelar o cerrar sesión se detiene el servicio y se borra el último punto desde Android; el backend también elimina la ocurrencia al cerrar el viaje.

Los campos enviados son `latitude`, `longitude`, `accuracy`, `bearing`, `speed` y `updatedAt` con timestamp de servidor. Las reglas están en [`firebase/database.rules.json`](firebase/database.rules.json): solo miembros confirmados leen y únicamente el UID del conductor escribe.

`RutaULiveMap` muestra el mapa con OpenStreetMap y osmdroid, sin una API key de Google Maps ni facturación de Google Cloud.

### App Check

`FirebaseAppCheckSetup` usa el proveedor Debug para compilaciones `debug` y Play Integrity para `release`. El enforcement debe mantenerse desactivado hasta validar métricas de dispositivos reales. Para APK distribuidas fuera de Google Play no se debe exigir la etiqueta `PLAY_RECOGNIZED`.

### Crashlytics

Crashlytics se inicializa en `RutaUApplication`. `SafeCrashReporter` registra solo una categoría técnica de error; no manda tokens, contraseñas, correos, teléfonos ni coordenadas. La recopilación está activada por build type mediante `CRASHLYTICS_COLLECTION_ENABLED`.

Para confirmar la integración antes de una entrega, agregue temporalmente un botón o una llamada de prueba que lance una excepción, instale la app, provoque el error y vuelva a abrirla. Después elimine esa prueba. Nunca debe dejarse un crash intencional en una versión distribuida.

## Comunicación con el backend

### Dirección y configuración de red

`app/build.gradle.kts` genera estas variables `BuildConfig`:

| Variable | Valor actual |
| --- | --- |
| `API_BASE_URL` | `https://rutau-back.onrender.com/api/v1/` |
| `REALTIME_DATABASE_URL` | `https://ruta-u-ebf74-default-rtdb.firebaseio.com/` |
| `CRASHLYTICS_COLLECTION_ENABLED` | `true` |

Actualmente `debug` y `release` usan la API desplegada. Para probar una API local desde el emulador, la URL es `http://10.0.2.2:3000/api/v1/`; en ese caso hay que habilitar tráfico HTTP solo para el build local de desarrollo.

### Retrofit y manejo de errores

`RutaUNetworkClient` crea Retrofit y OkHttp. `BackendApi` declara endpoints, `BackendNetworkModels.kt` contiene DTOs de red y `BackendMappers.kt` los convierte a modelos de `core/model`.

Todas las respuestas exitosas del backend siguen esta forma:

```json
{
  "data": {},
  "meta": { "requestId": "...", "nextCursor": null }
}
```

Los errores usan `error.code`. Las pantallas muestran errores entendibles, pero las decisiones de lógica se toman con el código, no comparando el texto.

Las listas utilizan `meta.nextCursor`; no hay paginación por número. La contribución simulada se transmite en centavos enteros de GTQ, nunca como `Float` o `Double`. Las operaciones de creación que admite el servidor usan `IdempotencyKeyStore` para reutilizar una misma clave al reintentar una operación.

## Datos locales y privacidad

### DataStore

DataStore no reemplaza la base de datos remota. Solo almacena:

| Archivo | Archivo local lógico | Información guardada |
| --- | --- | --- |
| `ThemePreferenceRepository.kt` | `rutau_theme` | `SYSTEM`, `LIGHT` o `DARK`. |
| `PopupMessageRepository.kt` | `rutau_popup_messages` | IDs de anuncios vistos, fecha de visualización y preferencia de popups. |

No se almacenan ID tokens, contraseñas, perfiles, coordenadas ni objetos de negocio sensibles en DataStore.

### Permisos Android

El `AndroidManifest.xml` declara internet, notificaciones, ubicación aproximada y precisa, y servicio foreground de ubicación. La aplicación solicita cada permiso cuando hace falta: notificaciones al iniciar sesión y ubicación al iniciar un viaje como conductor.

## Configuración y ejecución

### Requisitos

- Android Studio actualizado con JDK integrado (JDK 17 o superior; JDK 21 está probado en el proyecto).
- Android SDK Platform 37.1.
- Un dispositivo o emulador con Android 13 / API 33 o superior.
- Acceso al proyecto Firebase `ruta-u-ebf74` para habilitar proveedores de inicio de sesión y consultar Crashlytics/Firestore si corresponde.

### Configuración relevante

| Propiedad | Valor |
| --- | --- |
| `applicationId` | `com.apprutau.rutau` |
| `namespace` Kotlin | `uvg.edu.rutau` |
| `minSdk` | 33 |
| `targetSdk` | 37 |
| `compileSdk` | 37.1 |
| `versionCode` | 1 |
| Distribución configurada | APK con Firebase App Distribution |

1. Abra este proyecto en Android Studio y espere la sincronización de Gradle.
2. Confirme que `app/google-services.json` está presente y pertenece al proyecto Firebase correcto.
3. En Firebase Authentication, habilite **Email/Password** y **Google**.
4. Registre los SHA-1/SHA-256 de la firma que use para probar Google Sign-In.
5. Seleccione un dispositivo, acepte permisos al ejecutar y pruebe una cuenta Firebase real.

Compilación desde PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat :app:assembleDebug
```

La APK queda en `app/build/outputs/apk/debug/`.

Para distribuir una APK mediante Firebase App Distribution se debe usar una cuenta con acceso a Firebase y ejecutar la tarea correspondiente después de compilar. No se guardan credenciales de distribución en este repositorio.

## Pruebas y diagnóstico

Pruebas unitarias y compilación Kotlin:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest
```

Pruebas instrumentadas, con dispositivo o emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Lint:

```powershell
.\gradlew.bat :app:lintDebug
```

Las pruebas, mocks y previews permiten comprobar UI y mapeos sin depender de Render. Las pruebas manuales contra servicios reales deberían cubrir como mínimo inicio de sesión, renovación del ID token, trayectos, solicitudes, registro FCM, anuncio de Firestore, ubicación con dos cuentas, falta de red y primer inicio lento del servicio gratuito de Render.

Para verificar el comportamiento sin conexión, active el modo avión con la aplicación abierta. Trayectos y Solicitudes deben mostrar un estado de error con **Reintentar**, o datos que ya estén disponibles en memoria, sin cerrar la aplicación ni dejar un indicador de carga permanente.

## Límites de responsabilidad

- Android presenta y solicita datos; no decide cupos ni estados de negocio.
- NestJS/PostgreSQL valida roles, plazas, solicitudes y coordinaciones.
- Firebase Auth prueba la identidad; Firebase Realtime Database comparte solo ubicación activa; Firestore sirve anuncios generales; FCM entrega eventos personales.
- No añadir servicios Firebase nuevos ni campos de API sin actualizar primero el contrato OpenAPI y las reglas de seguridad correspondientes.
