# Unidades de trabajo — CU07-FE

**Caso de uso:** Pantalla web «Auditar expediente de finalista» del Administrador DUBSS: ver el expediente y sus documentos, registrar la auditoría (resultado, observaciones y puntaje académico) y llegar desde la lista de finalistas del CU-21.
**Fuente CU:** [`CU07 - Auditar expediente finalista`](../../../CasosDeUso/CU07%20-%20Auditar%20expediente%20finalista.md)
**Diagramas de grado:** maqueta `107` (`Mockup_CU07_Auditar_expediente_finalista`), análisis `079`, secuencia `094`.
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **Feature nueva `features/expedientes/`** (api, schemas, tipos, componentes y pantalla). Usa lo de `features/finalistas` solo por su barrel (notificar y registrar la entrega, que este CU incluye del CU-21).
- **Se entra desde la lista de finalistas del CU-21**, que gana la columna «Auditoría» y la acción «Auditar»; no hay lista aparte ni entrada nueva en el menú. *(Decisión del equipo.)*
- **Ruta** `routes/_authenticated/finalistas_.$codigo.tsx`, hermana de `finalistas.tsx` y no hija, para no cambiar la pantalla del CU-21.
- **Puntaje:** el de la IA (0–70) se ve solo de lectura; el administrador escribe únicamente el puntaje académico (0–30) y solo cuando el resultado es «Validado». El total se calcula en el backend y se muestra.
- **Lo de la maqueta que el CU no pide** («Devolver para corrección», que es el CU-19, y el segundo cuadro de observaciones, que está duplicado) **no se construye**.
- **Formularios** con TanStack Form + Zod, errores inline solo en campos tocados, observaciones con contador de 500 caracteres; la confirmación del guardado usa `AlertDialog`.
- **Documentos:** se abren con la URL temporal que entrega el backend (MinIO), nunca guardando rutas en el cliente.
- **Mensajes** en español y errores vía `getErrorMessage` / `mapearErrorExpedientes`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| UT-CU07-FE-01 | API: hooks, keys, schemas y mensajes de error | UT-CU07-BE-10 |
| UT-CU07-FE-02 | Columna «Auditoría» y acción «Auditar» en la tabla de finalistas | 01 |
| UT-CU07-FE-03 | Componentes del expediente (datos, documentos, puntajes, historial) | 01 |
| UT-CU07-FE-04 | Formulario de auditoría con confirmación | 01, 03 |
| UT-CU07-FE-05 | Pantalla y ruta del expediente, con notificar y registrar entrega | 02, 03, 04 |

**Cadena:** `01 → 02, 03`; `01, 03 → 04`; `02, 03, 04 → 05`.

## Alcance de cada UT

- **01:** `useExpediente`, `useRegistrarAuditoria`, `useAccesoDocumento`, `keys.ts`, schema Zod, tipos y mapeo de errores; invalidación de `['expedientes','detail']` y `['finalistas','list']`.
- **02:** insignia de auditoría, total como dato secundario y botón «Auditar» en la tabla de finalistas.
- **03:** `ExpedienteResumen`, `DocumentosDigitales`, `PuntajePanel` y `HistorialAuditorias`.
- **04:** `AuditoriaForm` con resultado, puntaje académico, observaciones, confirmación y estados deshabilitados con su motivo.
- **05:** `ExpedienteScreen`, la ruta, los atajos de notificar y registrar entrega del CU-21, y los estados de carga y error.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–3 (lista y «Auditar») | 02, 05 |
| Paso 4, A4 (expediente y documentos) | 01, 03, 05 |
| Paso 5 (notificar y entrega, CU-21 incluido) | 05 |
| Pasos 6–9, A6, A7, A8, A12 | 01, 04 |
| A9, A11, A13, A14 | 05 (`401` lleva al login por el cliente HTTP; la ruta exige el rol) |
| CA01–CA21 | 01–05 |

## Follow-ups (no se implementan en estas UT)

- «Devolver para corrección», archivar o rechazar desde el expediente (CU-19, CU-20).
- Exportar el expediente.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- La app móvil.
- Calificar documento por documento.
