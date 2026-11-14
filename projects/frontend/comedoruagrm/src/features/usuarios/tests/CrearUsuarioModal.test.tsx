import { screen, waitFor } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./clienteSimulado')
  return { etiquetaRol: sim.etiquetaRolSimulada, useCerrarSesion: () => ({ mutate: sim.cerrarSesion }) }
})
vi.mock('@/layouts', () => ({
  AdminLayout: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}))
vi.mock('@/components/ui/toast', () => ({
  toastInfo: vi.fn(),
  toastSuccess: vi.fn(),
  toastWarning: vi.fn(),
  toastError: vi.fn(),
}))

import { toastInfo } from '@/components/ui/toast'
import { ApiError } from '@/store'

import { cliente, montar, pagina, reiniciarCliente, usuario } from './montarPantalla'

const CONTRASENA = 'Temporal-9xK2'
const creada = { ...usuario(9, { nombres: 'Rosa', apellidos: 'Soliz', rol: 'DIRECTIVO' }), contrasenaTemporal: CONTRASENA }

async function abrirYLlenar(user: UserEvent) {
  await screen.findAllByText('Apellido1 Nombre1')
  await user.click(screen.getByRole('button', { name: 'Nueva cuenta' }))
  await user.type(await screen.findByLabelText('Nombres'), 'Rosa')
  await user.type(screen.getByLabelText('Apellidos'), 'Soliz')
  await user.type(screen.getByLabelText('CI'), '7654321')
  await user.type(screen.getByLabelText('Correo electrónico'), 'rosa@uagrm.edu.bo')
}

const enviar = (user: UserEvent) => user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

