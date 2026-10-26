# RutaU — Requisitos actualizados de backend para NestJS

> Este documento da el contexto funcional y técnico para implementar el backend de RutaU con NestJS. Consolida los requisitos del producto, las reglas ya usadas por Android y las correcciones necesarias antes de crear la API.

## 1. Propósito y alcance

RutaU conecta a estudiantes que ya realizan trayectos similares hacia un campus. No es una aplicación de taxi, pagos, GPS, mapas, cálculo de rutas alternativas ni cálculo de desvíos.

Una persona puede crear una cuenta, publicar trayectos, buscar estudiantes compatibles, enviar solicitudes o invitaciones, aceptar o rechazar solicitudes y consultar viajes coordinados.

### Incluido en el MVP

- Cuentas de estudiante y perfil propio.
- Catálogos de universidades, campus y zonas.
- Trayectos habituales de conductor o pasajero.
- Compatibilidad por zona, campus, día y horario.
- Solicitudes, invitaciones, aceptación, rechazo, cancelación y expiración.
- Cupos por fecha concreta y coordinación de viaje.
- Bandeja interna de notificaciones.
- Contribución manual en quetzales como referencia no vinculante.

### Fuera del MVP

- Mapas, GPS, rutas alternativas, tráfico, desvíos o ubicación en tiempo real.
- Pagos, cobros, comisiones, tarifas dinámicas o marketplace comercial.
- Verificación de vehículo.
- Push obligatorio; puede agregarse más adelante.
- Rol flexible.
- Cálculo automático de precio mediante una matriz de distancias, hasta aprobación expresa.

## 2. Decisiones de producto ya aprobadas

~~~
Roles: DRIVER y PASSENGER únicamente.
Un trayecto tiene un solo rol.
Capacidad de conductor: 1 a 3 pasajeros.
Máximo total por vehículo: 4 personas, incluido el conductor.
Diferencia horaria máxima: 30 minutos, inclusive.
Una solicitud pendiente no consume cupo.
Una solicitud aceptada consume una plaza para una fecha concreta.
Moneda: GTQ.
Zona horaria de negocio: America/Guatemala.
~~~

No implementar FLEXIBLE aunque aparezca en documentos anteriores. El frontend actual y el MVP funcionan solo con conductor y pasajero.

## 3. Integración con Android

El frontend actual usa nombres de código en inglés y textos visibles en español. El backend debe permitir reemplazar los repositorios mock sin alterar las reglas de negocio.

~~~
TripRole: DRIVER, PASSENGER
RequestType: JOIN_REQUEST, DRIVER_INVITATION
RequestStatus: PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
~~~

### Tipos de solicitud

| Tipo | Emisor | Conductor | Pasajero |
| --- | --- | --- | --- |
| JOIN_REQUEST | Pasajero | targetTripId | senderTripId |
| DRIVER_INVITATION | Conductor | senderTripId | targetTripId |

La API puede guardar internamente driverTripId y passengerTripId, pero la respuesta debe incluir type, senderTripId y targetTripId.

### Privacidad

| Vista | Datos permitidos |
| --- | --- |
| Coincidencia | Nombre, foto, universidad, campus, trayecto general, razones y cupos |
| Solicitud pendiente | Datos académicos y ruta general; nunca teléfono, WhatsApp o punto exacto |
| Coordinación confirmada | Contacto autorizado de la contraparte, si fue registrado |
| Rechazada, cancelada o expirada | Nunca contacto privado |

Nunca devolver correo institucional, teléfono, matrícula, dirección, token, placa o datos de otros viajes a usuarios no autorizados.

## 4. Stack y arquitectura

Usar el proyecto NestJS existente. Antes de instalar o sustituir dependencias, revisar package.json, estructura, configuración y decisiones ya presentes en el repositorio.

### Tecnologías esperadas

- NestJS y TypeScript.
- PostgreSQL.
- Un solo ORM o query builder ya existente en el proyecto; usar migraciones versionadas.
- Validación global de DTOs.
- Autenticación Bearer JWT o proveedor administrado verificado por NestJS.
- OpenAPI o Swagger generado desde DTOs.
- Pruebas unitarias e integración con PostgreSQL aislado.

