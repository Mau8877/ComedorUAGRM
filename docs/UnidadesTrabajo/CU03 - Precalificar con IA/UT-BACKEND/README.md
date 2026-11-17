# Unidades de trabajo — CU03-BACKEND

**Caso de uso:** precalificar cada postulación enviada con el módulo de IA (puntaje de vulnerabilidad de 0 a 70), en segundo plano, con reintento automático.
**Fuente CU:** [`CU03 - Precalificar con IA.md`](../../../CasosDeUso/CU03%20-%20Precalificar%20con%20IA.md)
**Diagramas de grado:** contexto img 040 · análisis img 053 · secuencia img 123 · mockup img 136 (la 123, la 053 y la 136 revisadas; **no se abrió** la 040)
**Recurso HTTP / contrato:** no hay endpoint público. El contrato es interno entre el core y la IA: `POST {app.ia.base-url}/api/v1/puntaje`.
**Estado:** implementadas. Estas fichas se redactaron **después** de construir el CU; el flujo real y las diferencias con los diagramas están en [`Implementacion/CU03`](../../../Implementacion/CU03%20-%20Precalificar%20con%20IA.md).

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md),
[`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md),
[`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md),
[`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md),
[`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) y, para el módulo de IA,
[`PUNTAJE_IA.md`](../../../../.claude/rules/backend-ia/PUNTAJE_IA.md). Estas fichas no sustituyen esas reglas.

## Decisiones de diseño

- **Dos proyectos, una sola capa de UT.** El CU toca el core (Java) y el módulo de IA (Python); ambos son «backend» según `CLAUDE.md`, así que comparten esta carpeta. Las UT-01 y UT-02 son de `comedor-uagrm-ia` y las demás del core.
- **La IA no guarda estado:** cada solicitud lleva la versión y las ponderaciones (`modelo`, `parametros`), y la respuesta repite la versión. Así el reprocesamiento del CU26 usa el mismo contrato.
- **No se llama a la IA dentro de una transacción:** el cálculo puede tardar hasta 5 s y retendría una conexión; se llama sin transacción y solo registrar el resultado va en una transacción corta (con la postulación bloqueada).
- **El estado cambia en otro módulo:** `postulaciones` es dueño del estado de la postulación, así que `preseleccion` lo cambia a través de `CalificacionPostulacionService`, nunca desde su repositorio (regla de dependencia).
- **El puntaje de la IA nunca pasa de 70:** lo exige la base de datos y la documentación de grado; el cliente del core también lo comprueba.
- **No se audita en la bitácora:** el cambio de estado queda en `postulacion_historial` (sin usuario); `@Auditar` es para acciones de una persona.
- **Una variable sin dato no puntúa** y el 100 % se reparte entre las que sí tienen: así una postulación anterior al formulario ampliado no sale perjudicada por no tener esos datos.
- **`v1.0` se siembra inactiva** para no ocupar el único lugar de modelo activo que otros tests y cargadores usan.

## Contrato de datos

