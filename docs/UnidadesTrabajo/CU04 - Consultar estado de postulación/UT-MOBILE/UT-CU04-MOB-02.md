# UT-CU04-MOB-02 — Pantalla «Mis postulaciones»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-02` |
| **Nombre** | Pantalla «Mis postulaciones» |
| **Caso de uso** | CU04-MOB |
| **Depende de** | `UT-CU04-MOB-01` |

## Antes de implementar

- [`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (widgets de la feature en `widgets/`, tema y `AppColors`), [`ARQUITECTURA_MOBILE.md`](../../../../.claude/rules/ARQUITECTURA_MOBILE.md).
- Como referencia: `M/lib/features/perfil/widgets/perfil_vista.dart` (vista que vive dentro del shell).

## Solicitud de negocio

El postulante quiere ver sus postulaciones y su estado desde el teléfono.

## Objetivo

Existe la vista de la lista con filtro por gestión, scroll infinito, estado vacío y errores.

## Alcance

- `M/lib/features/postulaciones/widgets/mis_postulaciones_vista.dart`: vista (sin `Scaffold`, para montarse dentro de `EstudianteShell`) con `ListView` de tarjetas (convocatoria, gestión, fecha de envío y estado), orden de la más reciente a la más antigua, carga de la página siguiente al llegar al final, `RefreshIndicator`.
- `M/lib/features/postulaciones/widgets/estado_postulacion_chip.dart`: chip por estado con colores del tema.
- Filtro por gestión académica (menú o `ChoiceChip`s) usando `misPostulacionesUiProvider`; al cambiarlo se recarga desde la página 1.
- Estado vacío (A1) con mensaje informativo; la invitación a postular solo si existe la pantalla del CU-02.
- Error o falla de conexión (A10): mensaje claro y botón «Reintentar».
- Tocar una tarjeta navega al detalle (`context.go`/`push` a `/mis-postulaciones/{codigo}`).
- Tests de widget solo de lo no trivial: estado vacío, filtro que recarga y error con reintento.

Archivos esperados (crear): vista, chip y tests.

## Fuera de alcance (no tocar)

- El detalle (UT-03) y el cableado de rutas/shell (UT-04).
- Cálculo del estado visible en la app.
- Colores hexadecimales sueltos.

## Resultado esperado

El postulante ve y filtra su lista de postulaciones.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Con datos de prueba, la lista muestra las postulaciones por fecha descendente y el filtro las reduce.