### Organización sugerida

~~~
src/
  common/
    auth/
    database/
    errors/
    pagination/
    time/
  modules/
    auth/
    students/
    catalog/
    trips/
    matching/
    requests/
    coordinations/
    notifications/
    pricing/
~~~

Cada módulo debe separar controladores, DTOs, servicios, repositorios, entidades y políticas. Los controladores no deben contener compatibilidad, transacciones ni reglas de cupos.

### Convenciones

- Identificadores y DTOs en inglés.
- Interfaz y comentarios simples en español.
- No confiar en IDs de actor enviados por el cliente.
- No usar entidades de base de datos directamente como respuesta HTTP.
- No usar float o number para persistir dinero; usar enteros en centavos.

## 5. Autenticación, cuenta y seguridad

El backend recibe un token Bearer y obtiene el actor autenticado desde ese token. Nunca acepta un studentId enviado por el cliente como prueba de identidad.

### Cuenta de estudiante

~~~
id
authSubject
displayName
photoUrl
institutionalEmail
phone                    # opcional, privado
emailVerified
universityId
campusId
accountStatus
createdAt
updatedAt
deletedAt
~~~

Estados de cuenta:

~~~
PENDING_VERIFICATION
ACTIVE
SUSPENDED
DELETED
~~~

La eliminación de cuenta es lógica. Debe cancelar o resolver de forma consistente solicitudes y coordinaciones activas antes de marcar la cuenta como eliminada.

### Seguridad obligatoria

- HTTPS fuera de local.
- Validación de DTOs en todos los endpoints.
- Autorización por recurso.
- Límite de peticiones para autenticación, búsquedas y solicitudes.
- Secretos solo en variables de entorno o gestor de secretos.
- Logs estructurados sin tokens, correos completos, teléfonos ni cuerpos sensibles.
- Fotos mediante URL https validada o almacenamiento propio.

La verificación institucional debe quedar preparada con dominios aprobados, pero no bloqueará el MVP hasta disponer de proveedor de correo y flujo real de verificación.

## 6. Modelo de datos

Usar UUID como identificadores públicos. Guardar auditoría como UTC. Evaluar fechas, próximos viajes y expiraciones en America/Guatemala.

### Catálogos

~~~
University: id, name, active, createdAt
UniversityEmailDomain: id, universityId, domain, active
Campus: id, universityId, name, active
Zone: id, name, municipality, department, active
~~~

Restricciones:

- La combinación universityId y domain es única.
- La combinación universityId y campus name es única.
- La combinación zone name, municipality y department es única.
- La base debe asegurar que el campus de un estudiante pertenezca a su universidad.

### Student

~~~
id, authSubject, displayName, photoUrl, institutionalEmail, phone,
emailVerified, universityId, campusId, accountStatus,
createdAt, updatedAt, deletedAt
~~~

### Trip

Un trayecto es habitual; no representa una salida en una fecha específica.

~~~
id
studentId
originZoneId
destinationCampusId
dayOfWeek                # ISO: 1 lunes ... 7 domingo
departureTime            # hora habitual local
role                     # DRIVER o PASSENGER
seatCapacity             # null para PASSENGER; 1..3 para DRIVER
status                   # ACTIVE, INACTIVE o DELETED
version                  # control optimista
createdAt
updatedAt
deletedAt
~~~

Reglas:

- Pasajero: seatCapacity es null.
- Conductor: seatCapacity está entre 1 y 3.
- No permitir dos trayectos activos idénticos de la misma persona.
- Trayecto inactivo o eliminado no recibe solicitudes ni aparece en coincidencias.
- No borrar físicamente trayectos con relaciones; usar baja lógica o desactivación.

### TripOccurrence

Los cupos deben manejarse por fecha, nunca como saldo global del trayecto habitual.

~~~
driverTripId
travelDate
seatCapacitySnapshot
reservedSeats
updatedAt
~~~

