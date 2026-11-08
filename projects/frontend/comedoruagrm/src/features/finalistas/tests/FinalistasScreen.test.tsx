import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
// El menú de cuenta (CU15) navega con el router; acá no se prueba, así que se simula.
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@/utils/descargarArchivo', () => ({ descargarArchivo: vi.fn() }))
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

import { FinalistasScreen } from '../screens/FinalistasScreen'
import { hoyIso, mananaIso } from '../schemas'
import type { Finalista } from '../types'
import { cliente } from './clienteSimulado'

const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@dubss.com', rol: 'ADMINISTRADOR_DUBSS' }

const CONVOCATORIAS = [
  { codigo: 'conv-2', nombre: 'Beca 2027-I', gestion: '2027-I', estado: 'ABIERTA', fechaApertura: '', fechaCierre: '' },
  { codigo: 'conv-1', nombre: 'Beca 2026-II', gestion: '2026-II', estado: 'CERRADA', fechaApertura: '', fechaCierre: '' },
]

function finalista(n: number, extra: Partial<Finalista> = {}): Finalista {
  return {
    codigo: `fin-${n}`,
    posicion: n,
    nombres: `Nombre${n}`,
    apellidos: `Apellido${n}`,
    ci: `${9000 + n}`,
    puntaje: 95 - n,
    estadoNotificacion: 'SIN_ENVIAR',
    estadoPostulacion: 'calificada',
    resultadoAuditoria: 'SIN_AUDITAR',
    puntajeTotal: null,
    plazoEntrega: null,
    notificadoEn: null,
    documentacionRecibidaEn: null,
    ...extra,
  }
}

function responder(filas: Finalista[], convocatorias = CONVOCATORIAS) {
  cliente.get.mockImplementation((ruta: string) => {
    switch (ruta) {
      case '/lista-priorizada/convocatorias':
        return Promise.resolve({ data: convocatorias })
      case '/finalistas':
        return Promise.resolve({
          data: { data: filas, meta: { page: 1, pageSize: 20, totalItems: filas.length, totalPages: 1 } },
        })
      default:
        return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
    }
  })
}

function montar(onAuditar?: (finalista: Finalista) => void) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <FinalistasScreen titulo="Finalistas" activeHref="/finalistas" usuarioActual={yo} onAuditar={onAuditar} />
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const ultimaConsulta = () => {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/finalistas')
  return llamadas[llamadas.length - 1][1].params
}

