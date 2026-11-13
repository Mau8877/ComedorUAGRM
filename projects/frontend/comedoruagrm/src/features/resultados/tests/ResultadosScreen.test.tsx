import { screen, waitFor, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./clienteSimulado')
  return { etiquetaRol: () => 'Postulante', useCerrarSesion: () => ({ mutate: sim.cerrarSesion }) }
})
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  EstudianteLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { adjudicado, cliente, mio, montar, publicada, reiniciarCliente, responder, ultimaConsulta } from './montarPostulante'

describe('ResultadosScreen', () => {
  beforeEach(() => reiniciarCliente())

  it('sin convocatorias publicadas avisa que aún no hay resultados y no pide nada más', async () => {
    responder({ publicadas: [] })
    montar()

    expect(await screen.findByText('Aún no hay resultados publicados')).toBeInTheDocument()
    expect(cliente.get.mock.calls.every((c) => c[0] === '/resultados/convocatorias')).toBe(true)
  })

  it('abre por defecto la convocatoria más reciente y muestra la lista de adjudicados', async () => {
    responder({ filas: [adjudicado(1, { nota: 91.25 }), adjudicado(2)] })
    montar()

    expect(await screen.findAllByText('Nombre1')).not.toHaveLength(0)
    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Beca Comedor 2')
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-2/resultados/mio')
    expect(screen.getAllByText('Apellido1').length).toBeGreaterThan(0)
    expect(screen.getAllByText('91,25').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ingeniería de Sistemas').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Facultad de Ciencias y Tecnología').length).toBeGreaterThan(0)
    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('«Mi resultado» adjudicado muestra la insignia y su nota', async () => {
    responder({ mio: mio({ resultado: 'ADJUDICADA', nota: 88.5 }) })
    montar()

    const caja = await screen.findByRole('region', { name: 'Mi resultado' })
    await waitFor(() => expect(within(caja).getByText('Adjudicada')).toBeInTheDocument())
    expect(caja).toHaveTextContent('Nota: 88,50')
  })

  it('«Mi resultado» no adjudicado muestra la insignia, sin nota ni motivo', async () => {
    responder({ mio: mio({ resultado: 'NO_ADJUDICADA', nota: 40 }) })
    montar()

    const caja = await screen.findByRole('region', { name: 'Mi resultado' })
    await waitFor(() => expect(within(caja).getByText('No adjudicada')).toBeInTheDocument())
    expect(caja).not.toHaveTextContent('Nota')
    expect(caja).not.toHaveTextContent(/motivo/i)
  })

  it('«Mi resultado» de quien no fue finalista con decisión dice que no participó en esta etapa', async () => {
    responder({ mio: mio({ resultado: null, nota: null }) })
    montar()

    const caja = await screen.findByRole('region', { name: 'Mi resultado' })
    await waitFor(() => expect(within(caja).getByText('No participaste en esta etapa')).toBeInTheDocument())
  })

  it('la lista no muestra columnas de CI, correo ni motivo', async () => {
    responder()
    montar()
    await screen.findAllByText('Nombre1')

    const encabezados = screen.getAllByRole('columnheader').map((th) => th.textContent ?? '')
    expect(encabezados.join('|')).toMatch(/Nombre/)
    expect(encabezados.join('|')).toMatch(/Nota/)
    expect(encabezados.join('|')).toMatch(/Carrera/)
    expect(encabezados.join('|')).toMatch(/Facultad/)
    expect(encabezados.join('|')).not.toMatch(/CI|Correo|Motivo|Ingresos/i)
  })

  it('la paginación manda page y pageSize al backend', async () => {
    responder({ filas: [adjudicado(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')

    await user.click(screen.getByRole('button', { name: 'Página 2' }))

    await waitFor(() => expect(ultimaConsulta()).toEqual({ page: 2, pageSize: 20 }))
  })

  it('la búsqueda manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ filas: [adjudicado(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = cliente.get.mock.calls.filter((c) => /\/resultados$/.test(String(c[0]))).length

    await user.type(screen.getByPlaceholderText('Buscar por nombre, carrera o facultad...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
    const despues = cliente.get.mock.calls.filter((c) => /\/resultados$/.test(String(c[0]))).length
    expect(despues - antes).toBe(1)
  })

  it('ordenar manda sort con el campo que el backend documenta', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('Nombre1')

    await user.click(screen.getByRole('button', { name: /Apellido/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('apellidos'))
    await user.click(screen.getByRole('button', { name: /Nota/ }))
    // En una columna numérica el primer clic ordena de mayor a menor.
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-nota'))
  })

  it('al cambiar de convocatoria pide su resultado y su lista desde la página 1', async () => {
    responder({ filas: [adjudicado(1)], meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Nombre1')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))

    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: 'Beca Comedor 1' }))

    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-1/resultados/mio'))
    await waitFor(() => expect(ultimaConsulta().page).toBe(1))
  })

  it('sin adjudicados muestra el mensaje de vacío', async () => {
    responder({ filas: [], meta: { totalItems: 0, totalPages: 0 } })
    montar()

    expect((await screen.findAllByText('No hay adjudicados para mostrar.')).length).toBeGreaterThan(0)
  })

  it('ERR_PUB_04 se muestra como «Los resultados aún no están publicados.»', async () => {
    cliente.get.mockImplementation((ruta: string) => {
      if (ruta === '/resultados/convocatorias') {
        return Promise.resolve({
          data: { data: [publicada(2)], meta: { page: 1, pageSize: 50, totalItems: 1, totalPages: 1 } },
        })
      }
      return Promise.reject(new ApiError('ERR_PUB_04', 'x'))
    })
    montar()

    const alertas = await screen.findAllByRole('alert')
    expect(alertas.some((a) => a.textContent?.includes('Los resultados aún no están publicados.'))).toBe(true)
  })

  it('un error al pedir las convocatorias publicadas se muestra con «Reintentar»', async () => {
    cliente.get.mockRejectedValue(new ApiError('ERR_SYS_05', 'Parámetros inválidos'))
    montar()

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText('Parámetros inválidos')).toBeInTheDocument()
    expect(within(alerta).getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
