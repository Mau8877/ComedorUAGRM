import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))

import { ApiError } from '@/store'

import { CambiarEstadoConvocatoriaDialog } from '../components/CambiarEstadoConvocatoriaDialog'
import type { AccionEstadoConvocatoria } from '../components/CambiarEstadoConvocatoriaDialog'
import { EliminarConvocatoriaDialog } from '../components/EliminarConvocatoriaDialog'

import { cliente, convocatoria, montar, reiniciarCliente } from './montar'

function cambiarEstado(accion: AccionEstadoConvocatoria) {
  const alCerrar = vi.fn()
  const vista = montar(
    <CambiarEstadoConvocatoriaDialog
      open
      onOpenChange={alCerrar}
      convocatoria={convocatoria(1, accion === 'abrir' ? 'BORRADOR' : 'ABIERTA')}
      accion={accion}
    />,
  )
  return { ...vista, alCerrar }
}

function eliminar() {
  const alCerrar = vi.fn()
  const vista = montar(<EliminarConvocatoriaDialog open onOpenChange={alCerrar} convocatoria={convocatoria(1)} />)
  return { ...vista, alCerrar }
}

describe('CambiarEstadoConvocatoriaDialog — abrir', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue({ data: { data: [], meta: {} } })
  })

  it('pide confirmación y no llama al backend hasta confirmar', async () => {
    cambiarEstado('abrir')

    expect(await screen.findByRole('dialog', { name: /Abrir convocatoria/ })).toBeInTheDocument()
    expect(screen.getByText(/empezará a recibir postulaciones/)).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('confirmar hace POST /abrir con el código y cierra el diálogo', async () => {
    cliente.post.mockResolvedValue({ data: {} })
    const { user, alCerrar } = cambiarEstado('abrir')

    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c1/abrir'))
    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('cancelar no llama a nada', async () => {
    const { user, alCerrar } = cambiarEstado('abrir')

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(cliente.post).not.toHaveBeenCalled()
    expect(alCerrar).toHaveBeenCalledWith(false)
  })

  it('con otra convocatoria abierta en la gestión (ERR_CV_07) muestra el aviso y no cierra', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_CV_07', 'Ya hay otra convocatoria abierta en esa gestión académica'))
    const { user, alCerrar } = cambiarEstado('abrir')

    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya hay otra convocatoria abierta en esa gestión académica')
    expect(alCerrar).not.toHaveBeenCalledWith(false)
  })

  it('si ya cambió de estado (ERR_CV_08) avisa, cierra y refresca la lista', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_CV_08', 'Solo se puede abrir una convocatoria en borrador'))
    const { user, alCerrar, queryClient } = cambiarEstado('abrir')
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')

    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
    expect(invalidar).toHaveBeenCalled()
  })

  it('un doble clic en confirmar envía una sola vez', async () => {
    cliente.post.mockReturnValue(new Promise(() => {}))
    const { user } = cambiarEstado('abrir')

    await user.dblClick(screen.getByRole('button', { name: 'Abrir' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalled())
    expect(cliente.post).toHaveBeenCalledTimes(1)
  })
})

describe('CambiarEstadoConvocatoriaDialog — cerrar', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue({ data: { data: [], meta: {} } })
  })

  it('advierte que los borradores no enviados quedan bloqueados', async () => {
    cambiarEstado('cerrar')

    expect(await screen.findByText(/borradores que no se enviaron quedarán bloqueados/)).toBeInTheDocument()
  })

  it('confirmar hace POST /cerrar con el código y cierra el diálogo', async () => {
    cliente.post.mockResolvedValue({ data: {} })
    const { user, alCerrar } = cambiarEstado('cerrar')

    await user.click(screen.getByRole('button', { name: 'Cerrar convocatoria' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledWith('/convocatorias/c1/cerrar'))
    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('con un error de conexión ofrece reintentar y no cierra', async () => {
    cliente.post.mockRejectedValueOnce(Object.assign(new Error('Network Error'), { isAxiosError: true }))
    const { user, alCerrar } = cambiarEstado('cerrar')

    await user.click(screen.getByRole('button', { name: 'Cerrar convocatoria' }))

    expect(await screen.findByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
    expect(alCerrar).not.toHaveBeenCalledWith(false)
  })
})

describe('EliminarConvocatoriaDialog', () => {
  beforeEach(() => {
    reiniciarCliente()
    cliente.get.mockResolvedValue({ data: { data: [], meta: {} } })
  })

  it('pide confirmación y explica que solo se eliminan las de borrador y sin postulaciones', async () => {
    eliminar()

    expect(await screen.findByText('¿Eliminar convocatoria?')).toBeInTheDocument()
    expect(screen.getByText(/en borrador y sin postulaciones/)).toBeInTheDocument()
    expect(cliente.delete).not.toHaveBeenCalled()
  })

  it('confirmar hace DELETE con el código y cierra el diálogo', async () => {
    cliente.delete.mockResolvedValue({ data: null })
    const { user, alCerrar } = eliminar()

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(cliente.delete).toHaveBeenCalledWith('/convocatorias/c1'))
    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
  })

  it('con postulaciones (ERR_CV_10) avisa, cierra y refresca la lista', async () => {
    cliente.delete.mockRejectedValue(new ApiError('ERR_CV_10', 'La convocatoria tiene postulaciones'))
    const { user, alCerrar, queryClient } = eliminar()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(alCerrar).toHaveBeenCalledWith(false))
    expect(invalidar).toHaveBeenCalled()
  })

  it('con un error de conexión ofrece reintentar y no cierra', async () => {
    cliente.delete.mockRejectedValueOnce(Object.assign(new Error('Network Error'), { isAxiosError: true }))
    const { user, alCerrar } = eliminar()

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))

    expect(await screen.findByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
    expect(alCerrar).not.toHaveBeenCalledWith(false)
  })
})
