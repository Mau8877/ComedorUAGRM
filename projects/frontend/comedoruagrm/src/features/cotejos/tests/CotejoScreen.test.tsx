import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

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
// El selector de convocatorias viene del CU13 y el de planillas del CU09; acá solo importa lo que ofrecen.
vi.mock('@/features/convocatorias', async () => {
  const sim = await import('./clienteSimulado')
  return {
    useConvocatorias: () => ({
      data: { data: sim.convocatoriasPublicadas.data },
      isLoading: false,
      isSuccess: true,
      isError: false,
      error: null,
      refetch: vi.fn(),
    }),
  }
})
vi.mock('@/features/planillas', async () => {
  const sim = await import('./clienteSimulado')
  return {
    usePlanillas: (convocatoria?: string) => ({
      data: convocatoria ? { data: sim.planillasDelSelector.data } : undefined,
      isLoading: false,
      isSuccess: Boolean(convocatoria),
      isError: false,
      error: null,
      refetch: vi.fn(),
    }),
  }
})
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { convocatoriasPublicadas, planillasDelSelector } from './clienteSimulado'
import {
  cliente,
  cotejo,
  datos,
  montar,
  PLANILLA,
  reiniciarCliente,
  responder,
  ultimaConsulta,
} from './montar'

function escribir(etiqueta: string, valor: string) {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } })
}

function guardar() {
  fireEvent.click(screen.getByRole('button', { name: 'Guardar cotejo' }))
}

describe('CotejoScreen', () => {
  beforeEach(() => reiniciarCliente())

  it('muestra el resumen de la planilla, sus registros digitales y los cotejos ya registrados', async () => {
    responder()
    montar()

    expect(await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')).toBeInTheDocument()
    const resumen = screen.getByRole('group', { name: 'Resumen de la planilla' })
    expect(resumen).toHaveTextContent('880')
    expect(resumen).toHaveTextContent('861')
    expect(resumen).toHaveTextContent('700')
    expect((await screen.findAllByText('Apellido1 Nombre1')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('05/09/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Con discrepancias').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Faltan firmas del 05/09').length).toBeGreaterThan(0)
    expect(ultimaConsulta(`/planillas/${PLANILLA}/cotejos/registros`)).toEqual({ page: 1, pageSize: 20 })
  })

  it('sin registros digitales lo informa y deja registrar el cotejo igual (A1)', async () => {
    responder({ datos: datos({ registrosDigitales: 0, becadosConAsistencia: 0 }), registros: [] })
    montar()

    expect(await screen.findByRole('status')).toHaveTextContent(/Igual puedes registrar el cotejo/)
    expect(screen.getByRole('button', { name: 'Guardar cotejo' })).toBeEnabled()
  })

  it('sin convocatorias con resultados publicados lo informa y no muestra el formulario', async () => {
    responder()
    convocatoriasPublicadas.data = []
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias con resultados publicados')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar cotejo' })).not.toBeInTheDocument()
  })

  it('una convocatoria sin planillas lo informa y apunta a «Planillas»', async () => {
    responder()
    planillasDelSelector.data = []
    montar()

    expect(await screen.findByText(/todavía no tiene planillas generadas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar cotejo' })).not.toBeInTheDocument()
  })

  it('deduce las discrepancias y el resultado mientras se escribe, y las observaciones pasan a obligatorias', async () => {
    responder()
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    expect(screen.getByTestId('resultado')).toHaveTextContent('—')
    escribir('Total de registros', '880')
    escribir('Coincidencias', '880')
    expect(screen.getByTestId('discrepancias')).toHaveTextContent('0')
    expect(screen.getByTestId('resultado')).toHaveTextContent('Conforme')

    escribir('Coincidencias', '861')
    expect(screen.getByTestId('discrepancias')).toHaveTextContent('19')
    expect(screen.getByTestId('resultado')).toHaveTextContent('Con discrepancias')
    expect(screen.getByLabelText(/Observaciones \(obligatorias\)/)).toBeInTheDocument()
  })

  it('con discrepancias sin observaciones no llama al backend y lo avisa (A2)', async () => {
    responder()
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    escribir('Total de registros', '880')
    escribir('Coincidencias', '861')
    guardar()

    expect(await screen.findByText('Con discrepancias, las observaciones son obligatorias')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('sin datos pide el total y las coincidencias y no llama al backend', async () => {
    responder()
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    guardar()

    expect(await screen.findByText('El total de registros es obligatorio')).toBeInTheDocument()
    expect(screen.getByText('Las coincidencias son obligatorias')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('registra un cotejo con discrepancias: manda lo deducido, limpia el formulario y refresca el historial', async () => {
    responder()
    cliente.post.mockResolvedValue({ data: cotejo(2) })
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    escribir('Total de registros', '880')
    escribir('Coincidencias', '861')
    fireEvent.change(screen.getByLabelText(/Observaciones/), { target: { value: '  Faltan firmas del 05/09  ' } })
    guardar()

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith(`/planillas/${PLANILLA}/cotejos`, {
        totalRegistros: 880,
        coincidencias: 861,
        discrepancias: 19,
        resultado: 'CON_DISCREPANCIAS',
        observaciones: 'Faltan firmas del 05/09',
      }),
    )
    await waitFor(() => expect(screen.getByLabelText('Total de registros')).toHaveValue(null))
    const llamadasAlHistorial = cliente.get.mock.calls.filter((c) => c[0] === `/planillas/${PLANILLA}/cotejos`)
    await waitFor(() => expect(llamadasAlHistorial.length).toBeGreaterThanOrEqual(1))
  })

  it('registra un cotejo conforme sin observaciones', async () => {
    responder()
    cliente.post.mockResolvedValue({ data: cotejo(3, { resultado: 'CONFORME', discrepancias: 0, observaciones: null }) })
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    escribir('Total de registros', '10')
    escribir('Coincidencias', '10')
    guardar()

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith(`/planillas/${PLANILLA}/cotejos`, {
        totalRegistros: 10,
        coincidencias: 10,
        discrepancias: 0,
        resultado: 'CONFORME',
      }),
    )
  })

  it('muestra el error del servidor y conserva lo escrito', async () => {
    responder()
    cliente.post.mockRejectedValue(new ApiError('ERR_COT_02', 'x'))
    montar()
    await screen.findByText('Planilla del periodo 01/09/2026 – 30/09/2026')

    escribir('Total de registros', '10')
    escribir('Coincidencias', '10')
    guardar()

    expect(await screen.findByText('Las coincidencias y las discrepancias deben sumar el total de registros.')).toBeInTheDocument()
    expect(screen.getByLabelText('Total de registros')).toHaveValue(10)
  })

  it('sin historial dice que la planilla todavía no fue cotejada', async () => {
    responder({ cotejos: [] })
    montar()

    expect((await screen.findAllByText('Esta planilla todavía no fue cotejada')).length).toBeGreaterThan(0)
  })

  it('un error al cargar los datos se avisa con «Reintentar»', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string) =>
      ruta.endsWith('/cotejos/datos') ? Promise.reject(new ApiError('ERR_SYS_00', 'x')) : base(ruta),
    )
    montar()

    expect(await screen.findByText('Ocurrió un error en el servidor. Inténtalo de nuevo en unos minutos.')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Reintentar' }).length).toBeGreaterThan(0)
  })
})
