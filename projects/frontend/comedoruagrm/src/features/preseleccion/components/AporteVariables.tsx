import type { AporteVariable } from '../types'
import { nombreVariable, VARIABLES_MODELO } from '../schemas'

interface AporteVariablesProps {
  variables: AporteVariable[]
}

function puntos(valor: number | null): string {
  return valor === null ? '—' : valor.toFixed(1)
}

// El backend guarda el desglose en un jsonb, que no conserva el orden: se muestra siempre en el orden del modelo.
function ordenar(variables: AporteVariable[]): AporteVariable[] {
  const posicion = (clave: string) => {
    const i = VARIABLES_MODELO.findIndex((v) => v.clave === clave)
    return i === -1 ? VARIABLES_MODELO.length : i
  }
  return [...variables].sort((a, b) => posicion(a.clave) - posicion(b.clave))
}

/**
 * Cuánto aportó cada variable al puntaje: una barra por variable con «puntos / máximo». Una variable sin dato (la
 * postulación se envió antes de que el formulario la pidiera) no entra al puntaje y se marca así. Una variable sin
 * ponderación (peso 0) no aporta nada y se muestra atenuada.
 */
export function AporteVariables({ variables }: AporteVariablesProps) {
  if (variables.length === 0) {
    return <p className="text-sm text-muted-foreground">Esta precalificación no guardó el desglose por variable.</p>
  }
  return (
    <ul className="flex flex-col gap-4">
      {ordenar(variables).map((v) => {
        const sinDato = v.nivel === null
        const sinPeso = !sinDato && (v.maximo ?? 0) === 0
        const parte = v.maximo && v.puntos !== null ? Math.min(Math.max(v.puntos / v.maximo, 0), 1) : 0
        return (
          <li key={v.clave} className={sinDato || sinPeso ? 'opacity-60' : undefined}>
            <div className="flex items-baseline justify-between gap-2 text-sm">
              <span className="text-foreground">{nombreVariable(v.clave)}</span>
              <span className="font-medium text-foreground">
                {sinDato ? 'Sin dato' : sinPeso ? 'No pondera' : `${puntos(v.puntos)} / ${puntos(v.maximo)}`}
              </span>
            </div>
            <div
              className="mt-1 h-2 overflow-hidden rounded-full bg-muted"
              role="progressbar"
              aria-label={nombreVariable(v.clave)}
              aria-valuemin={0}
              aria-valuemax={v.maximo ?? 0}
              aria-valuenow={v.puntos ?? 0}
            >
              <div className="h-full rounded-full bg-primary" style={{ width: `${parte * 100}%` }} />
            </div>
          </li>
        )
      })}
    </ul>
  )
}
