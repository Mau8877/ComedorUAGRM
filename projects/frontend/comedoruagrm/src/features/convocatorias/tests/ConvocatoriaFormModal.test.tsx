import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))

import { ApiError } from '@/store'

import { ConvocatoriaFormModal } from '../components/ConvocatoriaFormModal'
import type { ModoConvocatoria } from '../components/ConvocatoriaFormModal'
import type { ConvocatoriaDetalle } from '../types'

import { cliente, detalle, montar, reiniciarCliente, simularBackend } from './montar'

function abrir(modo: ModoConvocatoria, codigo?: string) {
  const alCerrar = vi.fn()
  const vista = montar(<ConvocatoriaFormModal open onOpenChange={alCerrar} modo={modo} codigo={codigo} />)
  return { ...vista, alCerrar }
}

function escribir(etiqueta: string, valor: string) {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } })
}

async function completarAlta(user: ReturnType<typeof abrir>['user']) {
  escribir('Nombre', 'Beca Comedor 2026-II')
  await user.click(screen.getByRole('combobox', { name: 'Gestión académica' }))
  await user.click(await screen.findByRole('option', { name: '2026-II' }))
  escribir('Fecha de apertura', '2026-08-01')
  escribir('Fecha de cierre', '2026-08-31')
  await user.click(screen.getByRole('combobox', { name: 'Tipo de documento 1' }))
  await user.click(await screen.findByRole('option', { name: 'Carnet de identidad' }))
}

