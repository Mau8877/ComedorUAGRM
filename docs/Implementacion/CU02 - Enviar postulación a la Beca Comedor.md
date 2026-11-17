# CU02 — Enviar postulación a la Beca Comedor

- **Caso de uso:** [CU02](../CasosDeUso/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu02)
- **Diagramas de grado:** CU img 039 · análisis img 052 · secuencia img 122 · mockup img 135 (y sus secciones §5.4.2, §6.2.2, §7.4.2, §7.5.2). Revisadas las imágenes 122 y 135; **no se abrieron** la 039, la 052, la 090 ni la 091 (ver «Para rehacer»).
- **Estado frente a los diagramas:** Difiere (ver tabla). Implementados el backend, la web y la app móvil. **Probado con Postgres real (2026-11-17)** el repositorio (`EnvioPostulacionConsultaRepositoryTest`, 13 tests, incluidos `datos_extra`) y la cadena de migraciones completa, y probado de punta a punta el envío al módulo de IA (ver abajo); **sin probar con el stack completo levantado ni a mano en la web o la app**: la web se probó con el cliente HTTP simulado y la app con el servicio simulado. **La API del módulo de IA ya existe** (tarea B3, `POST /api/v1/puntaje`) y se probó de punta a punta con Postgres real: una postulación `pendiente` con `datos_extra` pasó por `PrecalificacionService`, la IA real respondió, quedó el puntaje vigente con el modelo `v1.0` y la postulación en `calificada`. Además el core necesita una versión de modelo registrada para aceptar el puntaje: la migración `V20261121100000__sembrar_modelo_preseleccion_v1_0.sql` registra `v1.0` **inactiva** con las diez ponderaciones por defecto (necesidad económica y familiar; el promedio académico pesa 0 porque el auditor ya suma hasta 30 puntos de rendimiento académico en el CU07), que es la versión con la que responde la IA mientras no haya una vigente (sin esa fila toda postulación quedaba `pendiente`). Al guardar la primera versión desde el CU26 se crea `v1.1` y `v1.0` queda en el historial.

## Flujo real implementado

Todo exige sesión de Postulante (`401` sin sesión, `403` `ERR_AUTH_07` sin el rol). El postulante sale del token, nunca del cuerpo.

**Iniciar** — `POST /api/v1/postulaciones` (`201` si crea el borrador, `200` si ya había uno):

1. `PostularScreen` (web: ruta `/postular`; móvil: `/postular`) → `EnvioPostulacionController.iniciar` → `EnvioPostulacionService.iniciar(usuario)`.
2. `EnvioPostulacionConsultaRepository.perfil()` (si la cuenta no es de un postulante: `403` `ERR_VAL_02`) y `convocatoriaAbierta()` (`ABIERTA` y dentro de fechas; si no hay: `409` `ERR_ENV_01` con la próxima convocatoria, de `proximaConvocatoria()`).
3. `ValidacionPostulacionService.validar()` (CU18): elegibilidad, convocatoria abierta y duplicados. Un `borrador` o una `devuelta` de esa convocatoria no cuentan como duplicado.
4. `postulacionDe()`: si hay una (`borrador`, o la `devuelta` a corregir) se devuelve; si no, `crearBorrador()` (`INSERT` en `borrador`; si dos peticiones llegan a la vez, el índice único hace fallar a la segunda y esta recibe `409` `ERR_VAL_01`).
5. Respuesta: `FormularioPostulacionResponse` con la postulación, la convocatoria, el perfil, las opciones de las listas (`OpcionesSocioeconomicas`) y las respuestas ya guardadas.

**Completar y adjuntar** — pasos 2 y 3 de la pantalla. Los documentos usan el CU16 (`/api/v1/postulaciones/{codigo}/documentos`) sobre la postulación ya creada. El formulario no guarda nada en el servidor hasta enviar, salvo que se pulse «Guardar borrador» ([CU17](CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md)).

**Enviar** — `POST /api/v1/postulaciones/{codigo}/enviar` (`200` con el comprobante):

