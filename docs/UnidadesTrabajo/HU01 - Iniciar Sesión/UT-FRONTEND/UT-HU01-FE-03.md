# UT-HU01-FE-03 — Schema Zod + `LoginForm` (TanStack Form)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU01-FE-03` |
| **Nombre** | Schema de validación (`loginSchema`) + componente `LoginForm` |
| **Historia** | HU01-FE |
| **Depende de** | — |

## Antes de implementar

Leer [FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
completo — en particular la regla de `isTouched` antes de mostrar un
error inline.

## Solicitud de negocio

El formulario de login necesita validar formato antes de llamar al
backend (CA04: campos vacíos o con formato inválido), con mensajes en
español inline por campo.

## Objetivo

Implementar el schema de Zod y el formulario controlado con
`@tanstack/react-form`, como componente de presentación puro (no llama a
la API directamente — eso lo hace la screen en UT-HU01-FE-04).

## Alcance

### `features/auth/schemas/loginSchema.ts`

```ts
import { z } from 'zod'

export const loginSchema = z.object({
  identificador: z.string().min(1, 'El usuario o correo es obligatorio'),
  password: z.string().min(1, 'La contraseña es obligatoria'),
})

export type LoginFormValues = z.infer<typeof loginSchema>
```

- `identificador` **no** valida formato de email (`.email()`) — acepta
  tanto `username` como `correo` indistintamente (ver
  [HU01 - Iniciar Sesión.md](../../../HistoriasDeUsuario/HU01%20-%20Iniciar%20Sesión.md)),
  así que solo se exige que no esté vacío.

### `features/auth/components/LoginForm.tsx`

Componente controlado, recibe callbacks por props (no conoce
`useLoginMutation` ni `useAuthStore` — los desacopla la screen):

```tsx
interface LoginFormProps {
  onSubmit: (values: LoginFormValues) => void
  isSubmitting: boolean
  errorMessage?: string
}

export function LoginForm({ onSubmit, isSubmitting, errorMessage }: LoginFormProps) {
  const form = useForm({
    defaultValues: { identificador: '', password: '' } satisfies LoginFormValues,
    validators: { onChange: loginSchema },
    onSubmit: async ({ value }) => onSubmit(value),
  })

  // Un <form.Field> por campo (identificador, password), cada error
  // inline condicionado a field.state.meta.isTouched (regla obligatoria
  // de FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) + onBlur={field.handleBlur}.
  // El campo "identificador" se etiqueta "Usuario o correo" en el label.
  // `errorMessage` (viene de la screen, es el error del backend -- ej.
  // "Credenciales inválidas") se muestra arriba del form, no por campo.
  // Botón de submit deshabilitado mientras isSubmitting.
}
```

- Usa componentes de `src/components/ui/` (shadcn) para `Input`/`Button`/
  `Label` — si alguno no existe todavía en el proyecto, agregarlo vía
  `pnpm dlx shadcn@latest add <componente>` (ver
  [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md#agregar-componentes-vía-cli-de-shadcn)),
  no armarlo a mano.
- Sin valores arbitrarios de Tailwind — clases de tema (`bg-card`,
  `text-foreground`, etc.).

### Archivos esperados (crear)

- `features/auth/schemas/loginSchema.ts`
- `features/auth/components/LoginForm.tsx`

## Fuera de alcance (no tocar)

- Llamada real a `useLoginMutation` (UT-HU01-FE-04 — este componente es
  puramente de presentación + validación de formato)
- La screen que lo monta (`Login.tsx`, UT-HU01-FE-04)
- Ruta `/login` (UT-HU01-FE-05)

## Resultado esperado

`LoginForm` valida formato en el cliente (campos vacíos) sin tocar la
red, mostrando el error inline solo en el campo que el usuario ya tocó.

## Validación

1. Dejar `identificador` vacío y tocar `password` → el error de
   `identificador` **no** aparece todavía (no está `touched`).
2. Tocar el campo `identificador` y dejarlo vacío (blur) → aparece "El
   usuario o correo es obligatorio" bajo ese campo.
3. Escribir cualquier valor no vacío en `identificador` (username o
   correo) → sin error de formato (el campo acepta ambos).
4. Con ambos campos completos, `onSubmit` se invoca con los valores
   tipados como `LoginFormValues`.
5. `isSubmitting=true` deshabilita el botón de envío.
