import { screen, waitFor, within } from '@testing-library/react'
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
    AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
  }
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
// El selector de convocatorias es el de CU06: acá se simula con lo que devuelve su endpoint.
vi.mock('@/features/priorizacion', async () => {
  const { useQuery } = await import('@tanstack/react-query')
  const { cliente } = await import('./clienteSimulado')
  return {
    useConvocatoriasPriorizables: () =>
      useQuery({
        queryKey: ['priorizacion', 'convocatorias'],
        queryFn: async () => (await cliente.get('/lista-priorizada/convocatorias')).data,
      }),
  }
})

import { ApiError } from '@/store'

import { cliente, decision, montar, reiniciarCliente, responder, resumen, ultimaConsulta } from './montarAdmin'

const UNA_CONVOCATORIA = [
  { codigo: 'conv-2', nombre: 'Beca 2027-I', gestion: '2027-I', estado: 'CERRADA' },
]

describe('PublicarResultadosScreen', () => {
  beforeEach(() => reiniciarCliente())

  it('preselecciona la convocatoria más reciente y muestra el resumen y las decisiones', async () => {
    responder({
      filas: [decision(1, { puntajeFinal: 88.5 }), decision(2, { resultado: 'NO_ADJUDICADA', puntajeFinal: null })],
    })
    montar()

    expect(await screen.findAllByText('Nombre1')).not.toHaveLength(0)
    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Beca 2027-I (2027-I)')
    const tarjetas = screen.getByRole('region', { name: 'Resumen de la publicación' })
    expect(tarjetas).toHaveTextContent('Adjudicados3')
    expect(tarjetas).toHaveTextContent('No adjudicados2')
    expect(tarjetas).toHaveTextContent('Decisiones pendientes0')
    expect(tarjetas).toHaveTextContent('Finalistas a avisar5')
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-2/resultados/resumen')
    expect(screen.getAllByText('Adjudicada').length).toBeGreaterThan(0)
    expect(screen.getAllByText('No adjudicada').length).toBeGreaterThan(0)
    expect(screen.getAllByText('88,50').length).toBeGreaterThan(0)
    // Puntaje nulo: «—», nunca un número inventado.
    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: /Publicar resultados/ })).toBeEnabled()
  })

  it('con decisiones pendientes deshabilita «Publicar» y muestra el motivo', async () => {
    responder({ resumenes: { 'conv-2': resumen({ calificadasPendientes: 3, puedePublicar: false }) } })
    montar()

    expect(await screen.findByText('Faltan 3 decisiones de adjudicación para poder publicar.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Publicar resultados/ })).toBeDisabled()
    expect(screen.getByRole('region', { name: 'Resumen de la publicación' })).toHaveTextContent(
      'Decisiones pendientes3',
    )
  })

  it('con la convocatoria sin cerrar deshabilita «Publicar» con su motivo', async () => {
    responder({
      resumenes: {
        'conv-2': resumen({
          convocatoria: { codigo: 'conv-2', nombre: 'Beca 2027-I', estado: 'ABIERTA' },
          puedePublicar: false,
        }),
      },
    })
    montar()

    expect(
      await screen.findByText('La convocatoria debe estar cerrada para publicar sus resultados.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Publicar resultados/ })).toBeDisabled()
  })

  it('ya publicada: sin botón, con quién y cuándo', async () => {
    responder({
      resumenes: {
        'conv-2': resumen({
          yaPublicada: true,
          puedePublicar: false,
          publicadoEn: '2026-11-12T15:30:00Z',
          publicadoPor: 'Admin DUBSS',
        }),
      },
    })
    montar()

    expect(await screen.findByText(/Resultados publicados el/)).toHaveTextContent('por Admin DUBSS')
    expect(screen.queryByRole('button', { name: /Publicar resultados/ })).not.toBeInTheDocument()
  })

  it('al cambiar de convocatoria pide el resumen y las decisiones de la elegida desde la página 1', async () => {
    responder({ filas: [decision(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: 'Beca 2026-II (2026-II)' }))

    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-1/resultados/resumen'))
    await waitFor(() => expect(ultimaConsulta().page).toBe(1))
    expect(cliente.get.mock.calls.some((c) => c[0] === '/convocatorias/conv-1/resultados/decisiones')).toBe(true)
  })

  it('la paginación manda page y pageSize al backend', async () => {
    responder({ filas: [decision(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })

    await user.click(screen.getByRole('button', { name: 'Página 2' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ page: 2, pageSize: 20 }))
  })

  it('ordenar manda sort con el campo que el backend documenta', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('Nombre1')

    await user.click(screen.getByRole('button', { name: /Puntaje final/ }))
    // En una columna numérica el primer clic ordena de mayor a menor.
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-puntajeFinal'))
    await user.click(screen.getByRole('button', { name: /Puntaje final/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('puntajeFinal'))
    await user.click(screen.getByRole('button', { name: /Apellido/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('apellidos'))
  })

  it('el filtro por resultado manda filter[resultado] y vuelve a la página 1', async () => {
    responder({ filas: [decision(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Resultado' }))
    await user.click(await screen.findByRole('option', { name: 'No adjudicada' }))

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ 'filter[resultado]': 'NO_ADJUDICADA', page: 1 }))
  })

  it('la búsqueda manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ filas: [decision(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = cliente.get.mock.calls.filter((c) => String(c[0]).endsWith('/decisiones')).length

    await user.type(screen.getByPlaceholderText('Buscar por nombre o apellido...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
    const despues = cliente.get.mock.calls.filter((c) => String(c[0]).endsWith('/decisiones')).length
    expect(despues - antes).toBe(1)
  })

  it('sin convocatorias avisa que todavía no hay', async () => {
    responder({ convocatorias: [] })
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias registradas.')).toBeInTheDocument()
    expect(cliente.get.mock.calls.some((c) => String(c[0]).includes('/resultados/'))).toBe(false)
  })

  it('sin decisiones muestra el mensaje de vacío', async () => {
    responder({ filas: [], meta: { totalItems: 0, totalPages: 0 } })
    montar()

    expect((await screen.findAllByText('Todavía no hay decisiones para mostrar.')).length).toBeGreaterThan(0)
  })

  it('un error al pedir las decisiones se muestra con el mensaje del backend', async () => {
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/lista-priorizada/convocatorias') return Promise.resolve({ data: UNA_CONVOCATORIA })
      if (ruta.endsWith('/resumen')) return Promise.resolve({ data: resumen() })
      return Promise.reject(new ApiError('ERR_SYS_05', 'No se puede ordenar por «ci»'))
    })
    montar()

    expect((await screen.findAllByText('No se puede ordenar por «ci»')).length).toBeGreaterThan(0)
  })

  it('un error del resumen se muestra con «Reintentar»', async () => {
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/lista-priorizada/convocatorias') return Promise.resolve({ data: UNA_CONVOCATORIA })
      if (ruta.endsWith('/resumen')) return Promise.reject(new ApiError('ERR_AUTH_07', 'x'))
      return Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } })
    })
    montar()

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText('No tienes permiso para esta acción.')).toBeInTheDocument()
    expect(within(alerta).getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('si la convocatoria ya no existe (404) avisa y la quita del selector', async () => {
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/lista-priorizada/convocatorias') {
        return Promise.resolve({
          data: [
            { codigo: 'conv-2', nombre: 'Beca 2027-I', gestion: '2027-I', estado: 'CERRADA' },
            { codigo: 'conv-1', nombre: 'Beca 2026-II', gestion: '2026-II', estado: 'CERRADA' },
          ],
        })
      }
      if (ruta === '/convocatorias/conv-2/resultados/resumen') return Promise.reject(new ApiError('ERR_PRI_01', 'x'))
      if (ruta.endsWith('/resumen')) return Promise.resolve({ data: resumen({ adjudicados: 1 }) })
      return Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } })
    })
    montar()

    expect(await screen.findByText('La convocatoria ya no existe. Elige otra.')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Beca 2026-II (2026-II)'),
    )
  })

  it('«Publicar resultados» abre la ventana de confirmación con la cantidad a avisar y «Cancelar» la cierra', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('Nombre1')

    await user.click(screen.getByRole('button', { name: /Publicar resultados/ }))

    expect(await screen.findByText(/Se avisará a/)).toHaveTextContent('Se avisará a 5 finalistas')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByText(/Se avisará a/)).not.toBeInTheDocument())
    expect(cliente.post).not.toHaveBeenCalled()
  })
})
