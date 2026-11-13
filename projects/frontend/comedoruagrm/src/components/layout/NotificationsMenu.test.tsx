import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { NotificationsMenu } from './NotificationsMenu'
import type { NotificationItem, NotificationsController } from './types'

function aviso(id: string, extra: Partial<NotificationItem> = {}): NotificationItem {
  return { id, tipo: 'info', titulo: `Aviso ${id}`, fecha: '2026-11-13T15:00:00Z', leida: false, ...extra }
}

function controlador(items: NotificationItem[], extra: Partial<NotificationsController> = {}): NotificationsController {
  return {
    items,
    meta: { page: 1, pageSize: 10, totalItems: items.length, totalPages: 1 },
    isLoading: false,
    isLoadingMore: false,
    unreadCount: items.filter((i) => !i.leida).length,
    onOpenChange: vi.fn(),
    onLoadMore: vi.fn(),
    onMarkAsRead: vi.fn(),
    onMarkAllAsRead: vi.fn(),
    onDeleteAll: vi.fn(),
    ...extra,
  }
}

async function abrir(c: NotificationsController) {
  const user = userEvent.setup()
  render(<NotificationsMenu notifications={c} />)
  await user.click(screen.getByRole('button', { name: /Notificaciones/ }))
  return user
}

describe('NotificationsMenu: avisos que apuntan a una postulación (CU04)', () => {
  it('tocar un aviso con postulación lo marca como leído y llama a onSelect con él', async () => {
    const onSelect = vi.fn()
    const c = controlador([aviso('1', { postulacionCodigo: 'post-1' })], { onSelect })
    const user = await abrir(c)

    await user.click(await screen.findByRole('button', { name: /Aviso 1/ }))

    expect(c.onMarkAsRead).toHaveBeenCalledWith('1')
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: '1', postulacionCodigo: 'post-1' }))
  })

  it('un aviso con postulación ya leído sigue siendo tocable: navega sin volver a marcarlo', async () => {
    const onSelect = vi.fn()
    const c = controlador([aviso('1', { postulacionCodigo: 'post-1', leida: true })], { onSelect })
    const user = await abrir(c)

    const boton = await screen.findByRole('button', { name: /Aviso 1/ })
    expect(boton).toBeEnabled()
    await user.click(boton)

    expect(onSelect).toHaveBeenCalledTimes(1)
    expect(c.onMarkAsRead).not.toHaveBeenCalled()
  })

  it('un aviso sin postulación solo se marca como leído: no navega', async () => {
    const onSelect = vi.fn()
    const c = controlador([aviso('1')], { onSelect })
    const user = await abrir(c)

    await user.click(await screen.findByRole('button', { name: /Aviso 1/ }))

    expect(c.onMarkAsRead).toHaveBeenCalledWith('1')
    expect(onSelect).not.toHaveBeenCalled()
  })

  it('un aviso sin postulación ya leído sigue deshabilitado, como antes', async () => {
    const c = controlador([aviso('1', { leida: true })], { onSelect: vi.fn() })
    await abrir(c)

    expect(await screen.findByRole('button', { name: /Aviso 1/ })).toBeDisabled()
  })

  it('sin onSelect (el controlador de siempre) el comportamiento no cambia, aunque el aviso traiga código', async () => {
    const c = controlador([aviso('1', { postulacionCodigo: 'post-1', leida: true })])
    await abrir(c)

    expect(await screen.findByRole('button', { name: /Aviso 1/ })).toBeDisabled()
  })
})
