import { screen, waitFor, within } from '@testing-library/react'
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

import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { ApiError } from '@/store'

import { cliente, montar, pagina, reiniciarCliente, usuario } from './montarPantalla'

const activa = usuario(1)
const desactivada = usuario(2, { activo: false })
const propia = usuario(3, { codigo: 'yo', rol: 'ADMINISTRADOR_DUBSS' })

function fila(nombre: string) {
  const celda = within(screen.getByRole('table')).getByText(nombre)
  return within(celda.closest('tr') as HTMLElement)
}

async function abrir(user: UserEvent, nombre: string, accion: string) {
  await screen.findAllByText('Apellido1 Nombre1')
  await user.click(fila(nombre).getByRole('button', { name: accion }))
}

const consultasDeLista = () => cliente.get.mock.calls.length

describe('acciones por fila', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue(pagina([activa, desactivada, propia]))
    cliente.post.mockResolvedValue({ data: activa })
    cliente.patch.mockResolvedValue({ data: activa })
    vi.mocked(toastSuccess).mockClear()
    vi.mocked(toastWarning).mockClear()
  })

  describe('qué acciones ofrece cada fila', () => {
    it('una cuenta activa ofrece Desactivar y no Reactivar; una desactivada, al revés', async () => {
      montar()
      await screen.findAllByText('Apellido1 Nombre1')

      expect(fila('Apellido1 Nombre1').getByRole('button', { name: 'Desactivar' })).toBeInTheDocument()
      expect(fila('Apellido1 Nombre1').queryByRole('button', { name: 'Reactivar' })).not.toBeInTheDocument()
      expect(fila('Apellido2 Nombre2').getByRole('button', { name: 'Reactivar' })).toBeInTheDocument()
      expect(fila('Apellido2 Nombre2').queryByRole('button', { name: 'Desactivar' })).not.toBeInTheDocument()
    })

    it('la propia cuenta no ofrece Cambiar rol ni Desactivar, pero sí Editar', async () => {
      montar()
      await screen.findAllByText('Apellido1 Nombre1')

      const propiaFila = fila('Apellido3 Nombre3')
      expect(propiaFila.getByRole('button', { name: 'Editar' })).toBeInTheDocument()
      expect(propiaFila.queryByRole('button', { name: 'Cambiar rol' })).not.toBeInTheDocument()
      expect(propiaFila.queryByRole('button', { name: 'Desactivar' })).not.toBeInTheDocument()
      expect(fila('Apellido1 Nombre1').getByRole('button', { name: 'Cambiar rol' })).toBeInTheDocument()
    })
  })

  describe('editar (UT-CU14-FE-06)', () => {
    it('precarga los datos y muestra el CI solo de lectura', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')

      const dialogo = within(await screen.findByRole('dialog'))
      expect(dialogo.getByLabelText('Nombres')).toHaveValue('Nombre1')
      expect(dialogo.getByLabelText('Apellidos')).toHaveValue('Apellido1')
      expect(dialogo.getByLabelText('Correo electrónico')).toHaveValue('u1@uagrm.edu.bo')
      expect(dialogo.getByLabelText('CI')).toHaveValue('1001')
      expect(dialogo.getByLabelText('CI')).toHaveAttribute('readonly')
    })

    it('envía solo lo que cambió', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')
      const correo = await screen.findByLabelText('Correo electrónico')

      await user.clear(correo)
      await user.type(correo, 'nuevo@uagrm.edu.bo')
      await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      await waitFor(() => expect(cliente.patch).toHaveBeenCalledTimes(1))
      expect(cliente.patch).toHaveBeenCalledWith('/usuarios/u1', { correo: 'nuevo@uagrm.edu.bo' })
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(toastSuccess).toHaveBeenCalled()
    })

    it('sin cambios no llama a la API y cierra', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')
      await screen.findByRole('dialog')

      await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(cliente.patch).not.toHaveBeenCalled()
    })

    it('no deja guardar un correo inválido y muestra el error al tocar el campo', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')
      const correo = await screen.findByLabelText('Correo electrónico')

      await user.clear(correo)
      await user.type(correo, 'xx')
      await user.tab()

      expect(await screen.findByText('El correo electrónico no es válido')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))
      expect(cliente.patch).not.toHaveBeenCalled()
    })

    it('ERR_US_02 marca el campo correo', async () => {
      cliente.patch.mockRejectedValue(new ApiError('ERR_US_02', 'Ya existe una cuenta con ese correo'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')
      const correo = await screen.findByLabelText('Correo electrónico')
      await user.clear(correo)
      await user.type(correo, 'otro@uagrm.edu.bo')

      await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      expect(await screen.findByText('Ya existe una cuenta con ese correo')).toBeInTheDocument()
      expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('aria-invalid', 'true')
    })

    it('ERR_US_06 avisa que la cuenta ya no existe, cierra y refresca la lista', async () => {
      cliente.patch.mockRejectedValue(new ApiError('ERR_US_06', 'La cuenta ya no existe'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Editar')
      const nombres = await screen.findByLabelText('Nombres')
      await user.type(nombres, 'X')
      const antes = consultasDeLista()

      await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      await waitFor(() => expect(toastWarning).toHaveBeenCalledWith('No se pudo editar', 'La cuenta ya no existe'))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
    })
  })

  describe('cambiar rol (UT-CU14-FE-06)', () => {
    it('el selector no ofrece el rol actual ni Postulante y avisa que tendrá que iniciar sesión de nuevo', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Cambiar rol')
      await screen.findByRole('dialog')

      expect(screen.getByText(/tendrá que iniciar sesión de nuevo/)).toBeInTheDocument()
      await user.click(screen.getByRole('combobox', { name: 'Nuevo rol' }))

      expect(await screen.findByRole('option', { name: 'Administrador DUBSS' })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: 'Personal del comedor' })).toBeInTheDocument()
      expect(screen.queryByRole('option', { name: 'Directivo' })).not.toBeInTheDocument()
      expect(screen.queryByRole('option', { name: 'Postulante' })).not.toBeInTheDocument()
    })

    it('cambia el rol, avisa con un toast y refresca la lista', async () => {
      cliente.post.mockResolvedValue({ data: { ...activa, rol: 'PERSONAL_COMEDOR' } })
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Cambiar rol')
      await user.click(await screen.findByRole('combobox', { name: 'Nuevo rol' }))
      await user.click(await screen.findByRole('option', { name: 'Personal del comedor' }))
      const antes = consultasDeLista()

      await user.click(screen.getByRole('button', { name: 'Cambiar rol' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/usuarios/u1/cambiar-rol', { rol: 'PERSONAL_COMEDOR' }))
      await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Rol actualizado', 'Nombre1 Apellido1 ahora es Personal del comedor.'))
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it.each(['ERR_US_08', 'ERR_US_09'])('%s se muestra como aviso y el modal sigue abierto sin cambiar nada', async (codigo) => {
      cliente.post.mockRejectedValue(new ApiError(codigo, 'No puedes hacer eso'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Cambiar rol')
      await screen.findByRole('dialog')
      const antes = consultasDeLista()

      await user.click(screen.getByRole('button', { name: 'Cambiar rol' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('No puedes hacer eso')
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(consultasDeLista()).toBe(antes)
      expect(toastSuccess).not.toHaveBeenCalled()
    })

    it.each(['ERR_US_12', 'ERR_US_06'])('%s avisa, cierra y refresca la lista', async (codigo) => {
      cliente.post.mockRejectedValue(new ApiError(codigo, 'La lista estaba desactualizada'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Cambiar rol')
      await screen.findByRole('dialog')
      const antes = consultasDeLista()

      await user.click(screen.getByRole('button', { name: 'Cambiar rol' }))

      await waitFor(() => expect(toastWarning).toHaveBeenCalledWith('No se cambió el rol', 'La lista estaba desactualizada'))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
    })
  })

  describe('desactivar y reactivar (UT-CU14-FE-07)', () => {
    it('pide confirmación, nombra a la persona y explica la consecuencia', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')

      const dialogo = within(await screen.findByRole('alertdialog'))
      expect(dialogo.getByText(/Nombre1 Apellido1 perderá el acceso/)).toBeInTheDocument()
      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('confirmar desactivar llama al endpoint, avisa y refresca la lista', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')
      const dialogo = within(await screen.findByRole('alertdialog'))
      const antes = consultasDeLista()

      await user.click(dialogo.getByRole('button', { name: 'Desactivar' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/usuarios/u1/desactivar'))
      await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Cuenta desactivada', expect.stringContaining('Nombre1 Apellido1')))
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })

    it('cancelar no llama a nada', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')
      const dialogo = within(await screen.findByRole('alertdialog'))

      await user.click(dialogo.getByRole('button', { name: 'Cancelar' }))

      await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('confirmar reactivar llama al endpoint correcto', async () => {
      const { user } = montar()
      await abrir(user, 'Apellido2 Nombre2', 'Reactivar')
      const dialogo = within(await screen.findByRole('dialog'))
      expect(dialogo.getByText(/recuperará el acceso/)).toBeInTheDocument()
      const antes = consultasDeLista()

      await user.click(dialogo.getByRole('button', { name: 'Reactivar' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/usuarios/u2/reactivar'))
      await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Cuenta reactivada', expect.stringContaining('Nombre2 Apellido2')))
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
    })

    it('ERR_US_09 muestra el aviso sin refrescar la lista (la cuenta no cambió)', async () => {
      cliente.post.mockRejectedValue(new ApiError('ERR_US_09', 'No puedes desactivar al último Administrador'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')
      const dialogo = within(await screen.findByRole('alertdialog'))
      const antes = consultasDeLista()

      await user.click(dialogo.getByRole('button', { name: 'Desactivar' }))

      await waitFor(() =>
        expect(toastWarning).toHaveBeenCalledWith('No se pudo completar la acción', 'No puedes desactivar al último Administrador'),
      )
      await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
      expect(consultasDeLista()).toBe(antes)
      expect(toastSuccess).not.toHaveBeenCalled()
    })

    it.each(['ERR_US_07', 'ERR_US_06'])('%s avisa, cierra y refresca la lista', async (codigo) => {
      cliente.post.mockRejectedValue(new ApiError(codigo, 'La cuenta ya estaba así'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')
      const dialogo = within(await screen.findByRole('alertdialog'))
      const antes = consultasDeLista()

      await user.click(dialogo.getByRole('button', { name: 'Desactivar' }))

      await waitFor(() => expect(toastWarning).toHaveBeenCalledWith('Sin cambios', 'La cuenta ya estaba así'))
      await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
      await waitFor(() => expect(consultasDeLista()).toBeGreaterThan(antes))
    })

    it('un error de red deja el diálogo abierto con el aviso y la opción de reintentar', async () => {
      const { AxiosError } = await import('axios')
      cliente.post.mockRejectedValueOnce(new AxiosError('Network Error'))
      const { user } = montar()
      await abrir(user, 'Apellido1 Nombre1', 'Desactivar')
      const dialogo = within(await screen.findByRole('alertdialog'))

      await user.click(dialogo.getByRole('button', { name: 'Desactivar' }))

      expect(await dialogo.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument()
      await user.click(dialogo.getByRole('button', { name: 'Reintentar' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(2))
      await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    })
  })
})
