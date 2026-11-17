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
vi.mock('@tanstack/react-router', async () => {
  const sim = await import('./clienteSimulado')
  return { useNavigate: () => sim.irA }
})
vi.mock('@/layouts', () => ({
  EstudianteLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { irA } from './clienteSimulado'
import {
  asistencia,
  beca,
  cliente,
  consultasHistorial,
  montar,
  reiniciarCliente,
  responder,
  ultimaConsulta,
} from './montar'

describe('MiBecaScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    irA.mockReset()
  })
  afterEach(() => vi.restoreAllMocks())

  it('muestra el estado de la beca, la convocatoria, la gestión y el total de asistencias', async () => {
    responder()
    montar()

    expect(await screen.findByText('Vigente')).toBeInTheDocument()
    expect(screen.getByText(/Convocatoria 2026-II · Gestión 2026-II/)).toBeInTheDocument()
    expect(screen.getByText('28/08/2026')).toBeInTheDocument()
    expect(screen.getByText('01/09/2026')).toBeInTheDocument()
    expect(screen.getByText('13')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Mi beca' })).toBeInTheDocument()
    expect(screen.queryByText(/Motivo:/)).not.toBeInTheDocument()
  })

  it('con la beca dada de baja muestra el estado y el motivo, y el historial sigue disponible', async () => {
    responder({ beca: beca({ estado: 'BAJA', motivo: 'Abandonó la universidad' }) })
    montar()

    expect(await screen.findByText('Dada de baja')).toBeInTheDocument()
    expect(screen.getByText('Abandonó la universidad')).toBeInTheDocument()
    expect((await screen.findAllByText('18/09/2026')).length).toBeGreaterThan(0)
  })

  it('lista las asistencias con su fecha y la hora en La Paz', async () => {
    responder()
    montar()

    expect((await screen.findAllByText('18/09/2026')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('17/09/2026').length).toBeGreaterThan(0)
    // 16:30 UTC son las 12:30 en La Paz.
    expect(screen.getAllByText('12:30').length).toBeGreaterThan(0)
  })

  it('es de solo lectura: no ofrece ninguna acción de edición', async () => {
    responder()
    montar()
    await screen.findAllByText('18/09/2026')

    for (const nombre of [/editar/i, /eliminar/i, /registrar/i, /modificar/i]) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument()
    }
  })

  it('sin asistencias muestra un mensaje informativo', async () => {
    responder({ filas: [], beca: beca({ totalAsistencias: 0 }) })
    montar()

    expect((await screen.findAllByText('Todavía no hay asistencias registradas')).length).toBeGreaterThan(0)
  })

  it('filtra por rango de fechas mandando filter[desde] y filter[hasta]', async () => {
    responder()
    montar()
    await screen.findAllByText('18/09/2026')

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-02' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-30' } })

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ page: 1 }))
    await waitFor(() => {
      const consulta = ultimaConsulta()
      expect(consulta.desde).toBeUndefined()
      expect(consulta['filter[desde]']).toBe('2026-09-02')
      expect(consulta['filter[hasta]']).toBe('2026-09-30')
    })
  })

  it('con el rango invertido avisa y no consulta al backend con él', async () => {
    responder()
    montar()
    await screen.findAllByText('18/09/2026')
    const antes = consultasHistorial()

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-30' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-01' } })

    expect(await screen.findByText('La fecha «desde» no puede ser posterior a «hasta»')).toBeInTheDocument()
    await waitFor(() => expect(consultasHistorial()).toBeGreaterThanOrEqual(antes))
    const consulta = ultimaConsulta()
    expect(consulta['filter[desde]']).toBeUndefined()
    expect(consulta['filter[hasta]']).toBeUndefined()
  })

  it('con un rango sin resultados explica que no hay asistencias en ese rango', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('18/09/2026')
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/mi-beca'
        ? Promise.resolve({ data: beca() })
        : Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } }),
    )

    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2027-01-01' } })

    expect((await screen.findAllByText('No hay asistencias en ese rango de fechas')).length).toBeGreaterThan(0)
    expect(user).toBeDefined()
  })

  it('sin beca (ERR_MBE_01) avisa y lleva a «Mis postulaciones»', async () => {
    responder({ beca: null })
    const { user } = montar()

    expect(await screen.findByText('No cuentas con una beca')).toBeInTheDocument()
    expect(screen.queryByText('Historial de asistencia')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ir a Mis postulaciones' }))
    expect(irA).toHaveBeenCalledWith({ to: '/mis-postulaciones' })
  })

  it('si falla la consulta muestra el error y permite reintentar', async () => {
    cliente.get.mockRejectedValue(new ApiError('ERR_SYS_00', 'Ocurrió un error inesperado'))
    const { user } = montar()

    expect(await screen.findByText('Ocurrió un error inesperado')).toBeInTheDocument()
    cliente.get.mockReset()
    responder()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Vigente')).toBeInTheDocument()
  })

  it('no muestra ids internos ni el código de la beca', async () => {
    responder({ filas: [asistencia(18)] })
    montar()
    await screen.findAllByText('18/09/2026')

    expect(screen.queryByText('beca-1')).not.toBeInTheDocument()
  })
})