Clave única: driverTripId y travelDate.

Se crea bajo demanda al aceptar la primera solicitud de esa fecha. Durante la aceptación se debe bloquear esa fila.

~~~
0 <= reservedSeats <= seatCapacitySnapshot <= 3
~~~

### RideRequest

~~~
id
type                     # JOIN_REQUEST o DRIVER_INVITATION
senderTripId
targetTripId
driverTripId
passengerTripId
travelDate
requestedSeats           # MVP: siempre 1
offeredContributionMinor
currency                 # GTQ
message                  # máximo 160 caracteres luego de trim
status
pairKey
createdAt
respondedAt
cancelledAt
expiredAt
~~~

Reglas:

- Emisor y receptor deben ser estudiantes diferentes.
- El servidor resuelve driverTripId y passengerTripId con los roles y type.
- requestedSeats debe ser 1 en este MVP.
- La contribución se congela al crear la solicitud.
- pairKey ordena ambos IDs de trayecto para evitar duplicados aunque se invierta la dirección.
- Debe existir un índice único parcial de pairKey y travelDate para estados PENDING y ACCEPTED.

### Coordination

Se crea exclusivamente dentro de la transacción de aceptación.

~~~
id
rideRequestId            # único
driverStudentId
passengerStudentId
driverTripId
passengerTripId
travelDate
plannedDepartureTime
originZoneId
destinationCampusId
agreedContributionMinor
currency
status                   # CONFIRMED, CANCELLED, COMPLETED
createdAt
cancelledAt
completedAt
~~~

Guardar hora, origen y destino de coordinación evita que una edición posterior del trayecto habitual altere el historial.

### Otras tablas necesarias

El esquema de migraciones debe incluir también:

~~~
notifications
device_tokens
idempotency_keys
university_email_domains
~~~

Si se aprueba precio automático después, agregar route_price_references, pricing_configurations y fare_quotes.

## 7. Compatibilidad

Las coincidencias son cálculos derivados; no crear una tabla de matches.

Un candidato es compatible si:

1. El trayecto fuente pertenece al actor y está activo.
2. El candidato pertenece a otra persona y está activo.
3. Comparten zona de origen.
4. Comparten campus de destino.
5. Comparten día de semana.
6. La diferencia de hora es menor o igual a 30 minutos.
7. Los roles son complementarios.
8. El conductor tiene plaza disponible para la fecha consultada.

Casos mínimos:

~~~
06:30 vs 07:00 -> compatible
06:30 vs 07:01 -> no compatible
~~~

Ordenar por menor diferencia horaria, nombre visible y UUID como desempate estable.

La respuesta debe mostrar razones explícitas:

~~~
SAME_ORIGIN_ZONE
SAME_DESTINATION_CAMPUS
SAME_DAY
WITHIN_30_MINUTES
COMPLEMENTARY_ROLE
SEATS_AVAILABLE
~~~

El perfil de un candidato debe consultarse en el contexto de una coincidencia autorizada. No permitir enumerar perfiles públicos por ID sin validar relación con un trayecto del actor.

## 8. Solicitudes, coordinación y estados

### Máquina de estados

~~~
PENDING -> ACCEPTED   (solo receptor)
PENDING -> REJECTED   (solo receptor)
PENDING -> CANCELLED  (solo emisor)
PENDING -> EXPIRED    (proceso automático)
ACCEPTED -> CANCELLED (cancelación de participación o viaje)
~~~

REJECTED, CANCELLED y EXPIRED no pueden cambiar otra vez.

### Crear solicitud

- El actor es dueño de senderTripId.
- El destino pertenece a otra persona.
- Ambos trayectos están activos.
- Se recalcula compatibilidad antes de guardar.
- La fecha es futura y coincide con el día habitual de ambos trayectos.
- No existe duplicado activo para el par y fecha.
- Mensaje opcional de máximo 160 caracteres tras trim.
- Contribución no negativa en centavos GTQ.

### Aceptación atómica

Dentro de una única transacción:

