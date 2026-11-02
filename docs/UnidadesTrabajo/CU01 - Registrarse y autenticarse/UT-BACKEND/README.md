# Unidades de trabajo — CU01-BE

**Caso de uso:** Backend de registro de un Postulante, verificación del usuario que inicia sesión (la sesión la maneja Firebase), registro del acceso en la bitácora y catálogo de carreras para el formulario.
**Fuente CU:** [`CU01 - Registrarse y autenticarse`](../../../CasosDeUso/CU01%20-%20Registrarse%20y%20autenticarse.md)
**Diagramas de grado:** `images/038_CU01_Registrarse_Autenticarse.png` (contexto), `images/051_Analisis_CU01_Registrarse_Autenticarse.jpg` (análisis), `images/121_Secuencia_CU01_Registrarse_Autenticarse.jpg` (secuencia) y `images/134_Mockup_CU01_Registrarse_autenticarse.png` (mockup).
**Recurso HTTP / contrato:** `GET /api/v1/carreras` (pública), `POST /api/v1/auth/registro`, `POST /api/v1/auth/sesion` y `GET /api/v1/auth/usuario-actual`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_BACKEND.md`, `CONVENCIONES_JAVA_BACKEND.md`, `PERSISTENCIA_BD_BACKEND.md`, `SEGURIDAD_AUTH_BACKEND.md`, `EXCEPCIONES_BACKEND.md`, `GUIA_ERRORES_BACKEND.md`, `RESPONSES_BACKEND.md`, `ENDPOINTS_BACKEND.md`, `LOGGING_BACKEND.md` y `TESTING_BACKEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

En estas fichas, `B/` = `projects/backend/src/main/java/com/comedoruagrm/backend/` y `T/` = `projects/backend/src/test/java/com/comedoruagrm/backend/`.

- **Diagramas de grado.** El análisis (051) y la secuencia (121) ya reflejan el diseño con Firebase Auth y coinciden con estas fichas. El mockup (134) sigue mostrando el diseño anterior (contraseña de mínimo 8 con letras y números, sin carrera, registro universitario ni teléfono, con casilla de términos y login/registro en pestañas): no se corrige y **ganan el CU y estas UT**.
- **Firebase hace el login; el backend solo verifica.** El backend nunca ve la contraseña ni los intentos fallidos: inicio de sesión, contraseña mínima de 6 caracteres, bloqueo por intentos y cierre de sesión son de Firebase y del cliente (ver [`UT-FRONTEND`](../UT-FRONTEND/README.md) y [`UT-MOBILE`](../UT-MOBILE/README.md)). Por eso el backend **no** tiene endpoint de login ni de logout (CA05, CA08, CA10, CA11 y CA17 a CA19 no tienen UT de backend).
- **Verificación del token con el resource server de Spring Security** (`spring-boot-starter-oauth2-resource-server`) contra las claves públicas de Firebase, validando firma, `iss` (`https://securetoken.google.com/{projectId}`), `aud` (el `projectId`) y expiración. Elegido sobre el Admin SDK de Firebase porque para verificar no hace falta credencial de servicio. El Admin SDK se agregará cuando el CU14 deba crear cuentas en Firebase.
- **Dos endpoints tras iniciar sesión.** `POST /api/v1/auth/sesion` se llama **una sola vez** justo después del login en Firebase: devuelve los datos del usuario y registra el acceso en la bitácora. `GET /api/v1/auth/usuario-actual` sirve para recuperar la sesión al recargar la página o reabrir la app **sin** volver a registrar un acceso.
- **Registro idempotente por `firebase_uid`.** El cliente crea la cuenta en Firebase y luego llama a `POST /api/v1/auth/registro` con el ID token. El `firebase_uid` y el correo salen **del token verificado**, nunca del body. Si ya existe un `usuario` con ese `firebase_uid`, el registro responde `200` con el usuario existente y no crea nada (A7, CA12).
- **Dependencia `security/` → `features/`.** `security/` no puede importar `features/` (regla de dependencia). El filtro que resuelve el usuario usa una interfaz que define `security/` (`UsuarioAutenticableProvider`) y que implementa `features/usuarios/`.
- **`GET /api/v1/carreras` es público.** El formulario de registro necesita la lista de carreras **antes** de que exista una cuenta ni token. Es la única excepción a "ningún endpoint de negocio es público" y se documenta en `SEGURIDAD_AUTH_BACKEND.md` (UT-CU01-BE-09). Es solo lectura de un catálogo.
- **Bitácora.** La tabla `bitacora` es del paquete P6 (CU25 la consulta), pero CU01 escribe en ella (CA20), así que se crea y se escribe acá. Solo registra eventos que el backend ve: `REGISTRO_USUARIO` e `INICIO_SESION`. Los intentos fallidos los ve Firebase, no el backend, y por eso no quedan en la bitácora.
- **Módulos.** `features/usuarios/` (P1: `usuario`, `rol`, `postulante`, `carrera`, y el catálogo de carreras), `features/auth/` (controller y service de registro/sesión) y `features/bitacora/`. `auth` usa el `service` de `usuarios` y el de `bitacora`, nunca sus repositorios.
- **Códigos de error nuevos:** `ERR_AUTH_01` a `ERR_AUTH_03` (`security/`) y `ERR_US_01` a `ERR_US_04` (`usuarios`), cada uno documentado en `docs/errors/` por la UT que lo lanza.
- **Prerrequisito manual (fuera del repo):** crear el proyecto de Firebase, habilitar el proveedor Correo/Contraseña y copiar su `projectId` a `FIREBASE_PROJECT_ID`. Todavía no existe.
- **Longitudes de columna** que el diagrama de grado deja sin tamaño (`rol.nombre`, `carrera.nombre`, `carrera.facultad`) se fijan en `varchar(50)`, `varchar(150)` y `varchar(150)`.

