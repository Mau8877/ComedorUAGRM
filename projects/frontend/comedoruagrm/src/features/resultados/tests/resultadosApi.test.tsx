import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { adjudicacionKeys } from '@/features/adjudicacion'
import { finalistasKeys } from '@/features/finalistas'
import { apiClient } from '@/store'

import {
  listarAdjudicadosApi,
  listarConvocatoriasPublicadasApi,
  listarDecisionesApi,
  obtenerMiResultadoApi,
  obtenerResumenApi,
  publicarResultadosApi,
  resultadosKeys,
  useAdjudicadosPublicos,
  useConvocatoriasPublicadas,
  useDecisionesResultado,
  useMiResultado,
  usePublicarResultados,
  useResumenPublicacion,
} from '../api'
import type { AdjudicadoPublico, DecisionResultado, MiResultado, ResumenPublicacion } from '../types'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('resultadosApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('obtenerResumenApi hace GET /convocatorias/{codigo}/resultados/resumen', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { adjudicados: 2 } })

    expect(await obtenerResumenApi('c-1')).toEqual({ adjudicados: 2 })
    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/resultados/resumen')
  })

  it('listarDecisionesApi manda page, pageSize, filter[resultado], search y sort, y omite lo vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarDecisionesApi('c-1', {
      page: 2,
      pageSize: 10,
      resultado: 'NO_ADJUDICADA',
      search: 'ana',
      sort: '-puntajeFinal',
    })
    await listarDecisionesApi('c-1', { page: 1, pageSize: 20, search: '' })

    expect(get).toHaveBeenNthCalledWith(1, '/convocatorias/c-1/resultados/decisiones', {
      params: { page: 2, pageSize: 10, 'filter[resultado]': 'NO_ADJUDICADA', search: 'ana', sort: '-puntajeFinal' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/convocatorias/c-1/resultados/decisiones', {
      params: { page: 1, pageSize: 20 },
    })
  })

  it('publicarResultadosApi hace POST /convocatorias/{codigo}/resultados/publicar sin cuerpo ni idempotencia', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { avisados: 5 } })

    expect(await publicarResultadosApi('c-1')).toEqual({ avisados: 5 })
    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/resultados/publicar')
  })

  it('listarConvocatoriasPublicadasApi hace GET /resultados/convocatorias con la página', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarConvocatoriasPublicadasApi({ page: 1, pageSize: 20 })

    expect(get).toHaveBeenCalledWith('/resultados/convocatorias', { params: { page: 1, pageSize: 20 } })
  })

  it('listarAdjudicadosApi hace GET /convocatorias/{codigo}/resultados con search y sort', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarAdjudicadosApi('c-1', { page: 3, pageSize: 50, search: 'beto', sort: 'nota' })

    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/resultados', {
      params: { page: 3, pageSize: 50, search: 'beto', sort: 'nota' },
    })
  })

  it('obtenerMiResultadoApi hace GET /convocatorias/{codigo}/resultados/mio', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { resultado: null } })

    expect(await obtenerMiResultadoApi('c-1')).toEqual({ resultado: null })
    expect(get).toHaveBeenCalledWith('/convocatorias/c-1/resultados/mio')
  })

  it('resultadosKeys sigue el esquema [módulo, tipo, código, ...params]', () => {
    expect(resultadosKeys.all).toEqual(['resultados'])
    expect(resultadosKeys.resumen('c-1')).toEqual(['resultados', 'resumen', 'c-1'])
    expect(resultadosKeys.decisiones('c-1', { page: 1, pageSize: 20 })).toEqual([
      'resultados',
      'decisiones',
      'c-1',
      { page: 1, pageSize: 20 },
    ])
    expect(resultadosKeys.publicados({ page: 1, pageSize: 20 })).toEqual([
      'resultados',
      'publicados',
      { page: 1, pageSize: 20 },
    ])
    expect(resultadosKeys.adjudicados('c-1', { page: 1, pageSize: 20 })).toEqual([
      'resultados',
      'adjudicados',
      'c-1',
      { page: 1, pageSize: 20 },
    ])
    expect(resultadosKeys.mio('c-1')).toEqual(['resultados', 'mio', 'c-1'])
  })

  it('los tipos aceptan null en puntajeFinal, nota, publicadoEn y publicadoPor', () => {
    const decision: DecisionResultado = {
      finalista: 'f',
      nombres: 'Ana',
      apellidos: 'Rojas',
      resultado: 'NO_ADJUDICADA',
      puntajeFinal: null,
      fechaAdjudicacion: '2026-11-12T10:00:00Z',
    }
    const adjudicado: AdjudicadoPublico = { nombres: 'A', apellidos: 'B', nota: null, carrera: 'C', facultad: 'D' }
    const mio: MiResultado = { resultado: null, nota: null, publicadoEn: '2026-11-12T10:00:00Z' }
    const resumen: ResumenPublicacion = {
      convocatoria: { codigo: 'c', nombre: 'N', estado: 'CERRADA' },
      adjudicados: 0,
      noAdjudicados: 0,
      calificadasPendientes: 0,
      totalFinalistas: 0,
      aAvisar: 0,
      yaPublicada: false,
      publicadoEn: null,
      publicadoPor: null,
      puedePublicar: true,
    }

    expect([decision.puntajeFinal, adjudicado.nota, mio.nota, resumen.publicadoEn, resumen.publicadoPor]).toEqual([
      null,
      null,
      null,
      null,
      null,
    ])
  })
})

