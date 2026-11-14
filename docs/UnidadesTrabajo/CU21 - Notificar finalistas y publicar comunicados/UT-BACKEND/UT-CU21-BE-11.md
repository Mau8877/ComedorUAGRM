# UT-CU21-BE-11 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-11` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU21-BE |
| **Depende de** | `UT-CU21-BE-04`, `UT-CU21-BE-09`, `UT-CU21-BE-10` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`PostgresTestDatabase`, limpiar lo que se commitea, un solo Postgres por JVM).
- Como referencia: `ListaPriorizadaIntegrationTest` y `NotificacionesIntegrationTest`.

## Solicitud de negocio

Comprobar de punta a punta que el CU cumple sus criterios de aceptación.

## Objetivo

Una prueba de integración recorre los flujos A, B, C y D contra Postgres real.

## Alcance

`FinalistasComunicadosIntegrationTest` (`@SpringBootTest` + `MockMvc`, canal de push simulado), que cubre: notificar a un finalista **sin teléfono** (aviso en bandeja y correo, CA03); notificar con campos vacíos y con plazo de hoy (`400`); un aviso sin `enviada_en` se ve «Fallida» y al reenviar queda «Enviada» (CA04); registrar la entrega con y sin fecha, fecha futura (`400`) y corrección con el valor anterior en la bitácora (CA07–CA10); comunicado a `TODOS` y a `FINALISTAS`, `ADJUDICADOS` y grupo vacío (`400`, CA11–CA13); la bandeja muestra solo los avisos propios y permite leerlos (CA14); la bitácora sin datos personales (CA15); **ninguna postulación cambia de estado** (CA16); `401`, `403` y `404` (CA18–CA19). Limpia lo que commitea en `@AfterEach`.

Archivos esperados (crear): el test de integración.

## Fuera de alcance (no tocar)

- Envío real a Firebase o SMTP.
- Pruebas de rendimiento del envío masivo.

## Resultado esperado

Los criterios de aceptación del backend quedan verificados con base real.

## Validación

1. `./mvnw test` pasa (si Docker no está, con `-Dtest.postgres.url` sobre una base vacía y descartable).
