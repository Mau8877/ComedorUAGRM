# Unidades de trabajo — CU17-MOB

**Caso de uso:** App móvil del borrador de la postulación: «Guardar borrador» con el aviso de la hora en el asistente de «Nueva postulación», y la tarjeta «Mi borrador» en «Mis postulaciones».
**Fuente CU:** [`CU17 - Guardar borrador de postulación`](../../../CasosDeUso/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md)
**Diagramas de grado:** `images/142` (mockup: es un diseño de teléfono) y `129` (secuencia); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** [`UT-BACKEND/README.md`](../UT-BACKEND/README.md#contrato-de-datos--recurso-http).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `FORMULARIOS_MOBILE.md` (las reglas deben coincidir con las del web), `WIDGETS_UI_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

`M/` = `projects/mobile/lib/features/postulaciones/`.

1. **Mismas reglas que el web** ([`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)): `ValoresFormulario.aBorrador()` (en `validacion_formulario.dart`) reutiliza `validarIngresos`, `validarDependientes` y `validarPromedio` solo para lo escrito; lo vacío va en `null`.
2. **Modelos del borrador en un archivo propio** (`models/borrador_postulacion.dart`), porque el contrato `…/borrador` es de este CU y no del envío.
3. **«Mi borrador» con un `FutureProvider.autoDispose`** (`miBorradorProvider`, convención `{feature}…Provider` de [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)); guardar lo invalida. La tarjeta (`widgets/tarjeta_borrador.dart`) se muestra también cuando todavía no hay postulaciones enviadas.
4. **El botón va en el paso 2 y en el paso 3**, igual que en el web.

## Contrato de datos / recurso HTTP

`PostulacionesService.guardarBorrador` (`PUT /postulaciones/{codigo}/borrador`) y `miBorrador` (`GET /postulaciones/mi-borrador`, `null` si no hay); `RespuestaSocioeconomica.guardadoEn`. El interceptor del sobre deja `data` en `null` cuando no hay borrador.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU17-MOB-01](./UT-CU17-MOB-01.md) | «Guardar borrador» en el asistente y tarjeta «Mi borrador» | UT-CU17-BE-02 |

**Cadena:** `BE-02 → MOB-01`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (guardar), A1 a A3 | Botón, validación de lo escrito, errores del backend | 01 |
| Flujo B (retomar) | Hora del último guardado y valores al reabrir | 01 |
| Flujo C («Mi borrador») | Tarjeta con lo que falta | 01 |
| CA01 a CA03, CA08, CA09 | Guardar, rechazo de inválidos, retomar, resumen | 01 |

## Follow-ups (no se implementan en estas UT)

- «Eliminar» el borrador (mockup, img 142), el guardado automático y la advertencia al salir con cambios sin guardar.
- Una prueba en un teléfono o emulador real: los tests son de widgets con el servicio simulado.

## Qué NO incluyen estas UT

- El backend ni la web.
- El envío, el comprobante y los documentos (CU02, CU16).
