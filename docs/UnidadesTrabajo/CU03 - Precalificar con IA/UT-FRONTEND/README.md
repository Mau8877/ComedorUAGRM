# Unidades de trabajo — CU03-FRONTEND

**Caso de uso:** la pantalla «Precalificación con IA» del Administrador DUBSS, de solo lectura.
**Fuente CU:** [`CU03 - Precalificar con IA.md`](../../../CasosDeUso/CU03%20-%20Precalificar%20con%20IA.md)
**Diagramas de grado:** mockup img 136 (§7.5.3).
**Recurso HTTP / contrato:** `GET /api/v1/postulaciones/{codigo}/precalificacion` ([UT-CU03-BE-10](../UT-BACKEND/UT-CU03-BE-10.md)); ruta web `/precalificacion/$codigo`.

## Antes de implementar cualquier UT

Leer [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).

## Decisiones de diseño

- Vive en la feature `preseleccion` (paquete P3), no en `priorizacion`: es la consulta del cálculo de la IA. Se abre desde el detalle de la lista priorizada (CU06) con un botón; no tiene ítem de menú propio porque ningún CU lista postulaciones sueltas.
- El mockup dice «de 100» y seis variables: aquí es «de 70» y diez, y no se muestra la duración (no se guarda). Los colores salen de los tokens del tema; el anillo y las barras llevan también el valor en texto.

## Contrato de datos

`Precalificacion` (ver `features/preseleccion/types`): postulante, convocatoria, estado, `puntaje`/`puntajeMaximo`/`modelo`/`fechaCalculo`/`posicion` (nulos si no está calificada), `variables` (`nivel`, `ponderacion`, `puntos`, `maximo`), `advertencias`, `marcadaParaRevision`, `intentos` y `traza`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU03-FE-01](./UT-CU03-FE-01.md) | Pantalla «Precalificación con IA» | UT-CU03-BE-10 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Mockup img 136 | Puntaje, aporte de cada variable, traza y nota | 01 |
| A1 | Aviso de «marcada para revisión» | 01 |

## Follow-ups (no se implementan en estas UT)

- Mostrar la duración del cálculo (hoy no se guarda).
- Un ítem de menú o un listado propio de postulaciones marcadas para revisión.

## Qué NO incluyen estas UT

- El cálculo del puntaje y los endpoints del core (UT-BACKEND).
- Cambiar la lista priorizada (CU06) más allá del botón de acceso.
