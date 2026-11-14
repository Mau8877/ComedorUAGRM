import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/store'

const navigate = vi.hoisted(() => vi.fn())
const toastSuccess = vi.hoisted(() => vi.fn())
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, className }: { to: string; children: ReactNode; className?: string }) => (
    <a href={to} className={className}>
      {children}
    </a>
  ),
  useNavigate: () => navigate,
}))
vi.mock('@/components/ui/toast', () => ({ toastSuccess }))
vi.mock('../api/useRestablecerContrasena', () => ({ useRestablecerContrasena: vi.fn() }))

import { useRestablecerContrasena } from '../api/useRestablecerContrasena'
import { RestablecerContrasenaScreen } from '../screens/RestablecerContrasenaScreen'

type Mutate = (r: unknown, o: { onSuccess: () => void; onError: (e: unknown) => void }) => void

function montar(opciones: { token?: string; isPending?: boolean; mutate?: Mutate } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useRestablecerContrasena).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  render(<RestablecerContrasenaScreen tokenInicial={opciones.token} />)
  return { mutate, user: userEvent.setup() }
}

async function llenar(user: ReturnType<typeof userEvent.setup>, contrasena: string, confirmacion: string) {
  await user.type(screen.getByLabelText('Nueva contraseña'), contrasena)
  await user.type(screen.getByLabelText('Confirmar contraseña'), confirmacion)
}

describe('RestablecerContrasenaScreen', () => {
  beforeEach(() => {
    navigate.mockReset()
    toastSuccess.mockReset()
    vi.mocked(useRestablecerContrasena).mockReset()
    window.localStorage?.clear?.()
  })

  it('sin token muestra directamente el aviso de enlace inválido', () => {
    montar({ token: undefined })

    expect(screen.getByText('El enlace ya no sirve')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solicitar un enlace nuevo' })).toHaveAttribute('href', '/recuperar-contrasena')
    expect(screen.queryByLabelText('Nueva contraseña')).not.toBeInTheDocument()
  })

  it('con token muestra el formulario y quita el token de la barra de direcciones', async () => {
    montar({ token: 'tok-secreto' })

    expect(screen.getByLabelText('Nueva contraseña')).toBeInTheDocument()
    await waitFor(() =>
      expect(navigate).toHaveBeenCalledWith({ to: '/restablecer-contrasena', search: {}, replace: true }),
    )
  })

  it('con la contraseña corta o sin coincidir muestra el error inline y no llama a la API (CA07)', async () => {
    const { mutate, user } = montar({ token: 'tok' })

    await llenar(user, '123', '123')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))
    expect(await screen.findByText('La contraseña debe tener al menos 6 caracteres')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Nueva contraseña'))
    await user.clear(screen.getByLabelText('Confirmar contraseña'))
    await llenar(user, 'secreta1', 'otra-1234')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it('muestra el requisito de la contraseña antes de escribir', () => {
    montar({ token: 'tok' })

    expect(screen.getByText(/de 6 a 72 caracteres/i)).toBeInTheDocument()
  })

  it('al guardar manda el token, la contraseña y su confirmación; avisa y lleva al login', async () => {
    const { mutate, user } = montar({ token: 'tok-123', mutate: (_r, o) => o.onSuccess() })

    await llenar(user, 'secreta1', 'secreta1')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    expect(mutate.mock.calls[0][0]).toEqual({
      token: 'tok-123',
      contrasena: 'secreta1',
      confirmacionContrasena: 'secreta1',
    })
    expect(toastSuccess).toHaveBeenCalledWith('Contraseña actualizada', 'Inicia sesión con tu nueva contraseña.')
    expect(navigate).toHaveBeenCalledWith({ to: '/login' })
  })

  it('el token no se guarda en el almacenamiento del navegador', async () => {
    const { user } = montar({ token: 'tok-secreto', mutate: (_r, o) => o.onSuccess() })

    await llenar(user, 'secreta1', 'secreta1')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/login' }))

    const guardado = JSON.stringify({ ...window.localStorage, ...window.sessionStorage })
    expect(guardado).not.toContain('tok-secreto')
    expect(guardado).not.toContain('secreta1')
  })

  it('ERR_AUTH_08 reemplaza el formulario por el aviso con el enlace para pedir uno nuevo (A2)', async () => {
    const { user } = montar({ token: 'tok', mutate: (_r, o) => o.onError(new ApiError('ERR_AUTH_08', 'x')) })

    await llenar(user, 'secreta1', 'secreta1')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('El enlace ya no sirve')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solicitar un enlace nuevo' })).toBeInTheDocument()
    expect(navigate).not.toHaveBeenCalledWith({ to: '/login' })
  })

  it('un error de red conserva lo escrito y ofrece reintentar (A10)', async () => {
    const { mutate, user } = montar({
      token: 'tok',
      mutate: (_r, o) => o.onError(new AxiosError('Network Error', 'ERR_NETWORK')),
    })

    await llenar(user, 'secreta1', 'secreta1')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
    expect(screen.getByLabelText('Nueva contraseña')).toHaveValue('secreta1')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(2))
  })

  it('deshabilita el botón mientras guarda', () => {
    montar({ token: 'tok', isPending: true })

    expect(screen.getByRole('button', { name: 'Guardar contraseña' })).toBeDisabled()
  })
})
