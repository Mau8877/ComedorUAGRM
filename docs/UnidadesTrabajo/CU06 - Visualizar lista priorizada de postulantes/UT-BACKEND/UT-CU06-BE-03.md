# UT-CU06-BE-03 — Cargador de datos de prueba (solo desarrollo)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-03` |
| **Nombre** | Cargador de datos de prueba (solo desarrollo) |
| **Caso de uso** | CU06-BE |
| **Depende de** | `UT-CU06-BE-02` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (restricciones de cada tabla) y [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (contraseñas, cuentas sin acceso).
- Como referencia de cuentas sin contraseña válida: las cuentas heredadas con `password_hash = '!'` (ver `UT-CU15-BE` README).

## Solicitud de negocio

Hasta que el CU-13, el CU-02 y el CU-03 existan, hay que poder ver y probar la lista con datos que parezcan reales, incluidos los casos difíciles (empates, reprocesos, otros estados, volumen).

## Objetivo

Un cargador opcional crea convocatorias, postulantes, postulaciones y puntajes de prueba, sin riesgo de correr en producción.

## Alcance

- `B/config/DatosPruebaListaPriorizada.java` (`@Component`, `CommandLineRunner`) con `@Profile("dev")` **y** `@ConditionalOnProperty("app.seed.priorizacion.enabled", havingValue = "true", matchIfMissing = true)` (por defecto activo; en desarrollo se apaga con `SEED_PRIORIZACION_ENABLED=false`). Propiedades en `application.properties` : `app.seed.priorizacion.enabled` y `app.seed.priorizacion.postulaciones` (por defecto `60`; se puede subir a `20000` para medir rendimiento).
- **Idempotente:** si ya existe la convocatoria marcada `[PRUEBA]`, no hace nada. Todo lo que crea lleva esa marca (nombre de convocatoria, correos `…@prueba.local`) para poder borrarlo.
- Crea, con inserciones por lotes (`JdbcTemplate.batchUpdate`) para soportar 20 000 filas:
  - Una gestión académica y un modelo de preselección activo `[PRUEBA]` (`creado_por` = un Administrador DUBSS existente; si no hay ninguno, **no carga nada** y avisa con un WARN claro; el cargador **no** crea administradores: eso lo hace `UT-CU06-BE-10`).
  - Tres convocatorias: una `CERRADA` con casi todas las postulaciones, una `ABIERTA` con pocas, y una `BORRADOR` sin postulaciones (para el caso de lista vacía).
  - Postulantes de prueba: cuentas `POSTULANTE` con `password_hash = '!'` (no pueden iniciar sesión), CI y registro universitario únicos, correo `…@prueba.local`, repartidos entre las carreras ya cargadas.
  - Postulaciones de la convocatoria grande con esta distribución aproximada: 70 % `calificada`, 15 % `pendiente`, 5 % `devuelta`, 5 % `archivada`, 3 % `adjudicada`, 2 % `no_adjudicada`; `fecha_envio` distinta entre ellas.
  - Puntajes de las calificadas con **empates a propósito** (valores con un decimal, rango estrecho) y con **más de un puntaje** en una de cada cinco (uno viejo `vigente = false` y uno nuevo `vigente = true`, de modelos distintos o fechas distintas); las `pendiente` no tienen puntaje.
- Tests: con base real, el cargador deja los conteos esperados, el índice único de puntaje vigente se respeta, correrlo dos veces no duplica y con `enabled=false` (o sin perfil `dev`) no se crea el bean.

Archivos esperados (crear): `DatosPruebaListaPriorizada.java` y su test. (Tocar): `application.properties` y los `.env.example` que corresponda.

## Fuera de alcance (no tocar)

- Crear administradores o cuentas con contraseña conocida.
- Datos de otros CU (documentos, respuestas socioeconómicas, finalistas, becas).
- Migraciones: es código de desarrollo, nunca va en `db/migration`.
- Correr solo en producción o en pruebas de integración por defecto.

## Resultado esperado

Con `app.seed.priorizacion.enabled=true` en perfil `dev`, la base tiene datos de prueba para ver la lista y probar empates, reprocesos y volumen; sin eso, nada cambia.

## Validación

1. `./mvnw test` pasa.
2. Con el perfil `dev` y la propiedad en `true`, la aplicación arranca, carga los datos una sola vez y un segundo arranque no duplica nada.
3. Con `app.seed.priorizacion.postulaciones=20000` la carga termina en un tiempo razonable (se anota cuánto en el PR).