## Contrato de datos / recurso HTTP

**Endpoints** (todos bajo `/api/v1`, respuestas en el sobre de `RESPONSES_BACKEND.md`):

| Método | Ruta | Auth | Body | Éxito | Errores |
|--------|------|------|------|-------|---------|
| `GET` | `/carreras` | Pública | — | `200` `data`: array de `{codigo, nombre, facultad}` + `meta` (`?pageSize=200` para el selector) | — |
| `POST` | `/auth/registro` | ID token de Firebase (no exige `usuario` previo) | `RegistroRequest` | `201` con `UsuarioResponse`; `200` con el existente si ya estaba registrado | `400` `ERR_SYS_01`; `404` `ERR_US_04`; `409` `ERR_US_01`/`02`/`03`; `401` `ERR_AUTH_01` |
| `POST` | `/auth/sesion` | ID token + `usuario` activo | — | `200` con `UsuarioResponse` y acceso anotado en la bitácora | `401` `ERR_AUTH_01`/`02`; `403` `ERR_AUTH_03` |
| `GET` | `/auth/usuario-actual` | ID token + `usuario` activo | — | `200` con `UsuarioResponse` | igual que `/auth/sesion` |

**`RegistroRequest`:** `nombres` (≤100), `apellidos` (≤100), `ci` (≤20), `registroUniversitario` (9 dígitos, no empieza por 0), `carreraCodigo` (UUID), `telefono` (≤20); todos obligatorios. Sin `correo`, `rol` ni `firebaseUid`: el correo sale del token y el rol siempre es `POSTULANTE` (CA06). El registro universitario son exactamente 9 dígitos y no empieza por 0 (`^[1-9]\d{8}$`); el formato del CI está **por definir con la DUBSS**: por ahora solo se valida que no esté vacío y su largo máximo.

**`UsuarioResponse`:** `codigo` (UUID), `nombres`, `apellidos`, `correo`, `rol` (nombre del rol).

**Códigos de error nuevos:**