function envoltorio() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  return { queryClient, wrapper }
}

describe('hooks de resultados', () => {
  afterEach(() => vi.restoreAllMocks())

  it('no consultan mientras no haya convocatoria elegida', () => {
    const get = vi.spyOn(apiClient, 'get')
    const { wrapper } = envoltorio()

    const resumen = renderHook(() => useResumenPublicacion(undefined), { wrapper })
    const decisiones = renderHook(() => useDecisionesResultado(undefined, { page: 1, pageSize: 20 }), { wrapper })
    const adjudicados = renderHook(() => useAdjudicadosPublicos(undefined, { page: 1, pageSize: 20 }), { wrapper })
    const mio = renderHook(() => useMiResultado(undefined), { wrapper })

    for (const r of [resumen, decisiones, adjudicados, mio]) {
      expect(r.result.current.fetchStatus).toBe('idle')
    }
    expect(get).not.toHaveBeenCalled()
  })

  it('useResumenPublicacion, useDecisionesResultado, useAdjudicadosPublicos y useMiResultado llaman a su ruta', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })
    const { wrapper } = envoltorio()

    const resumen = renderHook(() => useResumenPublicacion('c-1'), { wrapper })
    const decisiones = renderHook(
      () => useDecisionesResultado('c-1', { page: 1, pageSize: 20, resultado: 'ADJUDICADA' }),
      { wrapper },
    )
    const adjudicados = renderHook(() => useAdjudicadosPublicos('c-1', { page: 1, pageSize: 20, sort: 'nota' }), {
      wrapper,
    })
    const mio = renderHook(() => useMiResultado('c-1'), { wrapper })
    const publicadas = renderHook(() => useConvocatoriasPublicadas({ page: 1, pageSize: 20 }), { wrapper })

    await waitFor(() => {
      for (const r of [resumen, decisiones, adjudicados, mio, publicadas]) expect(r.result.current.isSuccess).toBe(true)
    })
    const rutas = get.mock.calls.map((llamada) => llamada[0])
    expect(rutas).toEqual(
      expect.arrayContaining([
        '/convocatorias/c-1/resultados/resumen',
        '/convocatorias/c-1/resultados/decisiones',
        '/convocatorias/c-1/resultados',
        '/convocatorias/c-1/resultados/mio',
        '/resultados/convocatorias',
      ]),
    )
  })

  it('usePublicarResultados llama a la API e invalida resultados, adjudicación y la lista de finalistas', async () => {
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { avisados: 3 } })
    const { queryClient, wrapper } = envoltorio()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => usePublicarResultados(), { wrapper })
    result.current.mutate('c-1')

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/convocatorias/c-1/resultados/publicar')
    const claves = invalidar.mock.calls.map((llamada) => llamada[0]?.queryKey)
    expect(claves).toEqual(
      expect.arrayContaining([resultadosKeys.all, adjudicacionKeys.all, finalistasKeys.lists()]),
    )
  })
})
