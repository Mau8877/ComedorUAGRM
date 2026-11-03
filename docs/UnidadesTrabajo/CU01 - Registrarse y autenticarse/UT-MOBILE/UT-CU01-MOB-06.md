# UT-CU01-MOB-06 — Acciones de registro, inicio y cierre de sesión con mapeo de errores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-06` |
| **Nombre** | Acciones de registro, inicio y cierre de sesión con mapeo de errores |
| **Caso de uso** | CU01-MOB |
| **Depende de** | `UT-CU01-MOB-01`, `UT-CU01-MOB-04`, `UT-CU01-MOB-05` |

## Antes de implementar

- [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md)
- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Los tres flujos del CU combinan la API y el almacén de tokens; esa lógica y la traducción de errores deben estar en un solo lugar, probada.

## Objetivo

Existe `authProvider` (un `Notifier<bool>`, `true` mientras hay una acción en curso) con `registrar`, `iniciarSesion` y `cerrarSesion`, que implementan los flujos A, B y C y devuelven errores ya traducidos.

## Alcance

- `lib/features/auth/providers/auth_provider.dart`: `Notifier<bool>` (síncrono y no `AsyncNotifier`, para que arranque en `false` y no en `loading`) con `registrar(RegistroRequest)` e `iniciarSesion(correo, contrasena)`, que devuelven `null` si salió bien (la sesión queda abierta con `authSesionProvider.abrir(...)` y el router lleva al módulo) o un `AuthError` ya traducido, y `cerrarSesion()`. Una segunda acción mientras hay una en curso se ignora (A7). `cerrarSesion` hace `POST /auth/logout` con el refresh token (ignora cualquier error de red o `401`: A10), borra la sesión y solo cierra este dispositivo.
- `lib/features/auth/auth_error_mapper.dart`: función pura que traduce un `ApiException.code` a un mensaje y, si aplica, al campo en conflicto (`ci`, `correo`, `registroUniversitario`): `ERR_AUTH_04` → credenciales incorrectas (siempre el mismo), `ERR_AUTH_05` → cuenta bloqueada (con el mensaje del backend, que dice los minutos que faltan), `ERR_AUTH_03` → cuenta desactivada, `ERR_US_01/02/03` → campo en conflicto, error de red → mensaje de conexión con reintento.
- Tests con `AuthService` simulado: cada fila de la tabla de errores del README; registro feliz guarda los tokens; un `409` marca el campo; cerrar sesión limpia aunque el backend falle; no se imprime ninguna contraseña ni token.

Archivos esperados (crear): `auth_provider.dart`, `auth_error_mapper.dart` y los tests.

## Fuera de alcance (no tocar)

- Las pantallas (`UT-CU01-MOB-07` y `08`).
- Reglas de contraseña o bloqueo: las aplica el backend; acá solo se muestran sus mensajes.

## Resultado esperado

La lógica del CU01 en la app está completa y probada; las pantallas solo la invocan.

## Validación

1. `flutter test` pasa con todos los escenarios.
2. `flutter analyze` sin errores.
3. Revisar que ningún `print`/`debugPrint` muestra contraseñas ni tokens.
