# Verificación de esta entrega

Fecha: 8 de octubre de 2026.

## Ejecutado

Pruebas HTTP reales con servidor Python en puerto local temporal:

- GET snapshot entrega 4 puntos y 52 lecturas: aprobado.
- Reenvío de evento con mismo ID no duplica acción y persiste en archivo: aprobado.
- Medición negativa: respuesta 400, aprobado.
- Intento de sobrescribir lectura original: respuesta 400, aprobado.
- Evento antiguo no reabre incidente cerrado: aprobado.

Resultado: 5 pruebas aprobadas (`python3 -m unittest -v` en mock_api).
Se analizó la sintaxis de los 14 archivos Kotlin con tree-sitter-kotlin sin errores. Esto no comprueba tipos, dependencias ni compilación.
Se verificó formato JSON de configuración y XML de recursos/manifest. Se conservó el wrapper Gradle.

## Preparado pero pendiente de ejecución

JUnit: clasificación en 0,199; 0,20; 0,50; 1,50; 2,00; 2,001; valores inválidos; permisos; límites por punto; rechazo de rangos invertidos; serialización local.

## Bloqueo de compilación Android

Comando intentado: `./gradlew :app:assembleDebug --no-daemon`.
Resultado: `java.net.SocketException: Network is unreachable` al descargar Gradle 9.4.1. No se alcanzó la fase de compilación.
El entorno no dispone de SDK Android, adb ni emulador. Por ello no hay APK, captura de aplicación, test de UI ni métricas de rendimiento obtenidas aquí. La revisión de código y las pruebas del servidor no sustituyen la compilación y prueba en Android Studio.

## Pruebas manuales que debe ejecutar el equipo

Consultar README, sección Demostración paso a paso. Verificar además teclado y scroll, rotación, tamaños de ventana, denegación del permiso de notificaciones, error API, primer inicio sin caché, datos antiguos, cola offline, cambio de usuario y perfil, modificación de rangos y cierre sin acción/lectura normal. Guardar capturas y registrar resultado real en Trello.
