# CU18 — Validar elegibilidad y duplicados

- **Caso de uso:** [CU18](../CasosDeUso/CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu18)
- **Diagramas de grado:** CU img 047 · análisis img 060 · secuencia img 130 · mockup img 143 (§5.4.18, §6.2.18, §7.4.18, §7.5.18)
- **Estado frente a los diagramas:** Difiere (ver tabla). Revisadas las imágenes 047, 060, 130 y 143; **no se abrieron** la 090 ni la 091 (ver «Para rehacer»).

## Flujo real implementado

No hay pantalla ni ruta HTTP: es un servicio interno que el CU02 llama al iniciar y al enviar una postulación ([`EnvioPostulacionService`](CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)).

1. `ValidacionPostulacionService.validar(usuarioCodigo, convocatoriaCodigo)` (`@PreAuthorize("hasRole('POSTULANTE')")`, solo lectura). El usuario sale del token verificado.
2. `ValidacionPostulacionConsultaRepository.convocatoria()` → si no existe, `404` `ERR_CV_01`.
3. `ValidacionPostulacionConsultaRepository.postulanteDe()` → si la cuenta no tiene datos de postulante, `403` `ERR_VAL_02`.
4. `verificarCondiciones()`: cuenta activa (`403` `ERR_VAL_02`). Aquí se suman las condiciones que defina la DUBSS.
5. `verificarConvocatoriaAbierta()`: estado `ABIERTA` y fecha actual entre apertura y cierre, con el `Clock` del proyecto (`409` `ERR_VAL_03`).
6. `verificarSinDuplicados()`: `postulacionesEnLaGestion()` trae las postulaciones del postulante en **todas** las convocatorias de la gestión. Un `borrador` se ignora; una `devuelta` de la misma convocatoria es el reenvío (se devuelve su código en `postulacionAReenviar`); cualquier otra es duplicada (`409` `ERR_VAL_01`, con la gestión, el estado visible y la fecha de envío).
7. Devuelve `ResultadoValidacion(postulanteId, convocatoriaId, postulacionAReenviar)`.

Cada rechazo se loguea `WARN` con su `errorCode`, sin datos personales. No se anota en la bitácora. No escribe nada: repetirla da el mismo resultado. Si la base falla, la excepción cae en el `500` genérico (`ERR_SYS_00`) y el CU02 no acepta la postulación (A5).

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Análisis (060) / Secuencia (130) | `ValidacionController` entre `PostulacionView` y las entidades | No hay controller ni ruta: `ValidacionPostulacionService`, que el CU02 llama dentro de su transacción | El actor es el sistema y el CU02 lo incluye (decisión 2 del CU) |
| Secuencia (130) | `validarElegibilidad(postulanteId, convocatoriaId)` | `validar(usuarioCodigo, convocatoriaCodigo)`: el postulante sale del token, ambos son códigos públicos | Seguridad y regla de identificadores públicos |
| Secuencia (130) | `find(postulante)`, `find(convocatoria)` y `find(postulacionVigente)` como tres consultas a entidades | Tres consultas JDBC (`postulanteDe`, `convocatoria`, `postulacionesEnLaGestion`); la última trae las de toda la gestión | Se decide en el servicio cuál es duplicado, borrador o reenvío |
| Secuencia (130) | `verificarCondiciones(postulante)` | `verificarCondiciones()` solo comprueba la cuenta activa; el resto de las condiciones queda por definir | «Por definir con la DUBSS» |
| Secuencia (130) | Sin comprobar la convocatoria | Se comprueba que esté `ABIERTA` y en fechas (`ERR_VAL_03`) | Precondición del CU (A7) |
| Secuencia (130) | Resultados `validacionExitosa()`, `postulacionDuplicada()`, `noElegible(motivo)` | Devuelve `ResultadoValidacion` o lanza `ConflictException` (`ERR_VAL_01`/`ERR_VAL_03`) o `ForbiddenException` (`ERR_VAL_02`) | Convención de errores del proyecto |
| Secuencia (130) | La duplicada se rechaza siempre | Un reenvío de una `devuelta` (A3) y un `borrador` no se rechazan; el reenvío devuelve la postulación a reenviar | A3 del CU y borradores del CU17 |
| Secuencia (130) | Duplicado solo "en la gestión" sin precisar | Vigente = enviada (no borrador) en cualquier convocatoria de la gestión | Decisión 1 del CU |
| Mockup (143), pantalla 1 (web) | Resultados de validación con conteos (elegibles, duplicadas, datos inconsistentes) y tabla por postulante | **No implementada** | Exige guardar cada resultado y no hay tabla en el diseño lógico (decisión 3 del CU) |
| Mockup (143), pantalla 1 | Resultado «Datos inconsistentes» (CI vs. registro universitario) | **No implementado** | No hay padrón contra el cual comparar |
| Mockup (143), pantalla 2 (móvil) | «Postulación duplicada» con «Ver mi postulación» y otros resultados | Esta validación solo devuelve el motivo (gestión, estado visible y fecha); la pantalla la arma el CU02 | Decisión 4 del CU |

