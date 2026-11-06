# UT-CUX01-BE-12 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-12` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-06, UT-CUX01-BE-11 |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (`PostgresTestDatabase`, limpieza en `@AfterEach`, pool chico)

## Solicitud de negocio

Asegurar que el registro de teléfonos y el despacho funcionan de verdad contra PostgreSQL, sin llamar nunca a Firebase.

## Objetivo

Una suite de integración que cubre los flujos A, B, C y E del CU con base real, un canal de push falso y un correo simulado.

## Alcance

- `T/features/notificaciones/NotificacionesIntegrationTest.java` (`@SpringBootTest` + `MockMvc` + `PostgresTestDatabase`), con un `CanalPush` y un `CorreoNotificacionService` de prueba que registran lo recibido:
  - Registrar un token (`201`), repetirlo (`200`, una sola fila), registrar el mismo token con otra cuenta (queda a nombre de la nueva) y dar de baja (idempotente, token ajeno sin efecto).
  - Aviso de prueba como administrador: fila en `notificacion`, push al teléfono registrado, correo, `enviada_en` lleno; como postulante `403`; sin sesión `401`.
  - Sin teléfonos: solo correo. Token que el canal falso declara inválido: queda eliminado.
  - Rollback de la transacción que notifica: no queda fila ni se envía nada.
  - Ningún log del test contiene el token.
- Limpieza de las tablas tocadas en `@AfterEach`.

Archivos esperados (crear): la clase de prueba y sus soportes de prueba.

## Fuera de alcance (no tocar)

- Firebase real (se comprueba a mano con el aviso de prueba, `UT-CUX01-MOB-05`).
- Tests de los eventos de negocio futuros.

## Resultado esperado

La suite pasa con Docker disponible y queda `SKIPPED` sin él, como el resto de los tests con Testcontainers.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
