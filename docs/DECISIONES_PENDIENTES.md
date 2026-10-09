# Respuestas pendientes de Ariztía y dónde cambiarlas

Todas las decisiones de esta tabla están SIN RESPUESTA DE LA EMPRESA. Los valores son una propuesta provisional para probar el Caso 2, no respuestas atribuidas a Ariztía. Las preguntas repetidas de la lista enviada se agrupan por tema, manteniendo los asuntos distintos.

Archivo: `app/src/main/assets/company_config.json`. La misma configuración se edita en Ajustes demo. **Activa** significa que cambiar el valor modifica el comportamiento implementado. **Anotación** significa que se puede registrar fácilmente la respuesta pero hará falta adaptar código para nuevas capacidades. Se evita ofrecer interruptores que prometan funciones inexistentes.

| Pregunta o decisión | Valor provisional | Clave o archivo | Efecto |
|---|---|---|---|
| Rango normal y límites aceptables | 0,50–1,50 inclusive | normalMin, normalMax | Activa |
| Rango crítico bajo y alto | menor que 0,20 o mayor que 2,00 | criticalLow, criticalHigh | Activa |
| Advertencia y límites exactos | Fuera de normal, sin superar umbrales críticos | Los cuatro límites; README | Activa, derivada |
| Tratamiento de cloro bajo frente a alto | Mismo flujo, valor visible diferencia dirección | pendingBusiness.lowVersusHigh | Anotación; flujo común implementado |
| Variación por granja, tanque, línea, bebedero o etapa del ave | Sin excepciones; cada punto puede tener cuatro límites propios | pointRanges, pendingBusiness.stagesAndPointTypes | Activa por punto; etapa requiere clasificación adicional |
| Normativa o política interna que fija rangos | Por confirmar | pendingBusiness.rangeAuthority | Anotación; no se inventó normativa |
| Cloro libre, total o ambos | Libre, conforme Caso 2 | analyte, pendingBusiness.totalChlorine | Libre implementado; ampliar modelo/API para total |
| Unidad y conversión | mg/L; conversión desactivada | unit, pendingBusiness.conversion, conversionFactor | Etiqueta activa; conversión pendiente |
| Decimales de visualización y registro | Dos al mostrar; valor de entrada sin redondear | decimals, pendingBusiness.displayPrecision | Activa visualización 0–4 |
| Registro actual en papel, Excel u otro sistema | Desconocido | pendingBusiness.currentRegister | Anotación |
| Instrumento y medición manual o automática | Simulación + formulario manual; instrumento desconocido | pendingBusiness.instrument, measurementMethod | Anotación; dos modos demo implementados |
| pH, temperatura y turbidez | Campo opcional de texto | extraFields | Activa lista de etiquetas; no sensores/unidades adicionales tipados |
| Frecuencia de muestreo de cada punto | Desconocida | pendingBusiness.samplingFrequency | Anotación; no confundir con refresco |
| Actualización de pantalla | Cada 60 s en primer plano | refreshSeconds | Activa |
| Lectura demasiado antigua | Después de 15 min | staleMinutes | Activa |
| Inicio de sesión, RUT, correo o credencial | Selector de usuarios ficticios OP01, SU01, JE01 | users; pendingBusiness.identity | Usuarios activos; autenticación real pendiente |
| Qué puede ver/crear cada perfil | Operario consulta/mide/actúa; supervisor además reconoce/cierra/corrige; jefatura consulta/exporta | roles | Activa; permisos de UI y ViewModel |
| Qué puede modificar o corregir cada perfil | Corrección del supervisor mediante nueva lectura | roles.correct, immutableMeasurements | Activa; no se sobrescribe original |
| Qué puede eliminar cada perfil | Nadie; conservación de auditoría | immutableMeasurements=true | Regla fija validada; borrado no implementado |
| Datos de solo consulta/inmutables | Lecturas y acciones originales | immutableMeasurements=true; API | Activa protección; cierres/ack permiten actualización |
| Multirrol y asignación a varias granjas | Admitidos por lista; se elige rol activo | users[].roles, users[].farms | Activa |
| Vistas separadas por login o compartidas | Pantallas comunes con funciones filtradas | roles, users; pendingBusiness.uiSeparation | Activa |
| Qué identifica un punto y sus datos | ID, granja, nombre de granja, galpón opcional, nombre | Point en Models.kt; POINTS del servidor y demoSnapshot | Activa en catálogos; editar ambas fuentes |
| Jerarquía de información | Granja > galpón opcional > punto > mediciones | pendingBusiness.hierarchy; Point | Modelo implementado; cambios de jerarquía requieren código |
| Cantidad de granjas y puntos | Dos granjas y cuatro puntos ficticios | demoSnapshot; mock_api/server.py POINTS | Catálogo editable en código |
| Destinatarios de alertas | Todos los roles, solo puntos asignados y dispositivo activo | notifyRoles, users[].farms | Activa localmente; push entre usuarios pendiente |
| Qué niveles notifican | Crítico y advertencia | notificationsEnabled, notifyWarning | Activa |
| Aviso inmediato o cada 15 min | Al detectar cambio en actualización; refresco 60 s | refreshSeconds; pendingBusiness.notifyLatency | Activa foreground; no push ni garantía inmediata |
| Qué dispara alerta/desviación | Primera lectura fuera de rango, automáticamente | autoDeviation; pendingBusiness.notificationDelay | Activa creación automática; tolerancia temporal pendiente |
| Qué es desviación | Caso abierto por punto que agrupa seguimiento; separado del aviso Android | Incident; autoDeviation | Implementado; ciclo revisable con empresa |
| Cierre de alerta y quién | Supervisor, acción registrada y lectura normal reciente | roles.close, closeRequiresNormal | Activa; historial conservado |
| Procedimiento ante crítico y quién lo resuelve | Texto de acción + supervisor cierra | pendingBusiness.correctiveProcedure; roles.close | Flujo implementado, no protocolo sanitario |
| Datos de acción correctiva | Texto de al menos 5 caracteres, autor, fecha, punto y desviación | pendingBusiness.actionRequiredFields | Implementado; fotos/firmas pendientes |
| Historial de alertas | Conservar abiertas y cerradas | Pantalla Alertas > Incluir cerradas | Implementado |
| Escalamiento si no se atiende | Indicador tras 30 min | escalationMinutes | Activa visual; notificación a terceros pendiente |
| Repetición y silenciamiento | No repetir por tiempo ni silenciar desde app | pendingBusiness.notificationRepeat, alertSilencing | Anotación; avisa al crear/cambiar gravedad |
| Comparar granjas o puntos | Conteos por estado por granja | showComparison | Activa; comparación avanzada pendiente |
| Dashboard de jefatura | Resumen, puntos y desviaciones abiertas | Home; roles.read | Implementado |
| Gráficos, promedio, mínimo, máximo, listado | Todos disponibles en detalle | showChart | Activa gráfico; lista y estadísticas incluidas |
| Histórico consultable y retención | 30 días en consulta, 90 días de lecturas locales | historyDays, retentionDays | Activa; se conservan acciones/alertas; servidor sin purga |
| Reportes exportables y formato | CSV | csvExport, roles.export; pendingBusiness.reportFormats | Activa CSV; PDF/Excel nativo pendiente |
| Qué debe funcionar sin internet | Consulta de snapshot, históricos/alertas cargadas; registro en cola | offlineWrites; pendingBusiness.connectivity | Activa registro; consulta local siempre |
| Internet real en galpones | Desconocido | pendingBusiness.connectivity | Anotación |
| Centralización de información | API Python compartida en red de prueba; modo local inicial | Ajustes: URL API y simulador local | Activa HTTP y cola; servidor demo sin auth |
| Origen AMINO/sensor/manual y existencia de API | API simulada incluida; interfaz empresarial desconocida | pendingBusiness.measurementMethod, amino | HTTP demo implementado; integración real futura |
| AMINO y ERP a futuro y para qué | Desactivados; faltan objetivo y contrato | pendingBusiness.amino, erp | Anotaciones; fuera del MVP del caso |
| Celulares propios/empresa, modelos, versión Android | Desconocido; minSdk 24 del proyecto | pendingBusiness.devices; app/build.gradle.kts | Compatibilidad Android conservada; flota pendiente |
| Seguridad y confidencialidad | Datos ficticios, almacenamiento privado, backup desactivado | pendingBusiness.security; AndroidManifest.xml | Medidas demo implementadas; autenticación/servidor productivo pendientes |
| Cantidad de usuarios y experiencia usando apps | 20 como estimación ficticia, navegación simple | pendingBusiness.expectedUsers, usability, userCount | Anotación; no límite ni prueba de carga |
| Indispensables en primera versión | Funciones del Caso 2 | pendingBusiness.mvp; README | MVP implementado en código; validación Android pendiente |

## Cómo aplicar una respuesta de la empresa

1. Registrar la respuesta y quién la confirmó, sin atribuir aprobación a valores de ejemplo.
2. Si la fila es activa, cambiar su clave, guardar y ejecutar las pruebas asociadas.
3. Si es anotación, reemplazar el texto pendiente y crear una tarea concreta para adaptar modelo, API, permisos, interfaz o pruebas. La anotación no habilita la función.
4. Probar límites, permisos, conexión/desconexión y cierre de alertas después de cambiar reglas.
5. Usar la misma configuración en todos los dispositivos de la demostración. No hay servicio central de configuración en este MVP.

## Tareas sugeridas para la pareja

Eduardo: abrir y ejecutar el proyecto, probar API y caché, revisar las claves activas con respuestas empresariales. Compañera: revisar navegación, formularios, tamaños de pantalla y matriz de permisos. Ambos: validar rangos, revisar seguimiento y cierre, recopilar evidencias y actualizar Trello. Reparto sugerido, ajustable por el equipo.
