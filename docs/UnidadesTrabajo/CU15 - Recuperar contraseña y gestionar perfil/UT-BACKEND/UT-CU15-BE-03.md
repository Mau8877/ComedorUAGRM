# UT-CU15-BE-03 — Envío de correo por SMTP: Mailpit en desarrollo y configuración

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-03` |
| **Nombre** | Envío de correo por SMTP: Mailpit en desarrollo y configuración |
| **Caso de uso** | CU15-BE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_BACKEND.md`](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (`config/`)
- [`docs/README.md`](../../../README.md) (variables de entorno y checklist de puesta en marcha)

## Solicitud de negocio

Para probar la recuperación de contraseña hace falta poder "enviar" correos sin mandar nada a personas reales, y poder pasar a un proveedor real más adelante sin cambiar código.

## Objetivo

El backend sabe enviar correos por SMTP a través de variables de entorno, y en desarrollo los correos caen en Mailpit, que se ve en `http://localhost:8025`.

## Alcance

- `pom.xml`: dependencia `spring-boot-starter-mail`.
- `docker-compose.yml`: servicio `mailpit` (imagen `axllent/mailpit` con una **versión fija**, verificada en Docker Hub; contenedor `comedor-uagrm-mailpit`; puerto `8025` publicado para la interfaz web y `1025` para SMTP; variables `MP_SMTP_AUTH_ACCEPT_ANY=1` y `MP_SMTP_AUTH_ALLOW_INSECURE=1`; healthcheck; red `comedor-uagrm-network`) y `core` con `depends_on` a él y las variables `MAIL_*` y `FRONTEND_URL` nuevas. Mailpit **no** pasa por el proxy: se abre directo en el puerto 8025.
- `application.properties`: `spring.mail.host=${MAIL_HOST:localhost}`, `spring.mail.port=${MAIL_PORT:1025}`, `spring.mail.username` y `password` (vacíos por defecto), `spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:false}`, `mail.smtp.starttls.enable=${MAIL_SMTP_STARTTLS:false}`; y propiedades propias `app.mail.from=${MAIL_FROM:no-responder@comedoru.local}`, `app.web.base-url=${FRONTEND_URL:http://localhost}` y `app.auth.reset-ttl-minutes=${RESET_TTL_MINUTES:60}`.
- `B/config/CorreoConfig.java`: `@Configuration` con `@EnableAsync` y un ejecutor chico dedicado a correo (`TaskExecutor` con nombre propio, 2 hilos), para que el envío no bloquee la request.
- `.env.example` (raíz): `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM` y `FRONTEND_URL`, con un comentario de qué cambia en producción (proveedor real: host y credenciales, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, `FRONTEND_URL` con el dominio HTTPS).
- `src/test/resources/config/application.properties`: valores que no intenten conectarse a ningún SMTP (los tests mockean el envío).
- `docs/README.md` y `CLAUDE.md`: mencionar Mailpit (puertos 8025/1025) en la lista de servicios del stack y las variables nuevas.

Archivos esperados (tocar): `pom.xml`, `docker-compose.yml`, `application.properties`, `.env.example`, la config de test, `docs/README.md` y `CLAUDE.md`. (Crear): `CorreoConfig.java`.

## Fuera de alcance (no tocar)

- Escribir o enviar un correo concreto (`UT-CU15-BE-05`).
- Elegir o configurar el proveedor SMTP real: queda por definir; con esta UT se cambia solo con variables.
- Publicar Mailpit detrás de nginx.

## Resultado esperado

`docker compose up` levanta Mailpit y el backend arranca con el mail configurado; abrir `http://localhost:8025` muestra la bandeja vacía.

## Validación

1. `docker compose up` deja `mailpit` y `core` en pie, y `http://localhost:8025` responde.
2. `./mvnw test` pasa y el contexto de Spring arranca sin SMTP disponible.
3. `./mvnw checkstyle:check` sin errores.