1. Bloquear la solicitud pendiente.
2. Verificar que el actor es receptor y que no expiró.
3. Revalidar trayectos, roles, fecha y compatibilidad.
4. Crear o bloquear la ocurrencia del conductor para esa fecha.
5. Verificar cupo disponible.
6. Cambiar la solicitud a ACCEPTED.
7. Incrementar reservedSeats en uno.
8. Crear la coordinación CONFIRMED.
9. Crear notificaciones internas.
10. Confirmar la transacción.

Si falla un paso, no debe quedar una plaza consumida, coordinación incompleta ni solicitud aceptada parcialmente.

### Cancelaciones

- Cancelar pendiente: solo el emisor; no modifica cupos.
- Cancelar participación: el pasajero cambia solicitud y coordinación a canceladas, y libera una plaza.
- Cancelar viaje: el conductor cancela todas las coordinaciones confirmadas de su trayecto y fecha, libera todas las plazas y notifica a pasajeros.

### Expiración

Un proceso programado debe marcar como EXPIRED las solicitudes pendientes cuya fecha y hora de salida ya pasaron en America/Guatemala. Una solicitud pendiente nunca libera cupo porque nunca lo reservó.


## 9. API REST v1

Base: /api/v1.

### Convenciones generales

- JSON UTF-8.
- IDs UUID como strings.
- Fechas como YYYY-MM-DD.
- Horas como HH:mm.
- Instantes en ISO-8601 UTC.
- Dinero como Long de centavos.
- El actor siempre se resuelve desde el token.
- Listados paginados con limit y cursor.
- Creaciones aceptan el header Idempotency-Key.

Respuesta exitosa sugerida:

~~~
{
  "data": {},
  "meta": {
    "requestId": "uuid",
    "nextCursor": null
  }
}
~~~

Respuesta de error:

~~~
{
  "error": {
    "code": "REQUEST_ALREADY_EXISTS",
    "message": "Ya existe una solicitud activa para estos trayectos y fecha.",
    "fieldErrors": [],
    "requestId": "uuid"
  }
}
~~~

Android debe reaccionar al campo code, no analizar el texto del mensaje.

### Salud y autenticación

| Método | Endpoint | Propósito |
| --- | --- | --- |
| GET | /health | Estado de aplicación y base de datos |
| POST | /auth/session | Validar o intercambiar token del proveedor si aplica |
| GET | /me | Perfil privado propio |
| PATCH | /me | Editar nombre, foto, campus y teléfono opcional |
| DELETE | /me | Baja lógica de cuenta |

### Catálogos

| Método | Endpoint |
| --- | --- |
| GET | /universities?limit=&cursor= |
| GET | /universities/:id/campuses?limit=&cursor= |
| GET | /zones?municipality=&query=&limit=&cursor= |

### Trayectos y coincidencias

| Método | Endpoint | Propósito |
| --- | --- | --- |
| POST | /trips | Crear trayecto habitual |
| GET | /trips?status=&limit=&cursor= | Listar trayectos propios |
| GET | /trips/:id | Ver trayecto propio |
| PATCH | /trips/:id | Editar trayecto propio usando versión |
| POST | /trips/:id/deactivate | Desactivar trayecto |
| DELETE | /trips/:id | Baja lógica solo si no rompe relaciones |
| GET | /trips/:id/matches?travelDate=YYYY-MM-DD&limit=&cursor= | Buscar compatibles |
| GET | /trips/:sourceTripId/matches/:candidateTripId | Perfil compatible autorizado |

El PATCH debe recibir version o usar If-Match. Si el dato cambió debe devolver 409 TRIP_VERSION_CONFLICT.

Crear trayecto:

~~~
{
  "originZoneId": "uuid",
  "destinationCampusId": "uuid",
  "dayOfWeek": 1,
  "departureTime": "06:30",
  "role": "PASSENGER",
  "seatCapacity": null,
  "version": 0
}
~~~

Una coincidencia debe incluir candidateTripId, perfil público, rol, hora, diferencia, razones, plazas disponibles para travelDate y canRequest. No incluir datos privados.

