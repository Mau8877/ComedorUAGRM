# UT-CU22-FE-04 — Pantalla «Resultados» del postulante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-04` |
| **Nombre** | Pantalla «Resultados» del postulante |
| **Caso de uso** | CU22-FE |
| **Depende de** | `UT-CU22-FE-01` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md) (layout `_estudiante`), [`TANSTACK_TABLE_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_TABLE_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

Tras la publicación, cada postulante necesita ver su resultado y quiénes fueron adjudicados.

## Objetivo

El postulante ve su resultado y la lista de adjudicados de una convocatoria publicada.

## Alcance

- Ruta dentro de `_authenticated/_estudiante/` y opción «Resultados» en el menú del layout del postulante.
- `F/features/resultados/screens/ResultadosScreen.tsx`:
  - Lista de convocatorias publicadas con `useConvocatoriasPublicadas`; abre por defecto la más reciente. Sin ninguna publicada, mensaje «Aún no hay resultados publicados».
  - Recuadro «Mi resultado» con `useMiResultado`: «Adjudicada» (con su nota), «No adjudicada» o, si el usuario no fue finalista con decisión, «No participaste en esta etapa». **Nunca** se muestra el motivo.
  - Lista de adjudicados con `useAdjudicadosPublicos` y `useDataTable`: `DataTable` en pantallas anchas y `DataCards` en móvil, columnas **Nombre y apellido, Nota, Carrera y Facultad** (una columna `grow`: la del nombre), buscador (`search`) y `enableSorting` solo en `nota`, `apellidos` y `nombres`.
  - No se muestran no adjudicados ni CI ni correo.
- Estados de carga, vacío y error (`ERR_PUB_04` se muestra como «Los resultados aún no están publicados»).
- Tests: sin convocatorias publicadas, resultado adjudicado, no adjudicado y ajeno, lista con paginación, búsqueda y orden, que no hay columnas de CI/correo ni de motivo, y estados de error.

Archivos esperados (crear): `ResultadosScreen.tsx`, columnas de la tabla, la ruta y su test. (Tocar): `screens/index.ts`, el menú del layout del postulante.

## Fuera de alcance (no tocar)

- El CU-04 (estado de la postulación): sigue como follow-up.
- Publicar o ver decisiones del Administrador.
- Cambiar los datos de privacidad: lo que se muestra lo fija el backend (UT-CU22-BE-07).

## Resultado esperado

El postulante consulta su resultado y la lista oficial sin ver nada que no corresponda.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
