# UT-HU01-FE-04 — Screen `Login`: orquesta form + mutation + store

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-04` |
| **Nombre** | Screen `Login`: conecta `LoginForm`, `useLoginMutation` y `useAuthStore` |
| **Historia** | HU01-FE |
| **Depende de** | UT-HU01-FE-01, UT-HU01-FE-02, UT-HU01-FE-03 |

## Antes de implementar

Releer [HU01 - Iniciar Sesión.md](../../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)
— los CA02/CA03/CA05/CA06/CA09 se resuelven acá mostrando el `message`
que ya viene armado del backend.

## Solicitud de negocio

Conectar las piezas ya construidas (formulario, mutación, store de
sesión) en la pantalla real de login: al enviar, llama al backend, y
según el resultado guarda la sesión y navega, o muestra el error.

## Objetivo

Implementar `features/auth/screens/Login.tsx`.

## Alcance

### `features/auth/screens/Login.tsx`

```tsx
export function Login() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const loginMutation = useLoginMutation()

  const handleSubmit = (values: LoginFormValues) => {
    loginMutation.mutate(values, {
      onSuccess: (data) => {
        setSession(data.accessToken, data.usuario)
        void navigate({ to: '/panel' })
      },
    })
  }

  const errorMessage = loginMutation.isError
    ? loginMutation.error instanceof ApiError
      ? loginMutation.error.message
      : 'No se pudo conectar con el servidor. Intente nuevamente.'
    : undefined

  return (
    <LoginForm
      onSubmit={handleSubmit}
      isSubmitting={loginMutation.isPending}
      errorMessage={errorMessage}
    />
  )
}
```

- **No** se mapea `error.code` a un texto propio del frontend: el
  `message` que devuelve el backend para `ERR_AUTH_01`/`ERR_AUTH_02`/
  `ERR_AUTH_03`/`ERR_SYS_00` ya es el texto final en español que se
  muestra al usuario (incluye, por ejemplo, los minutos restantes de
  bloqueo armados del lado del backend) — remapearlo en el frontend
  duplicaría un texto que puede desincronizarse.
- Un error de red (sin `response`, `loginMutation.error` no es
  `ApiError`) muestra un mensaje genérico propio del frontend, no
  técnico.
- Tras el éxito, navega a `/panel` (el placeholder ya existente de
  `_authenticated/panel.tsx`) — no hay bifurcación por rol todavía (ver
  follow-ups del [README](../README.md)).

### Barrel de la feature

`features/auth/index.ts` exporta `Login` (la screen) — es lo único que
`routes/login.tsx` (UT-HU01-FE-05) debe poder importar de esta feature.

### Archivos esperados (crear/tocar)

- `features/auth/screens/Login.tsx`
- Editar: `features/auth/index.ts` (export de `Login`)

## Fuera de alcance (no tocar)

- `routes/login.tsx` (UT-HU01-FE-05 — ahí se monta esta screen)
- `_authenticated.tsx` (UT-HU01-FE-05)
- Interceptor de `Authorization` (follow-up)

## Resultado esperado

Enviar el formulario con credenciales correctas guarda la sesión y
navega a `/panel`; con credenciales incorrectas/cuenta inactiva/cuenta
bloqueada, se ve el mensaje correspondiente sin navegar.

## Validación

1. Mock de `useLoginMutation` resolviendo con éxito → `setSession` se
   invoca con los datos correctos y `navigate` se llama con `{ to: '/panel' }`.
2. Mock rechazando con `new ApiError('ERR_AUTH_01', 'Credenciales inválidas')`
   → el texto "Credenciales inválidas" aparece en pantalla, sin navegar.
3. Mock rechazando con un error de red genérico (no `ApiError`) → aparece
   el mensaje genérico del frontend, no un stacktrace ni `undefined`.
4. Mientras `isPending` es `true`, el formulario se muestra deshabilitado
   (delegado a `LoginForm`, ver UT-HU01-FE-03).
