# UT-CU02-MOB-01 — Modelos, servicio, validación y errores del envío

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-01` |
| **Nombre** | Modelos, servicio, validación y errores del envío |
| **Caso de uso** | CU02-MOB |
| **Depende de** | — |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) y [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)

## Solicitud de negocio

La app necesita hablar con el backend y validar el formulario con las mismas reglas que el web.

## Objetivo

Existen los modelos, `PostulacionesService.iniciar/enviar`, las validaciones y el traductor de errores.

## Alcance

- `features/postulaciones/models/formulario_postulacion.dart`.
- `services/postulaciones_service.dart`: `iniciar()` y `enviar()`.
- `validacion_formulario.dart`: mismas reglas que el web y el backend, y `ValoresFormulario`.
- `envio_error_mapper.dart`.
- Prueba: `postular_logica_test.dart`.

Archivos esperados (crear/tocar): los listados y los fakes de `postulaciones_pantallas_test.dart`.

## Fuera de alcance (no tocar)

- La pantalla (`UT-CU02-MOB-02`).
- Guardar borrador (CU17).

## Resultado esperado

La lógica está probada.

## Validación

1. `flutter analyze` sin problemas y `flutter test test/features/postulaciones` en verde.
