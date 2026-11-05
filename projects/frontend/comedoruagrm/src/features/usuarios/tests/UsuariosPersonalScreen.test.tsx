import { screen, waitFor, within } from '@testing-library/react'
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
// El menú de cuenta (CU15) navega con el router; acá no se prueba, así que se simula.
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { cliente, montar, pagina, reiniciarCliente, ultimaConsulta, usuario } from './montarPantalla'

describe('UsuariosPersonalScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue(pagina([usuario(1), usuario(2, { activo: false })]))
  })

  it('pide la lista con el rol de la vista como filter[rol] y muestra las cuentas', async () => {
    montar()

    expect(await screen.findAllByText('Apellido1 Nombre1')).not.toHaveLength(0)
    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20, 'filter[rol]': 'DIRECTIVO' })
    expect(screen.getAllByText('Activa').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Desactivada').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Directivo').length).toBeGreaterThan(0)
  })

  it('la búsqueda manda search y vuelve a la página 1', async () => {
    cliente.get.mockResolvedValue(pagina([usuario(1)], { totalItems: 60, totalPages: 3 }))
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.type(screen.getByPlaceholderText('Buscar por nombre o CI...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
  })

  it('el filtro de estado manda filter[activo] y vuelve a la página 1', async () => {
    cliente.get.mockResolvedValue(pagina([usuario(1)], { totalItems: 60, totalPages: 3 }))
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Estado' }))
    await user.click(await screen.findByRole('option', { name: 'Desactivadas' }))

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ 'filter[activo]': false, page: 1 }))
  })

  it('cambiar de página y de tamaño pide al backend lo correspondiente', async () => {
    cliente.get.mockResolvedValue(pagina([usuario(1)], { totalItems: 60, totalPages: 3 }))
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')

    await user.click(screen.getByRole('button', { name: 'Página siguiente' }))
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ page: 2, pageSize: 20 }))

    await user.click(screen.getByRole('combobox', { name: 'Filas por página' }))
    await user.click(await screen.findByRole('option', { name: '50' }))
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ page: 1, pageSize: 50 }))
  })

  it('ordenar por una columna manda sort (asc y luego desc) y vuelve a la página 1', async () => {
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')
    const tabla = screen.getByRole('table')

    await user.click(within(tabla).getByRole('button', { name: 'Correo' }))
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ sort: 'correo', page: 1 }))

    await user.click(within(tabla).getByRole('button', { name: 'Correo' }))
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ sort: '-correo' }))
  })

  it('solo se pueden ordenar el nombre (apellidos), el CI y el correo', async () => {
    montar()
    await screen.findAllByText('Apellido1 Nombre1')
    const tabla = screen.getByRole('table')

    expect(within(tabla).getByRole('button', { name: 'Nombre' })).toBeInTheDocument()
    expect(within(tabla).getByRole('button', { name: 'CI' })).toBeInTheDocument()
    expect(within(tabla).queryByRole('button', { name: 'Rol' })).not.toBeInTheDocument()
    expect(within(tabla).queryByRole('button', { name: 'Estado' })).not.toBeInTheDocument()
  })

  it('ordenar por Nombre manda sort=apellidos', async () => {
    const { user } = montar()
    await screen.findAllByText('Apellido1 Nombre1')

    await user.click(within(screen.getByRole('table')).getByRole('button', { name: 'Nombre' }))

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ sort: 'apellidos' }))
  })

  it('muestra el estado de carga mientras llega la primera respuesta', () => {
    cliente.get.mockReturnValue(new Promise(() => {}))
    montar()

    expect(screen.queryByText('Apellido1 Nombre1')).not.toBeInTheDocument()
    expect(document.querySelector('[data-slot="skeleton"]')).not.toBeNull()
  })

  it('muestra la lista vacía', async () => {
    cliente.get.mockResolvedValue(pagina([]))
    montar()

    expect((await screen.findAllByText('No hay cuentas que coincidan.')).length).toBeGreaterThan(0)
  })

  it('ante un error muestra el mensaje del backend y permite reintentar', async () => {
    cliente.get.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'No se pudo cargar la lista'))
    cliente.get.mockResolvedValue(pagina([usuario(1)]))
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('No se pudo cargar la lista')

    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect((await screen.findAllByText('Apellido1 Nombre1')).length).toBeGreaterThan(0)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
