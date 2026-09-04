import { useState } from 'react'
import { EyeIcon, PencilIcon, PlusIcon, RefreshCwIcon, Trash2Icon } from 'lucide-react'
import type { ColumnDef, SortingState } from '@tanstack/react-table'

import { AdminLayout } from '@/layouts'
import { Button } from '@/components/ui/button'
import {
  DataCards,
  DataTable,
  DataTablePagination,
  DataTableToolbar,
  RowActionButton,
  toSortQueryParam,
  useDataTable,
} from '@/components/ui/tanstack-table'
import type { DataTableToolbarFilter } from '@/components/ui/tanstack-table'

import { StatusBadge } from '../components'
import { mockAdminUser, mockIngredientes } from '../mocks'
import type { CategoriaIngrediente, EstadoStock, Ingrediente } from '../types'

const estadoStockBadge: Record<
  EstadoStock,
  { label: string; variant: 'success' | 'warning' | 'destructive' }
> = {
  disponible: { label: 'Disponible', variant: 'success' },
  'bajo-stock': { label: 'Bajo stock', variant: 'warning' },
  agotado: { label: 'Agotado', variant: 'destructive' },
}

function getEstadoStock(ingrediente: Ingrediente): EstadoStock {
  if (ingrediente.stock <= 0) return 'agotado'
  if (ingrediente.stock <= ingrediente.stockMinimo) return 'bajo-stock'
  return 'disponible'
}

// Columnas del mockup -- una sola definición alimenta DataTable y
// DataCards (ver meta.cardTitle/cardLabel/cardOrder/hideInCard/cardImage).
// La foto va pegada al nombre en la misma celda de DataTable (así aparece
// ahí sin necesitar una columna aparte), y esa misma columna usa
// `meta.cardImage` para que DataCards la muestre además como la imagen
// grande de la variante "con imagen" (ver shared/types.ts) -- DataCards ya
// se encarga de no duplicar la miniatura dentro del título en ese caso.
const ingredientesColumns: ColumnDef<Ingrediente, unknown>[] = [
  {
    accessorKey: 'nombre',
    header: 'Ingrediente',
    meta: {
      cardTitle: true,
      grow: true,
      cardImage: (row) => row.foto,
      cardImageAlt: (row) => row.nombre,
    },
    cell: (ctx) => (
      <div className="flex items-center gap-3">
        <img
          src={ctx.row.original.foto}
          alt={ctx.getValue<string>()}
          className="size-9 shrink-0 rounded-full object-cover"
        />
        <span className="font-medium text-foreground">{ctx.getValue<string>()}</span>
      </div>
    ),
  },
  {
    accessorKey: 'categoria',
    header: 'Categoría',
    enableSorting: false,
    cell: (ctx) => (
      <StatusBadge variant="primary" dot={false}>
        {ctx.getValue<string>()}
      </StatusBadge>
    ),
    meta: { cardOrder: 0, width: 'w-32' },
  },
  {
    accessorKey: 'stock',
    header: 'Stock',
    cell: (ctx) => `${ctx.getValue<number>()} ${ctx.row.original.unidad}`,
    meta: { cardOrder: 1, align: 'left', width: 'w-28' },
  },
  {
    id: 'estado',
    header: 'Estado',
    enableSorting: false,
    cell: (ctx) => {
      const estado = getEstadoStock(ctx.row.original)
      return (
        <StatusBadge variant={estadoStockBadge[estado].variant}>
          {estadoStockBadge[estado].label}
        </StatusBadge>
      )
    },
    meta: { cardOrder: 2, width: 'w-32' },
  },
  {
    accessorKey: 'precioUnitario',
    header: 'Precio',
    cell: (ctx) => `Bs ${ctx.getValue<number>().toFixed(2)}`,
    meta: { cardOrder: 3, align: 'right', width: 'w-24' },
  },
]

const categoriaFilterOptions: { label: string; value: CategoriaIngrediente }[] = [
  { label: 'Verdura', value: 'Verdura' },
  { label: 'Fruta', value: 'Fruta' },
  { label: 'Lácteo', value: 'Lácteo' },
  { label: 'Carne', value: 'Carne' },
  { label: 'Grano', value: 'Grano' },
  { label: 'Condimento', value: 'Condimento' },
]

const DEMO_PAGE_SIZE = 20

