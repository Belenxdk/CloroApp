# CloroApp — Caso 2 DSY1105

Aplicación académica Android para consultar cloro libre por granja y punto, revisar históricos y alertas y registrar seguimiento. Parte del proyecto CloroApp adjuntado por Eduardo. **Todos los nombres, usuarios, mediciones y rangos son ficticios y pendientes de Ariztía. No utilizar para decisiones sanitarias reales.**

## Abrir en Android Studio

1. Descomprimir el ZIP en una carpeta nueva. El ZIP actualizado tiene `settings.gradle.kts` directamente en su raíz. En Android Studio elegir File > Open y abrir exactamente la carpeta que contiene ese archivo, `gradlew` y la carpeta `app`. No abrir una carpeta exterior.
2. Permitir la sincronización de Gradle. Se conservan AGP 9.2.1, Gradle 9.4.1, Compose BOM 2026.02.01 y compileSdk 36.1 del proyecto recibido. Usar el JDK integrado compatible con esa instalación de Android Studio y aceptar la instalación del SDK solicitado.
3. Crear un dispositivo en Device Manager (Android 7/API 24 o superior; se recomienda el dispositivo del curso) y pulsar Run.
4. Elegir Ana Demo / Operario, Luis Demo / Supervisor o Elena Demo / Jefatura. La primera carga genera datos de ejemplo.
5. Menú > Ajustes demo: solicitar permiso de notificaciones, editar reglas o probar desconexión.

**Validación pendiente:** en el entorno de generación no existe SDK/ADB ni emulador; `./gradlew :app:assembleDebug --no-daemon` falló descargando Gradle con `Network is unreachable`, antes de compilar. No se entrega APK ni se afirma ejecución Android. Las pruebas JVM quedan incluidas para ejecutar en Android Studio. La API Python sí fue ejecutada y probada (5 pruebas aprobadas).

## Qué incluye

- Jetpack Compose y MVVM: pantallas, ViewModel, modelos y repositorio separados.
- Navigation Compose con NavHost, rutas, TopAppBar, menú lateral y NavigationBar. En ancho mediano/expandido se usa NavigationRail y se redistribuye el resumen. WindowSizeUtils usa Window Size Classes de Material 3.
- Resumen por granja, consulta y filtro de puntos, detalle y fecha de última lectura, tres estados de cloro y aviso de antigüedad.
- Histórico (1, 7 o días configurados), tendencia, promedio, mínimo y máximo; hasta 300 registros visibles por punto y exportación CSV de lecturas para perfiles permitidos.
- Controles manuales, datos adicionales como texto opcional y corrección mediante nueva lectura que referencia la original.
- Desviaciones automáticas, reconocimiento, acciones con autor/fecha y cierre que exige acción registrada y, por defecto, lectura normal reciente. Historial de cerradas conservado.
- Persistencia privada en SharedPreferences de snapshot JSON, reglas y cola de eventos. Sobrevive a cerrar/reabrir la app. Backups Android desactivados.
- Modo local de demostración y cliente HTTP real para la API simulada incluida.
- Notificaciones Android locales al detectar nuevos casos o cambios de gravedad, con permiso en Android 13+. Abrir una notificación lleva a Alertas tras seleccionar usuario.
- Pruebas JUnit de límites, permisos, overrides por punto y serialización; pruebas HTTP Python.

## Cambiar respuestas pendientes

Archivo principal: `app/src/main/assets/company_config.json`.
También se puede cambiar desde **Ajustes demo > Reglas editables JSON > Validar y guardar reglas**. Se valida el orden de rangos y los valores admitidos. Guardar cierra la selección de usuario para aplicar sus permisos nuevamente.

Si se modifica el archivo en Android Studio después de haber guardado ajustes en la app, la copia persistida tiene prioridad. Cargar valores originales en el editor y guardar, o borrar los datos de la app, para usar la nueva configuración del archivo.

