# UT-CU20-BE-08 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-08` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU20-BE |
| **Depende de** | `UT-CU20-BE-05`, `UT-CU20-BE-06`, `UT-CU20-BE-07` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md).
- Como referencia: `T/features/postulaciones/DevolucionIntegrationTest.java` (CU-19, Testcontainers con `assumeTrue` de Docker).

## Solicitud de negocio

Confirmar que todo funciona junto contra una PostgreSQL de verdad.

## Objetivo

Existe una prueba de integración de punta a punta del CU.

## Alcance

- `T/features/postulaciones/CierrePostulacionIntegrationTest.java` con Testcontainers (se salta sin Docker, patrón obligatorio), sobre las migraciones reales y los datos de UT-07:
  - archivar con motivo válido: `200`, estado «archivada», una fila de historial (anterior, nuevo, usuario y motivo), una anotación de bitácora **sin** el motivo y un aviso en la bandeja del postulante con el motivo;
  - rechazar con motivo válido: igual, con estado «no adjudicada»;
  - archivar a un finalista sin auditorías: permitido *(por revisar)*;
  - motivo vacío, solo espacios o de 501 caracteres: `400` y nada cambia;
  - segunda decisión seguida: `409` `ERR_ARC_01` y una sola fila nueva en el historial;
  - dos peticiones concurrentes sobre la misma postulación: una `200` y otra `409`, con un solo aviso y una sola anotación de bitácora;
  - postulación ya devuelta, archivada o no adjudicada: `409` `ERR_ARC_01`;
  - la postulación cerrada no aparece en la lista priorizada, aparece con su estado en la lista de finalistas y ya no se puede auditar (`409`, error del CU-07 correspondiente); auditorías, puntaje y documentos siguen igual;
  - el historial conserva todas las filas anteriores;
  - `404` finalista inexistente, `401` sin sesión y `403` con otro rol.
- La regla de resultados publicados se verifica en el test del `service` (UT-04); aquí solo cuando el CU-22 ya aporte la marca.

Archivos esperados (crear): `CierrePostulacionIntegrationTest`.

## Fuera de alcance (no tocar)

- Pruebas de la pantalla.
- Publicar resultados (CU-22).

## Resultado esperado

El CU queda verificado de punta a punta.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
