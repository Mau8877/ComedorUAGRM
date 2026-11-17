# UT-CU24-MOB-03 — Vista «Mi beca» y sección del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-MOB-03` |
| **Nombre** | Vista «Mi beca» y sección del shell |
| **Caso de uso** | CU24-MOB |
| **Depende de** | UT-CU24-MOB-02 |

## Antes de implementar

[`WIDGETS_UI_MOBILE.md`](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (tema, sin colores sueltos) y [`NAVEGACION_MOBILE.md`](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md).

## Solicitud de negocio

El becado quiere ver en el celular el estado de su beca y su historial de asistencia.

## Objetivo

La sección «Mi beca» de la barra inferior muestra la beca y el historial.

## Alcance

- `M/features/mibeca/widgets/mi_beca_vista.dart`: `MiBecaVista` (tarjeta de estado con motivo si no está vigente, total y nota de la DUBSS; historial con «Filtrar por fechas» / «Quitar filtro»; scroll infinito; «tirar para recargar»; estados de carga, vacío, error con «Reintentar» y «sin beca» con `onVerPostulaciones`).
- `M/layouts/estudiante/estudiante_shell.dart`: nueva sección «Mi beca» antes de «Perfil» (índices: `_indiceMiBeca = 3`, `_indicePerfil = 4`).
- `T/features/postulaciones/navegacion_test.dart`: el orden de las secciones del shell.

Archivos esperados (crear/tocar): la vista, el shell y ese test.

## Fuera de alcance (no tocar)

El router (no hay ruta nueva: la vista vive en el shell), el backend y el calendario mensual.

## Resultado esperado

El postulante abre «Mi beca» y ve su estado e historial; sin beca ve el aviso con el botón hacia «Mis postulaciones».

## Validación

1. `flutter analyze` sin avisos y `flutter test` completo en verde.
