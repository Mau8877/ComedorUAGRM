# UT-CU05-FE-06 — Botones en el expediente y estado final

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-06` |
| **Nombre** | Botones en el expediente y estado final |
| **Caso de uso** | CU05-FE |
| **Depende de** | `UT-CU05-FE-05` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md), [`ESTADO_GLOBAL_FRONTEND.md`](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md), [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- `F/features/expedientes/screens/ExpedienteScreen.tsx` y `F/features/expedientes/components/motivoBloqueoCierre.ts` tal como los dejó el CU-20.

## Solicitud de negocio

El Administrador llega a la decisión final desde el expediente y ve que la postulación quedó decidida.

## Objetivo

El expediente ofrece «Adjudicar» y «No adjudicar», explica por qué no se puede cuando corresponde y refleja el estado final.

## Alcance

- `ExpedienteScreen`: botones «Adjudicar» y «No adjudicar» junto a «Devolver para corrección», «Archivar» y «Rechazar»; cada uno abre `DecidirAdjudicacionDialog` con su `accion` (se monta solo mientras está abierto). Al decidir, refresca el expediente y el panel.
- `F/features/expedientes/components/motivoBloqueoAdjudicacion.ts` (nuevo, mismo estilo que `motivoBloqueoCierre`): función que recibe el expediente, el panel y la acción y devuelve el motivo por el que **no** se puede intentar, o `null`. Orden de los motivos:
  1. postulación ya no «calificada»: «La postulación ya está adjudicada», «La postulación ya fue marcada como no adjudicada», «La postulación ya está archivada» o «La postulación ya no está calificada» (las dos acciones);
  2. resultados publicados o `decisionesHabilitadas` falso: «Los resultados ya se publicaron» o «La convocatoria debe estar cerrada y tener cupos y puntaje mínimo definidos» (las dos acciones);
  3. sin ninguna auditoría: «Primero hay que auditar el expediente» (las dos acciones);
  4. **solo adjudicar:** última auditoría no «Validado»: «La última auditoría no es “Validado”: audite de nuevo o use “No adjudicar”»; puntaje total menor al mínimo: «El puntaje total (X) no alcanza el mínimo (Y)»; cupos agotados: «Ya no quedan cupos disponibles».
  «No adjudicar» queda habilitado con cupos agotados o puntaje bajo. El backend sigue siendo quien decide; el frontend solo evita intentos inútiles. Si el panel aún no cargó, los botones esperan, no se asumen habilitados.
- Los botones se deshabilitan con ese motivo visible (texto o tooltip). Si el usuario no es Administrador DUBSS no ve las acciones (la ruta ya exige el rol, A14).
- `ExpedienteResumen` (o el encabezado) muestra la insignia «Adjudicada» o «No adjudicada» según `estadoPostulacion` (la de «No adjudicada» ya existe por el CU-20); el formulario de auditoría ya queda bloqueado con su motivo (`motivoBloqueoAuditoria`), que se amplía a «adjudicada» si hace falta.
- Tests de pantalla con la API simulada: botones habilitados, cada motivo de deshabilitado (incluido que «No adjudicar» sigue habilitado con puntaje bajo y cupos agotados), flujo completo de adjudicar y de no adjudicar, panel que aún no carga, insignias y auditoría bloqueada. Tests de `motivoBloqueoAdjudicacion` por separado.

Archivos esperados (crear): `motivoBloqueoAdjudicacion.ts` y su test. (Tocar): `ExpedienteScreen.tsx`, `ExpedienteResumen.tsx`, `motivoBloqueoAuditoria.ts` (si hace falta) y sus tests.

## Fuera de alcance (no tocar)

- Cambiar el estado de la postulación desde el cliente.
- Un botón de reabrir o de cambiar la decisión.
- Cambios en los botones «Devolver para corrección», «Archivar» y «Rechazar» más allá de convivir con los nuevos.
- La lista de finalistas (UT-07) y botones de acta o publicar resultados (CU-08, CU-22).

## Resultado esperado

Desde el expediente se adjudica o no adjudica la postulación y la pantalla queda en su estado final.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