## Para rehacer los diagramas

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 130 | Sí | Ver especificación abajo | [ ] |
| Análisis | 060 | Sí | Ver especificación abajo | [ ] |
| Mockup | 143 | Sí | Quitar (o marcar como futura) la pantalla web de resultados y el resultado «Datos inconsistentes»; dejar el mensaje móvil como parte del CU-02 | [ ] |
| Clases / dominio | 090 | Por verificar | No se abrió la imagen. El CU no agrega ni cambia clases del dominio; solo debe coincidir `Postulacion` (`estado`, `fechaEnvio`, `numeroEnvios`) | [ ] |
| Diseño lógico BD | 091 | Por verificar | No se abrió la imagen. Este CU **no agregó migraciones**: solo se apoya en `postulacion` y su índice único `(postulante_id, convocatoria_id)` | [ ] |
| Casos de uso | 047 | Sí | El diagrama dibuja al Postulante como actor; el RF-13 y la lista de CU dicen «Sistema (Módulo IA)». La validación la ejecuta el sistema dentro del CU02 (no hay módulo de IA en este CU): unificar el actor | [ ] |

Arquitectura (118–120): no cambia. Texto de grado: el RF-13 dice «por convocatoria» y el título y la secuencia «por gestión»; unificar en «por gestión» y decir que un borrador y el reenvío de una devuelta no cuentan.

### Especificación de cada diagrama afectado

#### Secuencia — img 130 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Control` CU02 (envío de la postulación) · `Control` ValidacionPostulacionService · `Entity` Postulante · `Entity` Convocatoria · `Entity` Postulacion. (El actor y `PostulacionView` quedan en el diagrama del CU02.)

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | CU02 → ValidacionPostulacionService | `validar(usuarioCodigo, convocatoriaCodigo)` | [cambia] |
| 2 | ValidacionPostulacionService → Convocatoria | `convocatoria(codigo)` / `DatosConvocatoria` | [cambia] |
| 3 | ValidacionPostulacionService → Postulante | `postulanteDe(usuarioCodigo)` / `DatosPostulante` | [cambia] |
| 4 | ValidacionPostulacionService → ValidacionPostulacionService | `verificarCondiciones(postulante)` | [igual] |
| 5 | ValidacionPostulacionService → ValidacionPostulacionService | `verificarConvocatoriaAbierta(convocatoria)` | [nuevo] |
| 6 | ValidacionPostulacionService → Postulacion | `postulacionesEnLaGestion(postulanteId, gestionId)` / lista | [cambia] |
| 7 | ValidacionPostulacionService → ValidacionPostulacionService | `verificarSinDuplicados()` (ignora borrador; detecta reenvío) | [cambia] |

**Bloque `alt`:**
- `[sin duplicados y elegible]` → `ResultadoValidacion(postulanteId, convocatoriaId, postulacionAReenviar)` al CU02 [cambia: reemplaza `validacionExitosa()`].
- `[ya existe postulación vigente en la gestión (A1)]` → `ConflictException` `ERR_VAL_01` [cambia: reemplaza `postulacionDuplicada()`].
- `[no cumple las condiciones (A2)]` → `ForbiddenException` `ERR_VAL_02` [cambia: reemplaza `noElegible(motivo)`].
- `[la convocatoria no recibe postulaciones]` → `ConflictException` `ERR_VAL_03` [nuevo].
- `[la convocatoria no existe]` → `NotFoundException` `ERR_CV_01` [nuevo].

Notas del diagrama: A3 (reenvío de una devuelta: no es duplicado, se devuelve `postulacionAReenviar`) [cambia]; A4 (idempotente, solo lectura) [igual]; A5 (si la base falla, `500` y no se acepta) [igual].

#### Análisis (comunicación) — img 060 (rehacer completo)

**Clases:** `Boundary` PostulacionView (del CU02) · `Control` ValidacionPostulacionService [cambia: antes `ValidacionController`] · `Entity` Postulante · `Entity` Convocatoria · `Entity` Postulacion · actor Postulante.

**Enlaces y mensajes:** Postulante → PostulacionView `1: enviarPostulacion()` [igual]; PostulacionView → ValidacionPostulacionService `1.1: validar(usuarioCodigo, convocatoriaCodigo)` [cambia]; ValidacionPostulacionService → Convocatoria `1.2: convocatoria(codigo)` [cambia]; → Postulante `1.3: postulanteDe(usuarioCodigo)` [cambia]; → Postulacion `1.4: postulacionesEnLaGestion(postulanteId, gestionId)` [cambia]; autollamada `1.5: verificarCondiciones()` y `1.6: verificarConvocatoriaAbierta()` [nuevo].
