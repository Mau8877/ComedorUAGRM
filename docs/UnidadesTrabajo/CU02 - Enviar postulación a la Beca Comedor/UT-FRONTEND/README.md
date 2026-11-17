# Unidades de trabajo — CU02-FE

**Caso de uso:** Pantalla web del envío de la postulación: asistente de tres pasos (perfil, formulario socioeconómico, documentos y envío) y comprobante.
**Fuente CU:** [`CU02 - Enviar postulación a la Beca Comedor`](../../../CasosDeUso/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md)
**Diagramas de grado:** `images/039` (contexto), `052` (análisis), `122` (secuencia) y `135` (mockup); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)
**Recurso HTTP / contrato:** Ruta `/postular` del panel web (solo `POSTULANTE`).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

`F/` = `projects/frontend/comedoruagrm/src/`. Todo en `features/postulaciones/` (misma feature que el CU04 y el CU16).

1. **Una sola ruta, `/postular`**, para «Nueva postulación» y «Corregir y reenviar»: el backend decide si abre un borrador nuevo o la devuelta.
2. **El perfil no se edita aquí.** Sale de la cuenta; si está mal, se corrige en `/perfil`.
3. **Las listas desplegables las trae el backend** (`opciones`): el catálogo está por definir con la DUBSS y no se repite en el cliente.
4. **Sin «Guardar borrador».** El mockup lo muestra, pero es el CU17 (B9).
5. **Reuso del CU16.** El paso 3 monta `DocumentosPostulacionPanel`; no se duplica la lógica de adjuntar.

## Contrato de datos / recurso HTTP

Ver `UT-BACKEND/README.md`. Los errores `ERR_ENV_*` y `ERR_VAL_*` se traducen en `api/mapearErrorEnvio.ts`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU02-FE-01](./UT-CU02-FE-01.md) | API, hooks, esquema y errores | — |
| [UT-CU02-FE-02](./UT-CU02-FE-02.md) | Pantalla, comprobante y ruta | 01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 5, 9 | Asistente y comprobante | 02 |
| A1, A3, A4, A8 | Errores del backend en pantalla | 01, 02 |
| A5 | Corregir y reenviar | 02 |
| A6 | Un envío a la vez | 02 |
| CA01 a CA09 | Flujo completo | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- «Guardar borrador» y recuperarlo: **hecho en el [CU17](../../CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n/UT-FRONTEND/README.md)**.

## Qué NO incluyen estas UT

- El backend y el móvil.
- Las pruebas con un navegador real del flujo completo (se hacen con el stack levantado).