`docs/DECISIONES_PENDIENTES.md` relaciona las preguntas con las claves. Los campos de `pendingBusiness` son **anotaciones editables**, no interruptores que implementen funciones. Cambiar el texto “push” o “ERP” no crea una integración. Las decisiones que sí cambian el comportamiento están señaladas como activas.

### Rangos ficticios iniciales

| Valor de cloro | Resultado |
|---|---|
| Menor que 0 o no numérico | Rechazado / inválido |
| 0 a menos de 0,20 | Crítico |
| 0,20 a menos de 0,50 | Advertencia |
| 0,50 a 1,50 inclusive | Normal |
| Más de 1,50 hasta 2,00 inclusive | Advertencia |
| Más de 2,00 | Crítico |

Unidad inicial mg/L; visualización 2 decimales. Se conserva la precisión de entrada y se clasifica el valor original, antes del redondeo visual. Cambiar la etiqueta a ppm **no realiza conversión**; la fuente debe entregar valores en la unidad acordada. Cloro total/ambos no está implementado: el validador protege el modelo de cloro libre del caso.

Para reglas específicas:

```json
"pointRanges": {
  "P1": {"criticalLow": 0.2, "normalMin": 0.5, "normalMax": 1.5, "criticalHigh": 2.0}
}
```

Deben indicarse los cuatro límites y respetar su orden. Los estados históricos se recalculan con la configuración vigente; no se mantiene versionado de límites por lectura. Confirmar si la empresa requiere esa auditoría.

### Usuarios y permisos provisionales

| Perfil | Permisos iniciales | Granjas demo |
|---|---|---|
| Operario | Consultar, medir, registrar acciones | F1 |
| Supervisor | Lo anterior, reconocer, cerrar y corregir | F1 y F2 |
| Jefatura | Consultar y exportar | F1 y F2 |

Permisos admitidos: `read`, `measure`, `action`, `ack`, `close`, `correct`, `export`. Editar `roles` y `users`. Un usuario puede tener varias granjas y roles; selecciona un rol activo al entrar. Las pantallas se comparten y las operaciones se filtran. Ajustes demo está disponible como herramienta de desarrollo a todos los perfiles, sin considerarlo un permiso empresarial.

## Probar consumo de API

Requiere Python 3 en el PC. No se conecta a sistemas de Ariztía.

```powershell
cd C:\ruta\CloroApp
py mock_api\server.py
```

En el emulador estándar de Android Studio: Ajustes demo, desactivar simulador local, URL `http://10.0.2.2:8080`, guardar origen y actualizar. El primer GET crea un conjunto de lecturas fijas; no inventa mediciones nuevas en cada consulta. Registrar controles desde la app permite producir nuevos estados. El conjunto local, en cambio, renueva sus lecturas cuando se actualiza.

Para un celular físico de prueba en la misma red, ejecutar `py mock_api\server.py --host 0.0.0.0` y usar `http://IP_DEL_PC:8080`. Habilitar el puerto solamente en la red privada de pruebas. El servidor académico no tiene autenticación, TLS, aislamiento entre empresas ni control de permisos del lado servidor: los permisos actuales son de la interfaz demo. No es un backend productivo.

- `GET /snapshot`: puntos, lecturas, desviaciones, acciones.
- `POST /events`: evento con ID único y payload. Reintentos con mismo ID no duplican registros.
- Persistencia servidor: `mock_api/state.json`. Eliminarlo con el servidor detenido reinicia únicamente los datos de esa API.
- Las escrituras quedan en cola; se envían al actualizar en modo HTTP. Si la red falla, permanecen pendientes. Eventos de alerta generados durante esa consulta se envían en la próxima actualización.
- Prioridad de lectura: se combinan lecturas por ID. Registros inmutables y cierre terminal en servidor. No hay resolución empresarial avanzada de cambios simultáneos.
- Para aislar una demostración API de una prueba local anterior, borrar datos de la aplicación antes de comenzar; la app preserva el historial al cambiar de origen.

## Demostración paso a paso