| Dirección | Campos |
| --- | --- |
| Core → IA (`SolicitudPuntaje`) | `postulacion` (código público), `ingresosFamiliares`, `dependientes`, `procedencia`, `carrera` (no puntúa), `patrimonio`, `estadoVulnerabilidad`, `promedioAcademico`, `modelo`, `parametros` (clave → ponderación), y del formulario oficial: `dependenciaEconomica`, `ocupacionSosten`, `tieneHijos`, `cantidadHijos`, `discapacidad`. Nunca nombre, CI ni contacto. |
| IA → Core (`PuntajeCalculado`) | `puntaje` (0 a 70, dos decimales), `modelo` (la versión usada), `detalle` (`variables` con nivel, ponderación y puntos de cada una; `puntaje_sobre_100`; `puntaje_maximo`; `advertencias`). |
| Errores de la IA | `422` datos que no sirven (el core: `IaDatosInvalidosException`); `404`, `5xx`, red o tiempo agotado (el core: `IaNoDisponibleException`). |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU03-BE-01](./UT-CU03-BE-01.md) | Módulo de IA: contrato HTTP del cálculo | — |
| [UT-CU03-BE-02](./UT-CU03-BE-02.md) | Módulo de IA: cálculo ponderado y explicable | 01 |
| [UT-CU03-BE-03](./UT-CU03-BE-03.md) | Core: contrato con la IA y cliente HTTP | 01 |
| [UT-CU03-BE-04](./UT-CU03-BE-04.md) | Core: datos del cálculo y modelo vigente | — |
| [UT-CU03-BE-05](./UT-CU03-BE-05.md) | Core: precalificar y registrar el puntaje | 03, 04 |
| [UT-CU03-BE-06](./UT-CU03-BE-06.md) | Core: ejecución en segundo plano y reintento | 05 |
| [UT-CU03-BE-07](./UT-CU03-BE-07.md) | Modelo inicial v1.0 y consistencia IA–core | 02 |
| [UT-CU03-BE-09](./UT-CU03-BE-09.md) | Core: marcar para revisión lo que la IA no puede calificar | 05, 06 |
| [UT-CU03-BE-10](./UT-CU03-BE-10.md) | Core: consultar la precalificación de una postulación | 05, 09 |
| [UT-CU03-BE-08](./UT-CU03-BE-08.md) | Módulo de IA: segunda opinión con aprendizaje automático | 02 |

**Cadena:** `01 → 02 → 07, 08` y `01 → 03`; `03, 04 → 05 → 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, pasos 1 a 2 | Preparar las variables y el modelo vigente | 04 |
| Flujo principal, pasos 3 a 6 | Pedir el cálculo y recibir el puntaje | 01, 02, 03 |
| Flujo principal, pasos 7 y 8 | Registrar el puntaje vigente y pasar a `calificada` | 05 |
| A1 | Datos incompletos o incoherentes (a los 3 rechazos, marcada para revisión) | 01, 05, 09 |
| A2 | IA no disponible o tiempo agotado | 03, 05, 06 |
| A3 | Reprocesamiento (CU26) | UT-CU26-BE-03 |
| A4 | Solicitud repetida | 05 |
| A5 | Picos de envíos | 06 |
| A6 | Versión no registrada o puntaje fuera de rango | 03, 05 |
| A7 | Postulación sin los datos del formulario oficial | 02, 04 |
| CA01, CA04, CA11, CA13 | Puntaje, idempotencia, tope de 70 y detalle | 02, 05 |
| CA02, CA03, CA09 | Reintento y picos | 06 |
| CA05 | Reprocesamiento | UT-CU26-BE-03 |
| CA06 | Versión no registrada o fuera de rango | 03, 05 |
| CA07 | Sin versión guardada | 07 |
| CA08 | Sin datos del formulario oficial | 02, 04 |
| CA10 | Aparece en la lista priorizada | CU06 |
| CA12 | La IA no recibe datos personales | 03 |

## Follow-ups (no se implementan en estas UT)

- **Pantalla o aviso para las «marcadas para revisión» (A1):** la marca existe (UT-09) pero sigue `pendiente`: no hay un estado nuevo ni dónde verlas. Falta decidir con la DUBSS si hace falta.
- **Duración del cálculo (mockup img 136):** no se guarda; guardarla exige una columna nueva en `puntaje_postulacion`.
- **Seguridad entre el core y la IA:** no hay autenticación; vale mientras la IA solo sea alcanzable desde la red interna.
- **Reentrenar con datos reales:** el modelo de la UT-08 está entrenado con datos sintéticos. Cuando la DUBSS entregue postulaciones históricas se cambia el generador por su lectura y se corre `python -m training.entrenar`.
- **Mostrar la segunda opinión al auditor (CU07):** no se hizo a propósito: un modelo entrenado con datos inventados no debe influir en quien decide una beca. Solo conviene si se reentrena con datos reales.

## Qué NO incluyen estas UT

- Parametrizar el modelo y reprocesar puntajes (CU26).
- Enviar la postulación y pedir la precalificación (CU02).
- Mostrar el puntaje en pantalla (CU06 y CU07).
- El criterio definitivo de puntaje: es un criterio adoptado que la DUBSS debe validar.
