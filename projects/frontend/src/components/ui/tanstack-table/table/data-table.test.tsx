import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { useReactTable, getCoreRowModel } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { DataTable } from './data-table'

interface Row {
  id: string
  nombre: string
  email: string
}

const columns: ColumnDef<Row, unknown>[] = [
  { accessorKey: 'nombre', header: 'Nombre' },
  { accessorKey: 'email', header: 'Correo' },
]

function useTestTable(data: Row[]) {
  return renderHook(function useWrapper() {
    return useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
    })
  }).result.current
}

describe('DataTable', () => {
  it('en loading, renderiza filas skeleton en vez de datos', () => {
    const table = useTestTable([{ id: '1', nombre: 'Ana', email: 'ana@test.com' }])

    render(<DataTable table={table} isLoading isError={false} skeletonRowCount={3} />)

    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
    expect(document.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(3 * columns.length)
  })

  it('en error, muestra el mensaje y no la tabla de datos', () => {
    const table = useTestTable([{ id: '1', nombre: 'Ana', email: 'ana@test.com' }])

    render(<DataTable table={table} isLoading={false} isError errorMessage="Falló la carga" />)

    expect(screen.getByText('Falló la carga')).toBeInTheDocument()
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
  })

  it('sin datos (no loading/error), muestra el mensaje de vacío', () => {
    const table = useTestTable([])

    render(<DataTable table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
  })

  it('caso normal, renderiza filas y celdas de los datos', () => {
    const table = useTestTable([
      { id: '1', nombre: 'Ana', email: 'ana@test.com' },
      { id: '2', nombre: 'Beto', email: 'beto@test.com' },
    ])

    render(<DataTable table={table} isLoading={false} isError={false} />)

    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('beto@test.com')).toBeInTheDocument()
  })
})
