# Handoff — Gestor ETECSA USSD

## Resumen

Se entregó una primera versión funcional del proyecto Android `com.manu.etecsaussd` en Java. La UI adjunta se convirtió en un asset local de WebView y el botón de sincronización ya está conectado a consultas USSD, parser, Room y actualización del DOM.

## Implementado

- `MainActivity` configura WebView, `WebViewAssetLoader`, JavaScript y permisos.
- `EtecsaJsBridge` expone operaciones asíncronas para dashboard, sync, SIMs, compras y transferencias.
- `UssdExecutor` usa `TelephonyManager.createForSubscriptionId(...).sendUssdRequest(...)`.
- La SIM predeterminada se persiste en `SharedPreferences`; el selector está en **Ajustes**.
- `EtecsaRepository` ejecuta las cuatro consultas y guarda snapshots en Room.
- `EtecsaSyncWorker` agenda la sincronización cada 24 horas mediante `WorkManager`.
- `EtecsaParsers` contiene regex y normalizadores para saldo, datos, voz/SMS y recarga.
- `docs/gestor_etecsa_app_mockup.html` conserva el HTML original recibido.
- `.github/workflows/android.yml` ejecuta tests y build debug en GitHub Actions.

## Verificación ejecutada

Comandos ejecutados con Android SDK 34 temporal:

```text
./gradlew :app:compileDebugJavaWithJavac       ✅
./gradlew testDebugUnitTest assembleDebug     ✅
```

Pruebas incluidas:

- `EtecsaParsersTest`: saldo CUP, GB/MB, minutos/SMS y fecha española.
- `EtecsaActionExecutorTest`: compra de paquete y transferencia de saldo.

APK debug local generado: `app/build/outputs/apk/debug/app-debug.apk`.

## Validación manual pendiente

La parte que no puede verificarse en este entorno es el módem real:

1. Instalar la APK en un Android 9+ con dos SIM.
2. Conceder permisos `CALL_PHONE` y `READ_PHONE_STATE`.
3. Abrir **Ajustes**, seleccionar la SIM ETECSA y cerrar/abrir la app para confirmar persistencia.
4. Probar `*222#`, `*222*328#`, `*222*869#` y `*222*732#`.
5. Comparar el texto de cada respuesta con los valores mostrados y guardar ejemplos anonimizados si hay diferencias.
6. Confirmar una compra de paquete en una cuenta de prueba antes de habilitar el flujo productivo.

## Decisiones y límites conocidos

- Si no hay selección manual, el sistema usa la SIM de datos predeterminada; si tampoco existe, usa la primera SIM activa.
- Las respuestas originales se guardan en Room para depuración de formatos, pero no se registran contraseñas de transferencia.
- El parser es tolerante, no una garantía contractual de formato. ETECSA puede cambiar etiquetas, unidades o idioma.
- La UI depende actualmente de recursos CDN del mockup. Para despliegues sin internet, descargar Tailwind, Inter y Font Awesome a `assets/` y cambiar las referencias.
- El APK debug no está firmado para distribución de Play Store.

## Próximos pasos recomendados

- Añadir una pantalla de compra de paquetes con catálogo y confirmación explícita.
- Añadir formulario de transferencia con ocultación de contraseña y confirmación de importe.
- Añadir historial gráfico desde las tablas Room.
- Cifrar la base local si se almacenan respuestas sensibles.
- Capturar fixtures reales de ETECSA y ampliar la suite de parsers.