describe('ConvocatoriaFormModal — crear', () => {
  beforeEach(() => {
    reiniciarCliente()
    simularBackend({})
    cliente.post.mockResolvedValue({ data: detalle(9) })
  })

  it('crea con los datos completos y manda los opcionales vacíos como null', async () => {
    const { user, alCerrar } = abrir('crear')
    await completarAlta(user)

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    expect(cliente.post).toHaveBeenCalledWith('/convocatorias', {
      nombre: 'Beca Comedor 2026-II',
      gestionCodigo: 'g2',
      fechaApertura: '2026-08-01',
      fechaCierre: '2026-08-31',
      cupos: null,
      puntajeMinimo: null,
      cantidadFinalistas: null,
      documentos: [{ tipoDocumentoCodigo: 't1', obligatorio: true }],
    })
    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('manda cupos, puntaje mínimo y cantidad de finalistas como números', async () => {
    const { user } = abrir('crear')
    await completarAlta(user)
    escribir('Cupos', '100')
    escribir('Puntaje mínimo', '40.5')
    escribir('Cantidad de finalistas', '120')

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalled())
    expect(cliente.post.mock.calls[0][1]).toMatchObject({ cupos: 100, puntajeMinimo: 40.5, cantidadFinalistas: 120 })
  })

  it('no envía y muestra los errores inline si faltan datos', async () => {
    const { user } = abrir('crear')

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    expect(await screen.findByText('El nombre es obligatorio')).toBeInTheDocument()
    expect(screen.getByText('La gestión académica es obligatoria')).toBeInTheDocument()
    expect(screen.getByText('La fecha de apertura es obligatoria')).toBeInTheDocument()
    expect(screen.getByText('La fecha de cierre es obligatoria')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('un campo que no se tocó no muestra error mientras se escribe en otro', async () => {
    abrir('crear')

    escribir('Nombre', 'B')

    expect(screen.queryByText('La fecha de apertura es obligatoria')).not.toBeInTheDocument()
  })

  it('la fecha de cierre igual a la de apertura se marca bajo el campo de cierre', async () => {
    const { user } = abrir('crear')
    await completarAlta(user)
    escribir('Fecha de cierre', '2026-08-01')

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    expect(await screen.findByText('La fecha de cierre debe ser posterior a la de apertura')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('no se puede enviar sin un documento obligatorio', async () => {
    const { user } = abrir('crear')
    await completarAlta(user)
    await user.click(screen.getByRole('checkbox'))

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    expect(await screen.findByText('Indica al menos un documento obligatorio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('un tipo de documento ya elegido no se ofrece en otra fila', async () => {
    const { user } = abrir('crear')
    await completarAlta(user)
    await user.click(screen.getByRole('button', { name: 'Agregar documento' }))

    await user.click(screen.getByRole('combobox', { name: 'Tipo de documento 2' }))

    expect(await screen.findByRole('option', { name: 'Certificado de notas' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Carnet de identidad' })).not.toBeInTheDocument()
  })

  it('se puede quitar un documento', async () => {
    const { user } = abrir('crear')
    await user.click(screen.getByRole('button', { name: 'Agregar documento' }))
    expect(screen.getByRole('combobox', { name: 'Tipo de documento 2' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Quitar documento 2' }))

    expect(screen.queryByRole('combobox', { name: 'Tipo de documento 2' })).not.toBeInTheDocument()
  })

  it.each([
    ['ERR_CV_04', 'La fecha de cierre debe ser posterior a la de apertura'],
    ['ERR_CV_05', 'Indica al menos un documento obligatorio'],
  ])('si el backend responde %s muestra su mensaje y conserva lo escrito', async (codigo, mensaje) => {
    cliente.post.mockRejectedValue(new ApiError(codigo, mensaje))
    const { user, alCerrar } = abrir('crear')
    await completarAlta(user)

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    expect((await screen.findAllByText(mensaje)).length).toBeGreaterThan(0)
    expect(screen.getByLabelText('Nombre')).toHaveValue('Beca Comedor 2026-II')
    expect(alCerrar).not.toHaveBeenCalledWith(false)
  })

  it('con un error de conexión ofrece reintentar', async () => {
    cliente.post.mockRejectedValueOnce(Object.assign(new Error('Network Error'), { isAxiosError: true }))
    const { user } = abrir('crear')
    await completarAlta(user)

    await user.click(screen.getByRole('button', { name: 'Crear convocatoria' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar')
  })

  it('un doble clic envía una sola vez', async () => {
    // Con la primera petición todavía en curso (nunca responde), el segundo clic no debe enviar otra.
    cliente.post.mockReturnValue(new Promise(() => {}))
    const { user } = abrir('crear')
    await completarAlta(user)

    await user.dblClick(screen.getByRole('button', { name: 'Crear convocatoria' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalled())
    expect(cliente.post).toHaveBeenCalledTimes(1)
  })
})

describe('ConvocatoriaFormModal — editar y ver', () => {
  beforeEach(() => {
    reiniciarCliente()
  })

  function conDetalle(d: ConvocatoriaDetalle) {
    simularBackend({ detalles: [d] })
    cliente.put.mockResolvedValue({ data: d })
  }

  it('carga el detalle y precarga los valores, incluidos los documentos', async () => {
    conDetalle(detalle(1, 'BORRADOR', { cupos: 100, puntajeMinimo: 40.5, cantidadFinalistas: 120 }))
    abrir('editar', 'c1')

    expect(await screen.findByLabelText('Nombre')).toHaveValue('Beca Comedor 1')
    expect(screen.getByLabelText('Fecha de apertura')).toHaveValue('2026-08-01')
    expect(screen.getByLabelText('Fecha de cierre')).toHaveValue('2026-08-31')
    expect(screen.getByLabelText('Cupos')).toHaveValue('100')
    expect(screen.getByLabelText('Puntaje mínimo')).toHaveValue('40.5')
    expect(screen.getByLabelText('Cantidad de finalistas')).toHaveValue('120')
    expect(screen.getByRole('combobox', { name: 'Tipo de documento 1' })).toHaveTextContent('Carnet de identidad')
  })

  it('guarda con PUT la convocatoria completa y un opcional vaciado va como null', async () => {
    conDetalle(detalle(1, 'BORRADOR'))
    const { user, alCerrar } = abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')

    escribir('Cupos', '')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(cliente.put).toHaveBeenCalledTimes(1))
    expect(cliente.put).toHaveBeenCalledWith('/convocatorias/c1', {
      nombre: 'Beca Comedor 1',
      gestionCodigo: 'g2',
      fechaApertura: '2026-08-01',
      fechaCierre: '2026-08-31',
      cupos: null,
      puntajeMinimo: 40.5,
      cantidadFinalistas: 120,
      documentos: [{ tipoDocumentoCodigo: 't1', obligatorio: true }],
    })
    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('con postulaciones, la fecha de apertura y los documentos quedan deshabilitados y se explica por qué', async () => {
    conDetalle(detalle(1, 'ABIERTA', { tienePostulaciones: true }))
    abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')

    expect(screen.getByRole('note')).toHaveTextContent('ya tiene postulaciones')
    expect(screen.getByLabelText('Fecha de apertura')).toBeDisabled()
    expect(screen.getByRole('combobox', { name: 'Tipo de documento 1' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Agregar documento' })).not.toBeInTheDocument()
    // Lo demás sí se edita.
    expect(screen.getByLabelText('Nombre')).toBeEnabled()
    expect(screen.getByLabelText('Fecha de cierre')).toBeEnabled()
  })

  it('en una convocatoria abierta la gestión no se puede cambiar', async () => {
    conDetalle(detalle(1, 'ABIERTA'))
    abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')

    expect(screen.getByRole('combobox', { name: 'Gestión académica' })).toBeDisabled()
    expect(screen.getByText(/solo se cambia en una convocatoria en borrador/)).toBeInTheDocument()
  })

  it('en una borrador la gestión sí se puede cambiar', async () => {
    conDetalle(detalle(1, 'BORRADOR'))
    abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')

    expect(screen.getByRole('combobox', { name: 'Gestión académica' })).toBeEnabled()
  })

  it.each(['CERRADA', 'PUBLICADA'] as const)('una %s se ve en solo lectura y sin botón de guardar', async (estado) => {
    conDetalle(detalle(1, estado))
    abrir('ver', 'c1')

    const nombre = await screen.findByLabelText('Nombre')

    expect(nombre).toBeDisabled()
    expect(screen.getByLabelText('Cupos')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Agregar documento' })).not.toBeInTheDocument()
  })

  it('si el backend dice que cambió de estado (ERR_CV_08) avisa, cierra y refresca', async () => {
    conDetalle(detalle(1, 'BORRADOR'))
    cliente.put.mockRejectedValue(new ApiError('ERR_CV_08', 'Solo se puede editar una convocatoria en borrador o abierta'))
    const { user, alCerrar } = abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')
    cliente.get.mockClear()

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
    await waitFor(() => expect(cliente.get).toHaveBeenCalled())
  })

  it('con postulaciones, si el backend responde ERR_CV_09 avisa, cierra y refresca', async () => {
    conDetalle(detalle(1, 'ABIERTA'))
    cliente.put.mockRejectedValue(new ApiError('ERR_CV_09', 'La convocatoria ya tiene postulaciones'))
    const { user, alCerrar } = abrir('editar', 'c1')
    await screen.findByLabelText('Nombre')

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('si no se puede cargar el detalle muestra el error y permite reintentar', async () => {
    cliente.get.mockImplementation((ruta: string) =>
      ruta.startsWith('/convocatorias/') ? Promise.reject(new ApiError('ERR_CV_01', 'La convocatoria indicada no existe')) : Promise.resolve({ data: { data: [], meta: {} } }),
    )
    abrir('editar', 'c1')

    const aviso = await screen.findByRole('alert')

    expect(within(aviso).getByText('La convocatoria indicada no existe')).toBeInTheDocument()
    expect(within(aviso).getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
