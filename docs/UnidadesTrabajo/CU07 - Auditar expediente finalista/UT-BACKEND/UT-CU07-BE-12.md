# UT-CU07-BE-12 — Pruebas de integración del expediente con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-12` |
| **Nombre** | Pruebas de integración del expediente con base real |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-10`, `UT-CU07-BE-11` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers, `PostgresTestDatabase`, sin Docker queda SKIPPED, limpiar lo commiteado).

## Solicitud de negocio

Los criterios de aceptación se prueban de punta a punta.

## Objetivo

Existe un test de integración que cubre el CU con Postgres real.

## Alcance

- `ExpedienteIntegrationTest` con `@SpringBootTest` y `MockMvc`, administrador con sesión: ver el expediente (CA01, CA02), registrar «Validado» con puntaje académico y comprobar el total (CA03, CA20), repetir la misma petición (CA09), una segunda auditoría que conserva la anterior (CA06) y el recálculo del total con otro puntaje de IA (CA21).
- Rechazos: observaciones vacías (CA04), puntaje fuera de rango o faltante en «Validado» (CA19), más de 500 caracteres (CA18), entrega sin registrar (CA07), postulación que no está «calificada» (CA08), finalista inexistente (CA15).
- El aviso queda en la bandeja del postulante con el resultado, y la auditoría se conserva aunque el despacho falle (CA17).
- Bitácora con la acción y sin observaciones ni datos personales (CA11); ninguna postulación cambia de estado ni de puntaje de IA (CA12).
- `401` sin sesión, `403` con rol Postulante (CA14).
- Al terminar deja la base vacía de lo que commiteó (`@AfterEach`).

Archivos esperados (crear): `ExpedienteIntegrationTest`.

## Fuera de alcance (no tocar)

- Pruebas de la pantalla (ver `UT-FRONTEND`).
- Pruebas de MinIO real (la subida se prueba con el cliente simulado).

## Resultado esperado

Los criterios CA01 a CA21 relativos al backend tienen un test que los cubre.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
