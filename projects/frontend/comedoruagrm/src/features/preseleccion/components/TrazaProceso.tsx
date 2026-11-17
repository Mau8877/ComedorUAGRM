import type { PasoTraza } from '../types'
import { formatearFechaHora } from './formatos'

/** Los pasos del proceso automático de la postulación, en orden, tal como quedaron en su historial. */
export function TrazaProceso({ pasos }: { pasos: PasoTraza[] }) {
  if (pasos.length === 0) {
    return <p className="text-sm text-muted-foreground">Todavía no hay pasos registrados.</p>
  }
  return (
    <div className="overflow-x-auto rounded-md border border-border">
      <table className="w-full text-sm">
        <thead className="bg-muted/60 text-left text-muted-foreground">
          <tr>
            <th scope="col" className="px-3 py-2 font-medium">
              Paso
            </th>
            <th scope="col" className="px-3 py-2 font-medium">
              Resultado
            </th>
            <th scope="col" className="px-3 py-2 font-medium">
              Fecha
            </th>
          </tr>
        </thead>
        <tbody>
          {pasos.map((p, i) => (
            <tr key={`${p.paso}-${i}`} className="border-t border-border">
              <td className="px-3 py-2 text-foreground">{p.paso}</td>
              <td className="px-3 py-2 text-foreground">{p.detalle ?? '—'}</td>
              <td className="px-3 py-2 whitespace-nowrap text-foreground">{formatearFechaHora(p.fecha)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
