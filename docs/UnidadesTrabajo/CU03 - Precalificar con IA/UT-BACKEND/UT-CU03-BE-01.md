# UT-CU03-BE-01 — Módulo de IA: contrato HTTP del cálculo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-01` |
| **Nombre** | Módulo de IA: contrato HTTP del cálculo |
| **Caso de uso** | CU03-BACKEND |
| **Depende de** | — |

## Antes de implementar

[`PUNTAJE_IA.md`](../../../../.claude/rules/backend-ia/PUNTAJE_IA.md) y [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer `SolicitudPuntaje` y `PuntajeCalculado` del core (`features/preseleccion/dto/`).

## Solicitud de negocio

El core necesita un servicio al que pedirle el puntaje de una postulación con las respuestas del formulario, sin mandarle datos personales.

## Objetivo

`POST /api/v1/puntaje` en `comedor-uagrm-ia`: recibe la solicitud del core, valida los datos y responde `{puntaje, modelo, detalle}`.

## Alcance

- `app/schemas.py`: `SolicitudPuntaje` (camelCase, con `alias_generator`) con las variables originales, los datos del formulario oficial como opcionales (`dependenciaEconomica`, `ocupacionSosten`, `tieneHijos`, `cantidadHijos`, `discapacidad`), `modelo` y `parametros`; y `PuntajeCalculado`.
- `app/main.py`: la ruta `POST /api/v1/puntaje` y el manejador que responde `422` a unas ponderaciones que no sirven (`ParametrosInvalidos`).
- Rangos de entrada (promedio 0 a 100, ingresos y dependientes no negativos, `postulacion` un UUID) con `422` al violarlos; los campos que no conoce se ignoran.
- `tests/test_puntaje.py` (`TestContrato`, `TestEntradas`) y `tests/test_health.py`.

Archivos esperados (crear/tocar): `app/schemas.py`, `app/main.py`, `tests/test_puntaje.py`, `tests/test_health.py`.

## Fuera de alcance (no tocar)

- El cálculo en sí (UT-CU03-BE-02): aquí la ruta puede devolver el resultado de `calcular()` ya hecho.
- El cliente HTTP del core (UT-CU03-BE-03).
- Autenticación entre el core y la IA: viajan por la red interna de Docker y no hay token (ver follow-ups).

## Resultado esperado

Una solicitud con la forma que arma el core responde `200` con `puntaje`, `modelo` y `detalle`; una con datos fuera de rango o sin una variable obligatoria responde `422`.

## Validación

1. `cd projects/backend/comedor-uagrm-ia && pytest tests/test_puntaje.py -k "Contrato or Entradas"` en verde.
2. `ruff check . ; ruff format --check .` sin avisos.