export function PruebaIngredientes() {
  // Igual que en la demo de usuarios: esto simula lo que en una feature
  // real haría un hook de api/ con TanStack Query contra el backend
  // (page/pageSize/search/filter[categoria]/sort -> meta). Acá se
  // resuelve client-side sobre el mock solo para verificación visual.
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(DEMO_PAGE_SIZE)
  const [sorting, setSorting] = useState<SortingState>([])
  const [search, setSearch] = useState('')
  const [categoriaFilter, setCategoriaFilter] = useState<string | undefined>(undefined)
  const sortParam = toSortQueryParam(sorting)

  const datosFiltrados = mockIngredientes.filter((ingrediente) => {
    const coincideBusqueda =
      search.trim() === '' || ingrediente.nombre.toLowerCase().includes(search.toLowerCase())
    const coincideCategoria = !categoriaFilter || ingrediente.categoria === categoriaFilter
    return coincideBusqueda && coincideCategoria
  })

  const datosOrdenados = [...datosFiltrados].sort((a, b) => {
    const [first] = sorting
    if (!first) return 0
    const campo = first.id as keyof Ingrediente
    const valorA = a[campo]
    const valorB = b[campo]
    const comparacion =
      typeof valorA === 'number' && typeof valorB === 'number'
        ? valorA - valorB
        : String(valorA).localeCompare(String(valorB))
    return first.desc ? -comparacion : comparacion
  })

  const totalItems = datosOrdenados.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const paginaData = datosOrdenados.slice((page - 1) * pageSize, page * pageSize)
  const ingredientesMeta = { page, pageSize, totalItems, totalPages }

  const ingredientesFilters: DataTableToolbarFilter[] = [
    {
      id: 'categoria',
      label: 'Categoría',
      value: categoriaFilter,
      onValueChange: (value) => {
        setPage(1)
        setCategoriaFilter(value)
      },
      options: categoriaFilterOptions,
    },
  ]

  const { table: ingredientesTable } = useDataTable({
    data: paginaData,
    columns: ingredientesColumns,
    meta: ingredientesMeta,
    page,
    pageSize,
    onPageChange: setPage,
    onPageSizeChange: (nextPageSize) => {
      setPage(1)
      setPageSize(nextPageSize)
    },
    enableSorting: true,
    sorting,
    onSortingChange: (nextSorting) => {
      setPage(1)
      setSorting(nextSorting)
    },
  })

  const renderIngredienteActions = (row: { original: Ingrediente }) => (
    <>
      <RowActionButton icon={<EyeIcon />} label="Ver detalles" onClick={() => {}} />
      <RowActionButton icon={<PencilIcon />} label="Editar" onClick={() => {}} />
      <RowActionButton
        icon={<Trash2Icon />}
        label={`Eliminar ${row.original.nombre}`}
        variant="destructive"
        onClick={() => {}}
      />
    </>
  )

  return (
    <AdminLayout title="Ingredientes" user={mockAdminUser}>
      <div className="rounded-2xl border border-border bg-card p-4">
        <h2 className="font-heading text-sm font-medium text-foreground">
          Gestión de ingredientes
        </h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Ejemplo de components/ui/tanstack-table aplicado a un dominio real
          del comedor -- foto + nombre en la misma celda (visible en tabla y
          en cards), categoría, stock con su unidad, estado derivado del
          stock mínimo, y precio. Reducí el ancho de la ventana para ver la
          vista de cards en mobile.
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          Query param que se mandaría al backend:{' '}
          <code>
            {[
              search.trim() && `search=${search.trim()}`,
              categoriaFilter && `filter[categoria]=${categoriaFilter}`,
              sortParam && `sort=${sortParam}`,
            ]
              .filter(Boolean)
              .join('&') || '(sin filtros/orden aplicados)'}
          </code>
        </p>

        <DataTableToolbar
          className="mt-4"
          searchValue={search}
          onSearchChange={(value) => {
            setPage(1)
            setSearch(value)
          }}
          searchPlaceholder="Buscar ingrediente..."
          filters={ingredientesFilters}
          actions={
            <>
              <Button variant="outline" size="sm" onClick={() => setPage(1)}>
                <RefreshCwIcon data-icon="inline-start" />
                Refrescar
              </Button>
              <Button size="sm">
                <PlusIcon data-icon="inline-start" />
                Nuevo ingrediente
              </Button>
            </>
          }
        />

        <div className="mt-4 hidden md:block">
          <DataTable
            table={ingredientesTable}
            isLoading={false}
            isError={false}
            showRowNumber
            renderActions={renderIngredienteActions}
          />
        </div>
        <div className="mt-4 md:hidden">
          <DataCards
            table={ingredientesTable}
            isLoading={false}
            isError={false}
            showRowNumber
            renderActions={renderIngredienteActions}
          />
        </div>

        <div className="mt-8 hidden md:block">
          <h3 className="font-heading text-sm font-medium text-foreground">
            Misma data, vista de cards (5 por fila)
          </h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Mismas columnas de `ingredientesColumns`, renderizadas con{' '}
            <code>DataCards</code> en vez de <code>DataTable</code> -- demuestra que ambas
            vistas salen de la misma definición de columnas.
          </p>
          <DataCards
            className="mt-4 grid-cols-2 lg:grid-cols-5"
            table={ingredientesTable}
            isLoading={false}
            isError={false}
            showRowNumber
            renderActions={renderIngredienteActions}
          />
        </div>

        <DataTablePagination
          className="mt-4"
          meta={ingredientesMeta}
          page={page}
          onPageChange={setPage}
          onPageSizeChange={(nextPageSize) => {
            setPage(1)
            setPageSize(nextPageSize)
          }}
        />
      </div>
    </AdminLayout>
  )
}
