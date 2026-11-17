# UT-CU24-MOB-04 — Pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU24-MOB-04` |
| **Nombre** | Pruebas de la app de Mi beca |
| **Caso de uso** | CU24-MOB |
| **Depende de** | UT-CU24-MOB-03 |

## Antes de implementar

[`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (se prueba la lógica y las pantallas con comportamiento, no los widgets triviales).

## Solicitud de negocio

Garantizar que la sección refleja el contrato y los flujos alternativos del CU.

## Objetivo

Hay pruebas de modelos, formatos, errores, providers y de la vista con un servicio simulado.

## Alcance

- `T/features/mibeca/mibeca_test.dart`: `fromJson` de la beca y de la página; formatos (La Paz); `mapearErrorMiBeca`; providers (página 1, rango, `cargarSiguiente`, fallo de la página siguiente); `MiBecaVista` (estado y resumen, baja con motivo, vacío, rango sin resultados y «Quitar filtro», sin beca, error con reintento, solo lectura).

Archivos esperados (crear/tocar): el test de arriba.

## Fuera de alcance (no tocar)

Tests de otras features.

## Resultado esperado

CA03 a CA06 y CA11 quedan cubiertos del lado de la app.

## Validación

1. `flutter test test/features/mibeca` en verde; `flutter test` completo sin regresiones.
