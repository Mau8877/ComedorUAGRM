import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', () => ({
  AlertaError: ({ children }: { children: ReactNode }) => <div role="alert">{children}</div>,
}))
const toastSuccess = vi.hoisted(() => vi.fn())
vi.mock('@/components/ui/toast', () => ({ toastSuccess }))

import { ApiError } from '@/store'

import { PublicarResultadosDialog } from '../components'
import type { ResumenPublicacion } from '../types'

import { cliente } from './clienteSimulado'
import { resumen } from './montarAdmin'

function publicacion(avisados = 5) {
  return {
    convocatoria: { codigo: 'conv-2', nombre: 'Beca 2027-I', estado: 'PUBLICADA' },
    publicadoEn: '2026-11-12T15:00:00Z',
    publicadoPor: 'Admin DUBSS',
    adjudicados: 3,
    noAdjudicados: 2,
    avisados,
  }
}

function montar(r: ResumenPublicacion = resumen(), props: Partial<React.ComponentProps<typeof PublicarResultadosDialog>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const onOpenChange = vi.fn()
  render(
    <QueryClientProvider client={queryClient}>
      <PublicarResultadosDialog open onOpenChange={onOpenChange} resumen={r} {...props} />
    </QueryClientProvider>,
  )
  return { onOpenChange, queryClient, user: userEvent.setup() }
}

describe('PublicarResultadosDialog', () => {
  beforeEach(() => {
    cliente.post.mockReset()
    toastSuccess.mockReset()
  })

  it('muestra la convocatoria, las cantidades, a cuántos se avisa y la advertencia de que no se deshace', () => {
    montar(resumen({ adjudicados: 3, noAdjudicados: 2, aAvisar: 5 }))

    expect(screen.getAllByText('Publicar resultados').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Beca 2027-I').length).toBeGreaterThan(0)
    expect(screen.getByText(/hay/)).toHaveTextContent('3 adjudicados y 2 no adjudicados')
    expect(screen.getByText(/Se avisará a/)).toHaveTextContent('Se avisará a 5 finalistas por bandeja, push y correo.')
    expect(screen.getByText(/no se puede deshacer y bloquea nuevas decisiones/)).toBeInTheDocument()
    expect(screen.queryByText(/No hay adjudicados/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publicar' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeEnabled()
  })

  it('sin adjudicados avisa pero no bloquea la publicación', () => {
    montar(resumen({ adjudicados: 0, noAdjudicados: 4, aAvisar: 4 }))

    expect(screen.getByText('No hay adjudicados: todos los finalistas recibirán “no adjudicada”.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publicar' })).toBeEnabled()
  })

  it('«Cancelar» cierra sin llamar a la API', async () => {
    const { onOpenChange, user } = montar()

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('«Publicar» llama a la API, muestra el toast y cierra; mientras tanto el botón queda deshabilitado', async () => {
    let terminar: (valor: { data: ReturnType<typeof publicacion> }) => void = () => undefined
    cliente.post.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    const onPublicados = vi.fn()
    const { onOpenChange, user } = montar(resumen(), { onPublicados })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Publicar' })).toBeDisabled())
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
    expect(cliente.post).toHaveBeenCalledTimes(1)
    expect(cliente.post).toHaveBeenCalledWith('/convocatorias/conv-2/resultados/publicar')
    terminar({ data: publicacion() })

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false))
    expect(toastSuccess).toHaveBeenCalledWith('Resultados publicados correctamente', 'Se avisó a 5 finalistas.')
    expect(onPublicados).toHaveBeenCalledWith(publicacion())
  })

  it('ERR_PUB_01 muestra cuántas decisiones faltan y pide refrescar el resumen', async () => {
    cliente.post.mockRejectedValue(
      new ApiError('ERR_PUB_01', 'Faltan 2 decisiones de adjudicación para publicar los resultados'),
    )
    const onDesactualizado = vi.fn()
    const { onOpenChange, user } = montar(resumen(), { onDesactualizado })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Faltan 2 decisiones de adjudicación')
    expect(onDesactualizado).toHaveBeenCalledTimes(1)
    expect(onOpenChange).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Publicar' })).toBeEnabled()
  })

  it('ERR_PUB_02 informa que ya estaba publicada y refresca el resumen', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_PUB_02', 'x'))
    const onDesactualizado = vi.fn()
    const { user } = montar(resumen(), { onDesactualizado })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Los resultados de esta convocatoria ya estaban publicados.')
    expect(onDesactualizado).toHaveBeenCalledTimes(1)
  })

  it('ERR_PUB_03 avisa que la convocatoria debe estar cerrada y refresca el resumen', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_PUB_03', 'x'))
    const onDesactualizado = vi.fn()
    const { user } = montar(resumen(), { onDesactualizado })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La convocatoria debe estar cerrada')
    expect(onDesactualizado).toHaveBeenCalledTimes(1)
  })

  it('ERR_PRI_01 avisa a la pantalla para volver a elegir y cierra la ventana', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_PRI_01', 'x'))
    const onConvocatoriaInexistente = vi.fn()
    const { onOpenChange, user } = montar(resumen(), { onConvocatoriaInexistente })

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    await waitFor(() => expect(onConvocatoriaInexistente).toHaveBeenCalledTimes(1))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('un error cualquiera muestra su mensaje y deja reintentar', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_AUTH_07', 'x'))
    const { user } = montar()

    await user.click(screen.getByRole('button', { name: 'Publicar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No tienes permiso para esta acción.')
    expect(screen.getByRole('button', { name: 'Publicar' })).toBeEnabled()
  })
})
