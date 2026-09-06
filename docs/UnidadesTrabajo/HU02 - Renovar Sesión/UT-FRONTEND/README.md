# Unidades de trabajo — HU02-FE

**Historia:** Renovar la sesión automáticamente — reintentar tras un `401` de token vencido, y restaurar la sesión al recargar la página (F5) usando la cookie `refreshToken` todavía vigente.
**Fuente HU:** [`../../HistoriasDeUsuario/HU02 - Renovar Sesión.md`](../../HistoriasDeUsuario/HU02%20-%20Renovar%20Sesión.md)
**Contrato HTTP:** [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md)
**Requiere que ya exista:** [HU01-FE](../../HU01%20-%20Iniciar%20Sesión/UT-FRONTEND/README.md) implementada completa (`apiClient` con `withCredentials`, `useAuthStore`, guard `_authenticated`, ruta `/login`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/frontend/`](../../../../.claude/rules/frontend/) y
[UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md)
(regla de no anticipar trabajo de otra HU).

## Decisión de diseño: el interceptor de `Authorization` nace en esta HU, no en HU01

HU01-FE dejó explícitamente pendiente ("follow-up") el interceptor que
adjunta `Authorization: Bearer` en cada request, porque el login por sí
solo nunca necesita mandarlo. Esta HU sí lo necesita de verdad: sin ese
header, ningún endpoint de negocio devolvería nunca un `401` por token
vencido, y no habría nada que "renovar" — por eso el interceptor se
construye acá, como parte genuina del alcance de HU02 (no es anticipar
trabajo ajeno, es un prerequisito de esta misma HU).

## Decisión de diseño: sin bootstrap global aparte — el guard intenta el refresh

En vez de un efecto separado al montar la app, `_authenticated.tsx`
intenta un refresh silencioso **dentro de su propio `beforeLoad`** cuando
no hay sesión en memoria (`beforeLoad` de TanStack Router soporta
`async`/`await` de forma nativa, el router espera la promesa antes de
decidir la navegación) — evita construir un mecanismo de "app bootstrap"
nuevo cuando el guard que ya existe puede resolverlo solo.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU02-FE-01](./UT-HU02-FE-01.md) | `refreshSession()` + interceptores de `apiClient` (`Authorization` + retry en 401) | — |
| [UT-HU02-FE-02](./UT-HU02-FE-02.md) | Guard `_authenticated` y ruta `/login`: intento de refresh silencioso | UT-HU02-FE-01 |
| [UT-HU02-FE-03](./UT-HU02-FE-03.md) | Pruebas unitarias | UT-HU02-FE-01, UT-HU02-FE-02 |

**Cadena:** `01 → 02 → 03`.

## Criterios de aceptación cubiertos

| CA (HU02) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Refresh válido → nueva sesión sin pedir login | 01, 02 |
| CA02/CA03/CA04/CA07 | Refresh inválido → sesión limpia, redirige a `/login` | 02, 03 |
| CA06 | Consumo del sobre estándar (ya resuelto por `apiClient`) | 01 |

## Follow-ups (no se implementan en estas UT)

- Botón de "Cerrar sesión" — [HU-03](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md).
- Cola de reintentos si varias requests fallan con `401` al mismo tiempo
  más allá del dedupe básico de un único refresh en vuelo (ver
  UT-HU02-FE-01) — no hay evidencia todavía de que el proyecto necesite
  algo más sofisticado.

## Qué NO incluyen estas UT

- `/auth/logout` del lado del cliente (HU-03)
- Layouts por rol
- Código backend (ver [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md))