| Código | HTTP | Excepción | Cuándo |
|--------|------|-----------|--------|
| `ERR_AUTH_01` | 401 | — (lo escribe el entry point) | Token ausente, mal firmado o vencido |
| `ERR_AUTH_02` | 401 | — (lo escribe el filtro) | Token válido pero no hay `usuario` con ese `firebase_uid` |
| `ERR_AUTH_03` | 403 | — (lo escribe el filtro) | El `usuario` existe pero está desactivado |
| `ERR_US_01` | 409 | `ConflictException` | El CI ya está registrado |
| `ERR_US_02` | 409 | `ConflictException` | El correo ya está registrado |
| `ERR_US_03` | 409 | `ConflictException` | El registro universitario ya está registrado |
| `ERR_US_04` | 404 | `NotFoundException` | La carrera indicada no existe |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-BE-01](./UT-CU01-BE-01.md) | Migración de tablas de cuentas y acceso | — |
| [UT-CU01-BE-02](./UT-CU01-BE-02.md) | Models y repositorios de los catálogos Rol y Carrera | 01 |
| [UT-CU01-BE-03](./UT-CU01-BE-03.md) | Models y repositorios de Usuario y Postulante | 01, 02 |
| [UT-CU01-BE-04](./UT-CU01-BE-04.md) | Bitácora: Model, repositorio y service para registrar eventos | 01, 03 |
| [UT-CU01-BE-05](./UT-CU01-BE-05.md) | Verificación del ID token de Firebase (resource server) | — |
| [UT-CU01-BE-06](./UT-CU01-BE-06.md) | Respuestas 401 en el sobre de la API y catálogo ERR_AUTH | 05 |
| [UT-CU01-BE-07](./UT-CU01-BE-07.md) | Filtro que resuelve el usuario autenticado y controla cuenta activa | 03, 05, 06 |
| [UT-CU01-BE-08](./UT-CU01-BE-08.md) | Anotación @CurrentUserId y seguridad por método | 07 |
| [UT-CU01-BE-09](./UT-CU01-BE-09.md) | GET /api/v1/carreras (catálogo público de carreras) | 02, 05 |
| [UT-CU01-BE-10](./UT-CU01-BE-10.md) | DTOs de registro y respuesta, y códigos ERR_US | 02 |
| [UT-CU01-BE-11](./UT-CU01-BE-11.md) | Lógica de registro de un Postulante | 03, 04, 07, 10 |
| [UT-CU01-BE-12](./UT-CU01-BE-12.md) | POST /api/v1/auth/registro | 07, 08, 10, 11 |
| [UT-CU01-BE-13](./UT-CU01-BE-13.md) | POST /api/v1/auth/sesion y GET /api/v1/auth/usuario-actual | 04, 08, 11 |
| [UT-CU01-BE-14](./UT-CU01-BE-14.md) | Pruebas de integración con base real del registro y la sesión | 09, 12, 13 |

**Cadena:** `01 → 02 → 03 → 04`; `05 → 06 → 07 → 08`; `02, 05 → 09`; `02 → 10`; `03, 04, 07, 10 → 11`; `07, 08, 10, 11 → 12`; `04, 08, 11 → 13`; `09, 12, 13 → 14`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A, pasos 4 a 6 (registro) | Validación de datos y unicidad, creación con rol Postulante, respuesta | 10, 11, 12 |
| Flujo B, pasos 10 a 12 (inicio de sesión) | Cuenta activa, rol, registro en bitácora, datos del usuario | 04, 07, 13 |
| Flujo B, paso 9 y Flujo C | Credenciales, bloqueo y cierre de sesión: los resuelven Firebase y el cliente (el backend es stateless) | — (UT-FRONTEND / UT-MOBILE) |
| A1 | CI, correo o registro universitario ya registrados → 409 | 11, 12 |
| A2 | Datos del formulario inválidos → 400 | 10, 12 |
| A4 | Cuenta desactivada → 403 | 07, 13 |
| A7 | Registro repetido idempotente | 11 |
| A8 | Token inválido o vencido → 401 | 05, 06, 07 |
| A9 | Error inesperado → 500 con mensaje genérico (ya existe `GlobalExceptionHandler`) | — (existente) |
| CA01, CA06, CA12 | Registro correcto, siempre como Postulante, una sola cuenta | 11, 12 |
| CA02, CA03, CA21 | Conflictos de CI, correo y registro universitario | 03, 11, 12, 14 |
| CA04 | Campos obligatorios y formato | 10, 12 |
| CA07, CA09 | Datos del usuario al iniciar sesión; cuenta desactivada → 403 | 07, 13 |
| CA14 | Mismo comportamiento web y móvil: es la misma API | 12, 13 |
| CA15 | Sesión inválida → 401 | 05, 06, 07 |
| CA20 | Acceso en la bitácora; sin contraseñas ni tokens en logs | 04, 11, 13 |

## Follow-ups (no se implementan en estas UT)

- Mantenimiento posterior del catálogo de carreras (altas, bajas, cambios): se define cuando un caso de uso lo pida.
- Formato del CI — por definir con la DUBSS.
- Consulta de la bitácora desde el panel: CU25.
- Alta de cuentas del personal (incluida su creación en Firebase con el Admin SDK): CU14.
- Protección por rol con `@PreAuthorize` en endpoints de negocio: la primera UT de otro CU que lo necesite. En este CU solo queda habilitada la seguridad por método (UT-CU01-BE-08).

## Qué NO incluyen estas UT

- Login, logout, refresh de token, bloqueo por intentos y política de contraseña: son de Firebase y del cliente.
- Recuperar contraseña y gestionar perfil (CU15), gestionar usuarios y roles (CU14).
- Cualquier endpoint de postulación, convocatoria o beca.
- Frontend y mobile (sus propias UT).
