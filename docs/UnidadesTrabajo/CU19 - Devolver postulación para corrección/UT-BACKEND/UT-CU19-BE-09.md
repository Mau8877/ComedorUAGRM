# UT-CU19-BE-09 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-09` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU19-BE |
| **Depende de** | `UT-CU19-BE-07`, `UT-CU19-BE-08` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `T/features/expedientes/ExpedienteIntegrationTest.java` (Testcontainers con `assumeTrue` de Docker).

## Solicitud de negocio

Confirmar que todo funciona junto contra una PostgreSQL de verdad.

## Objetivo

Existe una prueba de integración de punta a punta del CU.

## Alcance

- `T/features/postulaciones/DevolucionIntegrationTest.java` con Testcontainers (se salta sin Docker, patrón obligatorio), sobre las migraciones reales y los datos de UT-08:
  - devolver con motivo válido: `200`, la postulación queda «devuelta», una fila de historial con estado anterior, nuevo, usuario y motivo, una anotación de bitácora **sin** el motivo y un aviso en la bandeja del postulante (con las observaciones);
  - motivo vacío o de 501 caracteres: `400` y nada cambia;
  - sin auditoría previa: `409` `ERR_DEV_02`;
  - segunda devolución seguida: `409` `ERR_DEV_01` y una sola fila en el historial;
  - dos peticiones concurrentes sobre la misma postulación: una `200` y otra `409`;
  - una postulación devuelta ya no se puede auditar (`409` `ERR_EXP_02`), no aparece en la lista priorizada y aparece como `devuelta` en la lista de finalistas;
  - `404` finalista inexistente, `401` sin sesión y `403` con otro rol;
  - devolver de nuevo tras simular el reenvío (estado vuelto a «calificada» por SQL en el test) y una auditoría nueva: permitido.

Archivos esperados (crear): `DevolucionIntegrationTest`.

## Fuera de alcance (no tocar)

- Pruebas de la pantalla.
- El reenvío real del postulante (CU-02).

## Resultado esperado

El CU queda verificado de punta a punta.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
