import { screen, waitFor, within } from '@testing-library/react'
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
vi.mock('@/features/resultados', () => ({ resultadosKeys: { all: ['resultados'] } }))
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { acta, cliente, montar, reiniciarCliente, responder, ultimaConsulta } from './montarAdmin'

/** El botón de ícono (Tooltip.Trigger de base-ui) marca su bloqueo con `data-trigger-disabled`. */
function bloqueado(boton: HTMLElement): boolean {
  return boton.matches('[disabled], [aria-disabled="true"], [data-trigger-disabled]')
}

describe('ActasScreen', () => {
  beforeEach(() => reiniciarCliente())
  afterEach(() => vi.restoreAllMocks())

  it('muestra las actas con su número y sus datos', async () => {
    responder({ actas: [acta(1, { adjudicados: 7 }), acta(2, { generadaPor: 'Otra Persona' })] })
    montar()

    expect(await screen.findAllByText('001/2026')).not.toHaveLength(0)
    expect(screen.getAllByText('002/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Beca 1').length).toBeGreaterThan(0)
    expect(screen.getAllByText('2026-II').length).toBeGreaterThan(0)
    expect(screen.getAllByText('7').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Otra Persona').length).toBeGreaterThan(0)
    // 13/11/2026 15:30 UTC son las 11:30 en La Paz.
    expect(screen.getAllByText(/13\/11\/2026.*11:30/).length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Actas de adjudicación' })).toBeInTheDocument()
  })

  it('nunca muestra la ruta del archivo ni una dirección de descarga', async () => {
    responder()
    montar()
    await screen.findAllByText('001/2026')

    expect(document.body.textContent).not.toMatch(/actas\//)
    expect(document.body.textContent).not.toMatch(/https?:\/\//)
  })

  it('la primera consulta es la página 1 de 20 sin orden ni búsqueda', async () => {
    responder()
    montar()
    await screen.findAllByText('001/2026')

    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('la paginación manda la página pedida', async () => {
    responder({ actas: [acta(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('001/2026')

    await user.click(screen.getByRole('button', { name: 'Página 2' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ page: 2, pageSize: 20 }))
  })

  it('ordenar manda sort solo con los campos que el backend documenta', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('001/2026')

    await user.click(screen.getByRole('button', { name: /N\.º/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('numero'))
    await user.click(screen.getByRole('button', { name: /N\.º/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-numero'))
    await user.click(screen.getByRole('button', { name: /Fecha/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('generadaEn'))
    // Las demás columnas no se ordenan.
    expect(screen.queryByRole('button', { name: /Convocatoria/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Generada por/ })).not.toBeInTheDocument()
  })

  it('la búsqueda manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ actas: [acta(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('001/2026')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = cliente.get.mock.calls.filter((c) => c[0] === '/actas').length

    await user.type(screen.getByPlaceholderText('Buscar por número o convocatoria...'), '001')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: '001', page: 1 }))
    expect(cliente.get.mock.calls.filter((c) => c[0] === '/actas').length - antes).toBe(1)
  })

  it('sin actas muestra el mensaje de vacío', async () => {
    responder({ actas: [], meta: { totalItems: 0, totalPages: 0 } })
    montar()

    expect((await screen.findAllByText('Todavía no hay actas generadas')).length).toBeGreaterThan(0)
  })

  it('un error muestra el mensaje del backend con «Reintentar», que vuelve a pedir', async () => {
    responder()
    // Solo la primera consulta de actas falla (la del panel de pendientes responde normal); el reintento ya funciona.
    const normal = cliente.get.getMockImplementation() as (ruta: string) => Promise<unknown>
    let falla = true
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/actas' && falla) {
        falla = false
        return Promise.reject(new ApiError('ERR_SYS_05', 'No se puede ordenar por «ci»'))
      }
      return normal(ruta)
    })
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText('No se puede ordenar por «ci»')).toBeInTheDocument()

    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect((await screen.findAllByText('001/2026')).length).toBeGreaterThan(0)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  describe('descargar', () => {
    it('pide la dirección de la convocatoria del acta, la abre una vez y no la deja en la caché', async () => {
      responder()
      cliente.get.mockImplementation((ruta: string) =>
        ruta === '/convocatorias/conv-1/acta/descarga'
          ? Promise.resolve({ data: { url: 'http://minio.test/firmada', expiraEn: '2026-11-13T15:35:00Z' } })
          : Promise.resolve({
              data: { data: [acta(1)], meta: { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 } },
            }),
      )
      const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
      const { user, queryClient } = montar()
      await screen.findAllByText('001/2026')

      await user.click(screen.getAllByRole('button', { name: 'Descargar' })[0])

      await waitFor(() =>
        expect(abrir).toHaveBeenCalledExactlyOnceWith('http://minio.test/firmada', '_blank', 'noopener,noreferrer'),
      )
      expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-1/acta/descarga')
      expect(JSON.stringify(queryClient.getQueryCache().getAll().map((q) => q.state.data))).not.toContain('firmada')
    })

    it('mientras pide la dirección bloquea el botón de esa fila', async () => {
      responder()
      let resolver: (valor: unknown) => void = () => {}
      cliente.get.mockImplementation((ruta: string) =>
        ruta.endsWith('/descarga')
          ? new Promise((r) => {
              resolver = r
            })
          : Promise.resolve({
              data: { data: [acta(1)], meta: { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 } },
            }),
      )
      vi.spyOn(window, 'open').mockReturnValue(null)
      const { user } = montar()
      await screen.findAllByText('001/2026')

      await user.click(screen.getAllByRole('button', { name: 'Descargar' })[0])

      await waitFor(() => expect(bloqueado(screen.getAllByRole('button', { name: 'Descargar' })[0])).toBe(true))
      resolver({ data: { url: 'http://minio.test/x', expiraEn: 'x' } })
      await waitFor(() => expect(bloqueado(screen.getAllByRole('button', { name: 'Descargar' })[0])).toBe(false))
    })

    it.each([
      ['ERR_ACT_04', 'El archivo del acta no está disponible. Avisa a soporte.'],
      ['ERR_ACT_03', 'Esta convocatoria todavía no tiene acta.'],
    ])('%s muestra su mensaje en un aviso y no abre nada', async (codigo, mensaje) => {
      cliente.get.mockImplementation((ruta: string) =>
        ruta.endsWith('/descarga')
          ? Promise.reject(new ApiError(codigo, 'x'))
          : Promise.resolve({
              data: { data: [acta(1)], meta: { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 } },
            }),
      )
      const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
      const { user } = montar()
      await screen.findAllByText('001/2026')

      await user.click(screen.getAllByRole('button', { name: 'Descargar' })[0])

      expect(await screen.findByText(mensaje)).toBeInTheDocument()
      expect(abrir).not.toHaveBeenCalled()
    })
  })
})
