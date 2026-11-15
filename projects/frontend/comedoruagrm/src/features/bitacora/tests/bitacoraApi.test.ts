import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/store'

import { bitacoraKeys, listarAccionesApi, listarBitacoraApi } from '../api'
import { columnasBitacora, formatearFechaHora } from '../components'

const META = { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }

describe('bitacoraApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('listarBitacoraApi manda los filtros con la forma filter[campo] y omite lo vacío', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: { data: [], meta: META } })

    await listarBitacoraApi({
      page: 2,
      pageSize: 10,
      search: 'perez',
      accion: 'ADJUDICACION_BECA',
      desde: '2026-09-01',
      hasta: '2026-09-30',
      sort: '-fecha',
    })
    await listarBitacoraApi({ page: 1, pageSize: 20, search: '', accion: undefined })

    expect(get).toHaveBeenNthCalledWith(1, '/bitacora', {
      params: {
        page: 2,
        pageSize: 10,
        search: 'perez',
        'filter[accion]': 'ADJUDICACION_BECA',
        'filter[desde]': '2026-09-01',
        'filter[hasta]': '2026-09-30',
        sort: '-fecha',
      },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/bitacora', { params: { page: 1, pageSize: 20 } })
  })

  it('listarAccionesApi pide /bitacora/acciones', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: ['INICIO_SESION'] })

    expect(await listarAccionesApi()).toEqual(['INICIO_SESION'])
    expect(get).toHaveBeenCalledWith('/bitacora/acciones')
  })

  it('las claves de caché empiezan por el módulo y distinguen list de acciones', () => {
    const params = { page: 1, pageSize: 20 }
    expect(bitacoraKeys.list(params)).toEqual(['bitacora', 'list', params])
    expect(bitacoraKeys.acciones()).toEqual(['bitacora', 'acciones'])
  })
})

describe('columnasBitacora', () => {
  it('solo ordena por fecha, acción y entidad (lo que acepta el backend)', () => {
    const ordenables = columnasBitacora
      .filter((c) => c.enableSorting)
      .map((c) => ('accessorKey' in c ? String(c.accessorKey) : c.id))
    expect(ordenables).toEqual(['fecha', 'accion', 'entidad'])
  })

  it('muestra «Sistema» cuando no hay usuario', () => {
    const columna = columnasBitacora.find((c) => c.id === 'usuario')
    const accessorFn = (columna as unknown as { accessorFn: (fila: unknown, indice: number) => string }).accessorFn
    expect(accessorFn({ usuario: null }, 0)).toBe('Sistema')
    expect(accessorFn({ usuario: { nombres: 'Ana', apellidos: 'Pérez' } }, 0)).toBe('Ana Pérez')
  })

  it('formatea la fecha en hora de La Paz', () => {
    expect(formatearFechaHora('2026-09-30T14:42:05Z')).toContain('10:42:05')
  })
})
