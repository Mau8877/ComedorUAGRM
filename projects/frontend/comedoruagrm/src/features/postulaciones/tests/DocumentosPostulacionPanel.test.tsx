import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))

import { ApiError } from '@/store'

import { DocumentosPostulacionPanel } from '../components/DocumentosPostulacionPanel'
import type { DocumentosPostulacion } from '../types'

import { cliente } from './clienteSimulado'

const REGLAS = {
  tamanoMaximoBytes: 1024,
  formatosPermitidos: ['application/pdf', 'image/jpeg', 'image/png'],
  maximoArchivosPorTipo: 2,
}

function documentos(puedeEditar = true): DocumentosPostulacion {
  return {
    puedeEditar,
    reglas: REGLAS,
    requeridos: [
      {
        tipoDocumentoCodigo: 'tipo-carnet',
        nombre: 'Carnet de identidad',
        descripcion: 'Ambas caras',
        obligatorio: true,
        adjuntado: true,
        archivos: [
          {
            codigo: 'doc-1',
            nombreArchivo: 'carnet.pdf',
            mimeType: 'application/pdf',
            tamanoBytes: 2048,
            subidoEn: '2026-07-16T15:00:00Z',
          },
        ],
      },
      {
        tipoDocumentoCodigo: 'tipo-notas',
        nombre: 'Certificado de notas',
        descripcion: null,
        obligatorio: true,
        adjuntado: false,
        archivos: [],
      },
      {
        tipoDocumentoCodigo: 'tipo-luz',
        nombre: 'Fotocopia de luz',
        descripcion: null,
        obligatorio: false,
        adjuntado: false,
        archivos: [],
      },
    ],
  }
}

function montar(datos: DocumentosPostulacion = documentos()) {
  cliente.get.mockResolvedValue({ data: datos })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <DocumentosPostulacionPanel codigo="post-1" />
    </QueryClientProvider>,
  )
  return { user: userEvent.setup() }
}

function pdf(nombre = 'notas.pdf', tipo = 'application/pdf', bytes = 100) {
  return new File([new Uint8Array(bytes)], nombre, { type: tipo })
}

