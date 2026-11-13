import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { NotificationItem, NotificationsController } from '@/components/layout'

const navegar = vi.hoisted(() => vi.fn())
const bandeja = vi.hoisted(() => ({ items: [] as NotificationItem[], onMarkAsRead: vi.fn() }))

vi.mock('@tanstack/react-router', async (original) => ({
  ...(await original<object>()),
  useNavigate: () => navegar,
}))
// La bandeja real hace pedidos: acá se simula con los avisos del test.
vi.mock('@/features/notificaciones', () => ({
  useNotificaciones: (): NotificationsController => ({
    items: bandeja.items,
    meta: { page: 1, pageSize: 10, totalItems: bandeja.items.length, totalPages: 1 },
    isLoading: false,
    isLoadingMore: false,
    unreadCount: bandeja.items.length,
    onOpenChange: vi.fn(),
    onLoadMore: vi.fn(),
    onMarkAsRead: bandeja.onMarkAsRead,
    onMarkAllAsRead: vi.fn(),
    onDeleteAll: vi.fn(),
  }),
}))
// El AppShell real arrastra el router y el menú lateral: solo interesa la campanita de la bandeja.
vi.mock('@/components/layout', async (original) => {
  const { NotificationsMenu } = await import('@/components/layout/NotificationsMenu')
  return {
    ...(await original<object>()),
    AppShell: ({ notifications }: { notifications: NotificationsController }) => (
      <NotificationsMenu notifications={notifications} />
    ),
  }
})

import { EstudianteLayout } from './EstudianteLayout'

function aviso(id: string, extra: Partial<NotificationItem> = {}): NotificationItem {
  return { id, tipo: 'warning', titulo: `Aviso ${id}`, fecha: '2026-11-13T15:00:00Z', leida: false, ...extra }
}

async function abrirLaCampanita() {
  const user = userEvent.setup()
  render(
    <EstudianteLayout title="Inicio" activeHref="/" user={{ nombre: 'Pia Perez', rol: 'Postulante' }}>
      <div />
    </EstudianteLayout>,
  )
  await user.click(screen.getByRole('button', { name: /Notificaciones/ }))
  return user
}

describe('EstudianteLayout: avisos de la bandeja (CU04, A6)', () => {
  beforeEach(() => {
    navegar.mockReset()
    bandeja.onMarkAsRead.mockReset()
    bandeja.items = []
  })

  it('tocar un aviso con postulacionCodigo navega al detalle de esa postulación', async () => {
    bandeja.items = [aviso('1', { postulacionCodigo: 'post-9' })]
    const user = await abrirLaCampanita()

    await user.click(await screen.findByRole('button', { name: /Aviso 1/ }))

    expect(navegar).toHaveBeenCalledExactlyOnceWith({
      to: '/mis-postulaciones/$codigo',
      params: { codigo: 'post-9' },
    })
    expect(bandeja.onMarkAsRead).toHaveBeenCalledWith('1')
  })

  it('un aviso sin postulacionCodigo no navega: solo se marca como leído', async () => {
    bandeja.items = [aviso('2')]
    const user = await abrirLaCampanita()

    await user.click(await screen.findByRole('button', { name: /Aviso 2/ }))

    expect(navegar).not.toHaveBeenCalled()
    expect(bandeja.onMarkAsRead).toHaveBeenCalledWith('2')
  })
})
