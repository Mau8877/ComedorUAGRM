# UT-CU04-MOB-03 — Pantalla de detalle con línea de tiempo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-03` |
| **Nombre** | Pantalla de detalle con línea de tiempo |
| **Caso de uso** | CU04-MOB |
| **Depende de** | `UT-CU04-MOB-01` |

## Antes de implementar

- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md).
- Releer las «Decisiones de diseño» del [README](./README.md) (botón de corregir, estado visible).

## Solicitud de negocio

Al abrir una postulación, el postulante debe entender en qué paso está, qué falta y qué tiene que hacer.

## Objetivo

Existe la pantalla de detalle con estado, línea de tiempo y los bloques de información asociada.

## Alcance

- `M/lib/features/postulaciones/screens/detalle_postulacion_screen.dart`: recibe el `codigo`, usa `postulacionDetailProvider` y muestra convocatoria, gestión, fecha de envío y estado (chip); `Scaffold` propio con botón de volver.
- `M/lib/features/postulaciones/widgets/linea_de_tiempo.dart`: pasos verticales con fecha y hora; completados resaltados, pendientes atenuados; sin autor del cambio.
- Bloques condicionales en `widgets/`: observaciones (con «Corregir y reenviar» solo si `puedeCorregir` y existe el CU-02), motivo de archivo o rechazo, bloque «Eres finalista» (plazo, documentos a presentar, documentación recibida) y resultado (sin motivo, con «Ver resultados de la convocatoria»; mientras no exista esa pantalla en la app, el botón se oculta).
- Carga, `404` («Postulación no encontrada» con vuelta a la lista) y error de conexión con «Reintentar».
- Tests de lo no trivial: la línea de tiempo (pasos pendientes) y la elección de bloques por estado.

Archivos esperados (crear): pantalla, línea de tiempo, bloques y tests.

## Fuera de alcance (no tocar)

- La lista (UT-02) y las rutas/shell (UT-04).
- La pantalla de corrección del CU-02 y la de resultados (CU-22 en móvil).
- Recalcular el estado en la app.

## Resultado esperado

El postulante ve el recorrido de su postulación y lo que le toca hacer.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Con datos de prueba, cada estado muestra su bloque y ningún otro.
