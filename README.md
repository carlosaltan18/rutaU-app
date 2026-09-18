# RutaU

Aplicación Android nativa para que estudiantes universitarios encuentren compañeros compatibles y coordinen trayectos habituales hacia su campus.

> Estado: MVP de frontend en desarrollo. La infraestructura, autenticación y cuenta del integrante 1 están implementadas. Los módulos de Trayectos, compatibilidad y Solicitudes se integrarán desde los trabajos de los integrantes 2 y 3.

## Funcionalidades disponibles

- Inicio de sesión con sesión local.
- Registro de cuenta con foto opcional y catálogo de universidad/campus.
- Recuperación y restablecimiento simulado de contraseña.
- Perfil de cuenta: datos personales, foto, correo, contraseña, notificaciones y acciones legales.
- Confirmación para cerrar sesión y eliminación de cuenta.
- Navegación tipada con Navigation Compose y barra inferior persistente.
- Sistema visual RutaU basado en Material 3: colores, tipografía, espaciados, formas y componentes reutilizables.
- Datos compartidos en memoria mediante repositorios fake para el MVP.
- Publicación y edición de trayectos como pasajero o conductor (de 1 a 3 plazas).
- Compatibilidad por zona, campus, día, diferencia máxima de 30 minutos y roles complementarios.
- Perfiles públicos de candidatos y confirmación de solicitudes o invitaciones pendientes.

## Alcance actual

| Área | Estado |
| --- | --- |
| Diseño, tema y componentes reutilizables | Implementado |
| Modelos, rutas tipadas y `RutaUNavHost` | Implementado |
| Autenticación y recuperación de contraseña | Implementado con datos locales |
| Cuenta y configuración | Implementado con datos locales |
| Trayectos y compatibilidad | Implementado con datos locales |
| Solicitudes y viajes coordinados | Pendiente de integración del integrante 3 |
| Persistencia remota, backend y notificaciones reales | Fuera del alcance actual del frontend MVP |

## Tecnologías

- Kotlin y Jetpack Compose.
- Material 3.
- Navigation Compose con rutas serializables.
- ViewModel, StateFlow y Lifecycle.
- Coil para imágenes de perfil.
- JUnit y Compose UI Test.
- Gradle 9.4.1 y Android Gradle Plugin 9.2.1.

## Requisitos

- Android Studio actualizado con su JDK integrado (JDK 17 o superior).
- Android SDK Platform 37.1 instalado.
- Emulador o dispositivo con Android API 33 o superior.

Configuración Android actual:

| Propiedad | Valor |
| --- | --- |
| `applicationId` | `uvg.edu.rutau` |
| `minSdk` | 33 |
| `targetSdk` | 36 |
| `compileSdk` | 37.1 |

## Ejecutar el proyecto

1. Clona el repositorio y ábrelo desde Android Studio.
2. Permite que Gradle sincronice las dependencias.
3. Selecciona un emulador o dispositivo compatible.
4. Ejecuta la configuración `app`.

También puedes compilar desde PowerShell en Windows:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

La APK generada estará en `app/build/outputs/apk/debug/`.

## Credenciales del mock

El MVP utiliza datos en memoria, por lo que se reinician al cerrar la aplicación o al reinstalarla.

| Campo | Valor |
| --- | --- |
| Correo | `mateo@ejemplo.com` |
| Contraseña | `RutaU123` |

También se puede crear una cuenta nueva desde la pantalla de registro.

## Pruebas

Pruebas unitarias:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

Pruebas instrumentadas, con un emulador o dispositivo conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Las pruebas de interfaz cubren estos recorridos:

- Inicio de sesión hacia Trayectos.
- Registro hacia Trayectos.
- Recuperación hacia Restablecer contraseña mediante el enlace de demostración.
- Cierre de sesión que limpia el historial.
- Restauración de estado al cambiar en la barra inferior.
- Actualización de datos de Cuenta en el mock compartido.

## Estructura del proyecto

```text
app/src/main/java/uvg/edu/rutau/
├── app/                  # Shell, scaffold, estado y navegación global
├── core/
│   ├── data/             # Catálogos, mocks y contratos/repositorios fake
│   ├── designsystem/     # Componentes reutilizables de RutaU
│   ├── model/            # Modelos compartidos
│   └── navigation/       # Contrato de rutas tipadas
├── feature/
│   ├── account/          # Cuenta y configuración
│   ├── auth/             # Inicio de sesión, registro y recuperación
│   └── trips/            # Trayectos, compatibilidad, perfiles y confirmación
└── ui/theme/             # Colores, tipografía, espaciado y formas
```

## Convenciones de integración

- El código fuente usa identificadores en inglés; la interfaz visible permanece en español.
- Las rutas de navegación transportan solo IDs, nunca objetos completos.
- Los módulos nuevos deben usar los modelos y componentes de `core/` antes de crear duplicados.
- Durante el MVP, los repositorios fake comparten `MockRutaUStore` como fuente de verdad.
- La adopción de Hilt queda pendiente de acuerdo del equipo, para no romper la integración actual.

## Limitaciones conocidas del MVP

- No hay backend ni persistencia entre sesiones.
- El enlace de recuperación es una simulación visible en la pantalla “Revisa tu correo”.
- Las fotos seleccionadas se manejan como URI local mientras la app está activa.
- Las pantallas de Trayectos y Solicitudes son destinos temporales hasta integrar los módulos correspondientes.

