# Unidades de trabajo — HU03-FE

**Historia:** Cerrar sesión desde la UI — invalidar el refresh token en el backend, limpiar la sesión en memoria, y volver a `/login`.
**Fuente HU:** [`../../HistoriasDeUsuario/HU03 - Cerrar Sesión.md`](../../HistoriasDeUsuario/HU03%20-%20Cerrar%20Sesión.md)
**Contrato HTTP:** [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md)
**Requiere que ya exista:** [HU01-FE](../../HU01%20-%20Iniciar%20Sesión/UT-FRONTEND/README.md) y [HU02-FE](../../HU02%20-%20Renovar%20Sesión/UT-FRONTEND/README.md) implementadas completas

## Antes de implementar cualquier UT

Leer [`.claude/rules/frontend/`](../../../../.claude/rules/frontend/) y
[UNIDADES_DE_TRABAJO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md).

## Decisión de diseño: `clearSession` nace en esta HU, editando el store de HU01

`useAuthStore` (creado por HU01-FE) quedó explícitamente sin
`clearSession` — esta HU se lo agrega, porque es la primera que
realmente necesita cerrar una sesión (ver
[HISTORIAS_DE_USUARIO.md](../../../../.claude/rules/UNIDADES_DE_TRABAJO.md)
sobre no anticipar trabajo de otra HU). Si HU02-FE ya implementó su
interceptor de reintento (UT-HU02-FE-01) usando el `setState` directo
como solución temporal (ver la nota de esa UT), esta UT reemplaza esa
línea por la llamada real a `clearSession()`.

## Decisión de diseño: dónde vive el botón de logout

Todavía no existen los layouts por rol (`AdminLayout`/`EstudianteLayout`
son [follow-ups de HU01-FE](../../HU01%20-%20Iniciar%20Sesión/UT-FRONTEND/README.md#follow-ups-no-se-implementan-en-estas-ut)) —
el único lugar autenticado que existe hoy es el placeholder
`routes/_authenticated/panel.tsx`. El botón se agrega ahí por ahora (mismo
criterio que ya deja documentado
[RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
para ese placeholder: "se reemplaza en cuanto exista la primera feature
real"). Cuando exista un layout real, ese layout es quien monta el botón
de logout — no es responsabilidad de esta HU anticipar esa estructura.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-HU03-FE-01](./UT-HU03-FE-01.md) | `logoutSession()` + `clearSession` en `useAuthStore` | — |
| [UT-HU03-FE-02](./UT-HU03-FE-02.md) | `LogoutButton` + wiring en `panel.tsx` | UT-HU03-FE-01 |
| [UT-HU03-FE-03](./UT-HU03-FE-03.md) | Pruebas unitarias | UT-HU03-FE-01, UT-HU03-FE-02 |

**Cadena:** `01 → 02 → 03`.

## Criterios de aceptación cubiertos

| CA (HU03) | Descripción | UTs |
|-----------|-------------|-----|
| CA01 | Logout exitoso → limpia sesión, redirige a `/login` | 01, 02 |
| CA02 | Logout idempotente (ya resuelto por el backend) | 01 |
| CA04 | Error inesperado → mensaje genérico, no bloquea el logout local | 02 |
| CA06 | Consumo del sobre estándar (ya resuelto por `apiClient`) | 01 |

## Follow-ups (no se implementan en estas UT)

- Reubicar el botón dentro de un layout real por rol, cuando exista.
- "Cerrar sesión en todos los dispositivos" — sigue sin caso de uso.

## Qué NO incluyen estas UT

- Layouts por rol
- Código backend (ver [`../UT-BACKEND/README.md`](../UT-BACKEND/README.md))
