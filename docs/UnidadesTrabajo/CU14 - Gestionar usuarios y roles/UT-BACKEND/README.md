# Unidades de trabajo — CU14-BE

**Caso de uso:** Backend de la gestión de las cuentas del personal (Administrador DUBSS, Directivo y Personal del Comedor): listado con filtros, alta con contraseña temporal, edición, cambio de rol, desactivación y reactivación, todo anotado en la bitácora y restringido al Administrador DUBSS.
**Fuente CU:** [`CU14 - Gestionar usuarios y roles`](../../../CasosDeUso/CU14%20-%20Gestionar%20usuarios%20y%20roles.md)
**Diagramas de grado:** del PDF de la documentación de grado, la Figura 39 (contexto), la **Figura 79 (análisis: `UsuarioView` → `UsuarioController` → `Usuario` y `Rol`)**, la **Figura 111 (secuencia)** y la Figura 137 (mockup, no usado). En la carpeta `images/` de la documentación de grado son las imágenes del CU14 de cada serie (contexto, análisis, secuencia y mockup), que van a continuación de las del CU01 (`038`, `051`, `121` y `134`); ver [`CASOS_DE_USO.md`](../../../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado).
**Recurso HTTP / contrato:** `GET /api/v1/usuarios`, `POST /api/v1/usuarios`, `PATCH /api/v1/usuarios/{codigo}` y `POST /api/v1/usuarios/{codigo}/cambiar-rol`, `/desactivar` y `/reactivar`; todos exigen el rol `ADMINISTRADOR_DUBSS`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md`, `AUDITORIA_BITACORA_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/src/test/java/com/comedoruagrm/backend/`.

- **Qué sigue del diagrama de secuencia.** Crear: buscar CI y correo (`Usuario`), buscar el rol (`Rol`), y recién si no hay conflicto **generar la contraseña temporal y crear** la cuenta. Desactivar: **validar primero** que no sea la propia cuenta ni el último Administrador activo y recién después `activo = false` e **invalidar las sesiones**. Listar, editar y cambiar rol siguen el mismo patrón (`list`, `update`, `find(rol)`). Reactivar usa `activo = true`.
- **Un módulo y dos clases nuevas.** Todo vive en `features/usuarios/`: `UsuarioController` (`/api/v1/usuarios`, el `UsuarioController` del análisis) y `UsuarioPersonalService`, que se agrega **al lado** de `UsuarioService` (que sigue siendo del registro y las credenciales del CU01) para no mezclar dos responsabilidades en una clase. `UsuarioPersonalService` usa `SesionService` (módulo `auth`) y no toca su repositorio.
- **Solo personal.** Una cuenta con rol `POSTULANTE` no existe para este CU: no aparece en la lista, no se puede editar ni desactivar (`404`) y el rol Postulante no se puede asignar (`400`). «Personal» = rol distinto de `POSTULANTE`.
- **Seguridad en el `service`.** Cada método público de `UsuarioPersonalService` lleva `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")` (regla de [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#roles-y-permisos)). Hoy `@EnableMethodSecurity` está activo pero ningún `AccessDeniedException` se devuelve en el sobre de la API: la `UT-CU14-BE-02` lo resuelve (`403` `ERR_AUTH_07`).
- **El administrador que actúa sale del token.** El controller recibe `@CurrentUserId UUID` y lo pasa al service; el service lo resuelve a su `Usuario` para las protecciones (propia cuenta) y para la bitácora. Nunca viene del body.
- **Bitácora con `@Auditar`.** Cada operación que escribe se anota con la anotación de [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md). El **actor** de la fila es el administrador (no la cuenta afectada) y `entidad_id` es la cuenta afectada. Como el método tiene que devolver un `Auditable` y el controller solo conoce DTOs, se agrega en `common/auditoria/` un `ResultadoAuditado<T>` (lleva el DTO más ids y detalle, sin entidades JPA). Acciones nuevas en `BitacoraAcciones`: `ALTA_PERSONAL`, `EDICION_USUARIO`, `CAMBIO_ROL`, `DESACTIVACION_USUARIO` y `REACTIVACION_USUARIO`; `CAMBIO_ROL` guarda `rolAnterior` y `rolNuevo` en `detalle`. **Nunca** una contraseña en `detalle`.
- **Contraseña temporal.** La genera el sistema con `SecureRandom` (12 caracteres, sin los que se confunden: `0O1lI`), se guarda **solo su hash BCrypt** y viaja en la respuesta del alta **una única vez**, con `Cache-Control: no-store`. No se vuelve a poder consultar. Decisión del CU (ver ahí): no se fuerza el cambio en el primer acceso (por definir con la DUBSS; haría falta una columna nueva en `usuario`, que habría que agregar primero al diagrama de base de datos, y el CU15).
- **Qué se edita.** `nombres`, `apellidos` y `correo` (con `PATCH`: solo lo que llega). El `ci` y el rol no van en la edición: el rol tiene su propia acción y el CI no se modifica (RF-10).
- **Protecciones (A5).** En `desactivar` y en `cambiar-rol`: primero que no sea la propia cuenta (`403` `ERR_US_08`), después que no deje al sistema sin Administrador activo (`403` `ERR_US_09`). El conteo de administradores activos se hace **bloqueando esas filas** (`PESSIMISTIC_WRITE`) dentro de la transacción, para que dos desactivaciones simultáneas no dejen cero.
- **Cerrar sesiones.** Desactivar y cambiar el rol revocan **todos** los refresh tokens de la cuenta (`SesionService.revocarTodas`). Es distinto del logout del CU01, que cierra solo el dispositivo actual.
- **Listado.** `page`, `pageSize`, `search` (nombres, apellidos y CI), `filter[rol]`, `filter[activo]` y `sort` (`apellidos`, `ci`, `correo`, `rol`, `creadoEn`; por defecto `-creadoEn`). Un `filter[rol]=POSTULANTE`, un valor de `filter` no soportado o un `sort` no soportado dan `400` (no se ignoran).
- **Un código `ERR_US_*` por cada rechazo nuevo**, documentado en `docs/errors/` (`ERR_US_05` quedó retirado y no se reasigna).