1. `EnvioPostulacionService.enviar(usuario, codigo, request)`, `@Transactional`: `cabecera()` (ajena o inexistente: `404` `ERR_ENV_02`), `bloquearPostulacion()` (`FOR UPDATE`) y relectura del estado.
2. Si ya está `pendiente` o `calificada`: devuelve el comprobante de nuevo, sin tocar nada (A6). Si no está en `borrador` ni `devuelta`: `409` `ERR_ENV_03`.
3. `ValidacionPostulacionService.validar()` otra vez (A4, A8): `ERR_VAL_01`, `ERR_VAL_02` o `ERR_VAL_03`.
4. `validarRespuestas()`: los seis campos y los documentos obligatorios de la convocatoria sin archivo → `400` `ERR_ENV_04` con la lista de lo que falta; rangos, decimales y opciones → `400` `ERR_ENV_05`.
5. `guardarRespuesta()` (`UPSERT`), `Postulacion.enviar(ahora)` (`pendiente`, fecha de envío y un envío más) y una fila en `postulacion_historial` (estado anterior → `pendiente`; motivo «Reenvío tras corregir» si venía de `devuelta`).
6. Con el envío confirmado (`afterCommit`), `PrecalificacionService.solicitar(id)` en segundo plano. El comprobante se arma con la postulación ya actualizada.

**Precalificación** (CU03, solo el lado del core): `@Async` en el ejecutor `ejecutorPrecalificacion` → `PrecalificacionService.precalificar()`: lee las variables (`PrecalificacionConsultaRepository.datosPendiente`, solo si sigue `pendiente`), llama a la IA sin transacción (`IaHttpPrecalificacionClient`, 2 s de conexión y 5 s de lectura) y, con el puntaje, en una transacción corta: bloquea la fila, busca el modelo por la versión que informó la IA, retira el puntaje vigente, inserta el nuevo y `CalificacionPostulacionService.marcarCalificada()` (`calificada` + historial sin usuario). Si la IA no responde, rechaza los datos o su versión de modelo no está registrada, la postulación sigue `pendiente`; `PrecalificacionReintentoJob` la reintenta cada 5 minutos (lote de 500, y corta la pasada en el primer fallo de conexión).

**Contrato con la IA (implementado en `comedor-uagrm-ia`, tarea B3; ver su [README](../../projects/backend/comedor-uagrm-ia/README.md)):** `POST {IA_BASE_URL}/api/v1/puntaje` (por defecto `http://comedor-uagrm-ia:8000`).

```json
// Solicitud
{ "postulacion": "uuid", "ingresosFamiliares": 1800.00, "dependientes": 4, "procedencia": "Provincia",
  "carrera": "Ing. Informática", "patrimonio": "Sin vivienda propia",
  "estadoVulnerabilidad": "Hogar monoparental", "promedioAcademico": 72.5,
  "dependenciaEconomica": "Solo de padre o madre", "ocupacionSosten": "Comerciante minorista",
  "tieneHijos": false, "cantidadHijos": null, "discapacidad": false,
  "modelo": "v1.4", "parametros": { "ingresos_familiares": 28, "dependientes": 12, "procedencia": 10,
                                    "patrimonio": 9, "estado_vulnerabilidad": 7, "promedio_academico": 0,
                                    "dependencia_economica": 10, "ocupacion_sosten": 8,
                                    "discapacidad": 10, "hijos": 6 } }
// Respuesta 200
{ "puntaje": 63.4, "modelo": "<modelo_preseleccion.version registrada>", "detalle": { "…": "opcional" } }
```

**Actualizado con el [CU26](CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md):** la solicitud lleva además `modelo` (la versión vigente) y `parametros` (clave de la variable → ponderación), para que la IA no guarde estado y aplique los parámetros que recibe. Ambos son `null` solo si todavía no se guardó ninguna versión (entonces la IA usa su modelo por defecto y responde con la versión que usó). El reprocesamiento del CU26 usa la misma ruta y exige que la respuesta repita la versión pedida.

`puntaje` entre 0 y 100 según el cliente, **pero la tabla solo admite 0–70** (`CHECK` de la decisión del CU07: el auditor suma hasta 30): un puntaje mayor a 70 hace fallar el registro, así que la IA debe devolverlo ya normalizado a 0–70 (**pendiente con la DUBSS y B3**, ver el CU26); `modelo` debe existir en `modelo_preseleccion`. Un `4xx` distinto de `404` se trata como «la IA rechazó los datos»; un `404` o cualquier fallo de red o `5xx`, como «no disponible».

