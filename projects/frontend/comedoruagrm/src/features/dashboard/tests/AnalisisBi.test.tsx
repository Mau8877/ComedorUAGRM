import { screen, waitFor, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./simulados')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./simulados')
  return {
    etiquetaRol: sim.etiquetaRolSimulada,
    useCerrarSesion: () => ({ mutate: sim.cerrarSesion }),
  }
})
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
vi.mock('@/utils/descargarArchivo', async () => ({
  descargarArchivo: (await import('./simulados')).descargar,
}))

import {
  biConDatos,
  biVacio,
  cliente,
  montar,
  reiniciarCliente,
  responder,
  ultimaConsulta,
} from './montarPantalla'
import { avisos, descargar } from './simulados'

describe('Análisis BI del dashboard', () => {
  beforeEach(() => {
    reiniciarCliente()
    descargar.mockReset()
    Object.values(avisos).forEach((fn) => fn.mockReset())
  })

  it('muestra el embudo, la cobertura y las becas en riesgo', async () => {
    responder({ bi: biConDatos() })
    montar()

    expect(await screen.findByText('Embudo del proceso')).toBeInTheDocument()
    expect(screen.getByText('Precalificadas por la IA')).toBeInTheDocument()
    expect(screen.getByText('Cobertura de cupos')).toBeInTheDocument()
    expect(screen.getByText('10 becas vigentes de 30 cupos')).toBeInTheDocument()
    expect(screen.getByText('2 becas en riesgo (menos de 60 % de asistencia).')).toBeInTheDocument()
    expect(
      screen.getByText('Gestión 2026-II: +30,8 % de postulaciones frente a la anterior')
    ).toBeInTheDocument()
  })

  it('sin datos de BI no muestra la sección de análisis', async () => {
    responder({ bi: biVacio() })
    montar()

    expect(await screen.findByText('5.230')).toBeInTheDocument()
    expect(screen.queryByText('Embudo del proceso')).not.toBeInTheDocument()
  })

  it('al elegir una facultad muestra sus carreras, y al elegirla otra vez las oculta', async () => {
    responder({ bi: biConDatos() })
    const { user } = montar()

    await user.click(await screen.findByRole('button', { name: /Facultad de Ingeniería/ }))
    const detalle = screen.getByLabelText('Carreras de Facultad de Ingeniería')
    expect(within(detalle).getByText('Informática')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Facultad de Ingeniería/ }))
    expect(screen.queryByLabelText('Carreras de Facultad de Ingeniería')).not.toBeInTheDocument()
  })

  it('la comparación marca la variación y elegir una convocatoria filtra todo el dashboard por ella', async () => {
    responder({ bi: biConDatos() })
    const { user } = montar()

    expect(await screen.findByText('-25,7 %')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Beca B' }))

    await waitFor(() =>
      expect(ultimaConsulta()).toEqual({ 'filter[gestion]': 'g-2', 'filter[convocatoria]': 'c-b' })
    )
  })

  it('«Descargar Excel» pide el archivo con la selección aplicada y avisa', async () => {
    responder({ bi: biConDatos() })
    cliente.get.mockImplementationOnce(cliente.get.getMockImplementation()!)
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/bi/excel'
        ? Promise.resolve({
            data: new Blob(['x']),
            headers: { 'content-disposition': 'attachment; filename="bi-2026-II-20261118.xlsx"' },
          })
        : base(ruta, config)
    )
    const { user } = montar()

    await user.click(await screen.findByRole('button', { name: /Descargar Excel/ }))

    await waitFor(() =>
      expect(descargar).toHaveBeenCalledWith(expect.any(Blob), 'bi-2026-II-20261118.xlsx')
    )
    expect(avisos.toastSuccess).toHaveBeenCalled()
    expect(cliente.get).toHaveBeenCalledWith(
      '/dashboard/bi/excel',
      expect.objectContaining({ responseType: 'blob' })
    )
  })

  it('si el análisis falla ofrece reintentar sin perder los indicadores', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/bi' ? Promise.reject(new Error('caído')) : base(ruta, config)
    )
    montar()

    expect(await screen.findByText('5.230')).toBeInTheDocument()
    expect(await screen.findByText('No se pudo cargar el análisis BI.')).toBeInTheDocument()
  })
})
