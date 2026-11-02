# CloroApp 2.0 — Medición de cloro en línea

Renovación de la demo de Eduardo y Belén conforme a las 35 respuestas recibidas el 29/10/2026. Todos los nombres, datos y credenciales son ficticios. Las cantidades y límites son propuestas académicas a validar con el docente de DSY1105; no se presentan como condiciones confirmadas de Ariztía.

## Abrir
1. Descomprimir en una carpeta nueva. Abrir en Android Studio la carpeta que contiene `settings.gradle.kts`, `gradlew` y `app`.
2. Usar JDK integrado 21 e instalar SDK 37 si se solicita. Sincronizar Gradle.
3. Seleccionar `app`, celular o emulador y pulsar Run. La demo local funciona sin Python.
4. Iniciar sesión con una cuenta de la tabla.

| Perfil | Correo | Contraseña | Granjas |
|---|---|---|---|
| Operario | operario1@cloro.demo | Cloro2026! | Norte |
| Supervisor | supervisor1@cloro.demo | Cloro2026! | Norte |
| Jefatura | jefatura1@cloro.demo | Cloro2026! | Todas |

Hay 15 usuarios. Operarios 1–3: Norte; 4–6: Centro; 7–9: Sur. Supervisores 1, 2 y 3: Norte, Centro y Sur respectivamente. Jefaturas 1, 2 y 3: todas. Los correos siguen `operarioN@cloro.demo`, `supervisorN@cloro.demo`, `jefaturaN@cloro.demo`; contraseña común ficticia `Cloro2026!`.

## Cambios de interfaz 2.1
Esta entrega contiene el código actualizado, sin APK. La compilación anterior corresponde a 2.0 y no valida estos cambios. Abrir el proyecto en Android Studio, sincronizar y ejecutar app.

Navegación con iconos vectoriales de 30 dp y Ajustes en la barra inferior; teclado decimal; explicación de pestañas; opciones técnicas ocultas al operario; ajustes avanzados plegables; filtros de reportes plegables y botones principales de al menos 48 dp.

Cámara nativa: Punto → Seguimiento → Evidencia fotográfica → Abrir cámara. Invoca la aplicación de cámara mediante ActivityResultContracts.TakePicturePreview, sin permiso directo de cámara porque la captura se delega a otra aplicación. Guarda una imagen de resolución reducida por captura en almacenamiento privado del punto. No es un adjunto sincronizado ni prueba fotográfica de alta resolución. Se muestran las tres más recientes. Cancelar no crea un archivo. Desinstalar borra las fotos.

## Rangos iniciales
Cloro libre en ppm; dos decimales visuales; sin conversión. Se conserva la precisión de entrada y se clasifica antes del redondeo.

| Valor | Estado |
|---|---|
| Negativo, infinito o no numérico | Rechazado |
| Menor que 2 | Crítico, cloro insuficiente |
| Desde 2 hasta menos de 3 | Advertencia, cloro insuficiente |
| Desde 3 hasta 5 inclusive | Normal |
| Mayor que 5 hasta 6 inclusive | Advertencia, cloro excesivo |
| Mayor que 6 | Crítico, cloro excesivo |

Configuración: `app/src/main/assets/company_config.json`. `pointRanges` permite excepciones por ID de punto con los cuatro límites. En modo local supervisor puede editar rangos generales y reglas desde Configuración. En API las reglas se leen del servidor al entrar y sincronizar; editar el archivo y reiniciar servidor para cambiarlas.

## Flujo para probar
1. Operario1: Puntos → P01 → Control → guardar 1,50. Se genera alerta crítica por cloro insuficiente.
2. Abrir P02 y guardar 4,00. Su nombre y código deben cambiar; cada punto tiene su formulario.
3. Pulsar Actualizar: consulta registros, no genera valores nuevos ni restaura los iniciales.
4. Volver a P01 → Seguimiento. Escribir una verificación y acción ficticia. El protocolo sanitario concreto no está definido por la empresa: no se inventan dosis.
5. P01 → Control → guardar 4,00 después de registrar la acción.
6. Cerrar sesión desde Menú. Entrar como supervisor1, reconocer y validar/cerrar la alerta. El cierre requiere acción y una lectura normal reciente posterior (15 minutos configurables).
7. Revisar Alertas con “Incluir cerradas”. Se conservan autor y fecha de cierre.
8. Entrar como jefatura1 → Reportes: filtrar granja, punto y período; consultar mediciones, alertas y acciones; exportar CSV para Excel.

