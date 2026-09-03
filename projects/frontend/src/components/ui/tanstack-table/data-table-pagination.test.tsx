import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DataTablePagination } from './data-table-pagination'
import type { DataTablePageMeta } from './types'

const meta: DataTablePageMeta = { page: 2, pageSize: 20, totalItems: 45, totalPages: 3 }

describe('DataTablePagination', () => {
  it('deshabilita "anterior" en la primera página y "siguiente" en la última', () => {
    render(
      <DataTablePagination
        meta={{ ...meta, page: 1 }}
        page={1}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeEnabled()
  })

  it('llama a onPageChange con el valor correcto al hacer click en siguiente/anterior', async () => {
    const onPageChange = vi.fn()
    const user = userEvent.setup()

    render(
      <DataTablePagination meta={meta} page={2} onPageChange={onPageChange} onPageSizeChange={vi.fn()} />,
    )

    await user.click(screen.getByRole('button', { name: 'Página siguiente' }))
    expect(onPageChange).toHaveBeenCalledWith(3)

    await user.click(screen.getByRole('button', { name: 'Página anterior' }))
    expect(onPageChange).toHaveBeenCalledWith(1)
  })

  it('sin meta, no rompe: botones deshabilitados y fallback de texto', () => {
    render(
      <DataTablePagination meta={undefined} page={1} onPageChange={vi.fn()} onPageSizeChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeDisabled()
    expect(screen.getByText(/Página 1 de/)).toBeInTheDocument()
  })

  it('deshabilita ambos botones mientras isLoading', () => {
    render(
      <DataTablePagination
        meta={meta}
        page={2}
        onPageChange={vi.fn()}
        onPageSizeChange={vi.fn()}
        isLoading
      />,
    )

    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Página siguiente' })).toBeDisabled()
  })
})