**Formulario ampliado al de la DUBSS (FORM-DUBS-001):** además de los seis campos de columna, el cuerpo de `enviar` (y de `PUT …/borrador`, CU17) lleva un bloque `complementarios` (`DatosComplementarios`) con las preguntas del formulario en papel: de quién depende, ocupación, rango de ingresos, si tiene hijos y cuántos, grupo familiar (hasta 10 integrantes), residencia, tenencia de la vivienda, ambientes (dormitorios, baños, comedor, sala, patio), otro beneficio, discapacidad y comentario. **No hay migración:** se guardan en `respuesta_socioeconomica.datos_extra` (jsonb), que el diseño lógico ya preveía; `EnvioPostulacionConsultaRepository.guardarComplementarios()` los escribe justo después de `guardarRespuesta()` y `ComplementariosJson` los convierte de y hacia JSON. Obligatorios al enviar (`ERR_ENV_04`): de quién depende, ocupación, rango de ingresos, si tiene hijos (y cuántos, si dijo que sí), tenencia de la vivienda, otro beneficio y discapacidad; el resto es opcional. Las opciones de las listas salen de `OpcionesSocioeconomicas` y viajan en `opciones` (`dependencias`, `ocupacionesSosten`, `rangosIngresos`, `cantidadesHijos`, `tenenciasVivienda`). Lo que cada campo tiene de «por definir con la DUBSS» (qué es obligatorio, si entran al puntaje) sigue abierto. **La IA recibe cinco de ellos** (`dependenciaEconomica`, `ocupacionSosten`, `tieneHijos`, `cantidadHijos`, `discapacidad`), que el core lee de `datos_extra` en `PrecalificacionConsultaRepository.datos()`; el resto (grupo familiar, residencia, ambientes, otro beneficio, comentario) no puntúa. Son `null` en una postulación enviada antes del formulario ampliado: la IA no los cuenta y reparte el 100 % entre las variables que sí tienen dato.

