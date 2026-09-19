# Gestor ETECSA USSD

Aplicación Android nativa en Java para consultar y gestionar una línea móvil ETECSA de Cuba mediante códigos USSD. El proyecto entiende el flujo `*222#`, conserva el histórico local y presenta el dashboard Material 3 oscuro del mockup HTML adjunto dentro de un `WebView`.

Paquete: `com.manu.etecsaussd`  
SDK: min 28 · target/compile 34  
Repositorio: `ManuelAlmaguer/gestor-etecsa-ussd`

## Estado entregado

- Motor USSD con `TelephonyManager.sendUssdRequest()` y timeout de 35 segundos.
- Selección persistente de SIM para móviles dual-SIM. La línea elegida se usa en consultas, compras y transferencias; si no se selecciona, se usa la SIM de datos predeterminada y luego la primera SIM activa.
- Room para históricos de saldo, datos, voz/SMS y recargas.
- `WorkManager` para sincronización diaria.
- Parser defensivo de respuestas ETECSA en español.
- `WebViewAssetLoader` + `@JavascriptInterface` asíncrono para actualizar el DOM sin bloquear el hilo de la interfaz.
- Acciones para compra de paquetes y transferencia de saldo.
- Pruebas unitarias para parsers y construcción de códigos.
- Workflow de GitHub Actions para `testDebugUnitTest assembleDebug`.

## Arquitectura

```text
app/src/main/java/com/manu/etecsaussd/
├── data/
│   ├── EtecsaDatabase.java
│   ├── entity/       # snapshots Room
│   ├── dao/          # consultas latest + inserts
│   └── model/        # DashboardSnapshot
├── domain/
│   ├── EtecsaRepository.java
│   └── SyncReport.java
├── parser/
│   └── EtecsaParsers.java
├── telephony/
│   ├── UssdExecutor.java
│   ├── EtecsaUssdCodes.java
│   ├── EtecsaActionExecutor.java
│   └── UssdExecutionException.java
├── sync/
│   └── EtecsaSyncWorker.java
├── web/
│   └── EtecsaJsBridge.java
├── EtecsaApplication.java
└── MainActivity.java
```

La UI está en [app/src/main/assets/index.html](app/src/main/assets/index.html). El mockup original se conserva sin modificar en [docs/gestor_etecsa_app_mockup.html](docs/gestor_etecsa_app_mockup.html).

## Códigos soportados

| Uso | Código | Persistencia |
|---|---|---|
| Saldo principal | `*222#` | `balance_snapshots` |
| Datos móviles | `*222*328#` | `data_usage_snapshots` |
| Voz / SMS | `*222*869#` | `voice_sms_snapshots` |
| Estado de recarga | `*222*732#` | `recharge_statuses` |
| Compra de paquete | `*133*<opción>#` | respuesta inmediata |
| Transferencia | `*234*1*<número>*<contraseña>*<importe>#` | respuesta inmediata |

## Regex ETECSA

Las constantes públicas de `EtecsaParsers` son la fuente exacta que usa la aplicación. Están diseñadas para tolerar etiquetas, espacios, coma decimal, punto decimal, `CUP`, `MN`, `MB`, `GB`, `LTE`, SMS y meses en español.

```regex
MAIN_BALANCE_PATTERN = (?iu)(?:saldo(?:\s+principal)?|saldo\s+disponible|balance)[^0-9]{0,45}((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(?:CUP|MN|pesos?)?
CURRENCY_AMOUNT_PATTERN = (?iu)((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(?:CUP|MN|pesos?)\b
DATA_VALUE_PATTERN = (?iu)((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(GB|GIB|MB|MBytes?|megabytes?|gigabytes?)\b
VOICE_MINUTES_PATTERN = (?iu)(?<![0-9])([0-9]{1,6})\s*(?:min(?:utos?)?)\b
SMS_PATTERN = (?iu)(?<![0-9])([0-9]{1,6})\s*(?:SMS|mensajes?)\b
RECHARGE_AMOUNT_PATTERN = (?iu)(?:bono|recarga|importe|monto|saldo)[^0-9]{0,50}((?:\d{1,3}(?:[ .]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?))\s*(?:CUP|MN|pesos?)?
EXPIRATION_DATE_PATTERN = (?iu)(?:v[aá]lido\s+(?:hasta|hasta el)|vence(?:\s+el)?|expira(?:\s+el)?|vencimiento|fecha(?:\s+de)?\s+vencimiento)?[^0-9]{0,30}(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+(?:ene(?:ro)?|feb(?:rero)?|mar(?:zo)?|abr(?:il)?|may(?:o)?|jun(?:io)?|jul(?:io)?|ago(?:sto)?|sep(?:tiembre)?|oct(?:ubre)?|nov(?:iembre)?|dic(?:iembre)?)\s+\d{4})\b
```

Los valores se normalizan a CUP, megabytes, minutos, mensajes y fecha ISO `yyyy-MM-dd`. La respuesta original se guarda en Room para poder ajustar el parser si ETECSA cambia el texto.

## Bridge JavaScript

El HTML llama a `AndroidBridge`; todas las operaciones devuelven una respuesta asíncrona a `window.EtecsaNative.onResponse(requestId, payload)`.

```javascript
AndroidBridge.getDashboard(requestId)
AndroidBridge.sync(requestId)
AndroidBridge.getSimCards(requestId)
AndroidBridge.selectSim(subscriptionId, requestId)
AndroidBridge.purchasePackage(packageOption, requestId)
AndroidBridge.transferBalance(phoneNumber, password, amount, requestId)
```

La contraseña de transferencia no se persiste ni se escribe en logs. El bridge valida el `requestId` y no permite que el código USSD salga del formato esperado.

## Permisos y dual-SIM

En el primer arranque se solicitan `CALL_PHONE` y `READ_PHONE_STATE`. En el menú **Ajustes** se muestran las SIM activas con ranura, operador y `subscriptionId`. La selección se guarda localmente y se valida cada vez contra las SIM activas; si la SIM desaparece, se aplica el fallback de SIM de datos predeterminada.

`sendUssdRequest()` sigue dependiendo del soporte del fabricante y del operador. Algunos dispositivos pueden devolver timeout o rechazo aunque el código funcione desde la aplicación de teléfono.

## Compilar y probar

Requisitos: Android Studio reciente, JDK 17 y Android SDK 34.

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK debug generado: `app/build/outputs/apk/debug/app-debug.apk`.

Para usarlo en un teléfono físico:

1. Instala la APK debug o abre el proyecto en Android Studio.
2. Concede permisos de teléfono.
3. Entra en **Ajustes** y selecciona la línea ETECSA predeterminada.
4. Pulsa el botón de sincronización y valida las respuestas reales de la SIM.

## Notas de producción

- La base Room no está cifrada; si se requiere una distribución de producción, añadir SQLCipher o cifrado de almacenamiento según la política de privacidad.
- El mockup usa Tailwind CDN, Google Fonts y Font Awesome CDN. Para un modo completamente offline conviene empaquetar esos recursos localmente antes de publicar.
- Las respuestas reales de ETECSA pueden variar por plan, idioma o versión de red. Antes de congelar la release se deben capturar respuestas anonimizadas de las cuatro consultas y ampliar las pruebas de regresión.

