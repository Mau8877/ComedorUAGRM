# UT-CU20-FE-03 — Botones en el expediente y estado final

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-03` |
| **Nombre** | Botones en el expediente y estado final |
| **Caso de uso** | CU20-FE |
| **Depende de** | `UT-CU20-FE-02` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- `F/features/expedientes/screens/ExpedienteScreen.tsx` tal como la dejó el CU-19.

## Solicitud de negocio

El administrador llega al archivo y al rechazo desde el expediente y ve que la postulación quedó cerrada.

## Objetivo

El expediente ofrece «Archivar» y «Rechazar» y refleja el estado final.

## Alcance

- `ExpedienteScreen`: botones «Archivar» y «Rechazar» junto a «Devolver para corrección»; cada uno abre `CerrarPostulacionDialog` con su `accion` (se monta solo mientras está abierto). Al cerrar la postulación, refresca el expediente.
- Deshabilitados con el motivo visible cuando la postulación **ya no está «calificada»** («La postulación ya está archivada», «La postulación ya fue marcada como no adjudicada» o «La postulación ya no está calificada»). **No** se deshabilitan por falta de auditoría ni de entrega física *(por revisar)*.
- `ExpedienteResumen` (o el encabezado) muestra la insignia «Archivada» o «No adjudicada» según `estadoPostulacion`; el formulario de auditoría ya queda bloqueado con su motivo (`motivoBloqueoAuditoria`), que se amplía a estos dos estados si hace falta.
- Si el usuario no es Administrador DUBSS no ve las acciones (la ruta ya exige el rol, A7).
- Tests de pantalla: botones habilitados, deshabilitados por estado, flujo completo de archivar y de rechazar con la API simulada, insignias y que la auditoría queda bloqueada.

Archivos esperados (tocar): `ExpedienteScreen.tsx`, `ExpedienteResumen.tsx` y sus tests.

## Fuera de alcance (no tocar)

- Cambiar el estado de la postulación desde el cliente.
- Un botón de reabrir.
- Cambios en el botón «Devolver para corrección» más allá de convivir con los nuevos.
- La lista de finalistas (UT-04).

## Resultado esperado

Desde el expediente se archiva o rechaza la postulación y la pantalla queda en su estado final.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
