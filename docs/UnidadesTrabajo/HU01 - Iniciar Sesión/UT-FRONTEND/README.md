# Unidades de trabajo — HU01-FE

**Historia:** Pantalla de inicio de sesión — formulario de usuario-o-correo/contraseña, consumo de `POST /api/v1/auth/login`, sesión en memoria, guard de rutas privadas.
**Fuente HU:** [`../../HistoriasDeUsuario/HU01 - Iniciar Sesión.md`](../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)
**Contrato HTTP:** [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md)
**Bounded context UI:** `features/auth/`

## Antes de implementar cualquier UT

Leer [`.claude/rules/frontend/`](../../../../.claude/rules/frontend/) y
[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).
Prioridad según la unidad:
[TANSTACK_QUERY_FRONTEND.md](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md),
[ESTADO_GLOBAL_FRONTEND.md](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md),
[FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md),
[RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md),
[TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md),
[TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las
rules.

## Decisión de diseño: refresh token invisible para el frontend

Confirmado con la persona a cargo del proyecto: el refresh token viaja
como cookie `HttpOnly` seteada por el backend (ver
[UT-BACKEND/README.md](../UT-BACKEND/README.md#decisión-de-diseño-transporte-del-refresh-token)) —
el frontend **nunca** lo lee, guarda, ni envía a mano. Solo necesita:

- `apiClient` con `withCredentials: true` para que el browser adjunte/
  acepte la cookie en requests cross-origin (dev: `localhost:5173` →
  `localhost:8080`).
- Guardar el **access token** (sí, ese sí lo maneja el frontend) en
  memoria tras un login exitoso.

## Decisión de diseño: la sesión vive en un store de Zustand, no en TanStack Query

[ESTADO_GLOBAL_FRONTEND.md](../../../../.claude/rules/frontend/ESTADO_GLOBAL_FRONTEND.md)
dice que TanStack Query es el único responsable de cachear "datos cuya
fuente de verdad es el servidor" — pero el access token/usuario logueado
no es un recurso que se liste, pagine o refetchee: es **estado de sesión
del cliente**, necesario de forma **síncrona** en dos lugares que no son
componentes React (el interceptor de `apiClient` para el header
`Authorization`, y el `beforeLoad` del guard de rutas) — un hook de
TanStack Query no sirve ahí. Por eso vive en un store nuevo,
**`store/useAuthStore.ts`** (global, no de una feature puntual — mismo
criterio que `useUiStore.ts`, consumido desde fuera de `features/auth/`).
El login (`useLoginMutation`, sí es una mutación de TanStack Query) es lo
que **escribe** en este store al tener éxito.

**No** se usa el middleware `persist` de Zustand para este store (a
diferencia de `useUiStore`, que sí persiste el tema): recargar la página
(F5) pierde el access token en memoria a propósito. Recuperar la sesión
tras un F5 usando la cookie de refresh (`POST /api/v1/auth/refresh` al
montar la app) es responsabilidad de
[HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md), no de
esta HU.

## Contrato de datos (alineado a `UT-BACKEND`)

| Campo | Tipo | Notas |
|-------|------|-------|
| Request `POST /api/v1/auth/login` | `{ identificador: string, password: string }` | `identificador` acepta `username` o `correo` |
| Response éxito (`data`) | `{ accessToken: string, usuario: { codigo: string, username: string, correo: string, roles: string[] } }` | `codigo` es UUID, nunca un id numérico; `roles` puede tener más de un elemento |
| Errores | `ApiError { code, message }` (ya desempaquetado por `apiClient`) | `code` uno de `ERR_AUTH_01`/`ERR_AUTH_02`/`ERR_AUTH_03`/`ERR_SYS_01`/`ERR_SYS_00` |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU01-FE-01](./UT-HU01-FE-01.md) | Contratos TS + `useLoginMutation` + `withCredentials` en `apiClient` | — |
| [UT-HU01-FE-02](./UT-HU01-FE-02.md) | `useAuthStore` (sesión en memoria) | — |
| [UT-HU01-FE-03](./UT-HU01-FE-03.md) | Schema Zod + `LoginForm` (TanStack Form) | — |
| [UT-HU01-FE-04](./UT-HU01-FE-04.md) | Screen `Login`: orquesta form + mutation + store + mapeo de errores | UT-HU01-FE-01, 02, 03 |
| [UT-HU01-FE-05](./UT-HU01-FE-05.md) | Ruta `/login` + guard `_authenticated` | UT-HU01-FE-02, 04 |
| [UT-HU01-FE-06](./UT-HU01-FE-06.md) | Pruebas unitarias (`.test.ts`/`.test.tsx`) | UT-HU01-FE-01 … 05 |

**Cadena:** `01, 02, 03 → 04 → 05 → 06` (01-03 son independientes entre sí).

Backend con contrato ya fijado en [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md);
mientras el backend no esté implementado, se puede avanzar mockeando
`apiClient` en los tests (UT-HU01-FE-06) — este proyecto no tiene
convención de mocks HTTP a nivel de red (tipo MSW) todavía, así que no se
introduce acá una capa de mocks nueva sin que el proyecto ya la use en
otra feature.

## Criterios de aceptación cubiertos

| CA (HU01) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Login exitoso → guarda sesión, redirige a `/panel` | 01, 02, 04, 05 |
| CA02/CA03 | Credenciales inválidas → mensaje genérico inline | 03, 04, 06 |
| CA04 | Campos vacíos/formato inválido → error inline por campo | 03, 06 |
| CA05 | Cuenta inactiva → mensaje específico | 04, 06 |
| CA06 | Cuenta bloqueada → mensaje con tiempo restante | 04, 06 |
| CA09 | Error inesperado (`ERR_SYS_00`) → mensaje genérico, no técnico | 04 |
| CA10 | Consumo del sobre estándar ya resuelto por `apiClient` | 01 |

## Follow-ups (no se implementan en estas UT)

- Renovación silenciosa del access token al montar la app / al expirar
  (`POST /api/v1/auth/refresh`) — [HU-02](../../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md).
- Botón/flujo de "Cerrar sesión" — [HU-03](../../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md).
- Interceptor de `apiClient` que adjunte `Authorization: Bearer` desde
  `useAuthStore` en cada request — hoy ningún endpoint de negocio real lo
  exige todavía; se agrega cuando exista el primer endpoint privado a
  consumir.
- Redirección por rol a un layout específico (`AdminLayout`/`EstudianteLayout`,
  ver [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol)) —
  esta HU redirige siempre a `/panel` (el placeholder ya existente), no
  bifurca todavía por `rol`.

## Qué NO incluyen estas UT

- `/auth/refresh`, `/auth/logout` del lado del cliente (HU-02/HU-03)
- Interceptor de `Authorization` header
- Layouts por rol (`AdminLayout`/`EstudianteLayout`) — fuera de alcance de
  esta HU puntual
- Código backend (ver [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md))
