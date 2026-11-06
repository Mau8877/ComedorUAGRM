# UT-CUX01-BE-04 — Canal de push: FCM y modo log

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-BE-04` |
| **Nombre** | Canal de push: FCM y modo log |
| **Caso de uso** | CU-EXTRA-01-BE |
| **Depende de** | UT-CUX01-BE-03 |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTO como `record`, sin interfaz `IXxx` salvo más de una implementación: aquí hay dos reales)
- [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca tokens)
- [`TESTING_BACKEND.md`](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El resto del sistema debe poder mandar un push sin saber si hay Firebase o no, y sin que los tests o el CI llamen a Firebase.

## Objetivo

Una interfaz `CanalPush` con dos implementaciones intercambiables, que informa qué tokens ya no sirven y cuáles fallaron de forma pasajera.

## Alcance

- `B/features/notificaciones/service/`:
  - `MensajePush` (record: `titulo`, `cuerpo`, `datos` `Map<String,String>`).
  - `ResultadoPush` (record: `enviados`, `tokensInvalidos`, `tokensPasajeros`).
  - `CanalPush` (interfaz: `ResultadoPush enviar(List<String> tokens, MensajePush mensaje)`).
  - `CanalPushFcm`: usa `FirebaseMessaging.sendEach` en lotes de hasta 500 tokens; arma `notification` (título y cuerpo), `data` y el canal Android (`app.notificaciones.android-channel-id`). Errores `UNREGISTERED` e `INVALID_ARGUMENT` → `tokensInvalidos`; `UNAVAILABLE`, `INTERNAL`, `QUOTA_EXCEEDED` y fallos de red → `tokensPasajeros`; cualquier otro error se cuenta como no entregado y se registra.
  - `CanalPushLog`: solo escribe en el log cuántos teléfonos se habrían avisado y el título; no escribe tokens.
- Selección del bean: `CanalPushFcm` si existe `FirebaseMessaging`, `CanalPushLog` en caso contrario (clase `@Configuration` con `@ConditionalOnBean` / `@ConditionalOnMissingBean`).
- Logging con `kv("module", "notificaciones")`; **nunca** un token completo ni el cuerpo del mensaje.
- Tests: `CanalPushFcmTest` con `FirebaseMessaging` simulado (lote de 600 tokens se parte en 2, clasificación de errores), `CanalPushLogTest`.

Archivos esperados (crear): los 5 tipos de arriba, la configuración de selección y sus tests.

## Fuera de alcance (no tocar)

- Elegir a qué teléfonos se envía, eliminar tokens inválidos o reintentar (`UT-CUX01-BE-10`).
- Credenciales (`UT-CUX01-BE-03`).

## Resultado esperado

Inyectar `CanalPush` da FCM con credenciales y modo log sin ellas, y el resultado dice qué hacer con cada token.

## Validación

1. `./mvnw test` pasa sin tocar Firebase.
2. Los logs de los tests no contienen ningún token.
3. `./mvnw checkstyle:check` sin errores.