describe('CrearUsuarioModal', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue(pagina([usuario(1)]))
    cliente.post.mockResolvedValue({ data: creada, status: 201 })
    vi.mocked(toastInfo).mockClear()
    window.localStorage?.clear()
    window.sessionStorage?.clear()
  })

  it('un campo solo muestra su error si ya fue tocado', async () => {
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')
    await user.click(screen.getByRole('button', { name: 'Nueva cuenta' }))

    expect(screen.queryByText('Los nombres son obligatorios')).not.toBeInTheDocument()

    await user.click(await screen.findByLabelText('Nombres'))
    await user.tab()

    expect(await screen.findByText('Los nombres son obligatorios')).toBeInTheDocument()
    expect(screen.queryByText('Los apellidos son obligatorios')).not.toBeInTheDocument()
    expect(screen.queryByText('El CI es obligatorio')).not.toBeInTheDocument()
  })

  it('no envía si el formulario es inválido y marca los campos pendientes', async () => {
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')
    await user.click(screen.getByRole('button', { name: 'Nueva cuenta' }))

    await enviar(user)

    expect(cliente.post).not.toHaveBeenCalled()
    expect(await screen.findByText('El CI es obligatorio')).toBeInTheDocument()
    expect(screen.getByText('Los apellidos son obligatorios')).toBeInTheDocument()
  })

  it('valida el formato del correo', async () => {
    const { user } = montar()
    await abrirYLlenar(user)
    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'no-es-correo')
    await user.tab()

    expect(await screen.findByText('El correo electrónico no es válido')).toBeInTheDocument()
  })

  it.each([
    ['Directivo', 'DIRECTIVO'],
    ['Administrador DUBSS', 'ADMINISTRADOR_DUBSS'],
    ['Personal del comedor', 'PERSONAL_COMEDOR'],
  ])('envía una cuenta válida con el rol %s', async (etiqueta, rol) => {
    const { user } = montar()
    await abrirYLlenar(user)

    expect(screen.getByRole('combobox', { name: 'Rol' })).toHaveTextContent('Directivo')
    if (rol !== 'DIRECTIVO') {
      await user.click(screen.getByRole('combobox', { name: 'Rol' }))
      await user.click(await screen.findByRole('option', { name: etiqueta }))
    }
    await enviar(user)

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post).toHaveBeenCalledWith('/usuarios', {
      nombres: 'Rosa',
      apellidos: 'Soliz',
      ci: '7654321',
      correo: 'rosa@uagrm.edu.bo',
      rol,
    })
  })

  it('ERR_US_01 marca el campo CI y conserva lo escrito', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_US_01', 'Ya existe una cuenta con ese CI'))
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)

    expect(await screen.findByText('Ya existe una cuenta con ese CI')).toBeInTheDocument()
    expect(screen.getByLabelText('CI')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Nombres')).toHaveValue('Rosa')
    expect(screen.getByLabelText('CI')).toHaveValue('7654321')
  })

  it('ERR_US_02 marca el campo correo', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_US_02', 'Ya existe una cuenta con ese correo'))
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)

    expect(await screen.findByText('Ya existe una cuenta con ese correo')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('CI')).not.toHaveAttribute('aria-invalid')
  })

  it('ERR_US_10 se muestra en el formulario, sin marcar un campo', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_US_10', 'El rol indicado no es del personal'))
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('El rol indicado no es del personal')
  })

  it('un error de red muestra el aviso de conexión y permite reintentar', async () => {
    const { AxiosError } = await import('axios')
    cliente.post.mockRejectedValueOnce(new AxiosError('Network Error'))
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)
    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('No se pudo conectar con el servidor')

    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Cuenta creada')).toBeInTheDocument()
    expect(cliente.post).toHaveBeenCalledTimes(2)
  })

  it('la contraseña temporal aparece solo en la pantalla de resultado, se copia y se descarta al cerrar', async () => {
    const { user } = montar()
    await abrirYLlenar(user)
    expect(screen.queryByText(CONTRASENA)).not.toBeInTheDocument()

    await enviar(user)

    expect(await screen.findByText('Cuenta creada')).toBeInTheDocument()
    expect(screen.getByTestId('contrasena-temporal')).toHaveTextContent(CONTRASENA)
    expect(screen.getByText(/no se volverá a mostrar/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Copiar' }))
    expect(await navigator.clipboard.readText()).toBe(CONTRASENA)
    expect(await screen.findByRole('button', { name: 'Copiada' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Entendido/ }))

    await waitFor(() => expect(screen.queryByText('Cuenta creada')).not.toBeInTheDocument())
    expect(screen.queryByText(CONTRASENA)).not.toBeInTheDocument()
    expect(document.body.textContent).not.toContain(CONTRASENA)
    expect(JSON.stringify({ ...window.localStorage })).not.toContain(CONTRASENA)
    expect(JSON.stringify({ ...window.sessionStorage })).not.toContain(CONTRASENA)
  })

  it('la pantalla de resultado no se cierra con Escape', async () => {
    const { user } = montar()
    await abrirYLlenar(user)
    await enviar(user)
    await screen.findByText('Cuenta creada')

    await user.keyboard('{Escape}')

    expect(screen.getByText('Cuenta creada')).toBeInTheDocument()
    expect(screen.getByTestId('contrasena-temporal')).toBeInTheDocument()
  })

  it('un doble clic en «Crear cuenta» envía una sola vez', async () => {
    cliente.post.mockReturnValue(new Promise(() => {}))
    const { user } = montar()
    await abrirYLlenar(user)

    await user.dblClick(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(cliente.post).toHaveBeenCalledTimes(1)
  })

  it('avisa con un toast si la cuenta creada es de otro rol que el de la vista', async () => {
    cliente.post.mockResolvedValue({ data: { ...creada, rol: 'PERSONAL_COMEDOR' }, status: 201 })
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)
    await screen.findByText('Cuenta creada')

    expect(toastInfo).toHaveBeenCalledWith('Cuenta creada', expect.stringContaining('Personal del comedor'))
  })

  it('no avisa con toast si la cuenta es del mismo rol que la vista', async () => {
    const { user } = montar()
    await abrirYLlenar(user)

    await enviar(user)
    await screen.findByText('Cuenta creada')

    expect(toastInfo).not.toHaveBeenCalled()
  })
})
