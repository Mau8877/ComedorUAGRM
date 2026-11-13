import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/resultados', () => ({ resultadosKeys: { all: ['resultados'] } }))
vi.mock('@/features/auth', () => ({
  AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
}))
vi.mock('@/components/ui/toast', () => ({ toastSuccess: vi.fn() }))

import { ApiError } from '@/store'

import { GenerarActaPanel } from '../components'

import { cliente } from './clienteSimulado'
import { pendiente, reiniciarCliente, responder } from './montarAdmin'

const PDF = new Blob(['%PDF-'], { type: 'application/pdf' })

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <GenerarActaPanel />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}

/** Las llamadas a la vista previa que hizo el panel. */
function vistasPedidas(): string[] {
  return cliente.get.mock.calls.filter((c) => String(c[0]).endsWith('/vista-previa')).map((c) => String(c[0]))
}

/** Responde la vista previa con lo que se le pasa (una función, para poder fallar o demorar). */
function conVistaPrevia(respuesta: (ruta: string) => Promise<unknown>) {
  const base = cliente.get.getMockImplementation() as (ruta: string) => Promise<unknown>
  cliente.get.mockImplementation((ruta: string) => (ruta.endsWith('/vista-previa') ? respuesta(ruta) : base(ruta)))
}

/** Espera a que lleguen los pendientes: el selector ya muestra la primera convocatoria. */
async function listo() {
  const selector = await screen.findByRole('combobox', { name: 'Convocatoria' })
  await waitFor(() => expect(selector).toHaveTextContent(/Pendiente/))
  return selector
}

const crear = vi.fn()
const revocar = vi.fn()

