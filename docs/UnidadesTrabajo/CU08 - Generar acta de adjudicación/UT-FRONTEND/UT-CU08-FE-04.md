# UT-CU08-FE-04 — Ventana de confirmación y generación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-04` |
| **Nombre** | Ventana de confirmación y generación |
| **Caso de uso** | CU08-FE |
| **Depende de** | `UT-CU08-FE-01`, `UT-CU08-FE-02`, `UT-CU08-FE-03` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `PublicarResultadosDialog` (CU-22).

## Solicitud de negocio

El acta es definitiva: no se regenera ni se elimina, así que debe pedir confirmación clara.

## Objetivo

El Administrador confirma o cancela la generación del acta y ve el resultado de la operación.

## Alcance

- `F/features/actas/components/GenerarActaDialog.tsx`: título «Generar acta de adjudicación», nombre de la convocatoria, cantidad de adjudicados (si es cero, el aviso «No hay adjudicados: el acta dirá “Sin adjudicados”», que **no** bloquea), advertencia de que **el acta es definitiva: no se puede regenerar ni eliminar**, y botones «Cancelar» y «Generar».
- «Generar» llama a `useGenerarActa`; **se deshabilita mientras la petición está en curso** (texto «Generando…»). «Cancelar» o cerrar la ventana no cambia nada (CA11); la ventana se monta solo mientras está abierta.
- Éxito: la ventana pasa a mostrar «Acta N.º {número} generada» con los botones «Descargar» (usa `useAccesoActa`, igual que la tabla) y «Cerrar»; el panel y la tabla se refrescan solos (las keys se invalidaron en UT-01) y la convocatoria desaparece de los pendientes. Toast «Acta generada correctamente».
- Errores con `mapearErrorActas` dentro de la ventana: `ERR_ACT_01` informa que los resultados aún no están publicados y refresca los pendientes; `ERR_ACT_02` informa que ya tiene acta (con su número si el backend lo da) y refresca; `ERR_PRI_01` avisa al panel para volver a elegir convocatoria; error de red o `5xx` muestra un mensaje claro con la posibilidad de reintentar y no registra nada.
- Tests: muestra la cantidad y la advertencia, aviso con cero adjudicados que no bloquea, botón en curso, cancelar sin llamar a la API, éxito con el número y «Descargar», y cada error.

Archivos esperados (crear): `GenerarActaDialog.tsx` y su test. (Tocar): `components/index.ts` y `GenerarActaPanel.tsx` (para abrir la ventana).

## Fuera de alcance (no tocar)

- Regenerar, reemplazar o eliminar un acta.
- Un campo de número de acta (lo asigna el backend).
- Avisos a los postulantes (el acta no avisa a nadie).

## Resultado esperado

El Administrador genera el acta con conocimiento de que es definitiva y recibe mensajes claros ante cualquier error.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