## Contrato de datos / recurso HTTP

Todos devuelven el sobre de [`RESPONSES_BACKEND.md`](../../../../.claude/rules/backend/RESPONSES_BACKEND.md). `UsuarioPersonalResponse` = `codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo`.

| Método | Ruta | Cuerpo | Respuesta | Errores |
|--------|------|--------|-----------|---------|
| `GET` | `/api/v1/usuarios` | — (query: `page`, `pageSize`, `search`, `filter[rol]`, `filter[activo]`, `sort`) | `200` array de `UsuarioPersonalResponse` + `meta` | `400` `ERR_US_10`/`ERR_US_11`; `403` `ERR_AUTH_07` |
| `POST` | `/api/v1/usuarios` | `nombres`, `apellidos`, `ci`, `correo`, `rol` | `201` `UsuarioCreadoResponse` (= `UsuarioPersonalResponse` + `contrasenaTemporal`) | `400` `ERR_SYS_01`/`ERR_US_10`; `409` `ERR_US_01`/`ERR_US_02` |
| `PATCH` | `/api/v1/usuarios/{codigo}` | `nombres`, `apellidos`, `correo` (todos opcionales) | `200` `UsuarioPersonalResponse` | `400` `ERR_SYS_01`; `404` `ERR_US_06`; `409` `ERR_US_02` |
| `POST` | `/api/v1/usuarios/{codigo}/cambiar-rol` | `rol` | `200` `UsuarioPersonalResponse` | `400` `ERR_US_10`; `403` `ERR_US_08`/`ERR_US_09`; `404` `ERR_US_06`; `409` `ERR_US_12` |
| `POST` | `/api/v1/usuarios/{codigo}/desactivar` | — | `200` `UsuarioPersonalResponse` | `403` `ERR_US_08`/`ERR_US_09`; `404` `ERR_US_06`; `409` `ERR_US_07` |
| `POST` | `/api/v1/usuarios/{codigo}/reactivar` | — | `200` `UsuarioPersonalResponse` | `404` `ERR_US_06`; `409` `ERR_US_07` |

Cualquier endpoint sin sesión responde `401` (`ERR_AUTH_01`) y sin el rol, `403` (`ERR_AUTH_07`).