**Cliente web:** `PostularScreen` (`useIniciarPostulacion` al montar, con una guarda contra el doble `POST`; `useEnviarPostulacion`), `FormularioSocioeconomico` (TanStack Form + Zod, error inline solo en campos tocados), `DocumentosPostulacionPanel` en el paso 3 y `ComprobanteEnvio`. «Postular» y «Corregir y reenviar» (`enlacesCu02.ts`) abren `/postular`. **Cliente móvil:** `PostularScreen` (`StatefulWidget` + `PostulacionesService.iniciar/enviar`), validaciones de `validacion_formulario.dart`; el paso 3 lista los documentos y manda a la pantalla del CU16 para adjuntarlos.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (122) / Análisis (052) | `PostulacionController` con `iniciarPostulacion()`, `registrarPostulacion()` y `reenviarPostulacion()` | `EnvioPostulacionController` con dos rutas: `POST /postulaciones` (iniciar) y `POST /postulaciones/{codigo}/enviar` (enviar y reenviar, la misma ruta) | El reenvío es el mismo envío sobre una postulación `devuelta`; el backend decide por el estado |
| Secuencia (122) | `create(postulacion)` recién al enviar | La postulación se crea al iniciar, en `borrador`, y el envío la pasa a `pendiente` | Los documentos del CU16 necesitan una postulación existente antes de enviar |
| Secuencia (122) | `find(convocatoriaAbierta)` sobre `Convocatoria` | `convocatoriaAbierta(ahora)` (JDBC) y `proximaConvocatoria` | Se devuelve la próxima convocatoria en el mismo mensaje de error |
| Secuencia (122) | `validarElegibilidad(postulante, convocatoria)` como autollamada del controlador | `ValidacionPostulacionService.validar()` (CU18) | Servicio incluido, ver [CU18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md) |
| Secuencia (122) | `create(respuesta)` / `update(respuesta)` como dos mensajes | Un solo `guardarRespuesta()` con `INSERT … ON CONFLICT DO UPDATE` | La tabla es 1:1 con la postulación; crear y reenviar son el mismo guardado |
| Secuencia (122) | `solicitarPrecalificacion(postulacion)` como autollamada antes del comprobante | La precalificación corre **después** de confirmar el envío y en segundo plano (`PrecalificacionService.solicitar`, `@Async`); el comprobante no la espera | Una IA lenta o caída no debe retener al postulante ni deshacer el envío (A7) |
| Secuencia (122) | Sin historial, sin bloqueo, sin idempotencia | Fila de `postulacion_historial`, `FOR UPDATE` y comprobante repetido ante un envío repetido | A6 / RNF-03 y la trazabilidad de CU19/CU04 |
| Secuencia (122) | `validarCamposObligatorios()` en la vista | Se valida en la vista **y** en el servicio (`ERR_ENV_04` / `ERR_ENV_05`), incluidos los documentos obligatorios | El servidor no confía en el cliente |
| Secuencia (122) | Sin componentes de IA, ejecutor ni job | `IaHttpPrecalificacionClient`, `PrecalificacionService`, `CalificacionPostulacionService`, `PrecalificacionReintentoJob` y el ejecutor `ejecutorPrecalificacion` | Flujo A7: reintento automático |
| Mockup (135), pantalla 1 | Formulario con «Carrera» como lista editable y «Guardar borrador» | La carrera es un dato del perfil (solo lectura, paso 1); «Guardar borrador» lo agrega el [CU17](CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md) | La carrera no se falsea y el borrador es otro CU |
| Mockup (135), pantallas 1 y 2 | Tres pasos; el paso 3 lista los documentos con «Adjuntado» y el aviso «¿Enviar su postulación?» | Igual en la web y en la app; en la app, adjuntar abre la pantalla del CU16 | La app reutiliza la pantalla con cámara del CU16 |
| Mockup (135), pantalla 2 | Seis campos: ingresos, dependientes, procedencia, patrimonio, vulnerabilidad y promedio | Cinco secciones con los datos del formulario oficial de la DUBSS (FORM-DUBS-001): ingresos y rango, dependencia económica, grupo familiar (hasta 10), residencia y vivienda, y otros datos (otro beneficio, discapacidad, comentario). Los seis campos originales se conservan | El mockup se diseñó antes de tener el formulario real; el papel manda. El «estado de vulnerabilidad» y el «promedio» no figuran en el papel pero se mantienen porque la IA los usa |
| Diseño lógico (091) | `respuesta_socioeconomica` con seis columnas y `datos_extra` (jsonb) | Los datos del formulario oficial van dentro de `datos_extra`, con la forma de `DatosComplementarios` | Evita migrar el esquema mientras la DUBSS no fije qué campos puntúan; si pasan a puntuar se evalúa pasarlos a columnas |
| Mockup (135) | Opciones de «Procedencia», «Patrimonio» y «Estado de vulnerabilidad» | Salen del backend (`OpcionesSocioeconomicas`), no de cada cliente; el catálogo definitivo está por definir con la DUBSS | Un solo lugar a cambiar cuando la DUBSS fije el catálogo |
| Mockup (135) | El comprobante muestra el código | Muestra código, convocatoria, gestión, fecha de envío y estado | Datos útiles que ya tiene el servidor |
| Texto del CU (precondición) | La IA calcula el puntaje y la postulación pasa a «calificada» | Hoy toda postulación queda «pendiente» mientras la tarea B3 no exponga `POST /api/v1/puntaje` | El módulo de IA es un esqueleto |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 122 | Sí | Ver especificación abajo | [ ] |
| Análisis | 052 | Sí | Reemplazar `PostulacionController` por `EnvioPostulacionController`; agregar `EnvioPostulacionService`, `ValidacionPostulacionService` y `PrecalificacionService`; el comprobante sale del servicio, no de la vista | [ ] |
| Mockup | 135 | Sí | Rehacer la pantalla 2 con las cinco secciones del formulario oficial (ver la tabla de diferencias); «Guardar borrador» es el CU17 (ver su ficha); hacer «Carrera» un dato de solo lectura del paso 1; agregar el paso 1 «Datos del postulante» y la pantalla del comprobante | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. `Postulacion` suma `enviar()` y `calificar()`; `RespuestaSocioeconomica` debe tener todos sus atributos opcionales (nulos en un borrador) | [ ] |
| Diseño lógico BD | 091 | Por verificar | No se abrió la imagen. Anotar que `datos_extra` guarda el bloque del formulario oficial. Migración `V20261119100000__crear_respuesta_socioeconomica.sql`: columnas de datos nullables y `CHECK` de rango; coincide con [DiagramaConceptual.mermaid](../DiagramaConceptual.mermaid) | [ ] |
| Casos de uso | 039 | Por verificar | No se abrió la imagen. El «Módulo IA» queda como actor secundario del CU03, no del CU02 | [ ] |

