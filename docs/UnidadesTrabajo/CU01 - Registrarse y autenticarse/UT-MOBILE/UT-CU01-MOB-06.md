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

Los tres flujos del CU combinan Firebase y el backend; esa lógica y la traducción de errores deben estar en un solo lugar, probada.

## Objetivo

Existe `authProvider` (notifier) con `registrar`, `iniciarSesion` y `cerrarSesion`, que implementan los flujos A, B y C y devuelven errores ya traducidos.

## Alcance

- `lib/features/auth/providers/auth_provider.dart`: `AsyncNotifier` con los tres métodos según las decisiones del README. Evita el doble envío mientras hay una acción en curso (A7). `cerrarSesion` solo cierra este dispositivo y no muestra error si la sesión ya había expirado (A10).
- `lib/features/auth/auth_error_mapper.dart`: función pura que traduce errores de Firebase (`FirebaseAuthException.code`) y de la API (`ApiException.code`) a un mensaje y, si aplica, al campo en conflicto (`ci`, `correo`, `registroUniversitario`).
- Tests con Firebase y `AuthService` simulados: cada fila de la tabla de errores del README; registro feliz; correo existente en Firebase con la misma contraseña → continúa; con otra → "correo ya registrado"; un `409` marca el campo y no cierra sesión; no se imprime ninguna contraseña ni token.

Archivos esperados (crear): `auth_provider.dart`, `auth_error_mapper.dart` y los tests.

## Fuera de alcance (no tocar)

- Las pantallas (`UT-CU01-MOB-07` y `08`).
- Reglas de contraseña o bloqueo (de Firebase).

## Resultado esperado

La lógica del CU01 en la app está completa y probada; las pantallas solo la invocan.

## Validación

1. `flutter test` pasa con todos los escenarios.
2. `flutter analyze` sin errores.
3. Revisar que ningún `print`/`debugPrint` muestra contraseñas ni tokens.
