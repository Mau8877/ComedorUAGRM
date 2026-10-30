import { useForm } from '@tanstack/react-form'
import { PencilIcon, PlusIcon } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { ModalLayout } from '@/components/ui/modal-layout'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  CATEGORIAS_INGREDIENTE,
  UNIDADES_INGREDIENTE,
  ingredienteSchema,
  type IngredienteFormValues,
} from '../schemas'
import type { Ingrediente } from '../types'

const FORM_ID = 'ingrediente-form'

interface IngredienteFormModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** `undefined` = modo creación. Con un ingrediente = modo edición, precarga sus datos. */
  ingrediente?: Ingrediente
  isSubmitting: boolean
  onSubmit: (values: IngredienteFormValues) => void
}

function toDefaultValues(ingrediente: Ingrediente | undefined): IngredienteFormValues {
  if (!ingrediente) {
    return {
      nombre: '',
      foto: '',
      categoria: CATEGORIAS_INGREDIENTE[0],
      unidad: UNIDADES_INGREDIENTE[0],
      stock: '',
      stockMinimo: '',
      precioUnitario: '',
    }
  }
  return {
    nombre: ingrediente.nombre,
    foto: ingrediente.foto,
    categoria: ingrediente.categoria,
    unidad: ingrediente.unidad,
    stock: String(ingrediente.stock),
    stockMinimo: String(ingrediente.stockMinimo),
    precioUnitario: String(ingrediente.precioUnitario),
  }
}

export function IngredienteFormModal({
  open,
  onOpenChange,
  ingrediente,
  isSubmitting,
  onSubmit,
}: IngredienteFormModalProps) {
  const isEdit = Boolean(ingrediente)

  const form = useForm({
    defaultValues: toDefaultValues(ingrediente),
    validators: { onChange: ingredienteSchema },
    onSubmit: ({ value }) => onSubmit(value),
  })

  return (
    <ModalLayout
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Editar ingrediente' : 'Nuevo ingrediente'}
      icon={isEdit ? PencilIcon : PlusIcon}
      subtitle={
        isEdit
          ? `Actualizá los datos de "${ingrediente?.nombre}"`
          : 'Completá los datos del nuevo ingrediente'
      }
      formId={FORM_ID}
      isSubmitting={isSubmitting}
      confirmLabel={isEdit ? 'Guardar cambios' : 'Crear'}
    >
      <form
        id={FORM_ID}
        className="flex flex-col gap-4"
        onSubmit={(e) => {
          e.preventDefault()
          e.stopPropagation()
          void form.handleSubmit()
        }}
      >
        <form.Field name="nombre">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                Nombre
              </label>
              <Input
                id={field.name}
                name={field.name}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <form.Field name="foto">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                URL de la foto
              </label>
              <Input
                id={field.name}
                name={field.name}
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>

        <div className="grid grid-cols-2 gap-4">
          <form.Field name="categoria">
            {(field) => (
              <div className="flex flex-col gap-1">
                <span className="text-sm font-medium text-foreground">Categoría</span>
                <Select
                  value={field.state.value}
                  onValueChange={(value) => field.handleChange(value as typeof field.state.value)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORIAS_INGREDIENTE.map((categoria) => (
                      <SelectItem key={categoria} value={categoria}>
                        {categoria}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </form.Field>

          <form.Field name="unidad">
            {(field) => (
              <div className="flex flex-col gap-1">
                <span className="text-sm font-medium text-foreground">Unidad</span>
                <Select
                  value={field.state.value}
                  onValueChange={(value) => field.handleChange(value as typeof field.state.value)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {UNIDADES_INGREDIENTE.map((unidad) => (
                      <SelectItem key={unidad} value={unidad}>
                        {unidad}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </form.Field>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <form.Field name="stock">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                  Stock
                </label>
                <Input
                  id={field.name}
                  name={field.name}
                  type="number"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                {field.state.meta.isTouched && field.state.meta.errors[0] && (
                  <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
                )}
              </div>
            )}
          </form.Field>

          <form.Field name="stockMinimo">
            {(field) => (
              <div className="flex flex-col gap-1">
                <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                  Stock mínimo
                </label>
                <Input
                  id={field.name}
                  name={field.name}
                  type="number"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={(e) => field.handleChange(e.target.value)}
                />
                {field.state.meta.isTouched && field.state.meta.errors[0] && (
                  <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
                )}
              </div>
            )}
          </form.Field>
        </div>

        <form.Field name="precioUnitario">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                Precio unitario (Bs)
              </label>
              <Input
                id={field.name}
                name={field.name}
                type="number"
                step="0.01"
                value={field.state.value}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              {field.state.meta.isTouched && field.state.meta.errors[0] && (
                <span className="text-xs text-destructive">{field.state.meta.errors[0].message}</span>
              )}
            </div>
          )}
        </form.Field>
      </form>
    </ModalLayout>
  )
}
