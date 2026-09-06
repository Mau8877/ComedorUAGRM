# UT-HU03-FE-02 — `LogoutButton` + wiring en `panel.tsx`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU03-FE-02` |
| **Nombre** | Componente `LogoutButton` y su uso en el placeholder `panel.tsx` |
| **Historia** | HU03-FE |
| **Depende de** | UT-HU03-FE-01 |

## Antes de implementar

Leer [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

El usuario necesita un control visible para cerrar sesión explícitamente.

## Objetivo

Implementar el componente de logout y montarlo en el único lugar
autenticado que existe hoy (`panel.tsx`, ver
[decisión de diseño](../README.md#decisión-de-diseño-dónde-vive-el-botón-de-logout)).

## Alcance

### `features/auth/components/LogoutButton.tsx` (nuevo)

```tsx
export function LogoutButton() {
  const navigate = useNavigate()
  const clearSession = useAuthStore((s) => s.clearSession)
  const [isLoading, setIsLoading] = useState(false)

  const handleClick = async () => {
    setIsLoading(true)
    try {
      await logoutSession()
    } catch {
      // Si la request de red falla, igual se limpia la sesión local --
      // el usuario pidió salir, no tiene sentido dejarlo "atrapado"
      // logueado en el cliente por un error de red al avisarle al backend.
    } finally {
      clearSession()
      void navigate({ to: '/login' })
    }
  }

  return (
    <Button variant="outline" onClick={handleClick} disabled={isLoading}>
      Cerrar sesión
    </Button>
  )
}
```

- Usa `Button` de `src/components/ui/` (shadcn), sin valores arbitrarios
  de Tailwind.
- Exportado desde `features/auth/index.ts` (barrel) — mismo criterio que
  `Login` de HU01-FE.

### `routes/_authenticated/panel.tsx` — **editar** (placeholder ya existente)

Agregar el botón al contenido de prueba que ya tiene la pantalla (sin
reemplazar el resto del placeholder, que sigue pendiente de que exista la
primera feature real detrás del login).

### Archivos esperados (crear/editar)

- Crear: `features/auth/components/LogoutButton.tsx`
- Editar: `features/auth/index.ts` (export de `LogoutButton`)
- Editar: `routes/_authenticated/panel.tsx`

## Fuera de alcance (no tocar)

- `logoutSession()`/`clearSession` (UT-HU03-FE-01, ya cerrados — esta UT
  solo los **consume**)
- Layouts por rol

## Resultado esperado

Click en "Cerrar sesión" desde `/panel` invalida la sesión y termina en
`/login`, incluso si la request de red al backend falla.

## Validación

1. Click en el botón con red normal → llama a `logoutSession()`, limpia
   `useAuthStore`, navega a `/login`.
2. Mockear `logoutSession()` rechazando (error de red) → igual limpia la
   sesión y navega a `/login` (no se queda "colgado" logueado).
3. Mientras `isLoading` es `true`, el botón está deshabilitado (evita
   doble-click disparando dos logouts).
4. Tras el logout, navegar manualmente a `/panel` → el guard de
   `_authenticated` (HU02-FE) redirige a `/login` (sin sesión que
   restaurar, porque el refresh token ya fue revocado en el backend).