1. Operario: cargar datos y consultar P1 normal y P2 advertencia. No debe ver F2.
2. Supervisor: ver F1/F2, P3 crítico; registrar una acción en la desviación.
3. Registrar 0,9 en P3; cerrar la desviación antes de actualizar el simulador local (este vuelve a generar su lectura crítica). El histórico conserva lectura crítica, acción y cierre.
4. Introducir 0,20; 0,50; 1,50; 2,00 y comprobar la tabla de límites. Corregir como supervisor y verificar referencia al original.
5. Solicitar notificaciones; generar otra lectura crítica. Abrir la notificación.
6. Activar simulación sin conexión, cerrar y reabrir la app: las lecturas están guardadas. Para desconexión real, detener servidor/desactivar red en modo API.
7. Jefatura: consultar resumen y exportar CSV mediante selector de archivos Android.
8. Ejecutar en celular y tablet/redimensionable para comparar layouts y previews.
9. Probar API; guardar acción sin red, reconectar y actualizar dos veces; verificar que no se duplique.

## Pruebas

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
cd mock_api
py -m unittest -v
```

Las cinco pruebas HTTP se ejecutaron correctamente. JUnit y compilación Android **no ejecutados** por falta de dependencias/SDK en el entorno. No hay capturas ni video de emulador porque no se ejecutó aquí.

## Límites que requieren respuesta de Ariztía

Autenticación corporativa, push con app cerrada, notificaciones entre dispositivos, fotos/firmas, cloro total, conversión de unidades, reportes PDF y conexiones AMINO/ERP requieren desarrollo adicional después de definir el contrato. No se activan por cambiar un texto. El escalamiento implementado es visual; no envía mensajes al supervisor. La consulta automática se realiza solo mientras la actividad está visible.

Las lecturas locales se depuran según `retentionDays` durante una actualización correcta. Acciones, alertas y eventos del servidor no se eliminan automáticamente. El almacenamiento JSON es adecuado para la demostración pequeña, no está probado para cientos de miles de registros. El servidor no genera lecturas continuas por sí mismo. App y servidor no comparten automáticamente la configuración empresarial: el equipo debe usar la misma configuración en los dispositivos de prueba.

## Colaboración exigida por las guías

Se conserva CloroApp como nombre del proyecto recibido. Cuando sepan el grupo, acordar `AppCloro_GrupoX` con el docente. Crear repositorio privado, incorporar a compañera y docente, primer commit `Inicio de proyecto + estructura base MVVM` y commit de navegación `Navegación y menú generado`. Crear Trello con Por hacer, En curso, Finalizado; agregar tareas reales y capturas del emulador. Estos pasos de cuentas externas no se realizaron desde este paquete.

Fuentes del trabajo: Caso 2 secciones 2.1–5.1; guías 7, 9 y 10 entregadas. Referencias de implementación: https://developer.android.com/guide/navigation/design y https://developer.android.com/reference/kotlin/androidx/compose/material3/windowsizeclass/WindowSizeClass.


## Corrección de importación y dependencias del 8 de octubre

Se actualizaron Navigation Compose a 2.10.2, Lifecycle ViewModel/Runtime Compose y KTX a 2.11.0, y se declaró Coroutines Android 1.11.0 explícitamente. La versión anterior usaba Navigation 2.8.9 y Compose Lifecycle 2.8.7; coroutines llegaba mediante dependencias transitivas.

Si arriba aparece Add Configuration y el Manifest marca atributos válidos como no permitidos, comprobar primero que abrió la raíz Gradle correcta. Cerrar el proyecto, abrir la carpeta con settings.gradle.kts y esperar Sync. Luego seleccionar app, el emulador encendido y Run. Iniciar un emulador no instala la aplicación: Run debe compilarla e instalarla. No eliminar atributos válidos del Manifest para ocultar errores de importación.

Si Sync falla, copiar el primer error completo de Build/Sync. Esta actualización de dependencias no está compilada en el entorno de generación; no garantiza que la sincronización local carezca de otros problemas.
