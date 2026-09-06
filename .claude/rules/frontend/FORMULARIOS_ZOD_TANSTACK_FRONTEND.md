---
globs: projects/frontend/**/*
---

# Formularios (Zod + TanStack Form) — Frontend

## Un solo schema, dos usos

El schema de Zod de un recurso vive en `schemas/` dentro de la feature
(`src/features/{feature}/schemas/{recurso}Schema.ts`) y se reutiliza para:

1. Validar el formulario en el cliente, vía `@tanstack/react-form`.
2. Tipar y validar el payload que se manda a la API (el `Request` DTO del
   lado del frontend, inferido del schema con `z.infer<typeof schema>`).

**No se duplica** la validación en dos lugares (un schema para el form y un
`type CrearUsuarioRequest = {...}` escrito a mano aparte) — el segundo se
deriva del primero:

```ts
// src/features/usuarios/schemas/usuarioSchema.ts
import { z } from 'zod'

export const crearUsuarioSchema = z.object({
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  email: z.string().min(1, 'El correo es obligatorio').email('El correo no es válido'),
})

export type CrearUsuarioRequest = z.infer<typeof crearUsuarioSchema>
```

Si el schema de creación y el de edición difieren (ej. edición no pide
password), se definen como schemas separados pero uno puede derivar del otro
con `.partial()`/`.omit()`/`.extend()` en vez de copiar los campos a mano.

## Mensajes en español

El proyecto no es multi-idioma — todos los mensajes de error de Zod se
escriben en español directo en el schema (`.min(1, "El correo es obligatorio")`),
no se arma una capa de i18n para esto. Se muestran **inline, debajo del
campo correspondiente**, no en un toast/alert genérico que no indica qué
campo falló.

## Integración con TanStack Form

Las versiones instaladas (`@tanstack/react-form` ^1.33, `zod` ^4.4) soportan
pasar el schema de Zod **directamente** como validador (Zod 4 implementa el
protocolo Standard Schema, que TanStack Form v1 consume nativamente, sin
paquete adapter intermedio):

```tsx
const form = useForm({
  defaultValues: { nombre: '', email: '' },
  validators: {
    onChange: crearUsuarioSchema,
  },
  onSubmit: async ({ value }) => {
    // value ya viene tipado como CrearUsuarioRequest
  },
})
```

> Este patrón (schema directo, sin adapter) corresponde a las versiones
> exactas instaladas hoy en `package.json`. Si en algún momento se
> actualiza `@tanstack/react-form` o `zod` a un major distinto, **verificar
> primero** en el changelog de esa versión si el soporte de Standard Schema
> sigue funcionando igual antes de asumir que este ejemplo sigue vigente —
> es una integración que evoluciona rápido entre versiones de ambas
> librerías.

Se prefiere `validators.onChange` (valida mientras el usuario escribe) sobre
solo validar en `onSubmit`, para que el error aparezca inline apenas el
campo es inválido, no recién al intentar enviar -- pero ver la regla
siguiente antes de renderizar ese error.

## Un campo solo muestra su error si el usuario ya lo tocó

`validators.onChange` valida el **objeto completo** del form en cada
tecleo, no campo por campo -- por eso, al escribir en un solo campo,
TanStack Form recalcula el `errorMap` de **todos** los campos del schema a
la vez y se lo reparte a cada `field.state.meta.errors`, incluidos los que
el usuario todavía no tocó. Si un `form.Field` renderiza ese error apenas
existe, cualquier campo vacío obligatorio (`foto`, `stock`, etc.) aparece
"inválido" desde la primera letra que se escribe en **otro** campo,
aunque el usuario ni llegó a esa parte del formulario todavía.

**Regla obligatoria**: todo `form.Field` que muestra un error inline lo
condiciona también a `field.state.meta.isTouched` -- que TanStack Form
solo pone en `true` para el campo puntual que el usuario escribió o le
hizo blur (nunca para los demás, aunque su `errorMap` ya se haya
recalculado):

```tsx
<form.Field name="nombre">
  {(field) => (
    <>
      <Input
        value={field.state.value}
        onBlur={field.handleBlur}
        onChange={(e) => field.handleChange(e.target.value)}
      />
      {field.state.meta.isTouched && field.state.meta.errors[0] && (
        <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
      )}
    </>
  )}
</form.Field>
```

Por esto mismo, `onBlur={field.handleBlur}` en cada `Input`/`Select` no es
decorativo -- sin él, un campo que el usuario salta sin escribirle nunca
queda "tocado" antes del submit. Al intentar enviar el formulario
(`form.handleSubmit()`), TanStack Form sí marca todos los campos como
tocados de una sola vez, así que en ese momento aparecen correctamente
todos los errores pendientes, aunque el usuario nunca haya pasado por esos
campos.
