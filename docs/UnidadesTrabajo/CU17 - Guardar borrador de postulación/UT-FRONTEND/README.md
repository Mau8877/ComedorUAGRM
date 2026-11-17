# Unidades de trabajo — CU17-FE

**Caso de uso:** Web del borrador de la postulación: botón «Guardar borrador» con el aviso de la hora del guardado en el asistente de «Nueva postulación», y la tarjeta «Mi borrador» en «Mis postulaciones».
**Fuente CU:** [`CU17 - Guardar borrador de postulación`](../../../CasosDeUso/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md)
**Diagramas de grado:** `images/142` (mockup, pantallas 1 y 2) y `129` (secuencia); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** [`UT-BACKEND/README.md`](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`PUT /postulaciones/{codigo}/borrador`, `GET /postulaciones/mi-borrador`).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md`, `TESTING_FRONTEND.md` y `DOCUMENTACION_IMPLEMENTACION.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`F/` = `projects/frontend/comedoruagrm/src/features/postulaciones/`.

1. **Se amplía la feature `postulaciones`**: no hay feature nueva; el borrador es parte del asistente del CU02.
2. **El guardado no usa el esquema estricto.** `aBorrador(valores)` (en `schemas/respuestasSocioeconomicasSchema.ts`) deja `null` lo vacío y valida con las **mismas reglas por campo** lo escrito; así «Guardar borrador» no marca como error los campos que faltan. Si algo escrito es inválido no se llama al backend y se lista qué corregir.
3. **El botón vive en el paso 2 (formulario) y en el paso 3 (documentos)**; en el 3 guarda lo que se escribió en el 2 (ya validado al «Continuar»). El aviso «✓ Borrador guardado · 14:32» sale de la respuesta del guardado o, al retomar, de `respuesta.guardadoEn`.
4. **«Mi borrador» es una tarjeta en «Mis postulaciones»**, no una pantalla propia; «Continuar» abre `/postular`, que ya recupera el borrador (CU02). Si la consulta falla o no hay borrador, la lista de enviadas se ve igual.
5. **Mutación sin invalidar el formulario:** guardar solo invalida `miBorrador` (la tarjeta); el formulario ya tiene sus valores en el estado de la pantalla.

## Contrato de datos / recurso HTTP

Tipos en `F/types/envio.ts`: `GuardarBorradorRequest`, `BorradorGuardado`, `BorradorResumen`, y `RespuestaSocioeconomica.guardadoEn`. Funciones en `F/api/postulacionesApi.ts` (`guardarBorradorApi`, `miBorradorApi`) y hooks en `F/api/hooks.ts` (`useGuardarBorrador`, `useMiBorrador`); `postulacionesKeys.miBorrador()`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU17-FE-01](./UT-CU17-FE-01.md) | «Guardar borrador» en el asistente y tarjeta «Mi borrador» | UT-CU17-BE-02 |

**Cadena:** `BE-02 → FE-01`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (guardar), A1 a A3 | Botón, validación de lo escrito, errores del backend | 01 |
| Flujo B (retomar) | Hora del último guardado y valores al reabrir | 01 |
| Flujo C («Mi borrador») | Tarjeta con lo que falta | 01 |
| CA01 a CA03, CA08, CA09 | Guardar, rechazo de inválidos, retomar, resumen | 01 |

## Follow-ups (no se implementan en estas UT)

- «Eliminar» el borrador (botón del mockup, img 142): el CU no lo describe.
- Guardado automático al salir o cada cierto tiempo.
- Avisar al salir del paso 2 con cambios sin guardar.

## Qué NO incluyen estas UT

- El backend (`UT-BACKEND`) ni la app móvil (`UT-MOBILE`).
- El envío, el comprobante y los documentos (CU02, CU16).