En local los perfiles comparten la información de ese celular. En API, sincronizar antes de cambiar de usuario para que otro perfil/dispositivo vea los registros nuevos.

## Simulación
- Hay 3 granjas y 12 puntos, con históricos ficticios de hasta 30 días.
- `Actualizar` solo consulta. La pantalla indica fecha de última sincronización y antigüedad de lecturas.
- Supervisor → Configuración → Generar nueva ronda simulada registra nuevos valores en sus puntos.
- Opcional: activar generación cada 5 minutos con app abierta en local.
- En API, iniciar con `--auto` para rondas periódicas independientes de la app. Por defecto está desactivado para practicar cierres sin nuevas rondas imprevistas.
- La app consulta API cada 60 segundos mientras es visible; no es push.

## API REST y base académica SQLite
Python 3.10 o posterior, sin paquetes adicionales. En CMD desde la carpeta del proyecto:

```bat
py mock_api\server.py
```

En emulador: cerrar sesión, desplegar Conexión, desactivar demostración local, guardar `http://10.0.2.2:8080` y entrar otra vez.

En celular y PC en la misma red privada:

```bat
py mock_api\server.py --host 0.0.0.0
ipconfig
```

En el celular usar `http://IP_LOCAL_DEL_PC:8080`. Permitir Python en firewall de red privada si es necesario. `10.0.2.2` es para el emulador, no para un teléfono físico.

Para rondas automáticas:

```bat
py mock_api\server.py --host 0.0.0.0 --auto
```

Servidor conserva `mock_api/cloro_v2.sqlite`. Endpoints: POST /login; GET /config; GET /snapshot; POST /events; POST /simulate. Sesiones Bearer de 8 horas, permisos y granjas validados en servidor. Los eventos se aplican atómicamente y repetir un ID idéntico no duplica el registro.

## Sin conexión y trazabilidad
Cargar datos antes de perder conexión. Activar Configuración → Simular sin conexión o desconectar red. Consultar y registrar; los datos sobreviven al cierre de la app. En API, se guardan snapshot y cola juntos, separados por usuario y origen; al reconectar, Actualizar envía pendientes.

Se permite entrar sin red a la API solo si ese usuario ya se autenticó en ese dispositivo/origen y hay datos guardados. Al vencer sesión o reiniciar servidor, entrar nuevamente con red. No se borran pendientes al salir ni cambiar de origen.

Si el servidor rechaza un evento por conflicto (p. ej. otro supervisor cerró el caso), se mantiene el pendiente y se muestra el error. El MVP no incluye editor de conflictos; revisar antes de borrar datos. No prometer resolución automática de concurrencia.

Mediciones originales inalterables. Supervisor puede corregir mediante nueva lectura con motivo, fecha y autor, conservando referencia al original. Cada lectura guarda estado y límites originales. Cambiar límites modifica la clasificación actual, no la clasificación registrada en el histórico. Estadísticas incluyen todos los registros de auditoría, incluidas las originales corregidas.

## Alcance y límites
- pH, temperatura y turbidez: texto opcional; no tienen validación ni alertas propias.
- Acciones: descripción, autor, fecha/hora, punto, medición, alerta y estado de resolución. Supervisor valida al cerrar.
- Reportes: consulta filtrada y CSV. PDF y XLSX nativo quedan como ampliaciones.
- Notificaciones locales para operario/supervisor al detectar nuevas alertas o cambio de gravedad/tipo con la app visible; solicitar permiso en Configuración. No hay push ni servicio de fondo.
- Se muestra antigüedad de alertas; no hay escalamiento automático a otros usuarios.
- No hay sensores, AMINO, ERP, fotos, firmas o análisis predictivo.
- SQLite y caché JSON son para un volumen académico pequeño; sin política de eliminación automática ni afirmación de retención empresarial.
- Credenciales públicas demo, hashes SHA-256 de ejemplo y HTTP para red privada. Producción requeriría HTTPS, hash adaptativo, gestión real de identidades y protección adicional. No utilizar datos reales.
- Almacenamiento `cloro_v2` separado: no migra ni borra los registros anteriores de `cloro_private`.

## Desarrollo

```bat
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
cd mock_api
py -m unittest -v
```

Resultados efectivos y pruebas pendientes: `docs/VERIFICACION.md`. Detalle de decisiones: `docs/REQUISITOS_V2.md`.

Esta entrega renueva el ZIP de la demo anterior. Se revisó que el repositorio tiene ramas independientes de Eduardo y Belén; no se han sobrescrito ni integrado automáticamente. Revisar el proyecto completo en una nueva rama antes de combinarlo con sus avances.
