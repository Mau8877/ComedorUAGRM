import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
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
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, ...resto }: { to: string; children?: ReactNode }) => (
    <a href={to} {...resto}>
      {children}
    </a>
  ),
}))

import { ApiError } from '@/store'

import { PrecalificacionScreen } from '../screens/PrecalificacionScreen'
import type { Precalificacion } from '../types'
import { cliente } from './clienteSimulado'
import { yo } from './montar'

const CODIGO = 'po-1'

function precalificacion(extra: Partial<Precalificacion> = {}): Precalificacion {
  return {
    postulacion: CODIGO,
    nombres: 'Ana',
    apellidos: 'Rojas',
    ci: '9802041',
    convocatoria: 'Beca Comedor 2026-II',
    estado: 'calificada',
    puntaje: 59.5,
    puntajeMaximo: 70,
    modelo: 'v1.0',
    fechaCalculo: '2026-07-16T14:42:00Z',
    posicion: 5,
    variables: [
      { clave: 'ingresos_familiares', nivel: 1, ponderacion: 28, puntos: 28, maximo: 28 },
      { clave: 'promedio_academico', nivel: 0.7, ponderacion: 0, puntos: 0, maximo: 0 },
      { clave: 'discapacidad', nivel: null, ponderacion: 10, puntos: 0, maximo: null },
    ],
    advertencias: ['sin dato (no entran al puntaje): discapacidad'],
    marcadaParaRevision: false,
    revisionDesde: null,
    intentos: 0,
    traza: [
      { paso: 'Postulación enviada', detalle: null, fecha: '2026-07-16T14:41:00Z' },
      { paso: 'Ubicación en la lista priorizada', detalle: 'Posición 5', fecha: null },
    ],
    ...extra,
  }
}

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <PrecalificacionScreen titulo="Precalificación con IA" activeHref="/lista-priorizada" usuarioActual={yo} codigo={CODIGO} />
    </QueryClientProvider>,
  )
}

describe('PrecalificacionScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
  })

  it('pide la precalificación por el código de la postulación', async () => {
    cliente.get.mockResolvedValue({ data: precalificacion() })
    montar()

    await screen.findByText('Ana Rojas')
    expect(cliente.get).toHaveBeenCalledWith('/postulaciones/po-1/precalificacion')
  })

  it('muestra el puntaje sobre el máximo, el modelo, la posición y la fecha en hora de La Paz', async () => {
    cliente.get.mockResolvedValue({ data: precalificacion() })
    montar()

    await screen.findByText('Ana Rojas')
    expect(screen.getByRole('img', { name: 'Puntaje 59.5 de 70' })).toBeInTheDocument()
    expect(screen.getByText('v1.0')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByText('16/07/2026 10:42')).toBeInTheDocument()
    expect(screen.getByText('CI 9802041 · Beca Comedor 2026-II')).toBeInTheDocument()
    expect(screen.getByText('Calificada')).toBeInTheDocument()
  })

  it('muestra el aporte de cada variable con su nombre, y distingue la que no tiene dato y la que no pondera', async () => {
    cliente.get.mockResolvedValue({ data: precalificacion() })
    montar()

    await screen.findByText('Ana Rojas')
    const barras = screen.getAllByRole('progressbar')
    expect(barras).toHaveLength(3)
    expect(screen.getByText('28.0 / 28.0')).toBeInTheDocument()
    expect(screen.getByText('No pondera')).toBeInTheDocument()
    expect(screen.getByText('Sin dato')).toBeInTheDocument()
    expect(screen.getByText('Ingresos familiares')).toBeInTheDocument()
    expect(screen.getByText('Promedio académico')).toBeInTheDocument()
    expect(screen.getByText('sin dato (no entran al puntaje): discapacidad')).toBeInTheDocument()
  })

  it('muestra la traza del proceso, con guion cuando un paso no tiene fecha', async () => {
    cliente.get.mockResolvedValue({ data: precalificacion() })
    montar()

    await screen.findByText('Ana Rojas')
    const tabla = screen.getByRole('table')
    expect(within(tabla).getByText('Postulación enviada')).toBeInTheDocument()
    expect(within(tabla).getByText('Posición 5')).toBeInTheDocument()
    expect(within(tabla).getByText('16/07/2026 10:41')).toBeInTheDocument()
    expect(within(tabla).getAllByText('—').length).toBeGreaterThanOrEqual(2)
  })

  it('una postulación sin puntaje dice que todavía no lo tiene y no dibuja el anillo', async () => {
    cliente.get.mockResolvedValue({
      data: precalificacion({
        estado: 'pendiente',
        puntaje: null,
        puntajeMaximo: null,
        modelo: null,
        fechaCalculo: null,
        posicion: null,
        variables: [],
        advertencias: [],
      }),
    })
    montar()

    await screen.findByText('Ana Rojas')
    expect(screen.getByText('Todavía no tiene puntaje calculado.')).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: /Puntaje/ })).not.toBeInTheDocument()
    expect(screen.getByText('Esta precalificación no guardó el desglose por variable.')).toBeInTheDocument()
  })

  it('una postulación marcada para revisión lo avisa con los intentos', async () => {
    cliente.get.mockResolvedValue({
      data: precalificacion({
        estado: 'pendiente',
        puntaje: null,
        puntajeMaximo: null,
        variables: [],
        marcadaParaRevision: true,
        revisionDesde: '2026-07-17T12:00:00Z',
        intentos: 3,
      }),
    })
    montar()

    await screen.findByText('Ana Rojas')
    expect(screen.getByText('Marcada para revisión')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('después de 3 intentos')
  })

  it('con una postulación inexistente muestra el mensaje del error', async () => {
    cliente.get.mockRejectedValue(new ApiError('ERR_MOD_08', 'La postulación no existe'))
    montar()

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('La postulación no existe.'))
    expect(screen.queryByText('Ana Rojas')).not.toBeInTheDocument()
  })

  it('tiene el enlace para volver a la lista priorizada', async () => {
    cliente.get.mockResolvedValue({ data: precalificacion() })
    montar()

    await screen.findByText('Ana Rojas')
    expect(screen.getByText(/Volver a la lista priorizada/).closest('a')).toHaveAttribute('href', '/lista-priorizada')
  })
})
