# Unidades de trabajo — CU19-FE

**Caso de uso:** Ventana «Devolver para corrección» del Administrador DUBSS en el expediente del finalista: escribir el motivo, devolver la postulación y ver el estado «Devuelta» en el expediente y en la lista de finalistas.
**Fuente CU:** [`CU19 - Devolver postulación para corrección`](../../../CasosDeUso/CU19%20-%20Devolver%20postulación%20para%20corrección.md)
**Diagramas de grado:** maqueta `113` (`Mockup_CU19_Devolver_para_correccion`), análisis `085`, secuencia `100`.
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md` y `TESTING_FRONTEND.md`. Ganan sobre estas fichas.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`).

- **No hay feature nueva:** la devolución vive en `features/expedientes/` porque se hace desde el expediente; la insignia de la lista toca `features/finalistas/` (que ya expone su barrel).
- **Se entra desde el expediente del finalista (CU-07);** no hay lista aparte ni «Postulaciones» en el menú, a diferencia de lo que dibuja la maqueta `113`.
- **Sin cuadro «Notificar al postulante por push»:** siempre se avisa (bandeja, push y correo).
- **Sin idempotencia por clave:** el doble clic se evita deshabilitando el botón «Devolver» mientras la petición está en curso; si aun así llega una segunda petición, el backend responde `ERR_DEV_01`.
- **Motivo:** obligatorio, contador de 500 caracteres, error inline solo en campos tocados, mensajes en español vía `mapearErrorExpedientes`.
- **Botón deshabilitado** con su motivo visible: postulación ya no «calificada» y expediente sin auditorías. Tras un reenvío con auditorías viejas el backend decide con `ERR_DEV_02`.
- **Lista de finalistas:** el finalista devuelto sigue en la lista con la insignia «Devuelta».

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU19-FE-01](./UT-CU19-FE-01.md) | API: hook, schema, tipos y mensajes de error | UT-CU19-BE-07 |
| [UT-CU19-FE-02](./UT-CU19-FE-02.md) | Ventana «Devolver para corrección» | 01 |
| [UT-CU19-FE-03](./UT-CU19-FE-03.md) | Botón en el expediente y estado «Devuelta» | 02 |
| [UT-CU19-FE-04](./UT-CU19-FE-04.md) | Insignia «Devuelta» en la lista de finalistas | UT-CU19-BE-06, 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Alcance de cada UT

- **01:** `devolucionSchema`, `devolverPostulacionApi`, `useDevolverPostulacion` (invalida `['expedientes','detail']` y `['finalistas','list']`), tipos y mapeo de `ERR_DEV_01` y `ERR_DEV_02`.
- **02:** `DevolverPostulacionDialog` con motivo, contador, confirmación, estado en curso y errores.
- **03:** botón en `ExpedienteScreen`, motivos de deshabilitado e insignia «Devuelta» en el resumen.
- **04:** campo `estadoPostulacion` en `Finalista` y la insignia en la columna «Auditoría».

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–2 (expediente y «Devolver para corrección») | 03 |
| Pasos 3–4, A1, A2 (ventana, motivo, cancelar) | 02 |
| Paso 5, A3, A4, A5 (`404`, `409`) | 01, 02, 03 |
| Paso 8 (confirmación y estado «Devuelta») | 02, 03 |
| A7, A8 (`401` lleva al login por el cliente HTTP; la ruta exige el rol) | 03 |
| CA02, CA03, CA05, CA08, CA12 | 01–04 |

## Follow-ups (no se implementan en estas UT)

- Que el postulante vea el motivo en su pantalla de estado (CU-04) y reenvíe (CU-02).
- Archivar o rechazar desde el expediente (CU-20).

## Qué NO incluyen estas UT

- Cambios en el backend (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).
- La app móvil.
- Un plazo de reenvío: no existe.
