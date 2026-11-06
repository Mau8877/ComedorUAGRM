# UT-CUX01-BE-08 — Correo de notificaciones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-08` |
| **Nombre** | Correo de notificaciones |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-07 |

## Antes de implementar

- `B/features/auth/service/CorreoRestablecimientoService.java` (referencia de estilo; **no se modifica**)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca el correo del destinatario ni su contenido sensible)

## Solicitud de negocio

Cada aviso también debe llegar al correo de la persona, y un SMTP caído no debe afectar a nadie más.

## Objetivo

Un servicio que envía el correo de un aviso, con reintentos, sin propagar fallos.

## Alcance

- `B/features/notificaciones/service/CorreoNotificacionService.java`: `boolean enviar(String destino, String asunto, String texto)`, reutiliza `JavaMailSender` y el remitente `app.mail.from`. **No es `@Async`** (decisión al implementar): ya lo llama `DespachoNotificaciones` desde el ejecutor de notificaciones, y otro salto asíncrono con espera podría agotar los 4 hilos de ese ejecutor. Envuelve el envío en `Reintentos` (solo ante errores de transporte, no ante direcciones mal formadas). Devuelve si se entregó (`true`/`false`) para que `UT-CUX01-BE-10` sepa si marcar el aviso como enviado. Si agota los intentos escribe un WARN con el tipo de la excepción, **sin** destinatario.
- Cuerpo en texto plano con el texto del aviso y la misma firma que el correo del CU15 (DUBSS - UAGRM).
- Tests: `CorreoNotificacionServiceTest` con `JavaMailSender` simulado (envío correcto, fallo pasajero que se recupera, agotamiento sin excepción hacia afuera, error que no es de transporte no se reintenta).

Archivos esperados (crear): `CorreoNotificacionService.java` y su test.

## Fuera de alcance (no tocar)

- Plantillas HTML o con marca.
- El correo de restablecimiento de contraseña del CU15.
- Elegir el proveedor SMTP real.

## Resultado esperado

Un correo de aviso aparece en Mailpit (`http://localhost:8025`) y un SMTP caído solo deja un WARN.

## Validación

1. `./mvnw test` pasa.
2. Con Mailpit levantado y una llamada manual al servicio (o con el aviso de prueba de `BE-11`), el correo se ve en `http://localhost:8025`.
3. `./mvnw checkstyle:check` sin errores.
