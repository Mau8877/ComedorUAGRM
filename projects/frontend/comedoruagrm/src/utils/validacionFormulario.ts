import type { AnyFormApi, ValidationLogicFn } from '@tanstack/react-form'

/**
 * Lógica de validación de los formularios: el esquema se corre en cada cambio, en cada blur y al enviar, y
 * siempre escribe en **un solo** lugar (`errorMap.onDynamic`), así que el resultado más reciente reemplaza al
 * anterior.
 *
 * * Con `validators: { onChange: esquema, onBlur: esquema }` cada evento guarda su error por separado: el que
 * * deja el blur (o el envío) sobre un campo vacío NO se borra al escribir, porque al escribir solo se
 * * recalcula el de `onChange`. Resultado: «El campo es obligatorio» con el campo ya lleno.
 * ! Nunca usar `onChange` + `onBlur` con el mismo esquema en un `useForm`: usar `validacionFormulario()`.
 */
const validarEnCadaEvento: ValidationLogicFn = (props) => {
  const { type, async } = props.event
  if (type === 'mount' || type === 'server') {
    return props.runValidation({ validators: [], form: props.form })
  }
  return props.runValidation({
    validators: [{ fn: async ? props.validators?.onDynamicAsync : props.validators?.onDynamic, cause: 'dynamic' }],
    form: props.form,
  })
}

/** Opciones de `useForm` para validar con un esquema de Zod: `useForm({ defaultValues, ...validacionFormulario(esquema) })`. */
export function validacionFormulario<TSchema>(esquema: TSchema) {
  return {
    validators: { onDynamic: esquema },
    validationLogic: validarEnCadaEvento,
    // * Un envío inválido marca todos los campos como «ya salió del campo»: así su error se muestra (y el campo se
    // * vuelve a renderizar) aunque la persona nunca lo haya abandonado; ver `errorCampo.ts`.
    onSubmitInvalid: ({ formApi }: { formApi: AnyFormApi }) => {
      for (const nombre of Object.keys(formApi.fieldInfo)) {
        formApi.setFieldMeta(nombre, (m) => ({ ...m, isBlurred: true }))
      }
    },
  }
}
