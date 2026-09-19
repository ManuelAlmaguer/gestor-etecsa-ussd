# Manu ETECSA

Aplicación Android nativa en Java para gestionar líneas móviles ETECSA de Cuba mediante USSD. El nombre visible de la app es **Manu ETECSA** y el nombre del paquete se mantiene como `com.manu.etecsaussd`.

Repositorio: `ManuelAlmaguer/gestor-etecsa-ussd`
SDK: min 28 · target/compile 34
Interfaz: Material 3 oscuro, azul/cian, vistas Android nativas
Propietario mostrado en la app: Manuel Almaguer Sosa

## Estado actual

La app se comporta como una APK Android nativa. El HTML recibido se conserva únicamente como referencia visual en [docs/gestor_etecsa_app_mockup.html](docs/gestor_etecsa_app_mockup.html); no se carga en tiempo de ejecución, no hay `WebView`, `JavascriptInterface`, CDN, Google Fonts ni dependencia de Internet.

Incluye:

- Dashboard offline con saldo, datos de **todas las redes**, voz, SMS, vigencia de línea, vigencia de paquete y estado del límite mensual de recarga.
- Doble SIM mediante `SubscriptionManager` y `TelephonyManager.createForSubscriptionId(...).sendUssdRequest(...)`.
- Dos selecciones independientes: **SIM mostrada en Inicio** y **SIM predeterminada para acciones**.
- Compra de paquetes por el recorrido real `*133# → 1 Datos → 4 Planes → opción`.
- Transferencia de saldo con `*234*1*Número*Contraseña*Importe#`.
- Room para histórico por `subscriptionId` y respuestas USSD originales.
- WorkManager para sincronización diaria de cada SIM activa.
- Notificaciones por SIM: 5, 3 y 1 día antes del vencimiento de paquetes; día del límite de 360 CUP y día siguiente, cuando se puede volver a recargar.
- Configuración de permisos, SIM, tema y cada tipo de notificación.
- Temas visuales azul, cian y contraste, icono vectorial propio y sección Acerca de.

## Arquitectura

```text
app/src/main/java/com/manu/etecsaussd/
├── MainActivity.java                 # UI Android nativa y navegación lateral
├── EtecsaApplication.java            # Room, repositorio y motor USSD
├── data/
│   ├── AppPreferences.java           # SIM, tema y notificaciones
│   ├── EtecsaDatabase.java            # Room + migración 1 -> 2
│   ├── entity/                       # snapshots por SIM
│   ├── dao/                          # latest + inserts
│   └── model/DashboardSnapshot.java
├── domain/
│   ├── EtecsaRepository.java         # USSD -> parser -> Room
│   └── SyncReport.java
├── parser/EtecsaParsers.java          # Regex y normalización
├── telephony/
│   ├── UssdExecutor.java              # permisos, SIM y callback USSD
│   ├── EtecsaActionExecutor.java      # compras y transferencias
│   ├── EtecsaUssdCodes.java
│   └── UssdExecutionException.java
└── sync/
    ├── EtecsaSyncWorker.java          # sincronización diaria
    ├── EtecsaNotificationScheduler.java
    └── EtecsaReminderWorker.java
```

Recursos visuales nativos: `app/src/main/res/values/`, `drawable/ic_app_logo.xml` y `mipmap-anydpi-v26/`. El HTML original no es una dependencia de la aplicación.

## Datos ETECSA consultados

| Uso | Código | Datos guardados |
|---|---|---|
| Saldo, datos, voz, SMS y fechas | `*222#` | `balance_snapshots`, `data_usage_snapshots`, `voice_sms_snapshots`, `package_statuses` |
| Datos móviles | `*222*328#` | `data_usage_snapshots` |
| Voz / SMS | `*222*869#` | `voice_sms_snapshots` |
| Límite mensual de recarga | `*222*732#` | `recharge_statuses` |
| Compra de paquete | `*133*1*4*<opción>#` | respuesta inmediata |
| Transferencia | `*234*1*<número>*<contraseña>*<importe>#` | respuesta inmediata |

La respuesta real de `*222#` usada como fixture es:

```text
Saldo: 245.16 CUP. Datos: 1.38 GB. Voz: 00:34:15. SMS: 73.
Linea activa hasta 23-07-27 vence 19-01-28.
```

Se normaliza a 245.16 CUP, 1413 MB / 1.38 GB, 2055 segundos, 73 SMS, línea activa hasta 2027-07-23 y paquete hasta 2028-01-19.