**Aplicación móvil:** también envía el bloque `complementarios` (modelos `DatosComplementarios` y `OpcionesFormulario`, reglas `ValoresComplementarios` en `validacion_formulario.dart` y las secciones del formulario en `widgets/formulario_oficial.dart`, con `FormularioOficialController`). La pantalla usa un `SingleChildScrollView` en vez de un `ListView` perezoso, para que `Form.validate()` revise todos los campos aunque estén fuera de la pantalla. En el móvil, «Rango de ingresos» va en «Dependencia económica» y «Patrimonio» y «Estado de vulnerabilidad» quedan en la primera sección (en la web, patrimonio va en «Residencia y vivienda» y vulnerabilidad en «Otros datos»).

Arquitectura (118–120): cambia, se suma una integración (llamada HTTP del core al módulo de IA, un ejecutor de tareas y un job programado). Texto de grado: el CU-02 dice «el sistema solicita la precalificación» en el paso 9; ahora es posterior al comprobante y en segundo plano.

### Especificación de cada diagrama afectado

#### Secuencia — img 122 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Postulante · `Boundary` PostularScreen · `Control` EnvioPostulacionController · `Control` EnvioPostulacionService · `Control` ValidacionPostulacionService · `Entity` Convocatoria · `Entity` Postulacion · `Entity` RespuestaSocioeconomica · `Control` PrecalificacionService · `Object` ModuloIA.

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Postulante → PostularScreen | `nuevaPostulacion()` | [igual] |
| 2 | PostularScreen → EnvioPostulacionController | `POST /api/v1/postulaciones` | [cambia] |
| 3 | EnvioPostulacionController → EnvioPostulacionService | `iniciar(usuarioCodigo)` | [nuevo] |
| 4 | EnvioPostulacionService → Convocatoria | `convocatoriaAbierta(ahora)` / `ConvocatoriaAbierta` | [cambia] |
| 5 | EnvioPostulacionService → ValidacionPostulacionService | `validar(usuarioCodigo, convocatoriaCodigo)` | [nuevo] |
| 6 | EnvioPostulacionService → Postulacion | `postulacionDe(postulanteId, convocatoriaId)`; si no hay, `crearBorrador()` | [nuevo] |
| 7 | EnvioPostulacionService → PostularScreen | `FormularioPostulacionResponse` (perfil, convocatoria, opciones, respuestas) → `mostrarFormulario()` | [cambia] |
| 8 | Postulante → PostularScreen | `completarFormulario(datosSocioeconomicos)`, `adjuntarDocumentos(archivos)` (CU16) | [igual] |
| 9 | Postulante → PostularScreen | `enviarPostulacion(formulario)` + confirmación | [cambia] |
| 10 | PostularScreen → EnvioPostulacionController | `POST /api/v1/postulaciones/{codigo}/enviar` | [cambia] |
| 11 | EnvioPostulacionController → EnvioPostulacionService | `enviar(usuarioCodigo, codigo, request)` | [nuevo] |
| 12 | EnvioPostulacionService → Postulacion | `cabecera()`, `bloquearPostulacion()`, `estadoActual()` | [nuevo] |
| 13 | EnvioPostulacionService → ValidacionPostulacionService | `validar(usuarioCodigo, convocatoriaCodigo)` | [cambia] |
| 14 | EnvioPostulacionService → EnvioPostulacionService | `validarRespuestas()` (campos, documentos obligatorios, rangos y opciones) | [nuevo] |
| 15 | EnvioPostulacionService → RespuestaSocioeconomica | `guardarRespuesta(postulacionId, respuesta)` | [cambia] |
| 16 | EnvioPostulacionService → Postulacion | `enviar(ahora)`; fila de `PostulacionHistorial` | [cambia] |
| 17 | EnvioPostulacionService → PostularScreen | `ComprobantePostulacionResponse` → `mostrarComprobante(codigo)` | [cambia] |
| 18 | EnvioPostulacionService → PrecalificacionService | `solicitar(postulacionId)` (**tras confirmar**, asíncrono) | [cambia] |
| 19 | PrecalificacionService → ModuloIA | `POST /api/v1/puntaje` (sin transacción) | [nuevo] |
| 20 | PrecalificacionService → Postulacion | `registrar puntaje` y `calificar()` | [nuevo] |