### Cotización, fase posterior

| Método | Endpoint | Propósito |
| --- | --- | --- |
| POST | /fare-quotes | Obtener contribución sugerida si existe matriz aprobada |

Si no hay una matriz confiable, responder PRICE_REFERENCE_UNAVAILABLE y permitir oferta manual. Nunca inventar distancia ni precio.

### Solicitudes

| Método | Endpoint | Actor |
| --- | --- | --- |
| POST | /ride-requests | Emisor |
| GET | /ride-requests?box=sent|received&status=&limit=&cursor= | Participante |
| GET | /ride-requests/:id | Participante |
| POST | /ride-requests/:id/accept | Receptor |
| POST | /ride-requests/:id/reject | Receptor |
| POST | /ride-requests/:id/cancel | Emisor mientras está pendiente |

Crear solicitud:

~~~
{
  "senderTripId": "uuid",
  "targetTripId": "uuid",
  "type": "JOIN_REQUEST",
  "travelDate": "2026-10-12",
  "offeredContributionMinor": 1000,
  "currency": "GTQ",
  "message": "¿Podemos coordinar el punto de encuentro?"
}
~~~

La respuesta de detalle de solicitud debe incluir:

- id, type, status, travelDate, mensaje y contribución.
- senderTrip y targetTrip con ruta, día, hora y rol.
- sender y target con perfil público limitado.
- diferencia de horario y cupos disponibles, calculados para la fecha.
- coordinación resumida solo si está aceptada y el actor es participante.
- contacto solo si existe coordinación confirmada.

### Coordinaciones

| Método | Endpoint | Actor |
| --- | --- | --- |
| GET | /coordinations?status=&limit=&cursor= | Participante |
| GET | /coordinations/:id | Participante |
| GET | /ride-requests/:id/coordination | Participante |
| POST | /coordinations/:id/cancel | Conductor o pasajero participante |

El servicio decide si la acción significa cancelar la participación de una persona o todo el viaje, según el rol del actor.

### Notificaciones

| Método | Endpoint |
| --- | --- |
| GET | /notifications?cursor=&limit= |
| POST | /notifications/:id/read |
| POST | /devices |
| DELETE | /devices/:id |

## 10. Validaciones y errores

### Validaciones

#### Perfil

- displayName recortado, entre 2 y 120 caracteres.
- institutionalEmail normalizado en minúsculas, único y privado.
- Campus existente y perteneciente a la universidad seleccionada.
- Teléfono opcional y privado.

#### Trayecto

- Zona y campus existentes y activos.
- dayOfWeek entre 1 y 7.
- departureTime válida.
- DRIVER requiere seatCapacity entre 1 y 3.
- PASSENGER requiere seatCapacity null.
- No permitir trayectos activos idénticos del mismo estudiante.
- Si tiene coordinaciones confirmadas, no permitir reducir capacidad por debajo de reservas de futuras ocurrencias.

#### Solicitud

- travelDate futura y compatible con dayOfWeek.
- Mensaje opcional de máximo 160 caracteres después de trim.
- Contribución mayor o igual a cero, en GTQ y centavos implícitos.
- quoteId, si se habilita, no vencido y correspondiente a ambos trayectos.
- El backend no acepta actorId ni IDs de otra persona como sustituto del token.

### Errores requeridos

| HTTP | Código | Caso |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Datos inválidos |
| 401 | UNAUTHENTICATED | Token ausente o inválido |
| 403 | FORBIDDEN | Actor sin permiso |
| 404 | TRIP_NOT_FOUND | Trayecto inexistente o no visible |
| 404 | REQUEST_NOT_FOUND | Solicitud inexistente o no visible |
| 404 | COORDINATION_NOT_FOUND | Coordinación inexistente o no visible |
| 409 | REQUEST_ALREADY_EXISTS | Solicitud activa duplicada |
| 409 | REQUEST_NOT_PENDING | Transición inválida |
| 409 | NO_LONGER_COMPATIBLE | Los trayectos ya no coinciden |
| 409 | NO_SEATS_AVAILABLE | No quedan plazas para esa fecha |
| 409 | TRIP_VERSION_CONFLICT | Edición con versión obsoleta |
| 409 | TRIP_HAS_ACTIVE_RELATIONS | Intento de borrar trayecto relacionado |
| 410 | REQUEST_EXPIRED | La ocurrencia ya pasó |
| 422 | TRAVEL_DATE_DAY_MISMATCH | Fecha no coincide con el día habitual |
| 422 | PRICE_REFERENCE_UNAVAILABLE | No hay cotización automática |
| 429 | RATE_LIMITED | Límite excedido |
| 500 | INTERNAL_ERROR | Error interno no expuesto |

