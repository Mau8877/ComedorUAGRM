import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  EstudianteLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@tanstack/react-router', async (original) => {
  const sim = await import('./clienteSimulado')
  return { ...(await original<object>()), useNavigate: () => sim.navegar }
})

import { ApiError } from '@/store'

import { MisPostulacionesScreen } from '../screens/MisPostulacionesScreen'
import type { MiPostulacionResumen } from '../types'

import { cliente, navegar } from './clienteSimulado'
import { resumen, yo } from './datos'

function responder(filas: MiPostulacionResumen[], meta: Record<string, number> = {}) {
  cliente.get.mockResolvedValue({
    data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1, ...meta } },
  })
}

function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/postulaciones/mias')
  return llamadas[llamadas.length - 1][1].params
}

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <MisPostulacionesScreen titulo="Mis postulaciones" activeHref="/mis-postulaciones" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { user: userEvent.setup() }
}

describe('MisPostulacionesScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    navegar.mockReset()
  })

  it('muestra las postulaciones con su gestión, fecha de envío y estado visible', async () => {
    responder([
      resumen(1, { estado: 'calificada' }),
      resumen(2, { estado: 'no_adjudicada', fechaEnvio: '2026-10-01T15:00:00Z' }),
    ])
    montar()

    expect((await screen.findAllByText('Beca 1')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('Beca 2').length).toBeGreaterThan(0)
    expect(screen.getAllByText('2026-1').length).toBeGreaterThan(0)
    expect(screen.getAllByText('13/11/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('01/10/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Calificada').length).toBeGreaterThan(0)
    expect(screen.getAllByText('No adjudicada').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Mis postulaciones' })).toBeInTheDocument()
  })

  it('un badge por estado, con su texto', async () => {
    responder([
      resumen(1, { estado: 'pendiente' }),
      resumen(2, { estado: 'devuelta' }),
      resumen(3, { estado: 'archivada' }),
      resumen(4, { estado: 'adjudicada' }),
    ])
    montar()
    await screen.findAllByText('Beca 1')

    for (const texto of ['Pendiente', 'Devuelta', 'Archivada', 'Adjudicada']) {
      expect(screen.getAllByText(texto).length).toBeGreaterThan(0)
    }
  })

  it('la primera consulta es la página 1 de 20 sin filtro ni orden', async () => {
    responder([resumen(1)])
    montar()
    await screen.findAllByText('Beca 1')

    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('«Ver detalle» navega al detalle de esa postulación', async () => {
    responder([resumen(1), resumen(2)])
    const { user } = montar()
    await screen.findAllByText('Beca 2')

    await user.click(screen.getAllByRole('button', { name: 'Ver detalle' })[1])

    expect(navegar).toHaveBeenCalledWith({ to: '/mis-postulaciones/$codigo', params: { codigo: 'post-2' } })
  })

  it('ordenar por fecha de envío manda sort con el único campo que el backend documenta', async () => {
    responder([resumen(1)])
    const { user } = montar()
    await screen.findAllByText('Beca 1')

    await user.click(screen.getByRole('button', { name: /Fecha de envío/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('fechaEnvio'))
    await user.click(screen.getByRole('button', { name: /Fecha de envío/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-fechaEnvio'))
    expect(screen.queryByRole('button', { name: /Estado/ })).not.toBeInTheDocument()
  })

  it('la paginación manda la página pedida', async () => {
    responder([resumen(1)], { totalItems: 60, totalPages: 3 })
    const { user } = montar()
    await screen.findAllByText('Beca 1')

    await user.click(screen.getByRole('button', { name: 'Página 2' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ page: 2, pageSize: 20 }))
  })

  it('el filtro por gestión repite la consulta con el parámetro y vuelve a la página 1', async () => {
    responder([resumen(1), resumen(2)], { totalItems: 60, totalPages: 3 })
    const { user } = montar()
    await screen.findAllByText('Beca 1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Gestión' }))
    await user.click(await screen.findByRole('option', { name: '2026-2' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20, 'filter[gestionAcademica]': 'gestion-2' }))
  })

  it('al filtrar, las demás gestiones siguen disponibles para volver a elegirlas', async () => {
    responder([resumen(1), resumen(2)])
    const { user } = montar()
    await screen.findAllByText('Beca 1')
    await user.click(screen.getByRole('combobox', { name: 'Gestión' }))
    await user.click(await screen.findByRole('option', { name: '2026-2' }))
    responder([resumen(2)])
    await waitFor(() => expect(ultimaConsulta()['filter[gestionAcademica]']).toBe('gestion-2'))

    await user.click(screen.getByRole('combobox', { name: 'Gestión' }))

    expect(await screen.findByRole('option', { name: '2026-1' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: '2026-2' })).toBeInTheDocument()
  })

  it('sin postulaciones muestra el mensaje informativo y no invita a postular mientras no exista el CU-02', async () => {
    responder([], { totalItems: 0, totalPages: 0 })
    montar()

    expect(await screen.findByText('Todavía no enviaste ninguna postulación')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Postular' })).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('un filtro sin resultados muestra el vacío de la tabla, no el mensaje de «ninguna postulación»', async () => {
    responder([resumen(1)])
    const { user } = montar()
    await screen.findAllByText('Beca 1')
    responder([], { totalItems: 0, totalPages: 0 })

    await user.click(screen.getByRole('combobox', { name: 'Gestión' }))
    await user.click(await screen.findByRole('option', { name: '2026-1' }))

    expect((await screen.findAllByText('No hay postulaciones en esa gestión')).length).toBeGreaterThan(0)
    expect(screen.queryByText('Todavía no enviaste ninguna postulación')).not.toBeInTheDocument()
  })

  it('un error muestra el mensaje con «Reintentar», que vuelve a pedir', async () => {
    responder([resumen(1)])
    // Solo la primera consulta falla; el reintento ya recibe las postulaciones.
    cliente.get.mockRejectedValueOnce(new ApiError('ERR_SYS_05', 'No se puede ordenar por «ci»'))
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText('No se puede ordenar por «ci»')).toBeInTheDocument()
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect((await screen.findAllByText('Beca 1')).length).toBeGreaterThan(0)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
