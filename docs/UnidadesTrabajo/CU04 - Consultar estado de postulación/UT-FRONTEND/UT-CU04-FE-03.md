# UT-CU04-FE-03 — Pantalla de detalle con línea de tiempo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-03` |
| **Nombre** | Pantalla de detalle con línea de tiempo |
| **Caso de uso** | CU04-FE |
| **Depende de** | `UT-CU04-FE-01` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Releer las «Decisiones de diseño» del [README](./README.md) (botón de corregir, estado visible).

## Solicitud de negocio

Al abrir una postulación, el postulante debe entender en qué paso está, qué falta y qué tiene que hacer.

## Objetivo

Existe la pantalla de detalle con estado, línea de tiempo y los bloques de información asociada.

## Alcance

- `F/features/postulaciones/screens/DetallePostulacionScreen.tsx`: recibe el `codigo`, usa `usePostulacion` y muestra convocatoria, gestión, fecha de envío, estado (badge) y botón «Volver a mis postulaciones».
- `F/features/postulaciones/components/LineaDeTiempo.tsx`: lista vertical de pasos con fecha y hora; `COMPLETADO` con el color de éxito y `PENDIENTE` atenuado. No muestra quién hizo el cambio.
- Bloques condicionales (cada uno un componente pequeño): `BloqueObservaciones` (texto, fecha y «Corregir y reenviar» solo si `puedeCorregir` y existe el CU-02), `BloqueMotivo` (archivo o rechazo), `BloqueFinalista` («Eres finalista»: plazo de entrega, documentos a presentar y si la documentación ya fue recibida) y `BloqueResultado` (adjudicada o no adjudicada, sin motivo, con «Ver resultados de la convocatoria» hacia `/resultados`).
- Estados de carga, `404` («Postulación no encontrada» con enlace a la lista, mensaje de `mapearErrorPostulaciones`) y error de conexión con «Reintentar».
- Tests de componente: un caso por bloque (devuelta con y sin botón, archivada con motivo, finalista con y sin documentación, adjudicada y no adjudicada), línea de tiempo con pasos pendientes, `404` y reintento.

Archivos esperados (crear): pantalla, `LineaDeTiempo`, los cuatro bloques y tests.

## Fuera de alcance (no tocar)

- La lista (UT-02) y las rutas/menú (UT-04).
- La pantalla de corrección del CU-02.
- Recalcular el estado en el cliente.

## Resultado esperado

El postulante ve el recorrido de su postulación y lo que le toca hacer.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Con datos de prueba, cada estado muestra su bloque y ningún otro.
