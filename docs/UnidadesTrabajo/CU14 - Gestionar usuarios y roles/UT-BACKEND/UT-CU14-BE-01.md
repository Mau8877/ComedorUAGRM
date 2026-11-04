# UT-CU14-BE-01 — Códigos de error `ERR_US_06` a `ERR_US_12` y su catálogo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-01` |
| **Nombre** | Códigos de error `ERR_US_06` a `ERR_US_12` y su catálogo |
| **Caso de uso** | CU14-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md)
- [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)

## Solicitud de negocio

Todo rechazo que ve el cliente tiene que estar catalogado y documentado, para que soporte y el frontend sepan qué pasó sin leer el código.

## Objetivo

Existen las constantes `ERR_US_06` a `ERR_US_12` y su documentación, listas para que las usen las UT siguientes.

## Alcance

- `B/features/usuarios/UsuarioErrorCodes.java`: agrega `ERR_US_06` (404, cuenta no encontrada o de Postulante), `ERR_US_07` (409, ya está en ese estado), `ERR_US_08` (403, propia cuenta), `ERR_US_09` (403, dejaría al sistema sin Administrador activo), `ERR_US_10` (400, rol no válido para el personal), `ERR_US_11` (400, `sort` o `filter` no soportado) y `ERR_US_12` (409, la cuenta ya tiene ese rol). **No reasigna `ERR_US_05`** (retirado).
- `docs/errors/usuarios/ERRORES_USUARIOS.md`: una sección por código con el formato de la guía (HTTP, excepción, significado, dónde se lanza). «Dónde se lanza» apunta a la clase y el método previstos por las UT 07 a 11 (`UsuarioPersonalService.…`).
- `docs/errors/README.md`: un link por código nuevo en la sección de Usuarios.

Archivos esperados (crear/tocar): los tres de arriba.

## Fuera de alcance (no tocar)

- Lanzar las excepciones: lo hace cada UT del `service` que usa el código.
- `ERR_AUTH_07` (`403` por falta de rol): es de `UT-CU14-BE-02`.
- Modificar los códigos `ERR_US_01` a `ERR_US_04`.

## Resultado esperado

Los siete códigos existen en el código y en `docs/errors/`, con su índice al día.

## Validación

1. `./mvnw test` pasa (compila con las constantes nuevas).
2. Cada código aparece en `ERRORES_USUARIOS.md` con sus cuatro campos y tiene su link en el índice del `README.md`.
3. `./mvnw checkstyle:check` sin errores.