**Bloques `alt`:**
- `[no hay convocatoria abierta]` (tras el 4) → `ConflictException` `ERR_ENV_01` con las fechas de la próxima → `mostrarAviso()` [cambia: reemplaza `sinConvocatoria(fechasProxima)`].
- `[no elegible, duplicada o convocatoria cerrada]` (tras el 5 y el 13) → `ForbiddenException` `ERR_VAL_02` / `ConflictException` `ERR_VAL_01`, `ERR_VAL_03` → `mostrarMotivo()` [cambia].
- `[falta un campo o documento obligatorio]` (tras el 14) → `ValidationException` `ERR_ENV_04` [nuevo]; `[dato inválido]` → `ERR_ENV_05` [nuevo].
- `[la postulación ya estaba pendiente o calificada]` (tras el 12) → comprobante repetido, sin pasos 13 a 18 [nuevo, A6].
- `[postulación devuelta (A5)]`: los pasos 2 a 17 se repiten sobre la postulación `devuelta` (en el paso 6 se devuelve en vez de crear; en el 16 el historial dice «Reenvío tras corregir») [cambia: reemplaza el bloque `opt` con `update(postulacion)` y `update(respuesta)`].
- `[la IA no responde (A7)]` (tras el 19) → la postulación sigue `pendiente`; `PrecalificacionReintentoJob` repite el 19 y el 20 cada 5 minutos [nuevo].

#### Análisis (comunicación) — img 052 (rehacer completo)

**Clases:** actor Postulante · `Boundary` PostularScreen (web y móvil) · `Control` EnvioPostulacionController · `Control` EnvioPostulacionService · `Control` ValidacionPostulacionService · `Control` PrecalificacionService · `Entity` Convocatoria · `Entity` Postulacion · `Entity` RespuestaSocioeconomica · `Entity` PostulacionHistorial.

**Enlaces y mensajes:** Postulante → PostularScreen `1: nuevaPostulacion()`; PostularScreen → EnvioPostulacionController `1.1: iniciar()` [cambia]; → EnvioPostulacionService `1.2: iniciar(usuario)`; → Convocatoria `1.3: convocatoriaAbierta()`; → ValidacionPostulacionService `1.4: validar()`; → Postulacion `1.5: postulacionDe()/crearBorrador()`; Postulante → PostularScreen `2: enviarPostulacion()`; PostularScreen → EnvioPostulacionController `2.1: enviar()`; → EnvioPostulacionService `2.2: enviar(usuario, codigo, request)`; → ValidacionPostulacionService `2.3: validar()`; → RespuestaSocioeconomica `2.4: guardarRespuesta()`; → Postulacion `2.5: enviar()`; → PostulacionHistorial `2.6: registrar()`; → PrecalificacionService `2.7: solicitar()` [cambia].

#### Mockup — img 135 (rehacer)

- **Pantalla «Paso 1 de 3 · Datos del postulante»** [nuevo]: nombre, CI, carrera, convocatoria y fecha de cierre, de solo lectura; nota «Estos datos salen de tu perfil»; botón «Continuar».
- **Pantalla «Paso 2 de 3 · Datos socioeconómicos»** [cambia]: «Ingresos familiares mensuales (Bs) *», «N.º de dependientes *», «Procedencia *» (lista), «Patrimonio *» (lista), «Estado de vulnerabilidad *» (lista), «Promedio académico *» (`Ej. 72`); error inline «Este campo es obligatorio.»; aviso arriba «Revisa los campos marcados…»; botones «Atrás» y «Continuar» (sin «Guardar borrador»; la carrera deja de ser una lista).
- **Pantalla «Paso 3 de 3 · Documentos y envío»** [igual + cambio]: lista de documentos requeridos con «Adjuntado»/«Falta» y «Obligatorio»/«Opcional»; en la app, botón «Adjuntar documentos»; «Enviar postulación» y el diálogo «¿Enviar tu postulación?» con «Cancelar» y «Confirmar envío».
- **Comprobante** [nuevo]: «Postulación enviada» (o «Postulación reenviada»), estado, código, convocatoria, gestión, fecha de envío y «Ver mi postulación».

#### Diseño lógico de la BD — img 091

[nuevo en el CU] `respuesta_socioeconomica`: `postulacion_id bigint PK, FK`, `codigo_respuesta_socioeconomica uuid UQ`, `ingresos_familiares numeric(12,2) NULL`, `dependientes smallint NULL`, `procedencia varchar(150) NULL`, `patrimonio varchar(255) NULL`, `estado_vulnerabilidad varchar(50) NULL`, `promedio_academico numeric(5,2) NULL`, `datos_extra jsonb NULL`, `creado_en`, `actualizado_en`, `eliminado_en`; `CHECK` de `ingresos_familiares >= 0`, `dependientes >= 0` y `promedio_academico` entre 0 y 100. Relación 1:0..1 con `postulacion`.
