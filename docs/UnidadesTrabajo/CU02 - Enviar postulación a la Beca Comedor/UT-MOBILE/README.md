# Unidades de trabajo — CU02-MOB

**Caso de uso:** App móvil del envío de la postulación: asistente de tres pasos y comprobante.
**Fuente CU:** [`CU02 - Enviar postulación a la Beca Comedor`](../../../CasosDeUso/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)
**Diagramas de grado:** `images/039` (contexto), `052` (análisis), `122` (secuencia) y `135` (mockup); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** Ruta `/postular` de la app (rol `POSTULANTE`).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `FORMULARIOS_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`L/` = `projects/mobile/lib/`. Todo en `features/postulaciones/`.

1. **Mismas reglas que el web** para los campos (regla de consistencia de `FORMULARIOS_MOBILE.md`).
2. **Los documentos se adjuntan en la pantalla del CU16**, que ya trae cámara, galería y recorte; el paso 3 solo muestra el estado y lleva a ella.
3. **Sin «Guardar borrador»** (CU17).
4. **Estado de la pantalla con `StatefulWidget` + el servicio de Riverpod**, no un `AsyncNotifier`: el asistente tiene estado local de pasos y el envío es una acción puntual.

## Contrato de datos / recurso HTTP

Ver `UT-BACKEND/README.md`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU02-MOB-01](./UT-CU02-MOB-01.md) | Modelos, servicio, validación y errores | — |
| [UT-CU02-MOB-02](./UT-CU02-MOB-02.md) | Pantalla y ruta | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 5, 9 | Asistente y comprobante | 02 |
| A1, A3, A4, A8 | Errores del backend | 01, 02 |
| A5 | Corregir y reenviar | 02 |
| A6 | Un envío a la vez | 02 |

## Follow-ups (no se implementan en estas UT)

- «Guardar borrador» y recuperarlo: **hecho en el [CU17](../../CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n/UT-MOBILE/README.md)**.

## Qué NO incluyen estas UT

- El backend y el web.
- Pruebas en un dispositivo real.
