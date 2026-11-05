import { describe, it, expect, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ModalDetailLayout } from './modal-detail-layout'

function montar(props: { dismissible?: boolean; closeLabel?: string; onOpenChange?: (open: boolean) => void } = {}) {
  const onOpenChange = props.onOpenChange ?? vi.fn()
  render(
    <ModalDetailLayout open onOpenChange={onOpenChange} title="Detalle" dismissible={props.dismissible} closeLabel={props.closeLabel}>
      <p>contenido</p>
    </ModalDetailLayout>,
  )
  return onOpenChange
}

describe('ModalDetailLayout', () => {
  it('por default se cierra con Escape', () => {
    const onOpenChange = montar()

    fireEvent.keyDown(document, { key: 'Escape', code: 'Escape' })

    expect(onOpenChange).toHaveBeenCalledWith(false, expect.anything())
  })

  it('con dismissible=false ignora Escape', () => {
    const onOpenChange = montar({ dismissible: false })

    fireEvent.keyDown(document, { key: 'Escape', code: 'Escape' })

    expect(onOpenChange).not.toHaveBeenCalled()
    expect(screen.getByText('contenido')).toBeInTheDocument()
  })

  it('con dismissible=false el botón del footer sí cierra', async () => {
    const onOpenChange = montar({ dismissible: false, closeLabel: 'Cerrar el detalle' })

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar el detalle' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
