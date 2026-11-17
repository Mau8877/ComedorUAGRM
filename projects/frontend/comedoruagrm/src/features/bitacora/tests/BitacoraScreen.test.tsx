import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./clienteSimulado')
  return {
    etiquetaRol: sim.etiquetaRolSimulada,
    useCerrarSesion: () => ({ mutate: sim.cerrarSesion }),
  }
})
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

import { cliente, consultas, montar, registro, reiniciarCliente, responder, ultimaConsulta } from './montar'

describe('BitacoraScreen', () => {
  beforeEach(() => reiniciarCliente())
  afterEach(() => vi.restoreAllMocks())

  it('muestra las acciones con su usuario, entidad e IP, y la fecha en hora de La Paz', async () => {
    responder({ filas: [registro(1), registro(2, { usuario: null, entidad: null, ip: null })] })
    montar()

    expect((await screen.findAllByText('ACCION_1')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ana1 Pérez').length).toBeGreaterThan(0)
    expect(screen.getAllByText('adjudicacion').length).toBeGreaterThan(0)
    expect(screen.getAllByText('10.0.3.21').length).toBeGreaterThan(0)
    // 30/09/2026 14:42 UTC son las 10:42 en La Paz.
    expect(screen.getAllByText(/30\/09\/2026.*10:42:05/).length).toBeGreaterThan(0)
    // Lo hecho por el sistema no tiene usuario.
    expect(screen.getAllByText('Sistema').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Bitácora' })).toBeInTheDocument()
  })

  it('es de solo lectura: no ofrece ninguna acción de fila', async () => {
    responder()
    montar()
    await screen.findAllByText('ACCION_1')

    for (const nombre of [/editar/i, /eliminar/i, /borrar/i, /modificar/i]) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument()
    }
  })

  it('no muestra ids internos ni el detalle crudo', async () => {
    responder()
    montar()
    await screen.findAllByText('ACCION_1')

    expect(document.body.textContent).not.toMatch(/#\d+/)
    expect(document.body.textContent).not.toContain('abc')
  })

  it('la primera consulta es la página 1 de 20 sin filtros ni orden', async () => {
    responder()
    montar()
    await screen.findAllByText('ACCION_1')

    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('ordena solo por fecha, acción y entidad', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('ACCION_1')

    await user.click(screen.getByRole('button', { name: /Fecha y hora/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('fecha'))
    await user.click(screen.getByRole('button', { name: /Fecha y hora/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-fecha'))
    await user.click(screen.getByRole('button', { name: /Acción/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('accion'))
    expect(screen.queryByRole('button', { name: /Usuario/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^IP/ })).not.toBeInTheDocument()
  })

  it('la búsqueda por usuario manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('ACCION_1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = consultas()

    await user.type(screen.getByPlaceholderText('Usuario (nombre o correo)...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
    expect(consultas() - antes).toBe(1)
  })

  it('las fechas viajan como filter[desde] y filter[hasta]', async () => {
    responder()
    montar()
    await screen.findAllByText('ACCION_1')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-01' } })
    await waitFor(() => expect(ultimaConsulta()['filter[desde]']).toBe('2026-09-01'))
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-30' } })
    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ 'filter[desde]': '2026-09-01', 'filter[hasta]': '2026-09-30' }))

    expect(cliente.get).toHaveBeenCalledWith('/bitacora', {
      params: expect.objectContaining({ 'filter[desde]': '2026-09-01', 'filter[hasta]': '2026-09-30' }),
    })
  })

  it('con «desde» posterior a «hasta» avisa y no manda ese rango', async () => {
    responder()
    montar()
    await screen.findAllByText('ACCION_1')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-30' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-01' } })

    expect(await screen.findByText(/no puede ser posterior/)).toBeInTheDocument()
    const ultima = cliente.get.mock.calls.filter((c) => c[0] === '/bitacora').pop()
    expect(ultima?.[1].params['filter[desde]']).toBeUndefined()
    expect(ultima?.[1].params['filter[hasta]']).toBeUndefined()
  })

  it('sin resultados muestra «Sin resultados»', async () => {
    responder({ filas: [] })
    montar()

    expect((await screen.findAllByText('Sin resultados')).length).toBeGreaterThan(0)
  })

  it('si falla la consulta muestra el mensaje del backend y permite reintentar', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/bitacora'
        ? Promise.reject(new ApiError('ERR_AUTH_07', 'No tenés permiso para esta acción'))
        : Promise.resolve({ data: [] }),
    )
    const { user } = montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('No tenés permiso para esta acción')
    const antes = consultas()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(consultas()).toBeGreaterThan(antes))
  })
})
