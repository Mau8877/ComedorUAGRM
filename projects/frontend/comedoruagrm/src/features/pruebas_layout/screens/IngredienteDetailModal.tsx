import dayjs from 'dayjs'
import { SquareTextIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { ModalDetailLayout } from '@/components/ui/modal-layout'
import { StatusBadge } from '../components'
import type { EstadoStock, Ingrediente } from '../types'

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

function formatFecha(fecha: string | undefined): string {
  return fecha ? dayjs(fecha).format('DD/MM/YYYY') : 'No especificado'
}

interface DetailFieldProps {
  label: string
  children: ReactNode
}

function DetailField({ label, children }: DetailFieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      <span className="text-sm text-foreground">{children}</span>
    </div>
  )
}

interface IngredienteDetailModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  ingrediente: Ingrediente | null
  onEditar?: (ingrediente: Ingrediente) => void
}

export function IngredienteDetailModal({
  open,
  onOpenChange,
  ingrediente,
  onEditar,
}: IngredienteDetailModalProps) {
  if (!ingrediente) return null

  const estado = getEstadoStock(ingrediente)
  const valorTotalStock = ingrediente.stock * ingrediente.precioUnitario

  return (
    <ModalDetailLayout
      open={open}
      onOpenChange={onOpenChange}
      title={ingrediente.nombre}
      subtitle={`Detalle del ingrediente -- ${ingrediente.categoria}`}
      icon={SquareTextIcon}
      size="lg"
      actionLabel={onEditar ? 'Editar' : undefined}
      onAction={onEditar ? () => onEditar(ingrediente) : undefined}
    >
      <div className="flex flex-col gap-6">
        <div className="flex items-center gap-4">
          <img
            src={ingrediente.foto}
            alt={ingrediente.nombre}
            className="size-20 shrink-0 rounded-full object-cover"
          />
          <div className="flex flex-col gap-2">
            <StatusBadge variant="primary" dot={false}>
              {ingrediente.categoria}
            </StatusBadge>
            <StatusBadge variant={estadoStockBadge[estado].variant}>
              {estadoStockBadge[estado].label}
            </StatusBadge>
          </div>
        </div>

        {ingrediente.descripcion && (
          <DetailField label="Descripción">{ingrediente.descripcion}</DetailField>
        )}

        <div className="grid grid-cols-2 gap-4 rounded-lg border border-border bg-muted/40 p-4">
          <DetailField label="Stock actual">
            {ingrediente.stock} {ingrediente.unidad}
          </DetailField>
          <DetailField label="Stock mínimo">
            {ingrediente.stockMinimo} {ingrediente.unidad}
          </DetailField>
          <DetailField label="Precio unitario">Bs {ingrediente.precioUnitario.toFixed(2)}</DetailField>
          <DetailField label="Valor total en stock">Bs {valorTotalStock.toFixed(2)}</DetailField>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <DetailField label="Proveedor">{ingrediente.proveedor ?? 'No especificado'}</DetailField>
          <DetailField label="Ubicación en almacén">
            {ingrediente.ubicacionAlmacen ?? 'No especificado'}
          </DetailField>
          <DetailField label="Fecha de ingreso">{formatFecha(ingrediente.fechaIngreso)}</DetailField>
          <DetailField label="Fecha de vencimiento">
            {formatFecha(ingrediente.fechaVencimiento)}
          </DetailField>
        </div>
      </div>
    </ModalDetailLayout>
  )
}
