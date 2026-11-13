import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', () => ({
  AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
}))
vi.mock('@/features/resultados', () => ({ resultadosKeys: { all: ['resultados'] } }))
const toastSuccess = vi.hoisted(() => vi.fn())
vi.mock('@/components/ui/toast', () => ({ toastSuccess }))

import { ApiError } from '@/store'

import { GenerarActaDialog } from '../components'
import type { ConvocatoriaPendienteActa } from '../types'

import { cliente } from './clienteSimulado'
import { acta, pendiente } from './montarAdmin'

function montar(
  convocatoria: ConvocatoriaPendienteActa = pendiente(1, { nombre: 'Beca 2026-II', adjudicados: 3 }),
  props: Partial<React.ComponentProps<typeof GenerarActaDialog>> = {},
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <GenerarActaDialog open onOpenChange={onOpenChange} convocatoria={convocatoria} {...props} />
    </QueryClientProvider>,
  )
  return { onOpenChange, queryClient, user: userEvent.setup() }
}

describe('GenerarActaDialog', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
    toastSuccess.mockReset()
  })
  afterEach(() => vi.restoreAllMocks())

  it('muestra la convocatoria, la cantidad de adjudicados y la advertencia de que es definitiva', () => {
    montar()

    expect(screen.getAllByText('Generar acta de adjudicación').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Beca 2026-II').length).toBeGreaterThan(0)
    expect(screen.getByText(/el acta incluirá a/)).toHaveTextContent('3 adjudicados')
    expect(screen.getByText('El acta es definitiva: no se puede regenerar ni eliminar.')).toBeInTheDocument()
    expect(screen.queryByText(/No hay adjudicados/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generar' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeEnabled()
  })

  it('sin adjudicados avisa pero no bloquea la generación', () => {
    montar(pendiente(1, { adjudicados: 0 }))

    expect(screen.getByText('No hay adjudicados: el acta dirá “Sin adjudicados”.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generar' })).toBeEnabled()
  })

  it('cancelar cierra sin llamar a la API', async () => {
    const { user, onOpenChange } = montar()

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(cliente.post).not.toHaveBeenCalled()
    expect(cliente.get).not.toHaveBeenCalled()
  })

  it('mientras genera se deshabilita y dice «Generando…», y manda una sola petición', async () => {
    let resolver: (valor: unknown) => void = () => {}
    cliente.post.mockReturnValue(
      new Promise((r) => {
        resolver = r
      }),
    )
    const { user } = montar()

    await user.click(screen.getByRole('button', { name: 'Generar' }))

    const enCurso = await screen.findByRole('button', { name: 'Generando…' })
    expect(enCurso).toBeDisabled()
    await user.click(enCurso)
    expect(cliente.post).toHaveBeenCalledTimes(1)
    expect(cliente.post).toHaveBeenCalledWith('/convocatorias/pend-1/acta')
    resolver({ data: acta(1) })
    await screen.findByRole('status')
  })

  it('el éxito muestra el número, avisa con un toast y ofrece descargar y cerrar', async () => {
    cliente.post.mockResolvedValue({ data: acta(1, { numero: '007/2026', adjudicados: 3 }) })
    const onGenerada = vi.fn()
    const { user, onOpenChange } = montar(undefined, { onGenerada })

    await user.click(screen.getByRole('button', { name: 'Generar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Acta N.º 007/2026 generada')
    expect(toastSuccess).toHaveBeenCalledWith('Acta generada correctamente')
    expect(onGenerada).toHaveBeenCalledWith(expect.objectContaining({ numero: '007/2026' }))
    expect(screen.queryByText(/definitiva/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Descargar' })).toBeEnabled()
    // La «X» del encabezado también se llama «Cerrar»: el botón del pie es el último.
    await user.click(screen.getAllByRole('button', { name: 'Cerrar' }).at(-1) as HTMLElement)
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('«Descargar» pide la dirección del acta generada y la abre', async () => {
    cliente.post.mockResolvedValue({ data: acta(1, { convocatoria: { codigo: 'conv-9', nombre: 'Beca 9' } }) })
    cliente.get.mockResolvedValue({ data: { url: 'http://minio.test/firmada', expiraEn: 'x' } })
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    const { user } = montar()
    await user.click(screen.getByRole('button', { name: 'Generar' }))
    await screen.findByRole('status')

    await user.click(screen.getByRole('button', { name: 'Descargar' }))

    await waitFor(() =>
      expect(abrir).toHaveBeenCalledExactlyOnceWith('http://minio.test/firmada', '_blank', 'noopener,noreferrer'),
    )
    expect(cliente.get).toHaveBeenCalledWith('/convocatorias/conv-9/acta/descarga')
  })

  it('si la descarga falla con ERR_ACT_04 muestra el mensaje y no abre nada', async () => {
    cliente.post.mockResolvedValue({ data: acta(1) })
    cliente.get.mockRejectedValue(new ApiError('ERR_ACT_04', 'x'))
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    const { user } = montar()
    await user.click(screen.getByRole('button', { name: 'Generar' }))
    await screen.findByRole('status')

    await user.click(screen.getByRole('button', { name: 'Descargar' }))

    expect(await screen.findByText('El archivo del acta no está disponible. Avisa a soporte.')).toBeInTheDocument()
    expect(abrir).not.toHaveBeenCalled()
  })

  it.each([
    ['ERR_ACT_01', 'aún no están publicados'],
    ['ERR_ACT_02', 'ya tiene acta de adjudicación'],
  ])('%s informa dentro de la ventana y refresca los pendientes', async (codigo, texto) => {
    cliente.post.mockRejectedValue(new ApiError(codigo, 'x'))
    const onDesactualizado = vi.fn()
    const { user, onOpenChange } = montar(undefined, { onDesactualizado })

    await user.click(screen.getByRole('button', { name: 'Generar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(texto)
    expect(onDesactualizado).toHaveBeenCalledTimes(1)
    expect(onOpenChange).not.toHaveBeenCalled()
    expect(toastSuccess).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Generar' })).toBeEnabled()
  })

  it('ERR_PRI_01 avisa al panel y cierra la ventana', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_PRI_01', 'x'))
    const onConvocatoriaInexistente = vi.fn()
    const { user, onOpenChange } = montar(undefined, { onConvocatoriaInexistente })

    await user.click(screen.getByRole('button', { name: 'Generar' }))

    await waitFor(() => expect(onConvocatoriaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('un error del servidor muestra un mensaje claro y permite reintentar sin registrar nada', async () => {
    cliente.post.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x'))
    cliente.post.mockResolvedValueOnce({ data: acta(1, { numero: '001/2026' }) })
    const { user } = montar()

    await user.click(screen.getByRole('button', { name: 'Generar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('error en el servidor')
    expect(toastSuccess).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Generar' }))
    expect(await screen.findByRole('status')).toHaveTextContent('001/2026')
    expect(cliente.post).toHaveBeenCalledTimes(2)
  })
})
