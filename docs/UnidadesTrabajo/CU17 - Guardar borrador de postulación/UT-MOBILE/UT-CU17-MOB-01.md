# UT-CU17-MOB-01 — «Guardar borrador» en el asistente y tarjeta «Mi borrador»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-MOB-01` |
| **Nombre** | «Guardar borrador» en el asistente y tarjeta «Mi borrador» |
| **Caso de uso** | CU17-MOB |
| **Depende de** | UT-CU17-BE-02 |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)

## Solicitud de negocio

El postulante, desde el teléfono, quiere salir y retomar su postulación sin perder lo escrito.

## Objetivo

La pantalla «Nueva postulación» de la app guarda el borrador y avisa la hora; «Mis postulaciones» muestra el borrador y lo que falta.

## Alcance

- `models/borrador_postulacion.dart` (nuevo): `GuardarBorradorRequest`, `BorradorGuardado`, `BorradorResumen` (con `resumenAvance`); `RespuestaSocioeconomica.guardadoEn`.
- `services/postulaciones_service.dart`: `guardarBorrador` y `miBorrador`.
- `providers/borrador_provider.dart` (nuevo): `miBorradorProvider`.
- `validacion_formulario.dart`: `ValoresFormulario.aBorrador()` y `ResultadoBorrador`; `formatos.dart`: `formatearHora`.
- `screens/postular_screen.dart`: botón «Guardar borrador» en los pasos 2 y 3, aviso «✓ Borrador guardado · 14:32», datos a corregir y el error del backend.
- `widgets/tarjeta_borrador.dart` (nuevo) y `widgets/mis_postulaciones_vista.dart`: la tarjeta con «Continuar» a `/postular`, también cuando no hay postulaciones enviadas.
- Tests: `borrador_logica_test.dart` (nuevo), `postular_screen_test.dart` (4 nuevos) y `postulaciones_pantallas_test.dart` (2 nuevos; los servicios simulados suman los métodos nuevos).

Archivos esperados (crear/tocar): los listados arriba.

## Fuera de alcance (no tocar)

- «Eliminar» el borrador, el guardado automático y la advertencia al salir.
- El envío, el comprobante y los documentos (CU02, CU16).
- El backend y la web.

## Resultado esperado

En la app, «Guardar borrador» guarda lo escrito y muestra la hora; al reabrir «Nueva postulación» los datos siguen ahí; «Mis postulaciones» muestra la tarjeta del borrador.

## Validación

1. `flutter analyze` sin problemas y `flutter test test/features/postulaciones` en verde.
2. Con el stack y un teléfono o emulador: escribir solo los ingresos, «Guardar borrador» → «Borrador guardado · hh:mm»; salir, volver a «Nueva postulación» → los ingresos siguen ahí.
3. En «Mis postulaciones» aparece la tarjeta «Borrador» con «Faltan 5 campos…» y «Continuar» abre el formulario.
