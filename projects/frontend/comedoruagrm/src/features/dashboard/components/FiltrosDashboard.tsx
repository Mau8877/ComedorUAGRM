import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

import type { GestionFiltro } from '../types'

/** Valor interno de «Todas las convocatorias»: los selectores no aceptan un valor vacío. */
export const TODAS = 'todas'

interface FiltrosDashboardProps {
  gestiones: GestionFiltro[]
  /** La gestión que se está editando (no necesariamente la aplicada). */
  gestion: string | undefined
  /** La convocatoria que se está editando; `undefined` = todas. */
  convocatoria: string | undefined
  onGestionChange: (codigo: string) => void
  onConvocatoriaChange: (codigo: string | undefined) => void
  onAplicar: () => void
  deshabilitado?: boolean
}

/** Selectores de gestión y convocatoria con su botón «Aplicar» (CU12). */
export function FiltrosDashboard({
  gestiones,
  gestion,
  convocatoria,
  onGestionChange,
  onConvocatoriaChange,
  onAplicar,
  deshabilitado = false,
}: FiltrosDashboardProps) {
  const convocatorias = gestiones.find((g) => g.codigo === gestion)?.convocatorias ?? []

  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex flex-col gap-1">
        <span className="text-xs font-medium text-muted-foreground">Gestión</span>
        <Select value={gestion} onValueChange={(valor) => valor && onGestionChange(valor)} disabled={deshabilitado}>
          <SelectTrigger size="sm" aria-label="Gestión" className="min-w-40">
            <SelectValue>
              {(valor: string | null) => gestiones.find((g) => g.codigo === valor)?.etiqueta ?? ''}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            {gestiones.map((g) => (
              <SelectItem key={g.codigo} value={g.codigo}>
                {g.etiqueta}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex flex-col gap-1">
        <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
        <Select
          value={convocatoria ?? TODAS}
          onValueChange={(valor) => valor && onConvocatoriaChange(valor === TODAS ? undefined : valor)}
          disabled={deshabilitado}
        >
          <SelectTrigger size="sm" aria-label="Convocatoria" className="min-w-56">
            <SelectValue>
              {(valor: string | null) =>
                valor === TODAS || !valor
                  ? 'Todas las convocatorias'
                  : (convocatorias.find((c) => c.codigo === valor)?.nombre ?? '')
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={TODAS}>Todas las convocatorias</SelectItem>
            {convocatorias.map((c) => (
              <SelectItem key={c.codigo} value={c.codigo}>
                {c.nombre}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Button size="sm" onClick={onAplicar} disabled={deshabilitado}>
        Aplicar
      </Button>
    </div>
  )
}