## 11. Restricciones, concurrencia e idempotencia

### Índices y restricciones importantes

- students.authSubject único.
- Correo institucional único entre cuentas activas, según la política de baja lógica elegida.
- Índice de coincidencias por originZoneId, destinationCampusId, dayOfWeek y departureTime para trayectos activos.
- Índice de trayectos propios por studentId y status.
- Índice único parcial de solicitudes activas por pairKey y travelDate.
- Índice único de coordination.rideRequestId.
- Índice único de TripOccurrence por driverTripId y travelDate.
- Restricciones para cupos entre 1 y 3 y reservas entre 0 y la capacidad.

### Idempotencia

Las operaciones de creación deben aceptar Idempotency-Key:

- Crear trayecto.
- Crear solicitud.
- Crear cotización, si existe.
- Registrar token de dispositivo, si existe.

Guardar llave, actor, endpoint, hash de carga, respuesta y vencimiento. Misma llave con misma carga devuelve la respuesta original; misma llave con otra carga devuelve conflicto.

### Concurrencia

La aceptación debe bloquear la solicitud y la fila TripOccurrence. Dos aceptaciones simultáneas para la última plaza no pueden terminar ambas aceptadas.

El control optimista de Trip.version evita que una edición silenciosamente reemplace cambios recientes.

## 12. Notificaciones y procesos programados

Eventos mínimos para bandeja interna:

~~~
REQUEST_RECEIVED
REQUEST_ACCEPTED
REQUEST_REJECTED
REQUEST_CANCELLED
REQUEST_EXPIRED
COORDINATION_CANCELLED
~~~

Las notificaciones se crean dentro de la transacción que cambia el estado de negocio. Si después se agrega push, usar outbox y enviarlo luego del commit.

Procesos programados requeridos:

- Expirar solicitudes pendientes cuya salida ya pasó.
- Opcionalmente marcar coordinaciones pasadas como COMPLETED, sin alterar información histórica.

## 13. Variables de entorno

~~~
NODE_ENV
PORT
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD
AUTH_ISSUER
AUTH_AUDIENCE
AUTH_JWKS_URL
JWT_SECRET
CORS_ALLOWED_ORIGINS
RATE_LIMIT_SEARCH_PER_MINUTE
RATE_LIMIT_REQUEST_PER_HOUR
STORAGE_PUBLIC_BASE_URL
FCM_PROJECT_ID
FCM_CREDENTIALS_JSON
~~~

JWT_SECRET solo aplica si NestJS emite JWT propios. Las variables FCM solo aplican si se habilita push. Nunca guardar secretos reales en Git ni dentro del APK Android.


## 14. Pruebas obligatorias

### Unitarias

- Diferencia exacta de 30 minutos compatible.
- Diferencia de 31 minutos incompatible.
- Zona, campus o día distintos incompatibles.
- Autoexclusión.
- Roles iguales incompatibles.
- Conductor sin plazas no aparece como compatible.
- Validación de capacidades 1, 2 y 3.
- Solicitud pendiente sin consumo de cupo.
- Todas las transiciones permitidas y prohibidas.
- Contribución almacenada como centavos.
- Perfil público sin correo ni teléfono.
- Contacto solamente en coordinación confirmada y para participantes.

### Integración con PostgreSQL