describe('DocumentosPostulacionPanel', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
    cliente.put.mockReset()
    cliente.delete.mockReset()
  })

  it('muestra los requeridos con su estado y cuántos se adjuntaron', async () => {
    montar()

    expect(await screen.findByText('Adjuntos (1 de 3)')).toBeInTheDocument()
    expect(screen.getByText('Carnet de identidad')).toBeInTheDocument()
    expect(screen.getByText('Adjuntado')).toBeInTheDocument()
    expect(screen.getByText('Falta')).toBeInTheDocument()
    expect(screen.getByText('Opcional')).toBeInTheDocument()
    expect(screen.getByText('carnet.pdf')).toBeInTheDocument()
    expect(screen.getByText(/2 KB/)).toBeInTheDocument()
  })

  it('con la postulación no editable solo se puede ver y abrir', async () => {
    montar(documentos(false))

    expect(await screen.findByText(/ya no se puede modificar/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Abrir carnet.pdf' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Adjuntar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Reemplazar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Eliminar/ })).not.toBeInTheDocument()
  })

  it('al llegar al máximo de archivos de un documento ya no ofrece agregar otro', async () => {
    const datos = documentos()
    datos.requeridos[0].archivos.push({ ...datos.requeridos[0].archivos[0], codigo: 'doc-2', nombreArchivo: 'reverso.pdf' })
    montar(datos)

    await screen.findByText('Adjuntos (1 de 3)')

    expect(screen.queryByRole('button', { name: 'Agregar otro archivo' })).not.toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Adjuntar' })).toHaveLength(2)
  })

  it('adjunta un archivo válido: lo valida, lo sube como FormData y actualiza la lista', async () => {
    const { user } = montar()
    cliente.post.mockResolvedValue({ data: { codigo: 'doc-9' } })
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getAllByRole('button', { name: 'Adjuntar' })[0])
    const dialogo = await screen.findByRole('dialog')
    await user.upload(within(dialogo).getByLabelText('Archivo'), pdf())
    await user.click(within(dialogo).getByRole('button', { name: 'Adjuntar documento' }))

    await waitFor(() => expect(cliente.post).toHaveBeenCalledTimes(1))
    const [url, cuerpo] = cliente.post.mock.calls[0]
    expect(url).toBe('/postulaciones/post-1/documentos')
    expect((cuerpo as FormData).get('tipoDocumento')).toBe('tipo-notas')
    expect((cuerpo as FormData).get('archivo')).toBeInstanceOf(File)
    await waitFor(() => expect(cliente.get.mock.calls.length).toBeGreaterThan(1))
  })

  it('un archivo de formato no permitido muestra el motivo y no se sube', async () => {
    const { user } = montar()
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getAllByRole('button', { name: 'Adjuntar' })[0])
    const dialogo = await screen.findByRole('dialog')
    // `applyAccept: false`: el navegador ya filtra por `accept`, el test simula a quien lo esquiva.
    const entrada = within(dialogo).getByLabelText('Archivo')
    await userEvent.setup({ applyAccept: false }).upload(entrada, pdf('nota.txt', 'text/plain'))
    await user.click(within(dialogo).getByRole('button', { name: 'Adjuntar documento' }))

    expect(await within(dialogo).findByText(/Formato no permitido/)).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('un archivo demasiado grande muestra el tamaño máximo y no se sube', async () => {
    const { user } = montar()
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getAllByRole('button', { name: 'Adjuntar' })[0])
    const dialogo = await screen.findByRole('dialog')
    await user.upload(within(dialogo).getByLabelText('Archivo'), pdf('grande.pdf', 'application/pdf', 2000))
    await user.click(within(dialogo).getByRole('button', { name: 'Adjuntar documento' }))

    expect(await within(dialogo).findByText(/supera el tamaño máximo de 1 KB/)).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('si el servidor rechaza el archivo, el modal muestra el motivo y conserva el archivo elegido', async () => {
    const { user } = montar()
    cliente.post.mockRejectedValue(new ApiError('ERR_DOC_03', 'El contenido del archivo no corresponde a su formato'))
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getAllByRole('button', { name: 'Adjuntar' })[0])
    const dialogo = await screen.findByRole('dialog')
    await user.upload(within(dialogo).getByLabelText('Archivo'), pdf())
    await user.click(within(dialogo).getByRole('button', { name: 'Adjuntar documento' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('no corresponde a su formato')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('reemplaza un archivo con PUT sobre ese documento', async () => {
    const { user } = montar()
    cliente.put.mockResolvedValue({ data: { codigo: 'doc-1' } })
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getByRole('button', { name: 'Reemplazar carnet.pdf' }))
    const dialogo = await screen.findByRole('dialog')
    await user.upload(within(dialogo).getByLabelText('Archivo'), pdf('nuevo.pdf'))
    await user.click(within(dialogo).getByRole('button', { name: 'Reemplazar' }))

    await waitFor(() => expect(cliente.put).toHaveBeenCalledTimes(1))
    expect(cliente.put.mock.calls[0][0]).toBe('/postulaciones/post-1/documentos/doc-1')
  })

  it('elimina un archivo después de confirmar', async () => {
    const { user } = montar()
    cliente.delete.mockResolvedValue({ data: null })
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getByRole('button', { name: 'Eliminar carnet.pdf' }))
    const confirmacion = await screen.findByRole('alertdialog')
    expect(cliente.delete).not.toHaveBeenCalled()
    await user.click(within(confirmacion).getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(cliente.delete).toHaveBeenCalledWith('/postulaciones/post-1/documentos/doc-1'))
  })

  it('abre un archivo con su dirección temporal en otra pestaña', async () => {
    const { user } = montar()
    cliente.get.mockImplementation((url: string) =>
      Promise.resolve({
        data: url.endsWith('/acceso') ? { url: 'https://archivos.test/firmada', expiraEn: 'e' } : documentos(),
      }),
    )
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    await screen.findByText('Adjuntos (1 de 3)')

    await user.click(screen.getByRole('button', { name: 'Abrir carnet.pdf' }))

    await waitFor(() => expect(abrir).toHaveBeenCalledWith('https://archivos.test/firmada', '_blank', 'noopener,noreferrer'))
    abrir.mockRestore()
  })

  it('si la postulación no es del usuario muestra el error y permite reintentar', async () => {
    cliente.get.mockRejectedValue(new ApiError('ERR_DOC_04', 'La postulación no existe'))
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={queryClient}>
        <DocumentosPostulacionPanel codigo="ajena" />
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('no existe o no es tuya')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
