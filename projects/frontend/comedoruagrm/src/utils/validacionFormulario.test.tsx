import { useForm } from '@tanstack/react-form'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { z } from 'zod'

import { Label } from '@/components/ui/label'

import { validacionFormulario } from './validacionFormulario'

const esquema = z.object({ lugar: z.string().min(1, 'El lugar es obligatorio') })

function Formulario() {
  const form = useForm({
    defaultValues: { lugar: '' },
    ...validacionFormulario(esquema),
    onSubmit: () => {},
  })
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        void form.handleSubmit()
      }}
    >
      <form.Field name="lugar">
        {(campo) => (
          <>
            <Label htmlFor="lugar" required>
              Lugar
            </Label>
            <input
              id="lugar"
              value={campo.state.value}
              onBlur={campo.handleBlur}
              onChange={(e) => campo.handleChange(e.target.value)}
            />
            {campo.state.meta.isTouched && <span role="alert">{campo.state.meta.errors[0]?.message}</span>}
          </>
        )}
      </form.Field>
      <button>Enviar</button>
    </form>
  )
}

describe('validacionFormulario', () => {
  it('el error de «obligatorio» desaparece apenas se escribe, aunque haya salido del campo o se haya intentado enviar', async () => {
    const user = userEvent.setup()
    render(<Formulario />)

    await user.click(screen.getByLabelText('Lugar'))
    await user.tab() // blur con el campo vacío
    expect(await screen.findByRole('alert')).toHaveTextContent('El lugar es obligatorio')

    await user.click(screen.getByRole('button', { name: 'Enviar' })) // intento de envío vacío
    expect(screen.getByRole('alert')).toHaveTextContent('El lugar es obligatorio')

    await user.type(screen.getByLabelText('Lugar'), 'DUBSS')
    expect(screen.getByRole('alert')).toBeEmptyDOMElement()
  })

  it('vuelve a marcar el error si se borra lo escrito', async () => {
    const user = userEvent.setup()
    render(<Formulario />)

    await user.type(screen.getByLabelText('Lugar'), 'a')
    expect(screen.getByRole('alert')).toBeEmptyDOMElement()
    await user.clear(screen.getByLabelText('Lugar'))

    expect(screen.getByRole('alert')).toHaveTextContent('El lugar es obligatorio')
  })

  it('la etiqueta obligatoria lleva el asterisco como estilo, sin cambiar su texto', () => {
    render(<Formulario />)
    const etiqueta = screen.getByText('Lugar')
    expect(etiqueta).toHaveTextContent(/^Lugar$/)
    expect(etiqueta.className).toContain("after:content-['*']")
    expect(etiqueta.className).toContain('after:text-destructive')
  })
})
