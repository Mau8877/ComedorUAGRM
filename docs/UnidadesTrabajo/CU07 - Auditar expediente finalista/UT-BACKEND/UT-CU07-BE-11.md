# UT-CU07-BE-11 — Expedientes de prueba en el cargador de desarrollo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-11` |
| **Nombre** | Expedientes de prueba en el cargador de desarrollo |
| **Caso de uso** | CU07-BE |
| **Depende de** | `UT-CU07-BE-01`, `UT-CU07-BE-02`, `UT-CU07-BE-03`, `UT-CU07-BE-05` |

## Antes de implementar

- `B/config/DatosPruebaListaPriorizada.java` y `B/config/DatosPruebaFinalistas.java` como referencia (perfil `dev`, marca `[PRUEBA]`, idempotente).
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

El CU-02 y el CU-16 los hace otro integrante y el CU-05 viene después; sin datos de ejemplo no se puede probar este CU.

## Objetivo

En desarrollo, los 10 finalistas de la convocatoria `[PRUEBA]` tienen expediente de ejemplo con situaciones variadas.

## Alcance

- `B/config/DatosPruebaExpedientes.java` (`@Profile("dev")`, `@Order(4)`, apagable con `app.seed.expedientes.enabled=false`, no hace nada si no existen la convocatoria `[PRUEBA]` ni sus finalistas, e idempotente).
- Crea los tipos de documento de la maqueta (Carnet de identidad, Certificado de ingresos, Certificado de notas, Fotocopia de luz), los marca como exigidos en la convocatoria grande y registra la respuesta socioeconómica (con `promedio_academico`) de los 10 finalistas.
- Crea los `documento_adjunto` y sube a MinIO un archivo pequeño de ejemplo por cada uno (PDF generado en memoria) con `AlmacenamientoService`. **Si MinIO no está disponible**, loguea un `WARN` y guarda igual las filas: así se puede probar el error de A4 («no se pudo abrir el documento»).
- Situaciones: el finalista 1 con los 4 documentos, entrega recibida y auditoría «Validado» (puntaje académico 25); el 2 sin Certificado de notas y su aviso de «Observado» en la bandeja de la cuenta `postulante.finalista@prueba.local`; el 3 con entrega recibida y sin auditoría; el 4 con dos auditorías («Observado» y después «Validado»); el 5 con «No válido»; el resto con la entrega pendiente.
- Para tener esas situaciones registra la entrega (`documentacion_recibida_en`) de los finalistas que la necesitan; **no** cambia notificaciones ni estados de postulación, y se ajustan los tests del CU-21 que cuenten entregas «Recibida».
- Test del cargador con base real: crea los expedientes y las situaciones esperadas, y es idempotente.

Archivos esperados (crear): `DatosPruebaExpedientes.java` y su test. (Tocar): tests de CU-21 afectados y `docs/README.md` (cómo ver los expedientes de prueba).

## Fuera de alcance (no tocar)

- Cualquier cosa en producción.
- Crear postulantes, postulaciones o finalistas nuevos (los de CU-06 y CU-21).
- Los Models de documentos (CU-16).

## Resultado esperado

Con perfil `dev`, `GET /api/v1/finalistas/{codigo}/expediente` devuelve un expediente completo para cada finalista de prueba.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Arrancar en `dev` con MinIO: `select count(*) from documento_adjunto` > 0 y los archivos existen en el bucket.
