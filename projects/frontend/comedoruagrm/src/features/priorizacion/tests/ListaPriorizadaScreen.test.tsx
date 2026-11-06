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
vi.mock('@/utils/descargarArchivo', async () => ({ descargarArchivo: (await import('./clienteSimulado')).descargar }))

import { ApiError } from '@/store'

import { cliente, convocatoria, fila, montar, reiniciarCliente, responder, ultimaConsulta } from './montarPantalla'
import { descargar } from './clienteSimulado'

describe('ListaPriorizadaScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    descargar.mockReset()
  })

  it('preselecciona la convocatoria más reciente, muestra el resumen y la lista con la posición real', async () => {
    responder({ filas: [fila(37), fila(38)], resumen: { totalCalificadas: 140, totalPendientes: 30 } })
    montar()

    expect(await screen.findAllByText('Nombre37 Apellido37')).not.toHaveLength(0)
    expect(screen.getByText(/140 postulaciones calificadas · 30 pendientes de precalificación/)).toBeInTheDocument()
    expect(ultimaConsulta()).toEqual({ 'filter[convocatoriaId]': 'conv-2', page: 1, pageSize: 20 })
    // La posición es la del backend (37), no el número de fila de la página.
    expect(screen.getAllByText('37').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ingeniería de Sistemas').length).toBeGreaterThan(0)
    expect(screen.getAllByText('58.00').length).toBeGreaterThan(0)
  })

  it('nunca manda sort ni otros filtros', async () => {
    responder()
    montar()
    await screen.findAllByText('Nombre1 Apellido1')

    const params = ultimaConsulta()
    expect(Object.keys(params).sort()).toEqual(['filter[convocatoriaId]', 'page', 'pageSize'])
  })

  it('sin convocatorias avisa que todavía no hay (A9)', async () => {
    responder({ convocatorias: [] })
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias registradas.')).toBeInTheDocument()
    expect(cliente.get.mock.calls.some((c) => c[0] === '/lista-priorizada')).toBe(false)
  })

  it('sin postulaciones calificadas avisa y muestra los pendientes, sin tabla (A1, A4)', async () => {
    responder({ resumen: { totalCalificadas: 0, totalPendientes: 12 }, filas: [] })
    montar()

    expect(await screen.findByText('No hay postulaciones calificadas en esta convocatoria.')).toBeInTheDocument()
    expect(screen.getByText(/0 postulaciones calificadas · 12 pendientes/)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(cliente.get.mock.calls.some((c) => c[0] === '/lista-priorizada')).toBe(false)
  })

  it('al cambiar de convocatoria vuelve a la página 1 y pide la lista de la elegida', async () => {
    responder({ filas: [fila(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: 'Beca Comedor 1 (2026-1)' }))

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ 'filter[convocatoriaId]': 'conv-1', page: 1 }))
  })

  it('la búsqueda manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ filas: [fila(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = cliente.get.mock.calls.filter((c) => c[0] === '/lista-priorizada').length

    await user.type(screen.getByPlaceholderText('Buscar por nombre o CI...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
    const despues = cliente.get.mock.calls.filter((c) => c[0] === '/lista-priorizada').length
    expect(despues - antes).toBe(1)
  })

  it('el filtro de carrera manda filter[carreraId] y vuelve a la página 1', async () => {
    responder({ filas: [fila(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Carrera' }))
    await user.click(await screen.findByRole('option', { name: 'Derecho' }))

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ 'filter[carreraId]': 'car-b', page: 1 }))
  })

  it('cambiar de página y de tamaño pide al backend lo correspondiente', async () => {
    responder({ filas: [fila(21)], meta: { page: 2, totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre21 Apellido21')

    await user.click(screen.getByRole('button', { name: 'Página 3' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(3))
  })

  it('una búsqueda sin resultados muestra el mensaje y la lista vacía (A2)', async () => {
    responder({ filas: [], resumen: { totalCalificadas: 5, totalPendientes: 0 }, meta: { totalItems: 0, totalPages: 0 } })
    montar()

    expect((await screen.findAllByText('Sin resultados para esa búsqueda.')).length).toBeGreaterThan(0)
  })

  it('«Ver» abre el detalle de solo lectura con los datos de la fila', async () => {
    responder({ filas: [fila(7)] })
    const { user } = montar()
    await screen.findAllByText('Nombre7 Apellido7')

    await user.click(screen.getAllByRole('button', { name: 'Ver' })[0])

    const dialogo = await screen.findByRole('dialog')
    expect(within(dialogo).getByText('Posición 7 de la lista priorizada')).toBeInTheDocument()
    expect(within(dialogo).getByText('9007')).toBeInTheDocument()
    expect(within(dialogo).queryByRole('textbox')).not.toBeInTheDocument()
    await user.click(within(dialogo).getAllByRole('button', { name: 'Cerrar' })[0])
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('una convocatoria inexistente (ERR_PRI_01) avisa y permite elegir otra (A10)', async () => {
    responder()
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/lista-priorizada/convocatorias') return Promise.resolve({ data: [convocatoria(1)] })
      if (ruta === '/lista-priorizada/resumen') return Promise.reject(new ApiError('ERR_PRI_01', 'La convocatoria no existe'))
      return Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } })
    })
    montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('La convocatoria ya no existe. Elige otra.')
    expect(screen.getByRole('button', { name: 'Elegir otra convocatoria' })).toBeInTheDocument()
  })

  it('un error de conexión muestra el mensaje y «Reintentar»', async () => {
    responder()
    cliente.get.mockImplementation(() => Promise.reject(new ApiError('ERR_SYS_00', 'x')))
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('Ocurrió un error inesperado')
    responder()
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))
    expect((await screen.findAllByText('Nombre1 Apellido1')).length).toBeGreaterThan(0)
  })

  it('exportar manda los mismos filtros de la pantalla (no la página) y descarga el archivo', async () => {
    responder({ filas: [fila(1)], meta: { totalItems: 60, totalPages: 3 } })
    const archivo = new Blob(['%PDF-1.5'])
    cliente.post.mockResolvedValue({ data: archivo, headers: { 'content-disposition': 'attachment; filename="lista-x.pdf"' } })
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.type(screen.getByPlaceholderText('Buscar por nombre o CI...'), 'ana')
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana' }))
    await user.click(screen.getByRole('button', { name: 'Página 2' }))

    await user.click(screen.getByRole('button', { name: 'Exportar a PDF' }))

    await waitFor(() => expect(descargar).toHaveBeenCalledWith(archivo, 'lista-x.pdf'))
    expect(cliente.post).toHaveBeenCalledWith(
      '/lista-priorizada/exportaciones',
      { convocatoriaId: 'conv-2', search: 'ana' },
      { responseType: 'blob' },
    )
  })

  it('el botón de exportar queda bloqueado mientras genera y una sola descarga por clic', async () => {
    responder({ filas: [fila(1)] })
    let terminar: (valor: unknown) => void = () => {}
    cliente.post.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')

    await user.click(screen.getByRole('button', { name: 'Exportar a PDF' }))
    const generando = await screen.findByRole('button', { name: 'Generando…' })
    expect(generando).toBeDisabled()
    await user.click(generando)
    expect(cliente.post).toHaveBeenCalledTimes(1)

    terminar({ data: new Blob(['%PDF']), headers: {} })
    await waitFor(() => expect(descargar).toHaveBeenCalledTimes(1))
    expect(descargar.mock.calls[0][1]).toBe('lista-priorizada.pdf')
  })

  it('exportar con ERR_PRI_03 o un error de red no descarga nada', async () => {
    responder({ filas: [fila(1)] })
    cliente.post.mockRejectedValue(new ApiError('ERR_PRI_03', 'No hay postulaciones para exportar'))
    const { user } = montar()
    await screen.findAllByText('Nombre1 Apellido1')

    await user.click(screen.getByRole('button', { name: 'Exportar a PDF' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(descargar).not.toHaveBeenCalled()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Exportar a PDF' })).toBeEnabled())
  })

  it('con la lista vacía el botón de exportar está deshabilitado', async () => {
    responder({ filas: [], resumen: { totalCalificadas: 5, totalPendientes: 0 }, meta: { totalItems: 0, totalPages: 0 } })
    montar()

    expect(await screen.findByRole('button', { name: 'Exportar a PDF' })).toBeDisabled()
  })
})
