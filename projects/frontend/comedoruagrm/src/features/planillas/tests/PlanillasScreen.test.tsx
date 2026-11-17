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
    AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
  }
})
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
// El selector de convocatorias viene del CU13; acá solo importa lo que ofrece.
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
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { convocatoriasPublicadas } from './clienteSimulado'
import { cliente, CONVOCATORIA, montar, planilla, reiniciarCliente, responder, resumen, ultimaConsulta } from './montar'

function escribir(etiqueta: string, valor: string) {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } })
}

function periodo(desde: string, hasta: string) {
  escribir('Desde', desde)
  escribir('Hasta', hasta)
}

describe('PlanillasScreen', () => {
  const crear = vi.fn(() => 'blob:planilla')
  const revocar = vi.fn()

  beforeEach(() => {
    reiniciarCliente()
    crear.mockClear()
    revocar.mockClear()
    URL.createObjectURL = crear
    URL.revokeObjectURL = revocar
  })
  afterEach(() => vi.restoreAllMocks())

  it('muestra cuántos becados vigentes entran y las planillas ya generadas con periodo, becados y fecha', async () => {
    responder()
    montar()

    expect(await screen.findByText(/Se incluirán 880 becados VIGENTES/)).toBeInTheDocument()
    expect((await screen.findAllByText('01/09/2026 – 30/09/2026')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('884').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Admin Prueba').length).toBeGreaterThan(0)
    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('sin convocatorias con resultados publicados lo informa y no muestra el formulario', async () => {
    responder()
    convocatoriasPublicadas.data = []
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias con resultados publicados')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Generar planilla' })).not.toBeInTheDocument()
  })

  it('sin becas vigentes lo dice y bloquea generar y la vista previa (A2)', async () => {
    responder({ resumen: resumen({ becadosVigentes: 0 }) })
    montar()

    expect(await screen.findByText(/no tiene becas vigentes/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generar planilla' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Vista previa' })).toBeDisabled()
  })

  it('una fecha final anterior a la inicial se avisa bajo el campo y no llama al backend (A1)', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-09-30', '2026-09-01')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))

    expect(await screen.findByText('La fecha final no puede ser anterior a la inicial')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('un periodo fuera de la gestión o de más de 31 días se avisa y no llama al backend', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-06-01', '2026-06-30')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))
    expect(await screen.findByText('El periodo debe estar dentro de la gestión 2026-II')).toBeInTheDocument()

    periodo('2026-09-01', '2026-10-15')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))
    expect(await screen.findByText('El periodo no puede superar los 31 días')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('sin fechas pide las dos al intentar generar', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)

    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))

    expect(await screen.findByText('La fecha inicial es obligatoria')).toBeInTheDocument()
    expect(screen.getByText('La fecha final es obligatoria')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('genera la planilla con el periodo, avisa del resultado y ofrece descargarla', async () => {
    responder()
    cliente.post.mockResolvedValue({ data: planilla(8, { fechaDesde: '2026-10-01', fechaHasta: '2026-10-31' }) })
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-10-01', '2026-10-31')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith(`/convocatorias/${CONVOCATORIA}/planillas`, {
        fechaDesde: '2026-10-01',
        fechaHasta: '2026-10-31',
      }),
    )
    expect(await screen.findByText(/Planilla generada para 01\/10\/2026 – 31\/10\/2026 con 880 becados/)).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: /Descargar/ })[0])
    await waitFor(() =>
      expect(abrir).toHaveBeenCalledWith('https://minio.test/firmada', '_blank', 'noopener,noreferrer'),
    )
  })

  it('un error del backend al generar se muestra con su mensaje y no se anuncia éxito', async () => {
    responder()
    cliente.post.mockRejectedValue(new ApiError('ERR_PLA_04', 'x'))
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-09-01', '2026-09-30')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))

    expect(await screen.findByText(/no tiene becas vigentes: no hay a quién incluir/)).toBeInTheDocument()
    expect(screen.queryByText(/Planilla generada para/)).not.toBeInTheDocument()
  })

  it('un error de red al generar ofrece «Reintentar»', async () => {
    responder()
    cliente.post.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x')).mockResolvedValueOnce({ data: planilla(9) })
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-09-01', '2026-09-30')
    fireEvent.click(screen.getByRole('button', { name: 'Generar planilla' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(2))
    expect(await screen.findByText(/Planilla generada para/)).toBeInTheDocument()
  })

  it('la vista previa se pide con el periodo y se muestra en un iframe sin generar nada', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)

    periodo('2026-09-01', '2026-09-30')
    fireEvent.click(screen.getByRole('button', { name: 'Vista previa' }))

    const iframe = await screen.findByTitle('Vista previa de la planilla')
    expect(iframe).toHaveAttribute('src', 'blob:planilla')
    expect(cliente.get).toHaveBeenCalledWith(`/convocatorias/${CONVOCATORIA}/planillas/vista-previa`, {
      params: { desde: '2026-09-01', hasta: '2026-09-30' },
      responseType: 'blob',
    })
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('al cambiar el periodo la vista previa se descarta y se libera su dirección', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)
    periodo('2026-09-01', '2026-09-30')
    fireEvent.click(screen.getByRole('button', { name: 'Vista previa' }))
    await screen.findByTitle('Vista previa de la planilla')

    escribir('Hasta', '2026-09-15')

    await waitFor(() => expect(screen.queryByTitle('Vista previa de la planilla')).not.toBeInTheDocument())
    expect(revocar).toHaveBeenCalledWith('blob:planilla')
  })

  it('un error en la vista previa se muestra y no deja ningún PDF', async () => {
    responder()
    montar()
    await screen.findByText(/Se incluirán/)
    cliente.get.mockImplementation((ruta: string) =>
      ruta.endsWith('/vista-previa')
        ? Promise.reject(new ApiError('ERR_PLA_02', 'x'))
        : Promise.resolve({ data: ruta.endsWith('/resumen') ? resumen() : { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } }),
    )

    periodo('2026-09-01', '2026-09-30')
    fireEvent.click(screen.getByRole('button', { name: 'Vista previa' }))

    expect(await screen.findByText(/dentro de la gestión académica/)).toBeInTheDocument()
    expect(screen.queryByTitle('Vista previa de la planilla')).not.toBeInTheDocument()
  })

  it('descargar una planilla de la tabla pide la dirección temporal y la abre en otra pestaña', async () => {
    responder()
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    montar()
    await screen.findAllByText('01/09/2026 – 30/09/2026')

    fireEvent.click(screen.getAllByRole('button', { name: 'Descargar' })[0])

    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/planillas/pl-7/descarga'))
    await waitFor(() =>
      expect(abrir).toHaveBeenCalledWith('https://minio.test/firmada', '_blank', 'noopener,noreferrer'),
    )
  })

  it('un error al descargar se muestra y no abre nada', async () => {
    responder()
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    montar()
    await screen.findAllByText('01/09/2026 – 30/09/2026')
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, ...resto: unknown[]) =>
      ruta.endsWith('/descarga') ? Promise.reject(new ApiError('ERR_PLA_06', 'x')) : base(ruta, ...resto),
    )

    fireEvent.click(screen.getAllByRole('button', { name: 'Descargar' })[0])

    expect(await screen.findByText('El archivo de la planilla no está disponible. Avisa a soporte.')).toBeInTheDocument()
    expect(abrir).not.toHaveBeenCalled()
  })

  it('sin planillas generadas muestra el estado vacío', async () => {
    responder({ planillas: [] })
    montar()

    expect((await screen.findAllByText('Todavía no hay planillas generadas para esta convocatoria')).length).toBeGreaterThan(0)
  })

  it('ordenar por periodo pide al backend el orden por fechaDesde', async () => {
    responder()
    montar()
    await screen.findAllByText('01/09/2026 – 30/09/2026')

    fireEvent.click(screen.getAllByRole('button', { name: /Periodo/ })[0])

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ sort: 'fechaDesde' }))
  })

  it('un error al cargar las planillas se muestra con «Reintentar»', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, ...resto: unknown[]) =>
      ruta === `/convocatorias/${CONVOCATORIA}/planillas`
        ? Promise.reject(new ApiError('ERR_SYS_00', 'x'))
        : base(ruta, ...resto),
    )
    montar()

    expect(await screen.findByText(/error en el servidor/)).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Reintentar' }).length).toBeGreaterThan(0)
  })
})
