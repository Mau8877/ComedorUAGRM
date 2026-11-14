# UT-CU21-BE-12 — Datos de ejemplo del CU21: avisos, comunicado y postulante con sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-12` |
| **Nombre** | Datos de ejemplo del CU21: avisos, comunicado y postulante con sesión |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-04`, `UT-CU21-BE-09` |

## Antes de implementar

- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (contraseñas solo como hash BCrypt) y `B/config/DatosInicialesAdmin.java`.
- Como referencia: `DatosPruebaListaPriorizada` (`UT-CU21-BE-04`).

## Solicitud de negocio

Para ver la bandeja como Postulante y la pantalla del administrador con datos reales hace falta algo más que los finalistas: avisos, un comunicado y un postulante que pueda iniciar sesión.

## Objetivo

En desarrollo hay un comunicado publicado, avisos de ejemplo en la bandeja de los finalistas notificados, y un postulante finalista con el que se puede entrar.

## Alcance

- `B/config/DatosPruebaFinalistas.java` (`@Component`, `CommandLineRunner`, `@Order(3)`, `@Profile("dev")`, apagable con `app.seed.finalistas.enabled`), idempotente por la marca `[PRUEBA]`; si no está la convocatoria de prueba o el administrador, no hace nada.
- Habilita el inicio de sesión de **uno de los finalistas ya notificados** (el de la posición 2): le cambia el correo a `postulante.finalista@prueba.local` y le pone una contraseña de prueba (solo su hash BCrypt, propiedad `app.seed.finalistas.contrasena`). Los tres finalistas notificados reciben su aviso en la bandeja (el de la posición 3 sin `enviada_en`, para ver «Fallida»).
- Un comunicado `[PRUEBA]` (`TODOS`) publicado por el administrador, con su aviso para los primeros 15 postulantes de la convocatoria.
- No manda push ni correo reales. No cambia el estado de ninguna postulación existente.
- Documentado en `docs/README.md` (cuenta de prueba, solo desarrollo). La contraseña nunca se loguea.
- Test con base real: crea la cuenta, los avisos y el comunicado, y una segunda corrida no duplica nada.

Archivos esperados (crear): `DatosPruebaFinalistas.java` y su test. (Tocar): `application.properties`, `docs/README.md`.

## Fuera de alcance (no tocar)

- Crear convocatorias o elegir finalistas de verdad (CU-13, CU-05).
- Cuentas de otros roles y cualquier cosa en producción.

## Resultado esperado

Con perfil `dev` se puede entrar como `postulante.finalista@prueba.local` y ver su aviso, y como administrador ver la pantalla con finalistas en los tres estados.

## Validación

1. `./mvnw test` pasa.
2. Arrancar en `dev`: el login del postulante responde `200` y `GET /api/v1/notificaciones/me/resumen` cuenta su aviso.
