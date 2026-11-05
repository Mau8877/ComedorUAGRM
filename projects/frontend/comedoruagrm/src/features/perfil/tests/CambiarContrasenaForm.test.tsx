import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/store'

const hash = vi.hoisted(() => ({ valor: '' }))
const toastSuccess = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', () => ({ useLocation: () => hash.valor }))
vi.mock('@/components/ui/toast', () => ({ toastSuccess }))
vi.mock('../api/useCambiarContrasena', () => ({ useCambiarContrasena: vi.fn() }))

import { useCambiarContrasena } from '../api/useCambiarContrasena'
import { CambiarContrasenaForm } from '../components/CambiarContrasenaForm'

type Mutate = (r: unknown, o: { onSuccess: () => void; onError: (e: unknown) => void }) => void

function montar(opciones: { mutate?: Mutate; isPending?: boolean } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useCambiarContrasena).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  render(<CambiarContrasenaForm />)
  return { mutate, user: userEvent.setup() }
}

async function llenar(user: ReturnType<typeof userEvent.setup>, actual: string, nueva: string, confirmacion: string) {
  await user.type(screen.getByLabelText('Contraseña actual'), actual)
  await user.type(screen.getByLabelText('Contraseña nueva'), nueva)
  await user.type(screen.getByLabelText('Confirmar contraseña nueva'), confirmacion)
}

const boton = () => screen.getByRole('button', { name: 'Cambiar contraseña' })

describe('CambiarContrasenaForm', () => {
  beforeEach(() => {
    hash.valor = ''
    toastSuccess.mockReset()
    vi.mocked(useCambiarContrasena).mockReset()
  })

  it('muestra el requisito de la contraseña nueva y avisa que se cierran las demás sesiones', () => {
    montar()

    expect(screen.getByText(/de 6 a 72 caracteres/i)).toBeInTheDocument()
    expect(screen.getByText(/demás dispositivos/i)).toBeInTheDocument()
  })

  it('una contraseña nueva corta o sin coincidir muestra el error inline y no envía (CA07)', async () => {
    const { mutate, user } = montar()

    await llenar(user, 'actual-1', '123', '123')
    await user.click(boton())
    expect(await screen.findByText('La contraseña debe tener al menos 6 caracteres')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Contraseña nueva'))
    await user.clear(screen.getByLabelText('Confirmar contraseña nueva'))
    await user.type(screen.getByLabelText('Contraseña nueva'), 'nueva-123')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'otra-1234')
    await user.click(boton())

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it('envía las tres contraseñas y ningún token', async () => {
    const { mutate, user } = montar()

    await llenar(user, 'actual-1', 'nueva-123', 'nueva-123')
    await user.click(boton())

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    const cuerpo = mutate.mock.calls[0][0] as Record<string, string>
    expect(cuerpo).toEqual({
      contrasenaActual: 'actual-1',
      contrasenaNueva: 'nueva-123',
      confirmacionContrasena: 'nueva-123',
    })
    expect(Object.keys(cuerpo)).not.toContain('refreshToken')
  })

  it('el éxito avisa, limpia el formulario y no toca la sesión', async () => {
    const { user } = montar({ mutate: (_r, o) => o.onSuccess() })

    await llenar(user, 'actual-1', 'nueva-123', 'nueva-123')
    await user.click(boton())

    await waitFor(() =>
      expect(toastSuccess).toHaveBeenCalledWith(
        'Contraseña actualizada',
        'Cerramos tu sesión en los demás dispositivos.',
      ),
    )
    await waitFor(() => expect(screen.getByLabelText('Contraseña actual')).toHaveValue(''))
    expect(screen.getByLabelText('Contraseña nueva')).toHaveValue('')
  })

  it('ERR_AUTH_09 marca la contraseña actual y conserva lo escrito', async () => {
    const { user } = montar({ mutate: (_r, o) => o.onError(new ApiError('ERR_AUTH_09', 'x')) })

    await llenar(user, 'mala-1', 'nueva-123', 'nueva-123')
    await user.click(boton())

    expect(await screen.findByText('La contraseña actual es incorrecta.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña nueva')).toHaveValue('nueva-123')
    expect(toastSuccess).not.toHaveBeenCalled()
  })

  it('un error de red muestra el aviso de conexión con «Reintentar» (A10)', async () => {
    const { mutate, user } = montar({ mutate: (_r, o) => o.onError(new AxiosError('Network Error', 'ERR_NETWORK')) })

    await llenar(user, 'actual-1', 'nueva-123', 'nueva-123')
    await user.click(boton())

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(2))
  })

  it('con el hash #cambiar-contrasena hace scroll a la sección y enfoca la contraseña actual', () => {
    hash.valor = 'cambiar-contrasena'
    const scroll = vi.fn()
    Element.prototype.scrollIntoView = scroll

    montar()

    expect(scroll).toHaveBeenCalled()
    expect(screen.getByLabelText('Contraseña actual')).toHaveFocus()
  })

  it('deshabilita el botón mientras guarda', () => {
    montar({ isPending: true })

    expect(boton()).toBeDisabled()
  })
})
