import { screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./simulados')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./simulados')
  return { etiquetaRol: sim.etiquetaRolSimulada, useCerrarSesion: () => ({ mutate: sim.cerrarSesion }) }
})
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  DirectivoLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@/components/ui/toast', async () => (await import('./simulados')).avisos)
vi.mock('@/utils/descargarArchivo', async () => ({ descargarArchivo: (await import('./simulados')).descargar }))

import { ApiError } from '@/store'

import { cliente, indicadores, montar, reiniciarCliente, responder } from './montarPantalla'
import { avisos, descargar } from './simulados'

function pdfLlamadas() {
  return cliente.get.mock.calls.filter((c) => c[0] === '/dashboard/indicadores/pdf')
}

/** Monta la pantalla con indicadores y espera a que el botón se habilite. */
async function listo() {
  const vista = montar()
  await screen.findByText('5.230')
  await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled())
  return vista
}

describe('DescargarDashboardButton', () => {
  beforeEach(() => {
    reiniciarCliente()
    descargar.mockReset()
    Object.values(avisos).forEach((fn) => fn.mockReset())
  })

  it('manda exactamente la selección aplicada (no la que se edita) y descarga con el nombre del servidor', async () => {
    responder()
    const archivo = new Blob(['%PDF-1.5'])
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/indicadores/pdf'
        ? Promise.resolve({ data: archivo, headers: { 'content-disposition': 'attachment; filename="indicadores-x.pdf"' } })
        : base(ruta, config),
    )
    const { user } = await listo()
    // Se edita la convocatoria pero NO se aplica: el PDF sigue siendo el de la selección aplicada ({}).
    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: 'Beca A' }))

    await user.click(screen.getByRole('button', { name: 'Descargar PDF' }))

    await waitFor(() => expect(descargar).toHaveBeenCalledWith(archivo, 'indicadores-x.pdf'))
    expect(pdfLlamadas()[0][1]).toEqual({ params: {}, responseType: 'blob' })
    expect(avisos.toastSuccess).toHaveBeenCalledTimes(1)
  })

  it('tras aplicar un filtro, el PDF lleva esa selección', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/indicadores/pdf' ? Promise.resolve({ data: new Blob(['%PDF']), headers: {} }) : base(ruta, config),
    )
    const { user } = await listo()
    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: 'Beca B' }))
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled())

    await user.click(screen.getByRole('button', { name: 'Descargar PDF' }))

    await waitFor(() => expect(descargar).toHaveBeenCalledTimes(1))
    expect(pdfLlamadas()[0][1].params).toEqual({ 'filter[gestion]': 'g-2', 'filter[convocatoria]': 'c-b' })
  })

  it('queda bloqueado mientras genera y hace una sola descarga por clic', async () => {
    responder()
    let terminar: (valor: unknown) => void = () => {}
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/indicadores/pdf'
        ? new Promise((resolver) => (terminar = resolver))
        : base(ruta, config),
    )
    const { user } = await listo()

    await user.click(screen.getByRole('button', { name: 'Descargar PDF' }))
    const generando = await screen.findByRole('button', { name: 'Generando…' })
    expect(generando).toBeDisabled()
    await user.click(generando)
    expect(pdfLlamadas()).toHaveLength(1)

    terminar({ data: new Blob(['%PDF']), headers: {} })
    await waitFor(() => expect(descargar).toHaveBeenCalledTimes(1))
    expect(descargar.mock.calls[0][1]).toBe('indicadores.pdf')
  })

  it('ERR_DAS_02 avisa que no hay datos y no descarga nada', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/indicadores/pdf' ? Promise.reject(new ApiError('ERR_DAS_02', 'x')) : base(ruta, config),
    )
    const { user } = await listo()

    await user.click(screen.getByRole('button', { name: 'Descargar PDF' }))

    await waitFor(() => expect(avisos.toastWarning).toHaveBeenCalledWith('Nada que exportar', 'No hay datos para exportar.'))
    expect(descargar).not.toHaveBeenCalled()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled())
  })

  it('un error de red avisa con un mensaje claro, no descarga y permite reintentar', async () => {
    responder()
    const base = cliente.get.getMockImplementation()!
    cliente.get.mockImplementation((ruta: string, config?: unknown) =>
      ruta === '/dashboard/indicadores/pdf' ? Promise.reject(new AxiosError('Network Error')) : base(ruta, config),
    )
    const { user } = await listo()

    await user.click(screen.getByRole('button', { name: 'Descargar PDF' }))

    await waitFor(() =>
      expect(avisos.toastError).toHaveBeenCalledWith(
        'No se pudo descargar el PDF',
        expect.stringContaining('No se pudo conectar con el servidor'),
      ),
    )
    expect(descargar).not.toHaveBeenCalled()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled())
  })

  it('con sin datos el botón está deshabilitado', async () => {
    responder({ indicadores: indicadores({ sinDatos: true }) })
    montar()

    await screen.findByText('No hay datos para la selección elegida todavía.')
    expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeDisabled()
  })
})
