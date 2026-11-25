---
globs: projects/frontend/comedoruagrm/**/*
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
  ...validacionFormulario(crearUsuarioSchema),
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

**Siempre `...validacionFormulario(schema)`** (`src/utils/validacionFormulario.ts`),
nunca `validators: { onChange: schema, onBlur: schema }`. Esa forma guarda el
error de cada evento por separado: el que deja el blur (o un intento de envío)
sobre un campo vacío **no se borra al escribir**, porque al escribir solo se
recalcula el de `onChange`, y se ve «El campo es obligatorio» con el campo ya
lleno. `validacionFormulario` corre el schema en cada cambio, blur y envío y
escribe siempre en un único lugar (`errorMap.onDynamic`), así el resultado más
reciente reemplaza al anterior. Para renderizar ese error ver la regla
siguiente.

## Campos obligatorios llevan un asterisco rojo

Todo campo que el schema exige (`min(1)`, enum sin valor por defecto, etc.)
lleva un `*` rojo después de la etiqueta. Es un pseudo-elemento de CSS, no
texto, así que no cambia el nombre accesible de la etiqueta ni los tests con
`getByLabelText`:

- `<Label htmlFor="x" required>` (`components/ui/label`) o `required` en
  `CampoFormulario` / `CampoUsuario`.
- Para una etiqueta que no es un `<label>` (un `<span>` de un `Select`),
  `className={cn('text-sm font-medium', MARCA_OBLIGATORIO)}`.
- Si la obligatoriedad depende de otro campo (observaciones de una auditoría
  que no es «Validado»), `required` es una condición, no una constante.
- Los campos opcionales no llevan marca; los de solo lectura tampoco.

## Un campo muestra su error solo después de salir de él (o al enviar)

`validacionFormulario` valida el **objeto completo** en cada tecleo, así que
TanStack Form recalcula el error de **todos** los campos y se lo reparte a
cada `field.state.meta.errors`, también al que la persona todavía está
escribiendo por primera vez. Si un campo muestra ese error apenas existe,
«El campo es obligatorio» salta desde la primera letra, con el foco aún ahí.

**`field.state.meta.isTouched` no sirve para decidir si se muestra**: TanStack
lo pone en `true` al **teclear**, no solo al salir. La regla es: el error se
ve cuando el campo ya perdió el foco (`isBlurred`) o ya se intentó enviar el
formulario; una vez visible, se revalida al escribir y desaparece apenas el
dato es válido.

Eso lo resuelve `src/utils/errorCampo.ts`, y es lo que se usa **siempre**:

```tsx
import { errorVisible, mensajeError } from '@/utils/errorCampo'

<form.Field name="nombre">
  {(field) => (
    <>
      <Input
        value={field.state.value}
        aria-invalid={errorVisible(field) && field.state.meta.errors.length > 0 ? true : undefined}
        onBlur={field.handleBlur}
        onChange={(e) => field.handleChange(e.target.value)}
      />
      {errorVisible(field) && field.state.meta.errors[0] && (
        <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
      )}
    </>
  )}
</form.Field>
```

- `mensajeError(field)` devuelve el texto o `undefined` (para componentes que
  reciben `error={...}`); `CampoFormulario` ya lo aplica.
- `onBlur={field.handleBlur}` en cada `Input`/`Textarea` es obligatorio: sin
  él el campo nunca queda «blurred».
- `validacionFormulario()` marca todos los campos como blurred cuando un envío
  es inválido (`onSubmitInvalid`), así aparecen todos los errores pendientes.
- Un selector (`Select`) o un `DatePicker` sin `onBlur` son un solo gesto, no
  hay «escribiendo»: pueden seguir usando `isTouched`.
- Nunca `validators: { onChange: schema, onBlur: schema }`: ver arriba.