| Código | HTTP | Excepción | Significado |
|--------|------|-----------|-------------|
| `ERR_US_06` | 404 | `NotFoundException` | La cuenta no existe, está eliminada o es de un Postulante |
| `ERR_US_07` | 409 | `ConflictException` | La cuenta ya está en el estado pedido (desactivar una desactivada o reactivar una activa) |
| `ERR_US_08` | 403 | `ForbiddenException` | No se puede desactivar ni cambiar el rol de la propia cuenta |
| `ERR_US_09` | 403 | `ForbiddenException` | La operación dejaría al sistema sin Administrador activo |
| `ERR_US_10` | 400 | `ValidationException` | Rol no válido para el personal (inexistente o Postulante) |
| `ERR_US_11` | 400 | `ValidationException` | `sort` o `filter` no soportado en el listado |
| `ERR_US_12` | 409 | `ConflictException` | La cuenta ya tiene ese rol |
| `ERR_AUTH_07` | 403 | — (lo escribe el handler) | El usuario autenticado no tiene el rol necesario |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU14-BE-01](./UT-CU14-BE-01.md) | Códigos de error `ERR_US_06` a `ERR_US_12` y su catálogo | — |
| [UT-CU14-BE-02](./UT-CU14-BE-02.md) | `403` en el sobre por falta de rol (`ERR_AUTH_07`) | — |
| [UT-CU14-BE-03](./UT-CU14-BE-03.md) | Consultas del repositorio de usuarios del personal | — |
| [UT-CU14-BE-04](./UT-CU14-BE-04.md) | Revocar todas las sesiones de una cuenta | — |
| [UT-CU14-BE-05](./UT-CU14-BE-05.md) | Generador de contraseña temporal | — |
| [UT-CU14-BE-06](./UT-CU14-BE-06.md) | DTOs de la gestión de cuentas | — |
| [UT-CU14-BE-07](./UT-CU14-BE-07.md) | Listar las cuentas del personal | 01, 03, 06 |
| [UT-CU14-BE-08](./UT-CU14-BE-08.md) | Crear una cuenta del personal | 01, 05, 06 |
| [UT-CU14-BE-09](./UT-CU14-BE-09.md) | Editar los datos de una cuenta | 01, 06, 08 |
| [UT-CU14-BE-10](./UT-CU14-BE-10.md) | Cambiar el rol de una cuenta | 01, 03, 04, 06, 09 |
| [UT-CU14-BE-11](./UT-CU14-BE-11.md) | Desactivar y reactivar una cuenta | 01, 03, 04, 06, 10 |
| [UT-CU14-BE-12](./UT-CU14-BE-12.md) | `UsuarioController` (`/api/v1/usuarios`) | 02, 07, 08, 09, 10, 11 |
| [UT-CU14-BE-13](./UT-CU14-BE-13.md) | Pruebas de integración con base real | 12 |

**Cadena:** `01, 03, 06 → 07`; `01, 05, 06 → 08 → 09 → 10 → 11`; `03, 04 → 10, 11`; `02, 07, 08, 09, 10, 11 → 12 → 13`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (consultar la lista) | Lista paginada solo del personal, con filtros y búsqueda | 03, 06, 07, 12 |
| Flujo B (crear) | Validación, contraseña temporal, alta y bitácora | 05, 06, 08, 12 |
| Flujo C (editar) | Nombres, apellidos y correo | 06, 09, 12 |
| Flujo D (cambiar rol) | Rol nuevo, sesiones cerradas y bitácora con rol anterior y nuevo | 04, 10, 12 |
| Flujos E y F (desactivar y reactivar) | `activo`, sesiones y bitácora | 04, 11, 12 |
| A1, A2 | CI o correo duplicado (`409`); datos inválidos o rol inválido (`400`) | 01, 06, 08, 09 |
| A3, A4, A6 | Cambio de rol, desactivación con sesiones invalidadas, reactivación | 10, 11 |
| A5 | Propia cuenta o último Administrador activo | 03, 10, 11 |
| A7 | Sin sesión (`401`) o sin el rol Administrador (`403`) | 02, 07, 08, 09, 10, 11, 12 |
| A8, A9 | Cuenta inexistente o de Postulante (`404`); estado repetido (`409`) | 01, 09, 10, 11 |
| CA01–CA05 | Alta, duplicados, datos y rol inválidos | 01, 05, 06, 08 |
| CA06, CA07 | Lista solo del personal y sin Postulantes | 03, 07, 13 |
| CA08–CA10 | Edición, correo duplicado, CI inmodificable | 06, 09 |
| CA11, CA16 | Cambio de rol, sesiones cerradas, y no cambiar el propio rol | 04, 10 |
| CA12–CA15, CA17, CA18 | Desactivar, reactivar, protecciones y estado repetido | 03, 04, 11 |
| CA19, CA20 | `403` sin el rol y `401` sin sesión | 02, 12 |
| CA21 | Bitácora de cada operación y sin contraseñas | 08, 09, 10, 11, 13 |
| CA22, CA23 | Reintento del alta y errores de conexión | 08, 12 |

## Follow-ups (no se implementan en estas UT)

- Forzar el cambio de la contraseña temporal en el primer acceso y definir su vigencia (por definir con la DUBSS; necesita una columna nueva en `usuario` y el CU15).
- Entregar el acceso por un enlace de activación enviado por correo (CU15).

## Qué NO incluyen estas UT

- Las pantallas de la gestión (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md)). Este CU es solo web: no hay UT de mobile.
- La gestión de Postulantes: se registran solos (CU01) y no se administran desde acá.
- Cambiar la contraseña desde el perfil y recuperarla (CU15), y consultar la bitácora desde el panel (CU25).
- Eliminar una cuenta (nunca se elimina: se desactiva).
