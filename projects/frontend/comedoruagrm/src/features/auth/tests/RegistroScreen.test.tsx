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
vi.mock('../api/useRegistro', () => ({ useRegistro: vi.fn() }))
vi.mock('../api/useCarreras', () => ({ useCarreras: vi.fn() }))

import { useCarreras } from '../api/useCarreras'
import { useRegistro } from '../api/useRegistro'
import { RegistroScreen } from '../screens/RegistroScreen'

type Opciones = { onSuccess: (s: unknown) => void; onError: (e: unknown) => void }

function montar(opciones: { isPending?: boolean; mutate?: (v: unknown, o: Opciones) => void } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useRegistro).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  vi.mocked(useCarreras).mockReturnValue({
    data: [{ codigo: 'c1', nombre: 'Ingeniería Informática', facultad: 'Tecnología' }],
    isPending: false,
    isError: false,
    refetch: vi.fn(),
  } as never)
  render(<RegistroScreen />)
  return { mutate, user: userEvent.setup() }
}

async function llenar(user: ReturnType<typeof userEvent.setup>, confirmacion = 'secreta') {
  await user.type(screen.getByLabelText('Nombres'), 'Ana')
  await user.type(screen.getByLabelText('Apellidos'), 'Perez')
  await user.type(screen.getByLabelText('CI'), '1234567')
  await user.type(screen.getByLabelText('Registro universitario'), '219012345')
  await user.type(screen.getByLabelText('Teléfono'), '70000001')
  await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')
  await user.type(screen.getByLabelText('Contraseña'), 'secreta')
  await user.type(screen.getByLabelText('Confirmar contraseña'), confirmacion)
  await user.click(screen.getByRole('combobox'))
  await user.click(await screen.findByRole('option', { name: 'Ingeniería Informática' }))
}

describe('RegistroScreen', () => {
  beforeEach(() => navigate.mockReset())

  it('no muestra errores al abrir y los muestra solo en los campos tocados', async () => {
    const { user } = montar()
    expect(screen.queryByText('Los nombres son obligatorios')).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('Nombres'))
    await user.tab()

    expect(await screen.findByText('Los nombres son obligatorios')).toBeInTheDocument()
    expect(screen.queryByText('Los apellidos son obligatorios')).not.toBeInTheDocument()
  })

  it('marca la contraseña corta y la confirmación distinta', async () => {
    const { user } = montar()

    await user.type(screen.getByLabelText('Contraseña'), '12345')
    await user.type(screen.getByLabelText('Confirmar contraseña'), '123')
    await user.tab()

    expect(await screen.findByText('La contraseña debe tener al menos 6 caracteres')).toBeInTheDocument()
    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument()
  })

  it('no ofrece elegir rol ni crear cuentas del personal (CA16)', () => {
    montar()

    expect(screen.queryByText(/rol/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Crear cuenta' })).toBeInTheDocument()
  })

  it('envía el formulario completo y navega al módulo del rol', async () => {
    const { mutate, user } = montar({ mutate: (_v, o) => o.onSuccess({ sesion: { usuario: { rol: 'POSTULANTE' } } }) })

    await llenar(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    await waitFor(() => expect(mutate).toHaveBeenCalled())
    expect(mutate.mock.calls[0][0]).toMatchObject({ ci: '1234567', carreraCodigo: 'c1', correo: 'ana@uagrm.edu.bo' })
    expect(navigate).toHaveBeenCalledWith({ to: '/panel' })
  })

  it('un CI ya registrado se marca en el campo CI y se limpia al editarlo', async () => {
    const { user } = montar({
      mutate: (_v, o) => o.onError(new ApiError('ERR_US_01', 'Ya existe una cuenta con ese CI')),
    })

    await llenar(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Ya existe una cuenta con ese CI')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(navigate).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('CI'), '8')
    expect(screen.queryByText('Ya existe una cuenta con ese CI')).not.toBeInTheDocument()
  })

  it('un error de red muestra una alerta con opción de reintentar', async () => {
    const { user } = montar({
      mutate: (_v, o) => o.onError(Object.assign(new Error('Network Error'), { isAxiosError: true })),
    })

    await llenar(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })

  it('deshabilita el botón mientras envía (A7)', () => {
    montar({ isPending: true })

    expect(screen.getByRole('button', { name: 'Crear cuenta' })).toBeDisabled()
  })
})
