import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/store'

vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
  useNavigate: () => vi.fn(),
}))
vi.mock('../api/useSolicitarRecuperacion', () => ({ useSolicitarRecuperacion: vi.fn() }))

import { useSolicitarRecuperacion } from '../api/useSolicitarRecuperacion'
import { MENSAJE_RECUPERACION_ENVIADA, RecuperarContrasenaScreen } from '../screens/RecuperarContrasenaScreen'

type Mutate = (correo: string, o: { onSuccess: () => void; onError: (e: unknown) => void }) => void

function montar(opciones: { isPending?: boolean; mutate?: Mutate } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useSolicitarRecuperacion).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  render(<RecuperarContrasenaScreen />)
  return { mutate, user: userEvent.setup() }
}

describe('RecuperarContrasenaScreen', () => {
  beforeEach(() => vi.mocked(useSolicitarRecuperacion).mockReset())

  it('con el correo vacío o mal formado muestra el error inline y no llama a la API', async () => {
    const { mutate, user } = montar()

    await user.click(screen.getByLabelText('Correo electrónico'))
    await user.tab()
    expect(await screen.findByText('El correo electrónico es obligatorio')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Correo electrónico'), 'no-es-correo')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByText('El correo electrónico no es válido')).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it('con un correo válido llama a la API y muestra siempre el mismo mensaje neutro', async () => {
    const { mutate, user } = montar({ mutate: (_c, o) => o.onSuccess() })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    expect(mutate.mock.calls[0][0]).toBe('ana@uagrm.edu.bo')
    expect(await screen.findByRole('status')).toHaveTextContent(MENSAJE_RECUPERACION_ENVIADA)
    // Ni el correo ni ninguna pista de si la cuenta existe.
    expect(screen.queryByText(/ana@uagrm/)).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Correo electrónico')).not.toBeInTheDocument()
  })

  it('«Enviar de nuevo» vuelve al formulario para pedir otro enlace', async () => {
    const { user } = montar({ mutate: (_c, o) => o.onSuccess() })
    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))
    await screen.findByRole('status')

    await user.click(screen.getByRole('button', { name: 'Enviar de nuevo' }))

    expect(await screen.findByLabelText('Correo electrónico')).toHaveValue('')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('un error de red muestra el aviso de conexión con «Reintentar»', async () => {
    const { mutate, user } = montar({ mutate: (_c, o) => o.onError(new AxiosError('Network Error', 'ERR_NETWORK')) })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(2))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('el límite de peticiones se muestra sin revelar nada de la cuenta', async () => {
    const { user } = montar({ mutate: (_c, o) => o.onError(new ApiError('ERR_SYS_02', 'x')) })

    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Demasiados intentos')
  })

  it('deshabilita el botón mientras envía y ofrece volver al login', () => {
    montar({ isPending: true })

    expect(screen.getByRole('button', { name: 'Enviar enlace' })).toBeDisabled()
    expect(screen.getByRole('link', { name: 'Volver a iniciar sesión' })).toHaveAttribute('href', '/login')
  })
})
