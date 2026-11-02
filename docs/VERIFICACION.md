# Verificación Técnica y Validación de Requisitos - CloroApp 2.1

## Estado de la Verificación (Octubre 2026)
Se realizó una auditoría completa del código fuente, arquitectura, interfaz de usuario y reglas de negocio, contrastándola con el **Caso 2 (DSY1105)** y las **35 respuestas oficiales del documento de preguntas**.

### Correcciones Aplicadas en el Código:
1. **Interfaz TopAppBar (`AppScreens.kt`)**:
   - Se removió `Modifier.fillMaxWidth()` del botón `Actualizar` en el slot `actions` de la barra superior. Esto soluciona la deformación visual y compresión del título de la pantalla y del icono de navegación en pantallas de distintos tamaños.
2. **Diferenciación Visual de Errores vs. Éxitos (`CloroViewModel.kt` y `AppScreens.kt`)**:
   - Se implementó la propiedad reactiva `isError`. Los mensajes de validación fallida y excepciones ahora se renderizan en color de error (`MaterialTheme.colorScheme.error`), mientras que las confirmaciones operativas exitosas se muestran en verde corporativo (`teal`).
3. **Accesibilidad y Tamaño Táctil en Terreno (`AppScreens.kt`)**:
   - El botón principal "Guardar medición" en la pestaña Control fue actualizado a `Modifier.fillMaxWidth().heightIn(min = 48.dp)`, cumpliendo el estándar de 48 dp mínimos para uso en terreno.
4. **Validación de Correcciones (`AppScreens.kt`)**:
   - El checkbox para corregir mediciones previas ahora valida si existe al menos una lectura anterior (`hasLatest`), deshabilitándose de forma segura si el punto no cuenta con historial.
5. **Gráfico de Tendencias Resiliente (`AppScreens.kt` - `HistoryChart`)**:
   - Se añadió soporte para graficar un punto central mediante `drawCircle` cuando existe una única medición, evitando que el lienzo quede en blanco.
6. **Manejo Dinámico de Unidades (`AccessReports.kt`)**:
   - Se vinculó la unidad mostrada en reportes directamente a `CompanyConfig` (`ppm` o `mg/L`), en lugar de mantenerla estática.
7. **Filtrado Seguro de Evidencia Fotográfica (`PhotoEvidence.kt`)**:
   - Se restringió la lectura en la carpeta privada a archivos de imagen válidos (`.jpg`, `.jpeg`, `.png`).
8. **Codificación UTF-8 en Exportación CSV (`AppScreens.kt` y `AccessReports.kt`)**:
   - Se aseguró `bufferedWriter(Charsets.UTF_8)` junto con el prefijo BOM (`\uFEFF`) para compatibilidad directa y sin caracteres corruptos en Microsoft Excel.
9. **Timeout del Gradle Wrapper (`gradle-wrapper.properties`)**:
   - Se incrementó el `networkTimeout` a 60.000 ms (60 s) para evitar caídas prematuras por timeout durante la sincronización inicial de Android Studio.
10. **Resiliencia en Pruebas Unitarias (`ConfigTest.kt`)**:
    - Se adaptó la resolución del archivo `company_config.json` tanto para ejecuciones internas desde el módulo `app` como desde el directorio raíz.

### Matriz de Cumplimiento con el Caso 2 y las 35 Preguntas:
- **P1 - P5 (Rangos y Alertas)**: Umbrales 2.0 / 3.0 / 5.0 / 6.0 ppm; clasificación de estados Normal, Advertencia y Crítico; diferenciación entre Cloro insuficiente y Cloro excesivo.
- **P6 - P7 (Atributos de Puntos)**: Registro de granja, galpón, código de punto, ubicación, tipo de punto, fecha, hora, estado y parámetros complementarios (pH, temperatura, turbidez).
- **P8 - P10 (Infraestructura Simulada)**: 3 granjas (Norte, Centro, Sur) x 4 puntos = 12 puntos de medición en total.
- **P11 - P12 (Conectividad y Sincronización)**: Almacenamiento local mediante SharedPreferences, soporte offline completo con cola de eventos pendientes y consumo de API REST.
- **P13 - P16, P34 (Usuarios y Roles)**: 15 usuarios ficticios distribuidos en Operarios (por granja), Supervisores (por granja) y Jefaturas (vista transversal); autenticación por correo y contraseña.
- **P17 (Auditoría e Inalterabilidad)**: Las mediciones originales se mantienen inmutables; las correcciones se registran como eventos nuevos con motivo, autor y fecha.
- **P21 - P24 (Flujo de Cierre de Desviaciones)**: Detección -> Registro de acción correctiva -> Nueva medición dentro de rango normal -> Validación y cierre autorizado por supervisor.
- **P27 - P29 (Históricos y Reportes)**: Filtros de 24 horas, 7 días y 30 días; métricas de promedio, mínimo y máximo; visualización de tendencias y exportación a CSV para Excel.
