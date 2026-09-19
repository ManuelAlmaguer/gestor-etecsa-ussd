# Handoff — Manu ETECSA

## Identidad del proyecto

- Nombre visible: `Manu ETECSA`
- Propietario mostrado: `Manuel Almaguer Sosa`
- Package/Application ID: `com.manu.etecsaussd`
- Repositorio: `ManuelAlmaguer/gestor-etecsa-ussd`
- Rama de entrega: `main`
- SDK: min 28, target 34, Java 17, Gradle wrapper 8.7

## Estado entregado

La aplicación fue migrada de WebView a una implementación Android nativa. `MainActivity` crea vistas Material 3 directamente y no carga HTML, JavaScript, CDN ni recursos remotos. `docs/gestor_etecsa_app_mockup.html` es solamente la referencia original de diseño.

Funcionalidad actual:

- Dashboard offline con saldo, datos de todas las redes, voz, SMS, fechas de línea/paquete y límite de recarga.
- Soporte dual-SIM con `SubscriptionManager` y selección persistente.
- SIM mostrada en Inicio separada de SIM predeterminada para acciones.
- Compras con el flujo real `*133*1*4*opción#` y catálogo de cuatro paquetes.
- Transferencias `*234*1*Número*Contraseña*Importe#`.
- Room por SIM, incluyendo respuesta USSD cruda para diagnóstico.
- Sincronización diaria de cada SIM activa con WorkManager.
- Recordatorios por SIM a 5, 3 y 1 día del vencimiento de paquetes.
- Recordatorios por SIM de la fecha del límite de 360 CUP y del día siguiente disponible.
- Configuración de permisos, temas, SIM y seis controles de notificación.
- Icono vectorial propio y pantalla Acerca de.

## Archivos importantes

```text
app/src/main/java/com/manu/etecsaussd/MainActivity.java
app/src/main/java/com/manu/etecsaussd/data/AppPreferences.java
app/src/main/java/com/manu/etecsaussd/data/EtecsaDatabase.java
app/src/main/java/com/manu/etecsaussd/domain/EtecsaRepository.java
app/src/main/java/com/manu/etecsaussd/parser/EtecsaParsers.java
app/src/main/java/com/manu/etecsaussd/telephony/UssdExecutor.java
app/src/main/java/com/manu/etecsaussd/telephony/EtecsaActionExecutor.java
app/src/main/java/com/manu/etecsaussd/sync/EtecsaSyncWorker.java
app/src/main/java/com/manu/etecsaussd/sync/EtecsaNotificationScheduler.java
app/src/main/java/com/manu/etecsaussd/sync/EtecsaReminderWorker.java
app/src/main/res/drawable/ic_app_logo.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
docs/gestor_etecsa_app_mockup.html
```

No hay actualmente bridge JavaScript ni asset HTML de ejecución. Las referencias antiguas a `WebView`, `EtecsaJsBridge` o CDN deben considerarse obsoletas y no deben reintroducirse al continuar el proyecto.

## Decisiones funcionales relevantes

1. La pantalla principal no tiene por qué mostrar la SIM usada por las acciones. Ambas preferencias se almacenan con claves diferentes en `AppPreferences`.
2. Los datos móviles se guardan y muestran como total de todas las redes. La UI usa GB desde 1024 MB y MB por debajo de ese umbral.
3. La respuesta de recarga representa el ciclo mensual de 360 CUP, no un bono. `limitDateIso` es la fecha mostrada por ETECSA; `rechargeAvailableDateIso` es el día siguiente.
4. El texto `30-09-26` de la captura se presenta como fecha del límite `30/09/2026`, y la disponibilidad se presenta como `01/10/2026`.
5. La compra refleja el menú observado: `*133#`, opción 1 Datos, opción 4 Planes, opción 1–4. Se envía como código concatenado para evitar que la UI dependa de un flujo WebView.
6. No se solicita almacenamiento porque Room usa almacenamiento privado. La contraseña de transferencia no se persiste.

## Verificación ejecutada

```text
./gradlew testDebugUnitTest assembleDebug
```

La suite cubre saldo CUP, MB/GB, voz con duración `HH:MM:SS`, SMS con etiqueta delante o detrás, fecha de línea, vencimiento de paquete, límite de recarga y códigos de compra/transferencia.

Antes de entregar una actualización, ejecutar:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

El release debe firmarse con la misma clave privada de la aplicación. La clave no pertenece al repositorio y no debe subirse a GitHub.

## Validación pendiente en hardware

El entorno de build no puede emular un módem ETECSA. En un Android real dual-SIM hay que:

- conceder permisos de teléfono y notificaciones;
- seleccionar ambas SIM en Configuración;
- validar las cuatro respuestas USSD reales y los textos de fecha;
- confirmar que el fabricante permite `sendUssdRequest()` en segundo plano;
- probar una compra y una transferencia con cuenta de prueba;
- avanzar temporalmente el reloj o usar WorkManager para validar notificaciones.

Las acciones USSD pueden tener coste o efecto real. No deben probarse compras/transferencias sin confirmar la línea y el importe.

## Entrega

El APK release se copia a `artifacts/manu-etecsa-release.apk`. Los artefactos generados (`build/`, APKs intermedios y claves) permanecen fuera del control de versiones salvo el APK explícitamente destinado a `artifacts/`.
