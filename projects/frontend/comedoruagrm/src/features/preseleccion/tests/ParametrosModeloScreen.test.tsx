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
      data: { data: sim.convocatoriasDisponibles.data },
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
// El acceso a la lista priorizada usa el router; acá basta con un ancla.
vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, ...resto }: { to: string; children?: ReactNode }) => (
    <a href={to} {...resto}>
      {children}
    </a>
  ),
}))

import { ApiError } from '@/store'

import { cliente, CONVOCATORIA, lote, montar, reiniciarCliente, responder, resumen, vigente, version } from './montar'

function nuevo(nombre: string) {
  return screen.getByRole('textbox', { name: `Nuevo peso de ${nombre}` })
}

async function esperarFormulario() {
  await screen.findByRole('heading', { name: 'Pesos por variable (puntos máximos)' })
}

describe('ParametrosModeloScreen', () => {
  beforeEach(() => reiniciarCliente())
  afterEach(() => vi.restoreAllMocks())

  it('muestra la versión vigente, los pesos actuales y la próxima versión', async () => {
    responder()
    montar()
    await esperarFormulario()

    expect(screen.getByRole('status', { name: 'Versión vigente' })).toHaveTextContent('Versión vigente v1.3')
    expect(screen.getByRole('columnheader', { name: 'Actual (v1.3)' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Nuevo (v1.4)' })).toBeInTheDocument()
    expect(nuevo('Ingresos familiares')).toHaveValue('30')
    expect(screen.getByRole('button', { name: 'Guardar versión v1.4' })).toBeEnabled()
    expect(screen.getByText('✓ válido')).toBeInTheDocument()
  })

  it('el puntaje mínimo se informa pero no se edita aquí', async () => {
    responder()
    montar()
    await esperarFormulario()

    expect(screen.getByLabelText('Puntaje mínimo para ser finalista')).toBeDisabled()
  })

  it('sin versión vigente deja la columna «Actual» vacía, los pesos vacíos y no deja guardar ni reprocesar', async () => {
    responder({ vigente: null, resumen: resumen({ modelo: null, porReprocesar: 0 }) })
    montar()
    await esperarFormulario()

    expect(screen.getByRole('status', { name: 'Versión vigente' })).toHaveTextContent('Sin versión vigente')
    expect(nuevo('Dependientes')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Guardar versión v1.0' })).toBeDisabled()
    expect(await screen.findByText(/Todavía no hay una versión vigente/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeDisabled()
  })

  it('si no suman 100 lo indica y bloquea «Guardar»', async () => {
    responder()
    const { user } = montar()
    await esperarFormulario()

    await user.clear(nuevo('Ingresos familiares'))
    await user.type(nuevo('Ingresos familiares'), '28')

    expect(screen.getByText('Deben sumar 100')).toBeInTheDocument()
    expect(screen.getByLabelText('Suma de los nuevos pesos')).toHaveTextContent('98')
    expect(screen.getByRole('button', { name: 'Guardar versión v1.4' })).toBeDisabled()
  })

  it('un valor inválido muestra el error inline recién al salir del campo', async () => {
    responder()
    const { user } = montar()
    await esperarFormulario()
    expect(screen.queryByText('Un número de 0 a 100 con hasta 2 decimales')).not.toBeInTheDocument()

    await user.clear(nuevo('Patrimonio'))
    await user.type(nuevo('Patrimonio'), 'abc')
    // * Con el foco todavía en el campo, el error no aparece.
    expect(screen.queryByText('Un número de 0 a 100 con hasta 2 decimales')).not.toBeInTheDocument()

    await user.tab()

    expect(await screen.findByText('Un número de 0 a 100 con hasta 2 decimales')).toBeInTheDocument()
  })

  it('guarda la versión nueva con las diez ponderaciones como números', async () => {
    responder()
    cliente.post.mockResolvedValue({ data: vigente({ version: 'v1.4', codigo: 'mod-14' }) })
    const { user } = montar()
    await esperarFormulario()

    await user.clear(nuevo('Ingresos familiares'))
    await user.type(nuevo('Ingresos familiares'), '28')
    await user.clear(nuevo('Promedio académico'))
    await user.type(nuevo('Promedio académico'), '12')
    await user.click(screen.getByRole('button', { name: 'Guardar versión v1.4' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    const [ruta, cuerpo] = cliente.post.mock.calls[0]
    expect(ruta).toBe('/modelos-preseleccion')
    expect(cuerpo.parametros).toEqual([
      { clave: 'ingresos_familiares', ponderacion: 28 },
      { clave: 'dependientes', ponderacion: 20 },
      { clave: 'procedencia', ponderacion: 15 },
      { clave: 'patrimonio', ponderacion: 15 },
      { clave: 'estado_vulnerabilidad', ponderacion: 10 },
      { clave: 'promedio_academico', ponderacion: 12 },
      { clave: 'dependencia_economica', ponderacion: 0 },
      { clave: 'ocupacion_sosten', ponderacion: 0 },
      { clave: 'discapacidad', ponderacion: 0 },
      { clave: 'hijos', ponderacion: 0 },
    ])
    expect(cuerpo).not.toHaveProperty('descripcion')
  })

  it('después de guardar vuelve a pedir la versión vigente y el historial', async () => {
    responder()
    cliente.post.mockResolvedValue({ data: vigente({ version: 'v1.4', codigo: 'mod-14' }) })
    const { user } = montar()
    await esperarFormulario()
    const antes = cliente.get.mock.calls.filter((c) => c[0] === '/modelos-preseleccion/vigente').length

    await user.click(screen.getByRole('button', { name: 'Guardar versión v1.4' }))

    await waitFor(() =>
      expect(cliente.get.mock.calls.filter((c) => c[0] === '/modelos-preseleccion/vigente').length).toBeGreaterThan(antes),
    )
  })

  it('si el backend rechaza los parámetros (ERR_MOD_01) muestra su mensaje', async () => {
    responder()
    cliente.post.mockRejectedValue(new ApiError('ERR_MOD_01', 'Los parámetros no son válidos: falta Patrimonio'))
    const { user } = montar()
    await esperarFormulario()

    await user.click(screen.getByRole('button', { name: 'Guardar versión v1.4' }))

    expect(await screen.findByText('Los parámetros no son válidos: falta Patrimonio')).toBeInTheDocument()
  })

  it('«Restablecer» vuelve a los pesos de la versión vigente', async () => {
    responder()
    const { user } = montar()
    await esperarFormulario()
    expect(screen.getByRole('button', { name: 'Restablecer' })).toBeDisabled()

    await user.clear(nuevo('Dependientes'))
    await user.type(nuevo('Dependientes'), '5')
    await user.click(screen.getByRole('button', { name: 'Restablecer' }))

    expect(nuevo('Dependientes')).toHaveValue('20')
  })

  it('el historial lista las versiones con su fecha y estado', async () => {
    responder()
    montar()

    const historial = await screen.findByRole('region', { name: 'Historial de versiones' })
    expect((await within(historial).findAllByText('v1.3')).length).toBeGreaterThan(0)
    expect(within(historial).getAllByText('Vigente').length).toBeGreaterThan(0)
    expect(within(historial).getAllByText('Archivada').length).toBeGreaterThan(0)
    expect(within(historial).getAllByText('01/07/2026').length).toBeGreaterThan(0)
  })

  it('el historial pide la página 1 de 10 sin filtros ni orden', async () => {
    responder({ historial: [version(1)] })
    montar()
    await screen.findByRole('region', { name: 'Historial de versiones' })

    await waitFor(() => {
      const llamada = cliente.get.mock.calls.find((c) => c[0] === '/modelos-preseleccion')
      expect(llamada?.[1]).toEqual({ params: { page: 1, pageSize: 10 } })
    })
  })

  describe('reprocesar', () => {
    it('muestra las calificadas y las que faltan por recalcular de la convocatoria', async () => {
      responder()
      montar()

      const resumenLista = await screen.findByLabelText('Resumen del reprocesamiento')
      expect(within(resumenLista).getByText('Calificadas:').nextSibling).toHaveTextContent('120')
      expect(within(resumenLista).getByText('Por recalcular:').nextSibling).toHaveTextContent('118')
      expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeEnabled()
    })

    it('pide confirmación y, al confirmar, arranca el lote de la convocatoria', async () => {
      responder()
      cliente.post.mockResolvedValue({ data: lote({ estado: 'PENDIENTE', procesadas: 0 }) })
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      expect(await screen.findByText('¿Reprocesar los puntajes?')).toBeInTheDocument()
      expect(screen.getByText(/Se recalcularán 118 postulaciones/)).toBeInTheDocument()
      expect(cliente.post).not.toHaveBeenCalled()
      await user.click(screen.getByRole('button', { name: 'Confirmar reprocesamiento' }))

      await waitFor(() => expect(cliente.post).toHaveBeenCalledWith(`/convocatorias/${CONVOCATORIA}/reprocesar`))
    })

    it('cancelar no reprocesa nada (A6)', async () => {
      responder()
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      await user.click(await screen.findByRole('button', { name: 'Cancelar' }))

      expect(cliente.post).not.toHaveBeenCalled()
    })

    it('muestra el avance del lote que acaba de arrancar', async () => {
      responder({ lote: lote({ estado: 'EN_PROCESO', total: 10, procesadas: 4, fallidas: 1 }) })
      cliente.post.mockResolvedValue({ data: lote({ estado: 'PENDIENTE', procesadas: 0 }) })
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      await user.click(await screen.findByRole('button', { name: 'Confirmar reprocesamiento' }))

      expect(await screen.findByText(/4 de 10 procesadas/)).toBeInTheDocument()
      expect(screen.getByText(/1 con error/)).toBeInTheDocument()
      expect(screen.getByRole('progressbar', { name: 'Postulaciones procesadas' })).toHaveAttribute('value', '4')
    })

    it('al terminar con fallidas las lista y ofrece la lista priorizada', async () => {
      responder({ lote: lote({ estado: 'COMPLETADO', total: 10, procesadas: 10, fallidas: 2 }) })
      cliente.post.mockResolvedValue({ data: lote({ estado: 'PENDIENTE', procesadas: 0 }) })
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      await user.click(await screen.findByRole('button', { name: 'Confirmar reprocesamiento' }))

      expect(await screen.findByText(/Reprocesamiento terminado con la versión v1.3/)).toBeInTheDocument()
      expect(screen.getByText(/2 postulaciones no se pudieron calcular/)).toBeInTheDocument()
      expect(screen.getByText('Ver la lista priorizada').closest('a')).toHaveAttribute('href', '/lista-priorizada')
    })

    it('un lote interrumpido lo avisa y deja volver a reprocesar', async () => {
      responder({ lote: lote({ estado: 'FALLIDO', total: 10, procesadas: 3 }) })
      cliente.post.mockResolvedValue({ data: lote({ estado: 'PENDIENTE', procesadas: 0 }) })
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      await user.click(await screen.findByRole('button', { name: 'Confirmar reprocesamiento' }))

      expect(await screen.findByText(/El reprocesamiento se interrumpió/)).toBeInTheDocument()
    })

    it('con los resultados ya publicados avisa y no deja reprocesar (A3)', async () => {
      responder({ resumen: resumen({ resultadosPublicados: true }) })
      montar()

      expect(await screen.findByText(/ya fueron publicados/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeDisabled()
    })

    it('con un lote en curso al abrir la pantalla lo sigue y no deja lanzar otro (A4)', async () => {
      const enCurso = lote({ estado: 'EN_PROCESO', total: 10, procesadas: 6 })
      responder({ resumen: resumen({ loteEnCurso: enCurso }), lote: enCurso })
      montar()

      expect(await screen.findByText(/6 de 10 procesadas/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeDisabled()
      expect(screen.getByText(/Hay un reprocesamiento en curso/)).toBeInTheDocument()
    })

    it('sin nada por recalcular lo dice y bloquea el botón', async () => {
      responder({ resumen: resumen({ porReprocesar: 0 }) })
      montar()

      expect(await screen.findByText(/No hay postulaciones calificadas por reprocesar/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeDisabled()
    })

    it('con cambios sin guardar pide guardar la versión antes de reprocesar', async () => {
      responder()
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.clear(nuevo('Dependientes'))
      await user.type(nuevo('Dependientes'), '25')

      expect(await screen.findByText(/Hay cambios sin guardar/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Reprocesar puntajes' })).toBeDisabled()
    })

    it('si el backend dice que ya no se puede (ERR_MOD_04) avisa y cierra el diálogo', async () => {
      responder()
      cliente.post.mockRejectedValue(new ApiError('ERR_MOD_04', 'publicados'))
      const { user } = montar()
      await screen.findByLabelText('Resumen del reprocesamiento')

      await user.click(screen.getByRole('button', { name: 'Reprocesar puntajes' }))
      await user.click(await screen.findByRole('button', { name: 'Confirmar reprocesamiento' }))

      await waitFor(() => expect(screen.queryByText('¿Reprocesar los puntajes?')).not.toBeInTheDocument())
    })
  })

  it('un error al cargar la versión vigente ofrece reintentar', async () => {
    responder()
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/modelos-preseleccion/vigente'
        ? Promise.reject(new ApiError('ERR_SYS_00', 'boom'))
        : Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 10, totalItems: 0, totalPages: 0 } } }),
    )
    montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error en el servidor')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