describe('GenerarActaPanel', () => {
  beforeEach(() => {
    reiniciarCliente()
    crear.mockReset().mockImplementation(() => `http://localhost/vista-${crear.mock.calls.length}`)
    revocar.mockReset()
    URL.createObjectURL = crear
    URL.revokeObjectURL = revocar
  })
  afterEach(() => vi.restoreAllMocks())

  it('carga los pendientes, muestra nombre y adjudicados y preselecciona la primera', async () => {
    responder({ pendientes: [pendiente(1, { adjudicados: 5 }), pendiente(2)] })
    montar()

    const selector = await listo()
    expect(selector).toHaveTextContent('Pendiente 1 (5 adjudicados)')
    expect(cliente.get).toHaveBeenCalledWith('/actas/pendientes', { params: { page: 1, pageSize: 50 } })
    expect(screen.getByRole('button', { name: 'Vista previa' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Generar PDF' })).toBeEnabled()
  })

  it('no tiene campo de número de acta: una nota dice que lo asigna el sistema', async () => {
    responder({ pendientes: [pendiente(1)] })
    montar()
    await listo()

    expect(screen.queryByLabelText(/número/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('El número de acta lo asigna el sistema al generarla.')
  })

  it('sin pendientes lo informa y deshabilita los botones', async () => {
    responder({ pendientes: [] })
    montar()

    expect(
      await screen.findByText('No hay convocatorias con resultados publicados pendientes de acta'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Vista previa' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Generar PDF' })).toBeDisabled()
    expect(screen.getByText(/pulsa «Vista previa»/)).toBeInTheDocument()
  })

  it('la vista previa se pide con la convocatoria elegida y se muestra en un iframe', async () => {
    responder({ pendientes: [pendiente(1), pendiente(2)] })
    conVistaPrevia(() => Promise.resolve({ data: PDF }))
    const { user } = montar()
    await listo()

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    const marco = await screen.findByTitle('Vista previa del acta')
    expect(marco).toHaveAttribute('src', 'http://localhost/vista-1')
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/pend-1/acta/vista-previa', { responseType: 'blob' })
    expect(crear).toHaveBeenCalledWith(PDF)
    expect(screen.queryByText(/pulsa «Vista previa»/)).not.toBeInTheDocument()
  })

  it('mientras pide el PDF bloquea los botones y dice «Generando vista previa…»', async () => {
    responder({ pendientes: [pendiente(1)] })
    let resolver: (valor: unknown) => void = () => {}
    conVistaPrevia(
      () =>
        new Promise((r) => {
          resolver = r
        }),
    )
    const { user } = montar()
    await listo()

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    const enCurso = await screen.findByRole('button', { name: 'Generando vista previa…' })
    expect(enCurso).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Generar PDF' })).toBeDisabled()
    resolver({ data: PDF })
    expect(await screen.findByRole('button', { name: 'Vista previa' })).toBeEnabled()
  })

  it('libera la dirección al pedir otra vista previa', async () => {
    responder({ pendientes: [pendiente(1)] })
    conVistaPrevia(() => Promise.resolve({ data: PDF }))
    const { user } = montar()
    await listo()
    await user.click(screen.getByRole('button', { name: 'Vista previa' }))
    await screen.findByTitle('Vista previa del acta')

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    await waitFor(() => expect(screen.getByTitle('Vista previa del acta')).toHaveAttribute('src', 'http://localhost/vista-2'))
    expect(revocar).toHaveBeenCalledExactlyOnceWith('http://localhost/vista-1')
  })

  it('libera la dirección y quita el PDF al cambiar de convocatoria', async () => {
    responder({ pendientes: [pendiente(1), pendiente(2)] })
    conVistaPrevia(() => Promise.resolve({ data: PDF }))
    const { user } = montar()
    await listo()
    await user.click(screen.getByRole('button', { name: 'Vista previa' }))
    await screen.findByTitle('Vista previa del acta')

    await user.click(screen.getByRole('combobox', { name: 'Convocatoria' }))
    await user.click(await screen.findByRole('option', { name: /Pendiente 2/ }))

    await waitFor(() => expect(screen.queryByTitle('Vista previa del acta')).not.toBeInTheDocument())
    expect(revocar).toHaveBeenCalledExactlyOnceWith('http://localhost/vista-1')
    expect(screen.getByText(/pulsa «Vista previa»/)).toBeInTheDocument()
  })

  it('libera la dirección al desmontar', async () => {
    responder({ pendientes: [pendiente(1)] })
    conVistaPrevia(() => Promise.resolve({ data: PDF }))
    const { user, unmount } = montar()
    await listo()
    await user.click(screen.getByRole('button', { name: 'Vista previa' }))
    await screen.findByTitle('Vista previa del acta')

    unmount()

    expect(revocar).toHaveBeenCalledExactlyOnceWith('http://localhost/vista-1')
  })

  it('el PDF no se guarda en la caché', async () => {
    responder({ pendientes: [pendiente(1)] })
    conVistaPrevia(() => Promise.resolve({ data: PDF }))
    const { user, queryClient } = montar()
    await listo()

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))
    await screen.findByTitle('Vista previa del acta')

    const enCache = queryClient.getQueryCache().getAll().map((q) => JSON.stringify(q.queryKey))
    expect(enCache.some((k) => k.includes('vista'))).toBe(false)
    expect(queryClient.getMutationCache().getAll().every((m) => m.state.data === undefined || m.state.data === PDF)).toBe(
      true,
    )
  })

  it.each([
    ['ERR_ACT_01', 'aún no están publicados'],
    ['ERR_ACT_02', 'ya tiene acta de adjudicación'],
  ])('%s muestra su mensaje, refresca los pendientes y no deja un iframe roto', async (codigo, texto) => {
    responder({ pendientes: [pendiente(1)] })
    conVistaPrevia(() => Promise.reject(new ApiError(codigo, 'x')))
    const { user } = montar()
    await listo()
    const antes = cliente.get.mock.calls.filter((c) => c[0] === '/actas/pendientes').length

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent(texto)
    expect(screen.queryByTitle('Vista previa del acta')).not.toBeInTheDocument()
    await waitFor(() =>
      expect(cliente.get.mock.calls.filter((c) => c[0] === '/actas/pendientes').length).toBeGreaterThan(antes),
    )
  })

  it('ERR_PRI_01 avisa y vuelve a la selección refrescando la lista', async () => {
    responder({ pendientes: [pendiente(1), pendiente(2)] })
    conVistaPrevia(() => Promise.reject(new ApiError('ERR_PRI_01', 'x')))
    const { user } = montar()
    await listo()

    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La convocatoria no existe o fue eliminada.')
    expect(screen.getByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Pendiente 1')
  })

  it('un error de red muestra un mensaje con «Reintentar», que vuelve a pedir la vista previa', async () => {
    responder({ pendientes: [pendiente(1)] })
    let intentos = 0
    conVistaPrevia(() => {
      intentos += 1
      return intentos === 1 ? Promise.reject(new ApiError('ERR_SYS_00', 'x')) : Promise.resolve({ data: PDF })
    })
    const { user } = montar()
    await listo()
    await user.click(screen.getByRole('button', { name: 'Vista previa' }))

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('error en el servidor')
    expect(screen.queryByTitle('Vista previa del acta')).not.toBeInTheDocument()
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByTitle('Vista previa del acta')).toBeInTheDocument()
    expect(vistasPedidas()).toHaveLength(2)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('un error al pedir los pendientes se muestra con «Reintentar»', async () => {
    responder({ pendientes: [pendiente(1)] })
    // Solo la primera consulta falla; el reintento ya recibe los pendientes.
    cliente.get.mockRejectedValueOnce(new ApiError('ERR_AUTH_07', 'x'))
    const { user } = montar()

    const alerta = await screen.findByRole('alert')
    expect(within(alerta).getByText('No tienes permiso para esta acción.')).toBeInTheDocument()
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('combobox', { name: 'Convocatoria' })).toHaveTextContent('Pendiente 1')
  })

  it('«Generar PDF» abre la ventana de confirmación de la convocatoria elegida, y cancelar no llama a la API', async () => {
    responder({ pendientes: [pendiente(1, { adjudicados: 4 })] })
    const { user } = montar()
    await listo()

    await user.click(screen.getByRole('button', { name: 'Generar PDF' }))

    expect(await screen.findByText('El acta es definitiva: no se puede regenerar ni eliminar.')).toBeInTheDocument()
    expect(screen.getByText(/el acta incluirá a/)).toHaveTextContent('4 adjudicados')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    await waitFor(() => expect(screen.queryByText(/definitiva/)).not.toBeInTheDocument())
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('al generar, la convocatoria desaparece de los pendientes y la ventana muestra el número', async () => {
    responder({ pendientes: [pendiente(1)] })
    cliente.post.mockImplementation(() => {
      // Desde ahora el backend ya no la lista como pendiente.
      responder({ pendientes: [] })
      return Promise.resolve({ data: { codigo: 'a-1', numero: '001/2026', convocatoria: { codigo: 'pend-1', nombre: 'Pendiente 1' }, gestion: 'g', adjudicados: 4, generadaEn: '2026-11-13T15:00:00Z', generadaPor: 'x' } })
    })
    const { user } = montar()
    await listo()
    await user.click(screen.getByRole('button', { name: 'Generar PDF' }))

    await user.click(await screen.findByRole('button', { name: 'Generar' }))

    expect(await screen.findByText(/Acta N\.º/)).toHaveTextContent('001/2026')
    await waitFor(() =>
      expect(screen.getByText('No hay convocatorias con resultados publicados pendientes de acta')).toBeInTheDocument(),
    )
  })
})
