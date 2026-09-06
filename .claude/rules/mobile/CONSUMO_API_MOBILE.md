---
globs: projects/mobile/**/*
---

# Consumo de API — Mobile

## Cliente `dio` centralizado (ya implementado)

`lib/core/network/api_client.dart` — clase `ApiClient` con un getter
estático `instance` que arma un único `Dio` (lazy, se construye una sola
vez), con `baseUrl` leído de `flutter_dotenv` (`API_BASE_URL`, ver
`.env.example` de este proyecto — mismo mecanismo que `VITE_API_BASE_URL`
del lado web).

Un `Interceptor` desempaqueta el sobre estándar del backend
(`{status, data, message, error, timestamp, meta?}`, mismo contrato que
[RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)):

- Si `status == "success"`, el interceptor deja pasar `data` (y `meta`
  cuando exista) como el resultado real de la llamada — los `services` no
  desestructuran el sobre a mano en cada método.
- Si `status == "failed"`, el interceptor rechaza con un `DioException`
  cuyo `error` es una `ApiException(code, message)` tipada (clase exportada
  desde el mismo archivo) en vez de dejar pasar una respuesta 2xx con
  `status: "failed"` como si fuera éxito. Un `service` que necesita el
  código de negocio lo lee de `dioException.error as ApiException`.

`ENABLE_API_LOGS` (`.env`, `true`/`false`) prende un `LogInterceptor` de
`dio` — mismo criterio que `VITE_ENABLE_API_LOGS` del frontend, apagado por
default.

> Sin probar todavía contra un endpoint de negocio real (el backend no
> tiene ningún `@RestController` de features aún) — mismo estado que
> `apiClient.ts` del lado web, ver
> [TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md).
> Cuando exista el primer endpoint real, conviene re-verificar el
> desempaquetado de `data`/`meta` contra una respuesta con el sobre
> completo.

## Servicios por feature

Los servicios que llaman a la API viven en `services/` dentro de cada
feature, siguiendo el scaffold ya creado (`lib/features/auth/services/`):

```
lib/features/usuarios/
├── services/
│   └── usuarios_service.dart   # usa el ApiClient de core/network/
├── providers/
└── screens/
```

Un `service` recibe el `ApiClient` compartido (no crea su propia instancia
de `Dio`), y solo conoce endpoints de **su** feature.

### Endpoints compartidos entre features

Mismo criterio que en el frontend web (ver
[TANSTACK_QUERY_FRONTEND.md](../frontend/TANSTACK_QUERY_FRONTEND.md)): si un
endpoint se consume desde más de una feature, su lógica va en
`lib/core/network/` (no en `features/{feature}/services/` de ninguna de las
dos, y no duplicado en ambas). Se mueve ahí recién cuando aparece el segundo
consumidor real, no de forma anticipada.
