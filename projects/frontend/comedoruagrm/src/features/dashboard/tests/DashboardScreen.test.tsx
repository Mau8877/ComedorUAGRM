import { screen, waitFor, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./simulados')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./simulados')
  return { etiquetaRol: sim.etiquetaRolSimulada, useCerrarSesion: () => ({ mutate: sim.cerrarSesion }) }
})
// El menú de cuenta (CU15) navega con el router; acá no se prueba, así que se simula.
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  DirectivoLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@/components/ui/toast', async () => (await import('./simulados')).avisos)
vi.mock('@/utils/descargarArchivo', async () => ({ descargarArchivo: (await import('./simulados')).descargar }))

import { ApiError } from '@/store'

import {
  cliente,
  consultasDeIndicadores,
  indicadores,
  montar,
  reiniciarCliente,
  responder,
  ultimaConsulta,
} from './montarPantalla'
import { avisos, descargar } from './simulados'

async function elegir(user: ReturnType<typeof montar>['user'], combo: string, opcion: string) {
  await user.click(screen.getByRole('combobox', { name: combo }))
  await user.click(await screen.findByRole('option', { name: opcion }))
}

describe('DashboardScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    descargar.mockReset()
    Object.values(avisos).forEach((fn) => fn.mockReset())
  })

  it('muestra los totales con separador de miles, la gestión y la fecha de actualización', async () => {
    responder()
    montar()

    expect(await screen.findByText('5.230')).toBeInTheDocument()
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('Gestión 2026-II · actualizado el 13/11/2026 02:00')).toBeInTheDocument()
    expect(screen.getByText('Postulaciones por mes')).toBeInTheDocument()
    expect(screen.getByText('Asistencia por mes')).toBeInTheDocument()
    // Sin filtros aplicados el backend elige la gestión vigente.
    expect(ultimaConsulta()).toEqual({})
  })

  it('con el lote sin ejecutar avisa que aún no se calculan indicadores (A2)', async () => {
    responder({ indicadores: indicadores({ actualizadoEn: null }) })
    montar()

    expect(await screen.findByText('Aún no se calculan indicadores')).toBeInTheDocument()
  })

  it('sin datos muestra el mensaje y ceros, y no deja descargar (A1)', async () => {
    responder({
      indicadores: indicadores({
        sinDatos: true,
        totales: { postulacionesRecibidas: 0, finalistas: 0, becasAdjudicadas: 0, asistenciaPromedio: null },
        porMes: [],
      }),
    })
    montar()

    expect(await screen.findByText('No hay datos para la selección elegida todavía.')).toBeInTheDocument()
    for (const titulo of ['Postulaciones recibidas', 'Finalistas', 'Becas adjudicadas']) {
      const tarjeta = screen.getByText(titulo).closest('[data-slot="card"]') as HTMLElement
      expect(within(tarjeta).getByText('0')).toBeInTheDocument()
    }
    expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeDisabled()
  })

  it('sin gestiones lo dice y no rompe', async () => {
    responder({
      filtros: { gestiones: [] },
      indicadores: indicadores({ gestion: null, sinDatos: true, actualizadoEn: null, porMes: [] }),
    })
    montar()

    expect(await screen.findByText('Todavía no hay gestiones académicas registradas.')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Gestión' })).toBeDisabled()
  })

  it('la asistencia muestra «Sin datos» mientras llegue null', async () => {
    responder()
    montar()

    const tarjeta = (await screen.findByText('Asistencia promedio')).closest('[data-slot="card"]') as HTMLElement
    expect(within(tarjeta).getByText('Sin datos')).toBeInTheDocument()
    // El gráfico de asistencia también dice «Sin datos».
    expect(screen.getAllByText('Sin datos')).toHaveLength(2)
  })

  it('con asistencia real muestra el porcentaje con un decimal como máximo', async () => {
    responder({
      indicadores: indicadores({
        totales: { postulacionesRecibidas: 10, finalistas: 2, becasAdjudicadas: 1, asistenciaPromedio: 87.54 },
        porMes: [{ mes: '2026-10', postulaciones: 10, finalistas: 2, adjudicadas: 1, asistencia: 87.54 }],
      }),
    })
    montar()

    expect(await screen.findByText('87,5 %')).toBeInTheDocument()
  })

  it('«Aplicar» cambia la consulta; editar los selectores no la cambia', async () => {
    responder({
      porSeleccion: (s) => (s['filter[convocatoria]'] ? indicadores({ convocatoria: { codigo: 'c-a', nombre: 'Beca A' } }) : indicadores()),
    })
    const { user } = montar()
    await screen.findByText('5.230')
    const antes = consultasDeIndicadores()

    await elegir(user, 'Convocatoria', 'Beca A')
    expect(consultasDeIndicadores()).toBe(antes)

    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ 'filter[gestion]': 'g-2', 'filter[convocatoria]': 'c-a' }))
  })

  it('el selector de convocatoria ofrece solo las de la gestión elegida, y cambiar la gestión lo reinicia a «Todas»', async () => {
    responder()
    const { user } = montar()
    await screen.findByText('5.230')

    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    expect(await screen.findByRole('option', { name: 'Beca A' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Beca B' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Beca Z' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('option', { name: 'Beca A' }))
    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Beca A')

    await elegir(user, 'Gestión', '2026-I')

    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Todas las convocatorias')
    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    expect(await screen.findByRole('option', { name: 'Beca Z' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Beca A' })).not.toBeInTheDocument()
  })

  it('un error de red muestra su mensaje y permite reintentar', async () => {
    responder({ porSeleccion: () => new AxiosError('Network Error') })
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('No se pudo conectar con el servidor')
    responder()
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('5.230')).toBeInTheDocument()
  })

  it('con ERR_DAS_01 vuelve a la selección vigente y avisa', async () => {
    responder({
      porSeleccion: (s) => (s['filter[convocatoria]'] ? new ApiError('ERR_DAS_01', 'x') : indicadores()),
    })
    const { user } = montar()
    await screen.findByText('5.230')
    await elegir(user, 'Convocatoria', 'Beca A')

    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    await waitFor(() => expect(avisos.toastWarning).toHaveBeenCalledWith('Selección no disponible', expect.any(String)))
    await waitFor(() => expect(ultimaConsulta()).toEqual({}))
    expect(screen.getByText('5.230')).toBeInTheDocument()
  })

  it('muestra esqueletos mientras carga', async () => {
    cliente.get.mockImplementation(() => new Promise(() => {}))
    montar()

    expect(await screen.findByLabelText('Cargando indicadores')).toBeInTheDocument()
  })
})
