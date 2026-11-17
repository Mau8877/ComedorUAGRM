# UT-CU26-BE-02 — Versiones del modelo: validar, guardar y consultar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-02` |
| **Nombre** | Versiones del modelo: validar, guardar y consultar |
| **Caso de uso** | CU26-BACKEND |
| **Depende de** | UT-CU26-BE-01 |

## Antes de implementar

[`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md),
[`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y
[`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).

## Solicitud de negocio

El Administrador necesita ver las ponderaciones vigentes, el historial de versiones y guardar una versión nueva con valores consistentes.

## Objetivo

Los tres endpoints de versiones, con validación (`ERR_MOD_01`), numeración de versiones y registro en la bitácora.

## Alcance

- `VariablesModelo` (claves, nombres y suma 100), `VersionModelo.siguiente()`.
- DTO: `GuardarModeloRequest`, `ParametroModeloRequest`, `ModeloPreseleccionResponse`, `ModeloPreseleccionResumenResponse`, `ParametroModeloResponse`.
- `ModeloPreseleccionService`: `vigente()`, `listar()`, `guardar()` (`@PreAuthorize`, `@Transactional`, `@Auditar` `NUEVA_VERSION_MODELO`); `ModeloPreseleccionController` con `GET`, `GET /vigente` y `POST`.
- `ModeloPreseleccionErrorCodes` (`ERR_MOD_01` a `ERR_MOD_07`) y `docs/errors/preseleccion/ERRORES_PRESELECCION.md` + su índice en `docs/errors/README.md`.
- `BitacoraAcciones.NUEVA_VERSION_MODELO` (al final del archivo).
- Pruebas: `ModeloPreseleccionServiceTest`, `VersionModeloTest`.

Archivos esperados (crear/tocar): los de la lista, más `docs/errors/`.

## Fuera de alcance (no tocar)

- El reprocesamiento y su ejecución (BE-04).
- El puntaje mínimo para ser finalista (CU13) y `PUT`/`PATCH`/`DELETE` de versiones: una versión no se edita.
- Otras acciones de `BitacoraAcciones` (hay cambios sin commitear de otras tareas en ese archivo).

## Resultado esperado

`POST /api/v1/modelos-preseleccion` con seis ponderaciones que suman 100 responde `201` con `v{M}.{n+1}` vigente y la anterior archivada.

## Validación

1. `./mvnw -o -q test -Dtest='ModeloPreseleccionServiceTest,VersionModeloTest'` en verde.
2. Con el backend en `dev` y un Administrador: `POST` con una suma de 102 → `400` `ERR_MOD_01`; con las seis ponderaciones válidas → `201`; `GET /vigente` devuelve esa versión; `GET ?page=1&pageSize=20` la lista primero.
3. Sin el rol → `403` `ERR_AUTH_07`; la bitácora tiene una fila `NUEVA_VERSION_MODELO`.
