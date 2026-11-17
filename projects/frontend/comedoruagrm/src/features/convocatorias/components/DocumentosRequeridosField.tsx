import { PlusIcon, Trash2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

import { useTiposDocumento } from '../api'
import type { DocumentoRequerido } from '../types'

/** Una fila del formulario: el tipo elegido (`''` = todavía sin elegir) y si es obligatorio. */
export interface DocumentoForm {
  tipoDocumentoCodigo: string
  obligatorio: boolean
}

interface DocumentosRequeridosFieldProps {
  value: DocumentoForm[]
  onChange: (documentos: DocumentoForm[]) => void
  onBlur?: () => void
  disabled?: boolean
  /** Error ya decidido (de «al menos uno», «sin repetir» u «obligatorio»). */
  error?: string
  /** Los documentos que la convocatoria ya tiene: sus nombres se muestran aunque el catálogo no los traiga. */
  actuales?: DocumentoRequerido[]
}

const PAGE_SIZE_CATALOGO = 100

/**
 * La lista de documentos requeridos de una convocatoria (CU13): cada fila elige un tipo del catálogo y marca si es
 * obligatorio. Un tipo no se puede elegir dos veces. Al menos un documento obligatorio lo exige el backend; acá se
 * avisa antes.
 */
export function DocumentosRequeridosField({
  value,
  onChange,
  onBlur,
  disabled,
  error,
  actuales = [],
}: DocumentosRequeridosFieldProps) {
  const catalogo = useTiposDocumento({ page: 1, pageSize: PAGE_SIZE_CATALOGO, sort: 'nombre' })

  const tipos = new Map<string, string>()
  for (const actual of actuales) tipos.set(actual.tipoDocumento.codigo, actual.tipoDocumento.nombre)
  for (const tipo of catalogo.data?.data ?? []) tipos.set(tipo.codigo, tipo.nombre)

  const elegidos = new Set(value.map((fila) => fila.tipoDocumentoCodigo).filter(Boolean))

  const cambiarFila = (indice: number, cambios: Partial<DocumentoForm>) =>
    onChange(value.map((fila, i) => (i === indice ? { ...fila, ...cambios } : fila)))

  return (
    <fieldset className="flex flex-col gap-2" onBlur={onBlur}>
      <legend className="text-sm font-medium text-foreground">Documentos requeridos</legend>

      {catalogo.isError && !catalogo.data && (
        <span role="alert" className="text-xs text-destructive">
          No se pudo cargar el catálogo de tipos de documento.{' '}
          <button type="button" className="underline" onClick={() => void catalogo.refetch()}>
            Reintentar
          </button>
        </span>
      )}

      {value.length === 0 && <span className="text-xs text-muted-foreground">Todavía no hay documentos.</span>}

      {value.map((fila, indice) => (
        <div key={indice} className="flex flex-wrap items-center gap-3 rounded-md border border-border p-2">
          <div className="min-w-48 flex-1">
            <Select
              value={fila.tipoDocumentoCodigo}
              disabled={disabled}
              onValueChange={(valor) => cambiarFila(indice, { tipoDocumentoCodigo: valor ?? '' })}
            >
              <SelectTrigger className="w-full" aria-label={`Tipo de documento ${indice + 1}`}>
                <SelectValue>
                  {(valor: string | null) => (valor ? (tipos.get(valor) ?? 'Cargando…') : 'Selecciona un tipo')}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {[...tipos.entries()]
                  .filter(([codigo]) => codigo === fila.tipoDocumentoCodigo || !elegidos.has(codigo))
                  .map(([codigo, nombre]) => (
                    <SelectItem key={codigo} value={codigo}>
                      {nombre}
                    </SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>

          <label className="flex items-center gap-2 text-sm text-foreground">
            <Checkbox
              checked={fila.obligatorio}
              disabled={disabled}
              onCheckedChange={(marcado) => cambiarFila(indice, { obligatorio: marcado === true })}
              aria-label={`Obligatorio, documento ${indice + 1}`}
            />
            Obligatorio
          </label>

          {!disabled && (
            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              aria-label={`Quitar documento ${indice + 1}`}
              onClick={() => onChange(value.filter((_, i) => i !== indice))}
            >
              <Trash2Icon />
            </Button>
          )}
        </div>
      ))}

      {!disabled && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="w-fit"
          onClick={() => onChange([...value, { tipoDocumentoCodigo: '', obligatorio: false }])}
        >
          <PlusIcon data-icon="inline-start" />
          Agregar documento
        </Button>
      )}

      {error && <span className="text-xs text-destructive">{error}</span>}
    </fieldset>
  )
}
