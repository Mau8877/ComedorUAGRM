# UT-CU14-BE-13 — Pruebas de integración con base real

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-13` |
| **Nombre** | Pruebas de integración con base real |
| **Caso de uso** | CU14-BE |
| **Depende de** | `UT-CU14-BE-12` |

## Antes de implementar

- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md) (helper `PostgresTestDatabase`, qué pasa si no hay Docker)
- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md#tests)

## Solicitud de negocio

Hay que comprobar con la base de datos real que la gestión de cuentas cumple el CU entero: permisos, protecciones, sesiones y bitácora, que los tests con repositorios mockeados no pueden demostrar.

## Objetivo

Un test de integración recorre los flujos A a F del CU14 contra PostgreSQL y verifica los criterios de aceptación observables.

## Alcance

- `T/features/usuarios/UsuarioPersonalIntegrationTest.java` (`@SpringBootTest` + `MockMvc` + `PostgresTestDatabase`, como `AuthIntegrationTest`): limpia `bitacora`, `sesion`, `postulante` y `usuario` antes de cada test y prepara un Administrador, un Directivo y un Postulante con sesión.
- **Permisos:** cada endpoint responde `403` `ERR_AUTH_07` con un Directivo y con un Postulante, y `401` sin sesión.
- **Alta:** crea cuentas con los tres roles; el alta con `POSTULANTE` da `400` `ERR_US_10`; CI y correo repetidos dan `409` `ERR_US_01` y `ERR_US_02`; la persona **inicia sesión con la contraseña temporal** de la respuesta; la contraseña temporal no aparece en `bitacora.detalle` ni se guarda en texto plano; la respuesta del alta lleva `Cache-Control: no-store`.
- **Lista:** no trae Postulantes ni cuentas eliminadas; filtra por rol y por estado; busca por CI, nombre y apellido; pagina; `filter[rol]=POSTULANTE` y un `sort` inválido dan `400`.
- **Edición:** cambia los datos; no cambia el CI; un correo ajeno da `409`; una cuenta de Postulante da `404` `ERR_US_06`.
- **Cambio de rol:** el rol cambia, los refresh tokens de esa cuenta dejan de servir, y la bitácora tiene una fila `CAMBIO_ROL` con `rolAnterior` y `rolNuevo`, el **administrador como actor** y la cuenta afectada como entidad; no se puede cambiar el propio rol ni quitarle el rol al último Administrador activo.
- **Desactivar y reactivar:** al desactivar, el login de esa cuenta da `403` `ERR_AUTH_03` y sus sesiones dejan de renovarse; la fila de `usuario` sigue existiendo con `eliminado_en` nulo; no se puede desactivar la propia cuenta ni al último Administrador activo; reactivar recupera el acceso con el mismo rol; repetir cualquiera de las dos da `409` `ERR_US_07`.
- **Bitácora:** cada operación que escribe deja exactamente una fila con la acción esperada y la IP del request; los rechazos no dejan ninguna.
- **Concurrencia:** dos desactivaciones simultáneas de los dos únicos Administradores activos dejan al menos uno activo.

Archivos esperados (crear): el test de integración.

## Fuera de alcance (no tocar)

- Probar el aspecto de bitácora en general (`AuditoriaAspectTest`) y la lógica de cada `service` (tests unitarios de las UT 07 a 11).
- Pruebas del frontend.

## Resultado esperado

El CU14 del backend queda verificado de punta a punta contra una base real.

## Validación

1. `./mvnw test` pasa. Con Docker corre solo; sin él se puede apuntar a una base vacía y descartable con `-Dtest.postgres.url=...` (nunca la de desarrollo).
2. Sin Docker ni base disponible, el test queda `SKIPPED` y no rompe el build.
3. `./mvnw checkstyle:check` sin errores.
