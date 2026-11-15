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

import {
  beca,
  candidato,
  cliente,
  consultas,
  montar,
  reiniciarCliente,
  responder,
  resumen,
  ultimaConsulta,
} from './montar'

/** El botón de ícono (Tooltip.Trigger de base-ui) marca su bloqueo con `data-trigger-disabled`. */
function bloqueado(boton: HTMLElement): boolean {
  return boton.matches('[disabled], [aria-disabled="true"], [data-trigger-disabled]')
}

describe('PadronScreen', () => {
  beforeEach(() => reiniciarCliente())
  afterEach(() => vi.restoreAllMocks())

  it('muestra el padrón con nombre, CI, carrera, estado y la fecha de inicio', async () => {
    responder()
    montar()

    expect((await screen.findAllByText('Ana1 Rojas')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('101').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ing. Sistemas').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Vigente').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Baja').length).toBeGreaterThan(0)
    expect(screen.getAllByText('10/11/2026').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Padrón de becados', level: 1 })).toBeInTheDocument()
  })

  it('muestra el resumen de la convocatoria', async () => {
    responder({ resumen: resumen({ adjudicados: 5, vigentes: 3, suspendidas: 1, bajas: 1 }) })
    montar()
    await screen.findAllByText('Ana1 Rojas')

    const lista = screen.getByLabelText('Resumen del padrón')
    expect(within(lista).getByText('Adjudicados:').nextSibling).toHaveTextContent('5')
    expect(within(lista).getByText('Vigentes:').nextSibling).toHaveTextContent('3')
    expect(within(lista).getByText('Suspendidas:').nextSibling).toHaveTextContent('1')
    expect(within(lista).getByText('Bajas:').nextSibling).toHaveTextContent('1')
  })

  it('con los resultados sin publicar avisa y no pide el padrón', async () => {
    responder({ resumen: resumen({ resultadosPublicados: false }) })
    montar()

    expect(await screen.findByText(/todavía no fueron publicados/)).toBeInTheDocument()
    expect(consultas()).toBe(0)
    expect(screen.queryByRole('button', { name: /Dar de alta/ })).not.toBeInTheDocument()
  })

  it('sin convocatorias publicadas lo dice', async () => {
    responder()
    const { convocatoriasPublicadas } = await import('./clienteSimulado')
    convocatoriasPublicadas.data = []
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias con resultados publicados')).toBeInTheDocument()
    expect(cliente.get).not.toHaveBeenCalled()
  })

  it('la primera consulta es la página 1 de 20 sin filtros ni orden', async () => {
    responder()
    montar()
    await screen.findAllByText('Ana1 Rojas')

    expect(ultimaConsulta()).toEqual({ page: 1, pageSize: 20 })
  })

  it('ordena solo por los campos que acepta el backend', async () => {
    responder()
    const { user } = montar()
    await screen.findAllByText('Ana1 Rojas')

    await user.click(screen.getByRole('button', { name: /Becado/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('becado'))
    await user.click(screen.getByRole('button', { name: /^CI/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('ci'))
    await user.click(screen.getByRole('button', { name: /^CI/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('-ci'))
    await user.click(screen.getByRole('button', { name: /Inicio/ }))
    await waitFor(() => expect(ultimaConsulta().sort).toBe('fechaInicio'))
  })

  it('la búsqueda manda search (un solo pedido) y vuelve a la página 1', async () => {
    responder({ meta: { totalItems: 60, totalPages: 3 } })
    const { user } = montar()
    await screen.findAllByText('Ana1 Rojas')
    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    await waitFor(() => expect(ultimaConsulta().page).toBe(2))
    const antes = consultas()

    await user.type(screen.getByPlaceholderText('Buscar becado por nombre o CI...'), 'ana')

    await waitFor(() => expect(ultimaConsulta()).toMatchObject({ search: 'ana', page: 1 }))
    expect(consultas() - antes).toBe(1)
  })

  describe('alta', () => {
    it('ofrece «Dar de alta» con los pendientes, confirma y vuelve a pedir el padrón', async () => {
      responder({ resumen: resumen({ pendientesDeAlta: 2 }) })
      cliente.post.mockResolvedValue({ data: { creadas: 2 } })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Dar de alta (2)' }))
      const dialogo = await screen.findByRole('dialog')
      expect(within(dialogo).getByText(/adjudicados todavía no figuran/)).toBeInTheDocument()
      const antes = consultas()
      await user.click(within(dialogo).getByRole('button', { name: 'Dar de alta' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledWith(`/convocatorias/conv-1/padron/alta`))
      await waitFor(() => expect(consultas()).toBeGreaterThan(antes))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('sin pendientes no muestra el botón', async () => {
      responder({ resumen: resumen({ pendientesDeAlta: 0 }) })
      montar()
      await screen.findAllByText('Ana1 Rojas')

      expect(screen.queryByRole('button', { name: /Dar de alta/ })).not.toBeInTheDocument()
    })

    it('cancelar no da de alta ni cambia nada', async () => {
      responder({ resumen: resumen({ pendientesDeAlta: 1 }) })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Dar de alta (1)' }))
      await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('si ya no hay pendientes (ERR_BEC_04) avisa y cierra', async () => {
      responder({ resumen: resumen({ pendientesDeAlta: 1 }) })
      cliente.post.mockRejectedValue(new ApiError('ERR_BEC_04', 'No hay adjudicados pendientes de alta'))
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Dar de alta (1)' }))
      await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Dar de alta' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })
  })

  describe('acciones por fila', () => {
    it('baja y suspensión solo en vigentes; reemplazo solo en bajas', async () => {
      responder({ becas: [beca(1, 'VIGENTE'), beca(2, 'BAJA'), beca(3, 'SUSPENDIDA')] })
      montar()
      await screen.findAllByText('Ana1 Rojas')

      // La tabla y las tarjetas se montan juntas; los tres primeros de cada acción son los de la tabla, en orden.
      const bajas = screen.getAllByRole('button', { name: 'Dar de baja' })
      const suspensiones = screen.getAllByRole('button', { name: 'Suspender' })
      const reemplazos = screen.getAllByRole('button', { name: 'Reemplazar' })
      expect([0, 1, 2].map((i) => bloqueado(bajas[i]))).toEqual([false, true, true])
      expect([0, 1, 2].map((i) => bloqueado(suspensiones[i]))).toEqual([false, true, true])
      expect([0, 1, 2].map((i) => bloqueado(reemplazos[i]))).toEqual([true, false, true])
    })

    it('la baja pide el motivo: sin motivo no se envía y con motivo cambia la beca', async () => {
      responder()
      cliente.post.mockResolvedValue({ data: beca(1, 'BAJA') })
      const { user } = montar()
      await screen.findAllByText('Ana1 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Dar de baja' })[0])
      const dialogo = await screen.findByRole('dialog')
      expect(within(dialogo).getByText(/La baja es definitiva/)).toBeInTheDocument()
      await user.click(within(dialogo).getByRole('button', { name: 'Dar de baja' }))

      expect(await within(dialogo).findByText('El motivo es obligatorio')).toBeInTheDocument()
      expect(cliente.post).not.toHaveBeenCalled()

      await user.type(within(dialogo).getByLabelText('Motivo'), '  Dejó la universidad  ')
      await user.click(within(dialogo).getByRole('button', { name: 'Dar de baja' }))

      await waitFor(() =>
        expect(cliente.post).toHaveBeenCalledWith('/becas/beca-1/dar-de-baja', { motivo: 'Dejó la universidad' }),
      )
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('la suspensión manda el motivo a su ruta', async () => {
      responder()
      cliente.post.mockResolvedValue({ data: beca(1, 'SUSPENDIDA') })
      const { user } = montar()
      await screen.findAllByText('Ana1 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Suspender' })[0])
      const dialogo = await screen.findByRole('dialog')
      await user.type(within(dialogo).getByLabelText('Motivo'), 'Sin asistencia')
      await user.click(within(dialogo).getByRole('button', { name: 'Suspender' }))

      await waitFor(() =>
        expect(cliente.post).toHaveBeenCalledWith('/becas/beca-1/suspender', { motivo: 'Sin asistencia' }),
      )
    })

    it('cancelar la baja no cambia nada', async () => {
      responder()
      const { user } = montar()
      await screen.findAllByText('Ana1 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Dar de baja' })[0])
      await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('si la beca ya cambió (ERR_BEC_05) avisa y cierra', async () => {
      responder()
      cliente.post.mockRejectedValue(new ApiError('ERR_BEC_05', 'La beca no está vigente'))
      const { user } = montar()
      await screen.findAllByText('Ana1 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Dar de baja' })[0])
      const dialogo = await screen.findByRole('dialog')
      await user.type(within(dialogo).getByLabelText('Motivo'), 'x')
      await user.click(within(dialogo).getByRole('button', { name: 'Dar de baja' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })
  })

  describe('reemplazo', () => {
    it('muestra a quién se propone y, al confirmar, manda su código', async () => {
      responder({ candidato: candidato() })
      cliente.post.mockResolvedValue({ data: { becaLiberada: beca(2, 'BAJA'), becaReemplazante: beca(9) } })
      const { user } = montar()
      await screen.findAllByText('Ana2 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Reemplazar' })[1])
      const dialogo = await screen.findByRole('dialog')
      expect(await within(dialogo).findByText('Carla Peña')).toBeInTheDocument()
      expect(within(dialogo).getByText(/CI 109, Medicina/)).toBeInTheDocument()
      expect(within(dialogo).getByText(/puesto 3 de la lista priorizada, con 75 puntos/)).toBeInTheDocument()
      await user.click(within(dialogo).getByRole('button', { name: 'Reemplazar' }))

      await waitFor(() =>
        expect(cliente.post).toHaveBeenCalledWith('/becas/beca-2/reemplazar', { postulanteCodigo: 'post-9' }),
      )
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('sin postulante disponible lo informa y solo ofrece cerrar', async () => {
      responder()
      cliente.get.mockImplementation((ruta: string) => {
        if (/^\/becas\/[^/]+\/reemplazo$/.test(ruta)) {
          return Promise.reject(new ApiError('ERR_BEC_07', 'No hay un postulante disponible'))
        }
        if (ruta.endsWith('/padron/resumen')) return Promise.resolve({ data: resumen() })
        return Promise.resolve({
          data: { data: [beca(1), beca(2, 'BAJA')], meta: { page: 1, pageSize: 20, totalItems: 2, totalPages: 1 } },
        })
      })
      const { user } = montar()
      await screen.findAllByText('Ana2 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Reemplazar' })[1])
      const dialogo = await screen.findByRole('dialog')

      expect(await within(dialogo).findByText('No hay un postulante disponible para ocupar el cupo.')).toBeInTheDocument()
      // «Cerrar» también es la X del encabezado: el que confirma es el botón de envío del formulario.
      const cerrar = within(dialogo)
        .getAllByRole('button', { name: 'Cerrar' })
        .find((boton) => boton.getAttribute('type') === 'submit')
      await user.click(cerrar as HTMLElement)
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('si el candidato cambió (ERR_BEC_08) avisa y cierra para volver a abrirlo', async () => {
      responder({ candidato: candidato() })
      cliente.post.mockRejectedValue(new ApiError('ERR_BEC_08', 'El postulante cambió'))
      const { user } = montar()
      await screen.findAllByText('Ana2 Rojas')

      await user.click(screen.getAllByRole('button', { name: 'Reemplazar' })[1])
      const dialogo = await screen.findByRole('dialog')
      await within(dialogo).findByText('Carla Peña')
      await user.click(within(dialogo).getByRole('button', { name: 'Reemplazar' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })
  })

  it('si falla la consulta muestra el mensaje y permite reintentar', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta.endsWith('/padron/resumen')
        ? Promise.reject(new ApiError('ERR_AUTH_07', 'sin permiso'))
        : Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } }),
    )
    const { convocatoriasPublicadas } = await import('./clienteSimulado')
    convocatoriasPublicadas.data = [{ codigo: 'conv-1', nombre: 'Beca 2026-II' }]
    const { user } = montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.')
    const antes = cliente.get.mock.calls.length
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(cliente.get.mock.calls.length).toBeGreaterThan(antes))
  })
})
