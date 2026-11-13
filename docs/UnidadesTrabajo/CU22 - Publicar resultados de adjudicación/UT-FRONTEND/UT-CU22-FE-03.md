# UT-CU22-FE-03 — Ventana de confirmación y publicación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-03` |
| **Nombre** | Ventana de confirmación y publicación |
| **Caso de uso** | CU22-FE |
| **Depende de** | `UT-CU22-FE-01`, `UT-CU22-FE-02` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Como referencia: `CerrarPostulacionDialog` (CU-20).

## Solicitud de negocio

Publicar es irreversible y avisa a todos los finalistas: debe pedir confirmación clara.

## Objetivo

El Administrador confirma o cancela la publicación y ve el resultado de la operación.

## Alcance

- `F/features/resultados/components/PublicarResultadosDialog.tsx`: título «Publicar resultados», nombre de la convocatoria, resumen (adjudicados y no adjudicados; si no hay adjudicados, un aviso «No hay adjudicados: todos los finalistas recibirán “no adjudicada”» que **no** bloquea la publicación), `aAvisar` («Se avisará a N finalistas por bandeja, push y correo»), advertencia de que **la publicación no se puede deshacer y bloquea nuevas decisiones**, y botones «Cancelar» y «Publicar».
- «Publicar» llama a `usePublicarResultados`; **se deshabilita mientras la petición está en curso**. «Cancelar» o cerrar la ventana no cambia nada; se monta solo mientras está abierta.
- Éxito: toast «Resultados publicados correctamente», cierra la ventana y la pantalla se refresca (las keys ya se invalidaron en UT-01).
- Errores con `mapearErrorResultados` dentro de la ventana: `ERR_PUB_01` muestra cuántas decisiones faltan, `ERR_PUB_02` informa que ya estaba publicada y refresca el resumen, `ERR_PUB_03` y `ERR_PRI_01` avisan a la pantalla para volver a elegir convocatoria.
- Tests: muestra la cantidad y la advertencia, botón en curso, cancelar sin llamar a la API, éxito y cada error.

Archivos esperados (crear): `PublicarResultadosDialog.tsx` y su test. (Tocar): `components/index.ts` y `PublicarResultadosScreen.tsx` (para abrir la ventana).

## Fuera de alcance (no tocar)

- Un checkbox de «no avisar» (siempre se avisa).
- Reabrir o despublicar.
- El acta (CU-08).

## Resultado esperado

El Administrador publica con conocimiento de lo irreversible y recibe mensajes claros ante cualquier error.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