- Migraciones desde base vacía.
- Restricción de solicitud duplicada.
- Dos aceptaciones simultáneas para la última plaza.
- Rollback si falla la creación de coordinación.
- Cancelar participación libera una plaza.
- Cancelar viaje del conductor libera todas las plazas de esa fecha.
- Terceros no pueden leer, aceptar o cancelar recursos ajenos.
- Conflicto de versión al editar un trayecto.
- Expiración según America/Guatemala.
- Idempotency-Key no crea registros duplicados.

### Contrato

- Swagger u OpenAPI generado y validado en CI.
- DTOs con ejemplos, paginación y errores documentados.
- Pruebas de contrato para que Android distinga solicitud, invitación, estados y coordinación.

## 15. Datos semilla

Usar únicamente datos ficticios:

- Universidad, campus y zonas usadas en la demostración.
- Dominios institucionales ficticios.
- Estudiantes de demostración claramente identificados.
- Un pasajero con trayecto lunes 06:30.
- Un conductor con trayecto martes 07:00 y capacidad 3.
- Conductores y pasajeros compatibles con diferencias de 0, 15 y 30 minutos.
- Solicitudes en estados pendiente, aceptada, rechazada, cancelada y expirada.
- Un viaje parcial con 2 de 3 plazas ocupadas.
- Un viaje completo con 3 de 3 plazas ocupadas.

No usar datos reales de estudiantes sin consentimiento.

## 16. Orden recomendado de implementación

1. Inspeccionar el proyecto Nest existente y no reinicializar ni sustituir su configuración sin necesidad.
2. Configurar PostgreSQL, migraciones, validación global, manejo uniforme de errores y Swagger.
3. Implementar autenticación, guard y resolución del actor actual.
4. Crear catálogos de universidades, campus, dominios y zonas.
5. Implementar perfil propio y DTO público con privacidad.
6. Implementar Trip con capacidad 1 a 3, baja lógica y control de versión.
7. Implementar CompatibilityPolicy y sus pruebas.
8. Implementar RideRequest, deduplicación e idempotencia.
9. Implementar aceptación transaccional, TripOccurrence y Coordination.
10. Implementar rechazo, cancelación, expiración y notificaciones internas.
11. Validar el contrato OpenAPI con Android.
12. Agregar cotizaciones, push y verificación institucional solo si se aprueban.

## 17. Definición de terminado del backend MVP

El backend está listo para integrar cuando:

- Las migraciones levantan una base vacía sin pasos manuales.
- El actor autenticado no puede suplantarse con IDs enviados por el cliente.
- Trayectos, coincidencias, solicitudes y coordinaciones funcionan con PostgreSQL.
- Se respeta la diferencia máxima de 30 minutos y los roles complementarios.
- No se sobrevende una plaza bajo concurrencia.
- Una solicitud pendiente no consume cupos.
- Aceptar crea una coordinación y reserva una plaza de la fecha correcta.
- Cancelar una participación o viaje libera los cupos correctos.
- Contacto privado aparece solo después de aceptar y solo a participantes.
- OpenAPI describe DTOs, paginación y errores reales.
- Existen pruebas unitarias, de integración y de autorización para los flujos críticos.
- Android puede sustituir los repositorios fake por clientes HTTP contra un ambiente de prueba.

## 18. Instrucciones para el agente que implemente NestJS

- Trata este documento como requisitos de producto y arquitectura; no como permiso para borrar o reinicializar el proyecto existente.
- Primero inspecciona el repositorio, dependencias y configuración actual.
- Implementa por fases y verifica cada fase con pruebas.
- No agregues rol FLEXIBLE, mapas, rutas alternativas, pagos, precios dinámicos o verificación vehicular.
- No expongas datos privados en coincidencias o solicitudes pendientes.
- No uses float o number para persistir dinero.
- No consumas cupos al crear una solicitud pendiente.
- No implementes aceptación sin transacción y bloqueo de cupo por fecha.
- Mantén OpenAPI actualizado a medida que se agreguen endpoints.
- Si una decisión no está definida en este documento, detente y pide aclaración antes de inventar una regla de negocio.

