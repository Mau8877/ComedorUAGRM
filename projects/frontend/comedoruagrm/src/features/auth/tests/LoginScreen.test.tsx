import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/store'

const navigate = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
  useNavigate: () => navigate,
}))
vi.mock('../api/useLogin', () => ({ useLogin: vi.fn() }))

import { useLogin } from '../api/useLogin'
import { LoginScreen } from '../screens/LoginScreen'

const sesion = { usuario: { rol: 'POSTULANTE' } }

function montar(opciones: { isPending?: boolean; mutate?: (v: unknown, o: { onSuccess: (s: unknown) => void; onError: (e: unknown) => void }) => void } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useLogin).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  render(<LoginScreen />)
  return { mutate, user: userEvent.setup() }
}

describe('LoginScreen', () => {
  beforeEach(() => navigate.mockReset())

  it('no muestra errores al abrir y los muestra al salir de un campo vacío', async () => {
    const { user } = montar()
    expect(screen.queryByText('El correo electrónico es obligatorio')).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('Correo electrónico'))
    await user.tab()

    expect(await screen.findByText('El correo electrónico es obligatorio')).toBeInTheDocument()
    expect(screen.queryByText('La contraseña es obligatoria')).not.toBeInTheDocument()
  })

  it('no envía si el formulario es inválido', async () => {
    const { mutate, user } = montar()

    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(mutate).not.toHaveBeenCalled()
    expect(await screen.findByText('La contraseña es obligatoria')).toBeInTheDocument()
  })

  it('envía las credenciales y navega al módulo del rol', async () => {
    const { mutate, user } = montar({ mutate: (_v, o) => o.onSuccess(sesion) })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.type(screen.getByLabelText('Contraseña'), 'secreta')
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    await waitFor(() => expect(mutate).toHaveBeenCalled())
    expect(mutate.mock.calls[0][0]).toEqual({ correo: 'ana@uagrm.edu.bo', contrasena: 'secreta' })
    expect(navigate).toHaveBeenCalledWith({ to: '/panel' })
  })

  it('credenciales incorrectas muestra el mensaje genérico en una alerta, no en un campo', async () => {
    const { user } = montar({ mutate: (_v, o) => o.onError(new ApiError('ERR_AUTH_04', 'x')) })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.type(screen.getByLabelText('Contraseña'), 'mala')
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.')
    expect(navigate).not.toHaveBeenCalled()
  })

  it('una cuenta bloqueada muestra el mensaje del backend', async () => {
    const mensaje = 'Tu cuenta está bloqueada temporalmente. Vuelve a intentarlo en 5 minutos.'
    const { user } = montar({ mutate: (_v, o) => o.onError(new ApiError('ERR_AUTH_05', mensaje)) })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.type(screen.getByLabelText('Contraseña'), 'secreta')
    await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(mensaje)
  })

  it('deshabilita el botón mientras envía', () => {
    montar({ isPending: true })

    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeDisabled()
  })

  it('ofrece crear cuenta y recuperar la contraseña (CU15, CA21)', () => {
    montar()

    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/registro')
    expect(screen.getByRole('link', { name: '¿Olvidó su contraseña?' })).toHaveAttribute(
      'href',
      '/recuperar-contrasena',
    )
  })
})
