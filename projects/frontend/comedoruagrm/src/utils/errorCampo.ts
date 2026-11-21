import type { AnyFieldApi } from '@tanstack/react-form'

/**
 * Cuándo un campo muestra su error de validación: recién cuando la persona sale del campo (blur) o intenta
 * enviar el formulario. Mientras escribe por primera vez no aparece; una vez visible, `validacionFormulario`
 * lo revalida en cada cambio y desaparece apenas el dato es válido.
 *
 * * `isTouched` no sirve para esto: TanStack Form lo pone en `true` al TECLEAR, así que el error saltaba desde
 * * la primera letra. `isBlurred` solo se activa con `field.handleBlur`, y `submissionAttempts` cuenta los envíos.
 * ! No usar `field.state.meta.isTouched` para decidir si se muestra un error: usar `errorVisible()`/`mensajeError()`.
 * ? Los selectores y los campos sin `onBlur` que no se tecleen pueden seguir usando `isTouched` (el cambio es un solo gesto).
 */
export function errorVisible(field: Pick<AnyFieldApi, 'state' | 'form'>): boolean {
  return field.state.meta.isBlurred || field.form.state.submissionAttempts > 0
}

/** Mensaje de error del campo, o `undefined` si todavía no corresponde mostrarlo. */
export function mensajeError(field: Pick<AnyFieldApi, 'state' | 'form'>): string | undefined {
  if (!errorVisible(field)) return undefined
  const error = field.state.meta.errors[0] as { message?: string } | string | undefined
  return typeof error === 'string' ? error : error?.message
}
