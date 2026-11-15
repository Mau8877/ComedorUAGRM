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

import { ConvocatoriasScreen } from '../screens/ConvocatoriasScreen'

import { cliente, convocatoria, detalle, montar, reiniciarCliente, simularBackend, ultimaConsulta, yo } from './montar'

function pantalla() {
  return montar(<ConvocatoriasScreen titulo="Convocatorias" activeHref="/convocatorias" usuarioActual={yo} />)
}

/** Los botones de acción de la fila de esa convocatoria (la tabla ancha; las cards repiten los mismos). */
function accionesDe(nombre: string): string[] {
  const fila = screen.getAllByText(nombre)[0].closest('tr') as HTMLElement
  return within(fila)
    .getAllByRole('button')
    .map((b) => b.getAttribute('aria-label') ?? b.textContent ?? '')
}

describe('ConvocatoriasScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    simularBackend({
      convocatorias: [
        convocatoria(1, 'BORRADOR'),
        convocatoria(2, 'ABIERTA'),
        convocatoria(3, 'CERRADA', { cupos: null, puntajeMinimo: null, cantidadFinalistas: null }),
        convocatoria(4, 'PUBLICADA'),
      ],
      detalles: [detalle(1, 'BORRADOR'), detalle(2, 'ABIERTA'), detalle(3, 'CERRADA'), detalle(4, 'PUBLICADA')],
    })
  })

  it('pide la lista y muestra gestión, fechas dd/mm/aaaa, cupos y estado', async () => {
    pantalla()

    expect((await screen.findAllByText('Beca Comedor 1')).length).toBeGreaterThan(0)
    expect(ultimaConsulta('/convocatorias')).toEqual({ page: 1, pageSize: 20 })
    expect(screen.getAllByText('2026-II').length).toBeGreaterThan(0)
    expect(screen.getAllByText('01/08/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('31/08/2026').length).toBeGreaterThan(0)
    for (const estado of ['Borrador', 'Abierta', 'Cerrada', 'Publicada']) {
      expect(screen.getAllByText(estado).length).toBeGreaterThan(0)
    }
  })

  it('un cupo, puntaje o cantidad de finalistas sin definir se muestra «—»', async () => {
    pantalla()
    await screen.findAllByText('Beca Comedor 3')

    const fila = screen.getAllByText('Beca Comedor 3')[0].closest('tr') as HTMLElement

    expect(within(fila).getAllByText('—')).toHaveLength(3)
  })

  it('las acciones de cada fila dependen del estado', async () => {
    pantalla()
    await screen.findAllByText('Beca Comedor 1')

    expect(accionesDe('Beca Comedor 1')).toEqual(['Ver detalle', 'Editar', 'Abrir', 'Eliminar'])
    expect(accionesDe('Beca Comedor 2')).toEqual(['Ver detalle', 'Editar', 'Cerrar'])
    expect(accionesDe('Beca Comedor 3')).toEqual(['Ver detalle'])
    expect(accionesDe('Beca Comedor 4')).toEqual(['Ver detalle'])
  })

  it('la búsqueda manda search y vuelve a la página 1', async () => {
    simularBackend({ convocatorias: [convocatoria(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta('/convocatorias').page).toBe(2))

    await user.type(screen.getByPlaceholderText('Buscar por nombre...'), 'beca')

    await waitFor(() => expect(ultimaConsulta('/convocatorias')).toMatchObject({ search: 'beca', page: 1 }))
  })

  it('el filtro de estado manda filter[estado]', async () => {
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')

    await user.click(screen.getByRole('combobox', { name: 'Estado' }))
    await user.click(await screen.findByRole('option', { name: 'Abierta' }))

    await waitFor(() => expect(ultimaConsulta('/convocatorias')).toMatchObject({ 'filter[estado]': 'ABIERTA', page: 1 }))
  })

  it('el filtro de gestión manda filter[gestion] con el código de la gestión', async () => {
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')

    await user.click(screen.getByRole('combobox', { name: 'Gestión' }))
    await user.click(await screen.findByRole('option', { name: '2026-I' }))

    await waitFor(() => expect(ultimaConsulta('/convocatorias')).toMatchObject({ 'filter[gestion]': 'g1', page: 1 }))
  })

  it('ordenar por una columna ordenable manda sort', async () => {
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')

    await user.click(screen.getByRole('button', { name: /Cierre/ }))

    await waitFor(() => expect(ultimaConsulta('/convocatorias')).toMatchObject({ sort: 'fechaCierre' }))
  })

  it('sin convocatorias muestra el mensaje de lista vacía', async () => {
    simularBackend({ convocatorias: [] })
    pantalla()

    expect((await screen.findAllByText('Todavía no hay convocatorias')).length).toBeGreaterThan(0)
  })

  it('si falla la consulta muestra el error y permite reintentar', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/convocatorias' ? Promise.reject(new ApiError('ERR_SYS_00', 'Falló la consulta')) : Promise.resolve({ data: { data: [], meta: {} } }),
    )
    const { user } = pantalla()

    expect(await screen.findByRole('alert')).toHaveTextContent('Falló la consulta')
    cliente.get.mockClear()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/convocatorias', expect.anything()))
  })

  it('«Nueva convocatoria» abre el formulario de alta', async () => {
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')

    await user.click(screen.getByRole('button', { name: 'Nueva convocatoria' }))

    expect(await screen.findByRole('dialog', { name: /Nueva convocatoria/ })).toBeInTheDocument()
  })

  it('«Abrir» y «Cerrar» piden confirmación antes de llamar al backend', async () => {
    cliente.post.mockResolvedValue({ data: {} })
    const { user } = pantalla()
    await screen.findAllByText('Beca Comedor 1')

    await user.click(within(screen.getAllByText('Beca Comedor 1')[0].closest('tr') as HTMLElement).getByRole('button', { name: 'Abrir' }))

    expect(await screen.findByRole('dialog', { name: /Abrir convocatoria/ })).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })
})
