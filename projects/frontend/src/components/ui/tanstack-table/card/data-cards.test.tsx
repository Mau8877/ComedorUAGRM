import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { useReactTable, getCoreRowModel } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { DataCards } from './data-cards'
import '../shared/types'

interface Row {
  id: string
  nombre: string
  email: string
  rol: string
}

const rows: Row[] = [{ id: '1', nombre: 'Ana', email: 'ana@test.com', rol: 'ADMIN' }]

function useTestTable(columns: ColumnDef<Row, unknown>[], data: Row[] = rows) {
  return renderHook(function useWrapper() {
    return useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
    })
  }).result.current
}

describe('DataCards', () => {
  it('usa la columna marcada con meta.cardTitle como CardTitle, sin duplicarla en el body', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    const title = screen.getByText('Ana')
    expect(title.closest('[data-slot="card-title"]')).not.toBeNull()
    expect(screen.getAllByText('Ana')).toHaveLength(1)
  })

  it('respeta hideInCard: esa columna no aparece en la card', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo' },
      { accessorKey: 'rol', header: 'Rol', meta: { hideInCard: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.queryByText('ADMIN')).not.toBeInTheDocument()
  })

  it('respeta cardLabel como override del label mostrado', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
      { accessorKey: 'email', header: 'Correo', meta: { cardLabel: 'Correo electrónico' } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Correo electrónico')).toBeInTheDocument()
  })

  it('sin ninguna columna cardTitle, usa la primera columna visible como fallback', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre' },
      { accessorKey: 'email', header: 'Correo' },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Ana').closest('[data-slot="card-title"]')).not.toBeNull()
  })

  it('con renderCard, ignora la derivación automática', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(
      <DataCards
        table={table}
        isLoading={false}
        isError={false}
        renderCard={(row) => <div>Custom: {row.original.nombre}</div>}
      />,
    )

    expect(screen.getByText('Custom: Ana')).toBeInTheDocument()
  })

  it('en loading, renderiza skeletons en vez de cards', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading isError={false} skeletonCount={2} />)

    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
    expect(document.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0)
  })

  it('en error, muestra el mensaje y no las cards', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns)

    render(<DataCards table={table} isLoading={false} isError errorMessage="Falló la carga" />)

    expect(screen.getByText('Falló la carga')).toBeInTheDocument()
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
  })

  it('sin filas, muestra el mensaje de vacío', () => {
    const columns: ColumnDef<Row, unknown>[] = [
      { accessorKey: 'nombre', header: 'Nombre', meta: { cardTitle: true } },
    ]
    const table = useTestTable(columns, [])

    render(<DataCards table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
  })
})
