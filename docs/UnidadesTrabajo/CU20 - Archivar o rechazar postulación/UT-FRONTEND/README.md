# Unidades de trabajo — CU20-FE

**Caso de uso:** Ventanas «Archivar» y «Rechazar» del Administrador DUBSS en el expediente del finalista: escribir el motivo, cerrar la postulación y ver el estado «Archivada» o «No adjudicada» en el expediente y en la lista de finalistas.
**Fuente CU:** [`CU20 - Archivar o rechazar postulación`](../../../CasosDeUso/CU20%20-%20Archivar%20o%20rechazar%20postulación.md)
**Diagramas de grado:** CU-20 (maqueta, análisis y secuencia; números de figura por confirmar).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`). Lo marcado *(por revisar)* es propuesta del CU-20 aún sin confirmar.

- **No hay feature nueva:** el cierre vive en `features/expedientes/` porque se hace desde el expediente, junto a la devolución del CU-19; la insignia de la lista toca `features/finalistas/`.
- **Una sola ventana para dos acciones:** `CerrarPostulacionDialog` recibe `accion` (`archivar` | `rechazar`) y cambia título, texto y botón. El motivo, el contador y las validaciones son los mismos.
- **Se entra desde el expediente del finalista (CU-07);** no hay lista aparte.
- **Sin cuadro de «notificar»:** siempre se avisa al postulante.
- **Sin idempotencia por clave:** el doble clic se evita deshabilitando el botón mientras se envía; si aun así llega una segunda petición, el backend responde `ERR_ARC_01`.
- **Motivo:** obligatorio, contador de 500 caracteres, error inline solo en campos tocados, mensajes en español vía `mapearErrorExpedientes`.
- **Botones deshabilitados** con su motivo visible solo cuando la postulación ya no está «calificada». A diferencia de «Devolver», **no se pide auditoría previa** *(por revisar, preguntas 1 y 2)*.
- **Rechazar** deja «No adjudicada» *(nombre por revisar con la DUBSS, pregunta 5)*.
- **Lista de finalistas:** el finalista cerrado sigue en la lista con la insignia «Archivada» o «No adjudicada» *(por revisar)*.
- **Sin reapertura:** no hay botón para volver a «calificada».

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU20-FE-01](./UT-CU20-FE-01.md) | API: hooks, schema, tipos y mensajes de error | UT-CU20-BE-06 |
| [UT-CU20-FE-02](./UT-CU20-FE-02.md) | Ventana «Archivar» / «Rechazar» | 01 |
| [UT-CU20-FE-03](./UT-CU20-FE-03.md) | Botones en el expediente y estado final | 02 |
| [UT-CU20-FE-04](./UT-CU20-FE-04.md) | Insignias «Archivada» y «No adjudicada» en la lista de finalistas | UT-CU20-BE-05, 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Alcance de cada UT

- **01:** `cierreSchema`, `archivarPostulacionApi`/`rechazarPostulacionApi`, `useArchivarPostulacion`/`useRechazarPostulacion` (invalidan `['expedientes','detail']` y `['finalistas','list']`), tipos y mapeo de `ERR_ARC_01`.
- **02:** `CerrarPostulacionDialog` con motivo, contador, confirmación, estado en curso y errores.
- **03:** botones «Archivar» y «Rechazar» en `ExpedienteScreen`, motivo de deshabilitado e insignia de estado final en el resumen.
- **04:** nuevas insignias en la columna «Auditoría» de la lista.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–2 (expediente y «Archivar»/«Rechazar») | 03 |
| Pasos 3–4, A1, A2 (ventana, motivo, cancelar) | 02 |
| Paso 5, A3, A4 (`404`, `409`) | 01, 02, 03 |
| Paso 8 (confirmación y estado final) | 02, 03 |
| A6, A7 (`401` lleva al login por el cliente HTTP; la ruta exige el rol) | 03 |
| CA01–CA04, CA05, CA10, CA11, CA12 | 01–04 |

## Follow-ups (no se implementan en estas UT)

- Que el postulante vea estado y motivo en su pantalla (CU-04, CA13).
- Reapertura o apelación, si la DUBSS las confirma.

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- Devolver para corrección (CU-19) ni adjudicar o no adjudicar (CU-05).
- La app móvil.