describe('FinalistasScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
    cliente.patch.mockReset()
  })

  it('lista los finalistas de la convocatoria más reciente con posición, puntaje y estados (CA01)', async () => {
    responder([
      finalista(1, { estadoNotificacion: 'ENVIADA', documentacionRecibidaEn: '2026-11-05' }),
      finalista(2, { estadoNotificacion: 'FALLIDA' }),
      finalista(3),
    ])
    montar()

    expect(await screen.findAllByText('Nombre1 Apellido1')).not.toHaveLength(0)
    expect(ultimaConsulta()).toEqual({ 'filter[convocatoriaId]': 'conv-2', page: 1, pageSize: 20 })
    expect(screen.getAllByText('Enviada').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Fallida').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Sin enviar').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Recibida').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Pendiente').length).toBeGreaterThan(0)
    expect(screen.getAllByText('94.00').length).toBeGreaterThan(0)
  })

  it('muestra el resultado de la auditoría y el total junto al puntaje (CU07)', async () => {
    responder([
      finalista(1, { resultadoAuditoria: 'VALIDADO', puntajeTotal: 80.5 }),
      finalista(2, { resultadoAuditoria: 'OBSERVADO' }),
      finalista(3),
    ])
    montar()

    expect(await screen.findAllByText('Validado')).not.toHaveLength(0)
    expect(screen.getAllByText('Observado').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Sin auditar').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Total 80.50').length).toBeGreaterThan(0)
  })

  it('una postulación devuelta muestra la insignia «Devuelta» en la columna Auditoría (CU19)', async () => {
    responder([finalista(1, { estadoPostulacion: 'devuelta', resultadoAuditoria: 'OBSERVADO' }), finalista(2)])
    montar()

    expect((await screen.findAllByText('Devuelta')).length).toBeGreaterThan(0)
    expect(screen.queryAllByText('Observado')).toHaveLength(0)
    expect(screen.getAllByText('Sin auditar').length).toBeGreaterThan(0)
  })

  it('«Auditar» abre el expediente del finalista elegido (CU07)', async () => {
    responder([finalista(1), finalista(2)])
    const onAuditar = vi.fn()
    const user = montar(onAuditar)

    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getAllByRole('button', { name: 'Auditar' })[0])

    expect(onAuditar).toHaveBeenCalledWith(expect.objectContaining({ posicion: 1 }))
  })

  it('sin la acción de auditar la tabla no ofrece el botón', async () => {
    responder([finalista(1)])
    montar()

    await screen.findAllByText('Nombre1 Apellido1')
    expect(screen.queryByRole('button', { name: 'Auditar' })).not.toBeInTheDocument()
  })

  it('no manda sort ni otros filtros', async () => {
    responder([finalista(1)])
    montar()
    await screen.findAllByText('Nombre1 Apellido1')

    expect(Object.keys(ultimaConsulta()).sort()).toEqual(['filter[convocatoriaId]', 'page', 'pageSize'])
  })

  it('sin convocatorias avisa que todavía no hay', async () => {
    responder([], [])
    montar()

    expect(await screen.findByText('Todavía no hay convocatorias registradas.')).toBeInTheDocument()
  })

  it('una convocatoria sin finalistas lo dice y no muestra la tabla (A10)', async () => {
    responder([])
    montar()

    expect(await screen.findByText('Todavía no hay finalistas en esta convocatoria.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    // El flujo C sigue disponible.
    expect(screen.getByRole('button', { name: 'Revisar y publicar' })).toBeInTheDocument()
  })

  it('un error de conexión al listar muestra el mensaje en la tabla', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/lista-priorizada/convocatorias'
        ? Promise.resolve({ data: CONVOCATORIAS })
        : Promise.reject(new ApiError('ERR_SYS_00', 'Ocurrió un error inesperado')),
    )
    montar()

    expect(await screen.findAllByText('Ocurrió un error inesperado')).not.toHaveLength(0)
  })

  it('«Notificar seleccionados» está apagado sin selección y notifica a los elegidos tras confirmar (CA02)', async () => {
    responder([finalista(1), finalista(2)])
    cliente.post.mockResolvedValue({ data: { solicitados: 2, enviados: 2, fallidos: 0 } })
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    expect(screen.getByRole('button', { name: /Notificar seleccionados/ })).toBeDisabled()

    await user.click(screen.getAllByRole('checkbox', { name: 'Seleccionar a Nombre1 Apellido1' })[0])
    await user.click(screen.getAllByRole('checkbox', { name: 'Seleccionar a Nombre2 Apellido2' })[0])
    await user.click(screen.getByRole('button', { name: 'Notificar seleccionados (2)' }))

    const dialogo = await screen.findByRole('dialog', { name: /Notificar seleccionados/ })
    fireEvent.change(within(dialogo).getByLabelText('Plazo de entrega'), { target: { value: mananaIso() } })
    await user.type(within(dialogo).getByLabelText('Lugar y horario de entrega'), 'DUBSS oficina 3')
    await user.type(within(dialogo).getByLabelText('Documentos físicos a presentar'), 'CI y certificado')
    await user.click(within(dialogo).getByRole('button', { name: 'Enviar aviso' }))

    // Antes de enviar pide confirmación.
    expect(cliente.post).not.toHaveBeenCalled()
    await user.click(await screen.findByRole('button', { name: 'Confirmar envío' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post).toHaveBeenCalledWith('/finalistas/notificaciones', {
      finalistas: expect.arrayContaining(['fin-1', 'fin-2']),
      plazoEntrega: mananaIso(),
      lugarHorario: 'DUBSS oficina 3',
      documentos: 'CI y certificado',
    })
    expect(await screen.findByText(/Se enviaron/)).toHaveTextContent('Se enviaron 2 avisos y fallaron 0.')
  })

  it('no deja enviar con campos vacíos ni con el plazo de hoy y no manda nada (A6, CA05, CA06)', async () => {
    responder([finalista(1)])
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getAllByRole('checkbox', { name: 'Seleccionar a Nombre1 Apellido1' })[0])
    await user.click(screen.getByRole('button', { name: /Notificar seleccionados/ }))
    const dialogo = await screen.findByRole('dialog', { name: /Notificar seleccionados/ })

    await user.click(within(dialogo).getByRole('button', { name: 'Enviar aviso' }))
    expect(await within(dialogo).findByText('El plazo de entrega es obligatorio')).toBeInTheDocument()
    expect(within(dialogo).getByText('El lugar y horario de entrega son obligatorios')).toBeInTheDocument()
    expect(within(dialogo).getByText('Indica los documentos que deben presentar')).toBeInTheDocument()

    fireEvent.change(within(dialogo).getByLabelText('Plazo de entrega'), { target: { value: hoyIso() } })
    await user.click(within(dialogo).getByRole('button', { name: 'Enviar aviso' }))
    expect(await within(dialogo).findByText('El plazo debe ser posterior a hoy')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar envío' })).not.toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('«Seleccionar fallidos» deja marcados solo a los fallidos para reintentar (A2, CA04)', async () => {
    responder([finalista(1, { estadoNotificacion: 'ENVIADA' }), finalista(2, { estadoNotificacion: 'FALLIDA' })])
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')

    await user.click(screen.getByRole('button', { name: 'Seleccionar fallidos' }))

    expect(screen.getByRole('button', { name: 'Notificar seleccionados (1)' })).toBeInTheDocument()
    expect(screen.getAllByRole('checkbox', { name: 'Seleccionar a Nombre2 Apellido2' })[0]).toBeChecked()
    expect(screen.getAllByRole('checkbox', { name: 'Seleccionar a Nombre1 Apellido1' })[0]).not.toBeChecked()
  })

  it('registra la entrega con la fecha de hoy por defecto y la muestra como recibida (CA07, CA08)', async () => {
    responder([finalista(1, { estadoNotificacion: 'ENVIADA' })])
    cliente.patch.mockResolvedValue({ data: finalista(1, { documentacionRecibidaEn: hoyIso() }) })
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    expect(screen.getByText(/Elige un finalista de la tabla/)).toBeInTheDocument()

    await user.click(screen.getAllByRole('button', { name: 'Registrar entrega' })[0])
    expect(screen.getByLabelText('Fecha de recepción')).toHaveValue(hoyIso())
    await user.type(screen.getByLabelText('Observaciones (opcional)'), 'Todo completo')
    await user.click(screen.getByRole('button', { name: 'Marcar como recibida' }))

    await waitFor(() => expect(cliente.patch).toHaveBeenCalledTimes(1))
    expect(cliente.patch).toHaveBeenCalledWith('/finalistas/fin-1/entrega', {
      fechaRecepcion: hoyIso(),
      observaciones: 'Todo completo',
    })
    // El panel pasa a «corregir» y avisa que la fecha anterior queda en la bitácora (A5).
    expect(await screen.findByRole('button', { name: 'Corregir la fecha' })).toBeInTheDocument()
    expect(screen.getByText(/la anterior queda en la bitácora/)).toBeInTheDocument()
  })

  it('no deja registrar una fecha de recepción futura (A7, CA09)', async () => {
    responder([finalista(1)])
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.click(screen.getAllByRole('button', { name: 'Registrar entrega' })[0])

    fireEvent.change(screen.getByLabelText('Fecha de recepción'), { target: { value: mananaIso() } })
    await user.click(screen.getByRole('button', { name: 'Marcar como recibida' }))

    expect(await screen.findByText('La fecha de recepción no puede ser futura')).toBeInTheDocument()
    expect(cliente.patch).not.toHaveBeenCalled()
  })

  it('publica un comunicado con vista previa y confirmación, y dice a cuántos llegó (CA11)', async () => {
    responder([finalista(1)])
    cliente.post.mockResolvedValue({ data: { codigo: 'c-1', totalDestinatarios: 120 } })
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')

    await user.type(screen.getByLabelText('Título'), 'Nuevo horario')
    await user.type(screen.getByLabelText('Contenido'), 'El comedor abre a las 11')
    expect(screen.getByLabelText('Vista previa')).toHaveTextContent('Nuevo horario')
    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))

    expect(cliente.post).not.toHaveBeenCalled()
    await user.click(await screen.findByRole('button', { name: 'Confirmar publicación' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post).toHaveBeenCalledWith('/comunicados', {
      titulo: 'Nuevo horario',
      contenido: 'El comedor abre a las 11',
      destinatarios: 'TODOS',
      convocatoriaId: 'conv-2',
    })
    expect(await screen.findByText('Se envió a 120 destinatarios.')).toBeInTheDocument()
  })

  it('un comunicado sin título ni contenido no se publica (CA12)', async () => {
    responder([finalista(1)])
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')

    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))

    expect(await screen.findByText('El título es obligatorio')).toBeInTheDocument()
    expect(screen.getByText('El contenido es obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('si el backend rechaza el comunicado muestra su mensaje y no pierde lo escrito (CA13)', async () => {
    responder([finalista(1)])
    cliente.post.mockRejectedValue(new ApiError('ERR_COM_01', 'sin destinatarios'))
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    await user.type(screen.getByLabelText('Título'), 'Aviso')
    await user.type(screen.getByLabelText('Contenido'), 'Texto')
    await user.click(screen.getByRole('button', { name: 'Revisar y publicar' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar publicación' }))

    expect(await screen.findByText('No hay destinatarios para ese comunicado. No se publicó nada.')).toBeInTheDocument()
    expect(screen.getByLabelText('Título')).toHaveValue('Aviso')
  })

  it('la búsqueda consulta una sola vez al dejar de teclear y «Limpiar búsqueda» la quita', async () => {
    responder([finalista(1)])
    const user = montar()
    await screen.findAllByText('Nombre1 Apellido1')
    const antes = cliente.get.mock.calls.filter((c) => c[0] === '/finalistas').length

    cliente.get.mockImplementation((ruta: string) =>
      ruta === '/lista-priorizada/convocatorias'
        ? Promise.resolve({ data: CONVOCATORIAS })
        : Promise.resolve({ data: { data: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 } } }),
    )
    await user.type(screen.getByPlaceholderText('Buscar por nombre o CI...'), 'zzz')
    await screen.findByRole('button', { name: 'Limpiar búsqueda' })

    const despues = cliente.get.mock.calls.filter((c) => c[0] === '/finalistas').length
    expect(despues - antes).toBe(1)
    expect(ultimaConsulta().search).toBe('zzz')

    responder([finalista(1)])
    await user.click(screen.getByRole('button', { name: 'Limpiar búsqueda' }))
    await screen.findAllByText('Nombre1 Apellido1')
    expect(ultimaConsulta().search).toBeUndefined()
    expect(screen.getByPlaceholderText('Buscar por nombre o CI...')).toHaveValue('')
  })

  it('si la convocatoria ya no existe (404) avisa y vuelve al selector con la otra convocatoria (A9)', async () => {
    let conv2Eliminada = false
    cliente.get.mockImplementation((ruta: string, opciones?: { params?: Record<string, string> }) => {
      if (ruta === '/lista-priorizada/convocatorias') {
        return Promise.resolve({ data: conv2Eliminada ? CONVOCATORIAS.slice(1) : CONVOCATORIAS })
      }
      if (opciones?.params?.['filter[convocatoriaId]'] === 'conv-2') {
        conv2Eliminada = true
        return Promise.reject(new ApiError('ERR_FIN_02', 'La convocatoria no existe'))
      }
      return Promise.resolve({
        data: { data: [finalista(1)], meta: { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 } },
      })
    })
    montar()

    expect(await screen.findByText('La convocatoria ya no existe. Elige otra.')).toBeInTheDocument()
    expect(await screen.findAllByText('Nombre1 Apellido1')).not.toHaveLength(0)
    expect(ultimaConsulta()['filter[convocatoriaId]']).toBe('conv-1')
  })
})
