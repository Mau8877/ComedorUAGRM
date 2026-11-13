# UT-CU22-BE-11 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-11` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU22-BE |
| **Depende de** | `UT-CU22-BE-08`, `UT-CU22-BE-09`, `UT-CU22-BE-10` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md): patrón de Testcontainers con `assumeTrue` (el test se salta, no falla, sin Docker).
- Como referencia: `UT-CU20-BE-08` y `UT-CU05-BE-11`.

## Solicitud de negocio

Publicar toca varias tablas y envía muchos avisos: se prueba contra PostgreSQL real, de punta a punta.

## Objetivo

Hay pruebas de integración que recorren el flujo completo con base real y los avisos simulados.

## Alcance

- `T/features/resultados/PublicacionResultadosIntegracionTest.java` (Testcontainers + `MockMvc`, con el despacho de push y correo simulado):
  - Publicar con todo decidido: `201`, fila en `publicacion_resultado`, convocatoria «publicada», un aviso por cada finalista con decisión (cada uno con su resultado), ninguno para archivados, rechazados ni devueltos, y bitácora sin datos sensibles.
  - Con pendientes: `409` `ERR_PUB_01`, nada guardado, ningún aviso.
  - Segunda publicación y doble petición simultánea: `409` `ERR_PUB_02` y una sola fila y un solo envío.
  - Convocatoria no cerrada: `409` `ERR_PUB_03`.
  - Un fallo de envío simulado deja el aviso en la bandeja sin deshacer la publicación.
  - Después de publicar: adjudicar, no adjudicar, devolver, archivar y rechazar responden `409`.
  - Lista pública: solo adjudicados con nombre, apellido, nota, carrera y facultad (sin CI ni correo), paginada y con `search`; antes de publicar `404` `ERR_PUB_04`.
  - `mio`: adjudicado, no adjudicado y usuario ajeno (`resultado = null`).
  - Publicar sin ningún adjudicado: funciona y cada finalista recibe «no adjudicada».
  - Avisos por lotes: con 5 y con 500 finalistas el número de consultas es el mismo.
  - Comunicado a `ADJUDICADOS` llega solo a los adjudicados.
  - Publicar sin acta funciona; ninguna postulación cambia de estado y no se crea el padrón.
  - `403` para un no administrador en publicar, resumen y decisiones; `401` sin sesión.
- Todo vuelve limpio entre pruebas.

Archivos esperados (crear): la clase de integración.

## Fuera de alcance (no tocar)

- Pruebas unitarias ya cubiertas en las UT anteriores.
- Probar el envío real de Firebase o del correo.

## Resultado esperado

El flujo de publicar funciona completo contra una base real y respeta cada regla del CU.

## Validación

1. `./mvnw test` pasa (con Docker corre; sin Docker, se salta).
2. `./mvnw checkstyle:check` sin errores.