## Lógica de recarga

`*222*732#` se interpreta como límite mensual de 360 CUP, no como bono ni vencimiento de paquete.

- `rechargedThisCycleCup`: cantidad recargada durante el ciclo.
- `remainingRechargeCup`: cantidad que todavía se puede recargar.
- `limitDateIso`: fecha que muestra ETECSA como fin del límite actual.
- `rechargeAvailableDateIso`: `limitDateIso + 1 día`, fecha desde la que se puede volver a recargar.

La UI etiqueta ambas fechas para evitar confundir la fecha del límite con la fecha de disponibilidad. Las notificaciones de ambas fechas se programan separadas para cada SIM.

## Paquetes disponibles en la UI

El catálogo refleja el flujo de las capturas recibidas:

| Opción | Paquete | Precio |
|---:|---|---:|
| 1 | 4.5 GB | 240 CUP |
| 2 | 2 GB + 15 min + 20 SMS | 120 CUP |
| 3 | 4 GB + 35 min + 40 SMS | 240 CUP |
| 4 | 6 GB + 60 min + 70 SMS | 360 CUP |

## Regex y normalización

Las constantes públicas de `EtecsaParsers` son la fuente ejecutable. Las principales son:

```regex
MAIN_BALANCE_PATTERN = (?iu)(?:saldo(?:\s+principal)?|saldo\s+disponible|balance)[^0-9]{0,45}((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(?:CUP|MN|pesos?)?
DATA_VALUE_PATTERN = (?iu)((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(GB|GIB|MB|MBytes?|megabytes?|gigabytes?)\b
VOICE_DURATION_PATTERN = (?iu)\b([0-9]{1,3}):([0-5][0-9]):([0-5][0-9])\b
SMS_LABEL_PATTERN = (?iu)\b(?:SMS|mensajes?)\s*[:=-]?\s*([0-9]{1,6})\b
LINE_ACTIVE_UNTIL_PATTERN = (?iu)(?:l[ií]nea\s+activa|linea\s+activa)\s+(?:hasta|hasta\s+el)\s+((?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|...))
PACKAGE_EXPIRATION_PATTERN = (?iu)(?:vence|vencimiento|expira|paquete\s+(?:vence|expira))[^0-9]{0,24}((?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|...))
RECHARGE_LIMIT_REACHED_PATTERN = (?iu)(?:ha\s+alcanzado|alcanzado|l[ií]mite|limite)[^0-9]{0,45}(?:360|trescientos\s+sesenta)
```

El código fuente contiene las variantes completas de fechas en español. Las fechas se guardan como `yyyy-MM-dd`, los datos internamente como MB y la UI cambia a GB cuando el valor es igual o superior a 1024 MB.

## Permisos y privacidad local

La app solicita solo lo necesario:

- `CALL_PHONE`: enviar consultas y acciones USSD.
- `READ_PHONE_STATE`: identificar SIM activas y enrutar cada solicitud.
- `POST_NOTIFICATIONS` en Android 13 o superior: mostrar recordatorios.

No solicita almacenamiento: Room utiliza el almacenamiento privado de la app. La contraseña de transferencia vive solo en memoria durante la acción y no se guarda en Room ni en logs.

## Compilar, probar y producir APK

Requisitos: JDK 17, Android SDK 34 y Gradle wrapper incluido.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`
APK release distribuible: `artifacts/manu-etecsa-release.apk` (firmada localmente, no se incluye la clave privada en Git).

La clave de firma debe conservarse fuera del repositorio para futuras actualizaciones. No se debe generar otra clave al publicar una versión posterior de la misma app.

## Validación en teléfono dual-SIM

1. Instalar el APK en Android 9 o superior.
2. Conceder teléfono y notificaciones cuando se soliciten.
3. Abrir **Configuración** y seleccionar la SIM que se mostrará en Inicio.
4. Seleccionar por separado la SIM predeterminada para acciones.
5. Pulsar **Actualizar** y comprobar `*222#`, `*222*328#`, `*222*869#` y `*222*732#`.
6. Validar una compra o transferencia solo con una cuenta y un importe de prueba.

El envío USSD depende del módem, fabricante, versión de Android y disponibilidad de la SIM. Esa parte requiere validación en el dispositivo físico; el parser, la persistencia, la construcción de códigos y la UI se verifican en el proyecto.
