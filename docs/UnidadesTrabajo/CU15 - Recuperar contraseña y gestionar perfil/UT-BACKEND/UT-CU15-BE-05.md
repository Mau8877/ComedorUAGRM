# UT-CU15-BE-05 — Correo del enlace de restablecimiento

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-05` |
| **Nombre** | Correo del enlace de restablecimiento (asíncrono y tolerante a fallos) |
| **Caso de uso** | CU15-BE |
| **Depende de** | `UT-CU15-BE-03` |

## Antes de implementar

- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Quien pide recuperar su contraseña recibe un correo claro con el enlace; si el servicio de correo falla, la persona no se entera de si la cuenta existe y puede volver a pedirlo.

## Objetivo

Existe un servicio que arma y envía el correo con el enlace, sin bloquear la petición y sin propagar sus fallos.

## Alcance

- `B/features/auth/service/CorreoRestablecimientoService.java`: `@Service` con `void enviarEnlace(String correoDestino, String nombres, String token)`, anotado `@Async` (ejecutor de `CorreoConfig`). Arma el enlace `{app.web.base-url}/restablecer-contrasena?token={token}` (token URL-encoded), un asunto («Restablece tu contraseña de ComedorU») y un cuerpo en **español** de texto plano con el saludo, el enlace, la vigencia (`app.auth.reset-ttl-minutes` minutos) y la frase de que si no lo pidió puede ignorar el correo. Envía con `JavaMailSender` y el remitente `app.mail.from`.
- Un `MailException` o cualquier fallo de envío se **captura**: `LOG.warn("No se pudo enviar el correo de restablecimiento", kv("module","auth"))` sin el correo, el token ni el enlace, y no se relanza (A9, CA18).
- El logger **no** escribe nunca el enlace ni el token, ni siquiera a nivel `DEBUG`.
- Tests (con `JavaMailSender` mockeado): el mensaje enviado lleva el destinatario, el remitente configurado, el asunto, el enlace con el token y la vigencia; un `MailSendException` no se propaga y se loguea sin el token ni el correo (se verifica el contenido del log); el nombre con caracteres especiales no rompe el cuerpo.

Archivos esperados (crear): `CorreoRestablecimientoService.java` y su test.

## Fuera de alcance (no tocar)

- Decidir si hay que enviar o no (cuenta activa o no): lo hace UT-09.
- Plantilla HTML o imágenes: texto plano alcanza para este CU.
- Reintentos automáticos o cola de correos.

## Resultado esperado

Llamar al servicio con un token deja un correo en Mailpit con el enlace correcto, y un fallo del SMTP no afecta a quien lo llamó.

## Validación

1. `./mvnw test` pasa.
2. Con el stack levantado y la UT-09 hecha, el correo aparece en `http://localhost:8025`.
3. `./mvnw checkstyle:check` sin errores.
