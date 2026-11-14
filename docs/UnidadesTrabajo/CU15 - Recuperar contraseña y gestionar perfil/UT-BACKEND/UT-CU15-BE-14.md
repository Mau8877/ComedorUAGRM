# UT-CU15-BE-14 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-14` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-13` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Postgres real por JVM, limpiar la base al terminar, pool chico)
- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md#tests)

## Solicitud de negocio

Hay que confirmar, de punta a punta y contra una base real, que la recuperación y el perfil cumplen los criterios de aceptación.

## Objetivo

Un test de integración recorre el CU15 completo por HTTP y verifica base y bitácora.

## Alcance

- `T/features/auth/RecuperacionContrasenaIntegrationTest.java` y `T/features/usuarios/PerfilIntegrationTest.java` (`@SpringBootTest` + `MockMvc`, base con `PostgresTestDatabase`, `JavaMailSender` o `CorreoRestablecimientoService` como bean mockeado para capturar el enlace; **no** se conectan a Mailpit). Cada test deja la base vacía al terminar (`@AfterEach`).
- Recuperación: cuenta activa → la cuenta guarda el hash y su vencimiento (ningún token en claro en la base) y se capturó un enlace; correo inexistente y cuenta desactivada → misma respuesta `200` y sin correo; dos solicitudes → solo el segundo enlace sirve (CA06); restablecer → entra con la nueva y **no** con la vieja, la cuenta bloqueada queda desbloqueada (CA09), todas las sesiones previas dan `401` al renovar (CA08), el mismo enlace una segunda vez da `ERR_AUTH_08` (CA05), un enlace vencido (se ajusta `restablecimiento_expira_en` en la base) da `ERR_AUTH_08` (CA04); contraseña corta o sin coincidir → `400` y nada cambia (CA07); una cuenta heredada con hash `'!'` puede restablecer.
- Perfil: obtener como Postulante y como Administrador (CA10); actualizar teléfono (CA12); cambiar correo con la actual (CA13) y entrar luego con el correo nuevo; correo ajeno → `409` `ERR_US_02` sin cambios (CA14); sin contraseña o con una incorrecta → `400` `ERR_AUTH_09` sin cambios (CA15); `ci` en el cuerpo no cambia (CA11); cambiar la contraseña → otras sesiones muertas y la actual viva (CA16), con la cookie (web) y con el cuerpo (mobile); sin token → `401` (CA17).
- Bitácora (CA19): una fila por restablecimiento, cambio de contraseña y actualización de perfil, con actor, entidad e IP esperados; **ninguna** por la solicitud del enlace; ninguna fila ni texto con contraseñas, tokens ni enlaces; un `PATCH` sin cambios no deja fila.
- Logs (CA19): una verificación de que ningún log de esos flujos contiene el token ni la contraseña (captura con `ListAppender` de Logback).

Archivos esperados (crear): los dos tests.

## Fuera de alcance (no tocar)

- Conectarse a un SMTP real o a Mailpit en los tests.
- Pruebas de las pantallas.

## Resultado esperado

Los criterios CA01 a CA19 que le tocan al backend quedan cubiertos por una prueba que pasa contra Postgres real.

## Validación

1. `./mvnw test` pasa (las pruebas de integración quedan SKIPPED sin Docker; correrlas con Docker o con `-Dtest.postgres.url=...`).
2. `./mvnw checkstyle:check` sin errores.
