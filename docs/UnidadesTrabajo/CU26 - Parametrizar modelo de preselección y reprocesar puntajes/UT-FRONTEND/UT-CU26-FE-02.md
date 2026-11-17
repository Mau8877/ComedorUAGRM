# UT-CU26-FE-02 — Pantalla, formulario de pesos, historial, ruta y menú

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-FE-02` |
| **Nombre** | Pantalla, formulario de pesos, historial, ruta y menú |
| **Caso de uso** | CU26-FRONTEND |
| **Depende de** | UT-CU26-FE-01 |

## Antes de implementar

[`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (error inline solo si el campo fue tocado),
[`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md) y
[`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md).

## Solicitud de negocio

El Administrador necesita ver los pesos vigentes, escribir los nuevos, comprobar que suman 100 y guardar una versión.

## Objetivo

La pantalla «Parámetros del modelo» según el mockup 146 (sin el panel de reprocesar), su ruta y su ítem de menú.

## Alcance

- `FormularioParametros` (tabla Actual/Nuevo, suma con indicador, descripción opcional, puntaje mínimo deshabilitado, «Guardar versión vX», «Restablecer», aviso de cambios sin guardar al padre), `HistorialVersiones` + `columnasHistorial` + `EstadoVersionBadge` + `formatos`.
- `ParametrosModeloScreen` (insignia de versión vigente, errores de carga con «Reintentar») y los barrels de la feature.
- Ruta `routes/_authenticated/parametros-modelo.tsx` con `exigirRol('ADMINISTRADOR_DUBSS')` y el ítem «Parámetros del modelo» en `adminNavItems.ts` (después de «Lista priorizada») con su prueba en `adminNavItems.test.ts`.
- Pruebas en `tests/ParametrosModeloScreen.test.tsx` (`clienteSimulado.ts`, `montar.tsx`).

Archivos esperados (crear/tocar): lo anterior; **`adminNavItems.ts` y su test tienen cambios sin commitear de otras tareas: solo se agrega**.

## Fuera de alcance (no tocar)

- El panel de reprocesar (FE-03).
- El puntaje mínimo para ser finalista (CU13): solo se muestra deshabilitado.
- El layout del estudiante.

## Resultado esperado

En `/parametros-modelo` el Administrador ve la versión vigente, edita pesos con la suma en vivo, guarda `v{M}.{n+1}` y ve el historial paginado.

## Validación

1. `pnpm exec vitest run src/features/preseleccion src/layouts` en verde.
2. `pnpm exec tsc -b` y `pnpm exec eslint src` sin errores.
3. Con el stack levantado: guardar una versión que no suma 100 no es posible (botón bloqueado); con suma 100 aparece en el historial como «Vigente» y la anterior como «Archivada».
