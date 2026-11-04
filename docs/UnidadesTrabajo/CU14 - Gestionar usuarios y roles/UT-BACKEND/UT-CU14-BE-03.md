# UT-CU14-BE-03 — Consultas del repositorio de usuarios del personal

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-03` |
| **Nombre** | Consultas del repositorio de usuarios del personal |
| **Caso de uso** | CU14-BE |
| **Depende de** | — (edita el `UsuarioRepository` creado en `UT-CU01-BE-03`) |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)
- [`ENDPOINTS_BACKEND.md`](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La lista de cuentas del personal tiene que filtrarse por rol y estado y buscarse por CI o nombre, sin traer nunca a los Postulantes; y hay que poder contar los Administradores activos sin que dos operaciones simultáneas se pisen.

## Objetivo

`UsuarioRepository` ofrece el listado paginado del personal con filtros y búsqueda, la búsqueda de una cuenta del personal por su `codigo` y el bloqueo de los Administradores activos.

## Alcance

- `UsuarioRepository.buscarPersonal(String busqueda, String rol, Boolean activo, Pageable pageable)`: `Page<Usuario>` con `@Query` JPQL. Excluye siempre `rol.nombre = 'POSTULANTE'`; `busqueda` (opcional, sin distinguir mayúsculas) compara por `nombres`, `apellidos` y `ci`; `rol` y `activo` son opcionales. Las filas eliminadas ya quedan fuera por el `@SQLRestriction` de `BaseModel`.
- `UsuarioRepository.findPersonalByCodigo(UUID codigo)`: `Optional<Usuario>` que devuelve la cuenta solo si no es de un Postulante.
- `UsuarioRepository.bloquearAdministradoresActivos()`: devuelve los `id` de las cuentas con rol `ADMINISTRADOR_DUBSS` y `activo = true`, con `@Lock(LockModeType.PESSIMISTIC_WRITE)`; quien la llama cuenta el tamaño de la lista dentro de su transacción.
- Si hace falta un índice para el listado (por ejemplo sobre `usuario (rol_id, activo)`), va en una migración nueva con el formato `V{yyyyMMddHHmmss}__...sql` y se revisa antes contra el diseño lógico de la base de datos.
- Tests (`@DataJpaTest` con la base real del helper `PostgresTestDatabase`): excluye Postulantes y cuentas eliminadas; filtra por rol y por estado; busca por CI, nombre y apellido; ordena y pagina; `findPersonalByCodigo` no devuelve un Postulante; el bloqueo cuenta solo Administradores activos.

Archivos esperados (crear/tocar): `UsuarioRepository.java`, su test, y una migración solo si el índice hace falta.

## Fuera de alcance (no tocar)

- La validación de `sort` y `filter` y el armado del `Pageable`: es de `UT-CU14-BE-07`.
- Cualquier cambio de columnas de `usuario`.
- Las consultas del login y el registro (CU01).

## Resultado esperado

El `service` puede listar el personal con filtros, encontrar una cuenta del personal por `codigo` y contar Administradores activos con bloqueo.

## Validación

1. `./mvnw test` pasa (con Docker, o con `-Dtest.postgres.url=...` a una base vacía, ver [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)).
2. Un test con un Postulante y un Directivo comprueba que la lista solo trae al Directivo.
3. `./mvnw checkstyle:check` sin errores.
