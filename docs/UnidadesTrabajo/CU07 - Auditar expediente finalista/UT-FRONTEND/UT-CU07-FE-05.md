# UT-CU07-FE-05 — Pantalla y ruta del expediente, con notificar y registrar entrega

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-05` |
| **Nombre** | Pantalla y ruta del expediente, con notificar y registrar entrega |
| **Caso de uso** | CU07-FE |
| **Depende de** | `UT-CU07-FE-02`, `UT-CU07-FE-03`, `UT-CU07-FE-04` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) (guard, rol) y [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/frontend/ARQUITECTURA_FRONTEND.md) (barrels).
- Referencias: `F/routes/_authenticated/finalistas.tsx` y `FinalistasScreen.tsx` (CU-21).

## Solicitud de negocio

El administrador trabaja el CU en una pantalla, como en la maqueta, y llega a ella desde la lista de finalistas.

## Objetivo

Existe la pantalla «Auditar expediente de finalista», accesible solo para el administrador.

## Alcance

- `F/features/expedientes/screens/ExpedienteScreen.tsx`: encabezado con nombre, posición y puntaje; a la izquierda `DocumentosDigitales` y `ExpedienteResumen`; a la derecha `PuntajePanel`, el estado de la entrega con el `RegistrarEntregaPanel` y un botón «Notificar» (`NotificarFinalistasDialog` con ese finalista) del CU-21 —importados por el barrel de `features/finalistas`, que se amplía para exportarlos—, el `AuditoriaForm` y `HistorialAuditorias`.
- Ruta `F/routes/_authenticated/finalistas_.$codigo.tsx` (el `_` evita anidarla bajo `finalistas.tsx`), con `exigirRol` de administrador y enlace «Volver a finalistas». `FinalistasScreen` navega a ella desde «Auditar» (UT-02). Un finalista inexistente (`404`, `ERR_FIN_01`), ya sea al abrir el expediente o al registrar la auditoría, muestra un aviso (toast), refresca la lista de finalistas y vuelve a ella (A11).
- Estados de carga y de error con «Reintentar» (A15). Tras registrar la entrega o notificar desde el expediente, este se refresca.
- Tests de pantalla completa contra un cliente simulado: abrir el expediente, abrir un documento, registrar la entrega, registrar la auditoría y el `404`; y que otro rol no ve la ruta.

Archivos esperados (crear): `ExpedienteScreen.tsx`, la ruta y sus tests. (Tocar): `FinalistasScreen.tsx`, `features/finalistas/index.ts` y `features/finalistas/components/index.ts`.

## Fuera de alcance (no tocar)

- El botón «Devolver para corrección» (CU-19) y archivar o rechazar (CU-20).
- Una entrada nueva en el menú: se entra desde «Finalistas».
- Cambios en el backend.

## Resultado esperado

El administrador audita un expediente de punta a punta desde la lista de finalistas.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el backend y los datos de prueba (`UT-CU07-BE-11`), la pantalla abre los expedientes de los 10 finalistas, permite abrir un documento y registrar una auditoría.
