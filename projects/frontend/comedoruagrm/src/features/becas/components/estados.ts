import type { EstadoBeca } from '../types'

// * Colores solo con tokens de tema (success / warning / destructive), igual que los demás badges de estado. Vive
// * aparte del componente del badge para que ese archivo exporte solo componentes (react-refresh).
export const ESTILOS_ESTADO_BECA: Record<EstadoBeca, { etiqueta: string; fondo: string; punto: string }> = {
  VIGENTE: { etiqueta: 'Vigente', fondo: 'bg-success/15 text-success', punto: 'bg-success' },
  SUSPENDIDA: { etiqueta: 'Suspendida', fondo: 'bg-warning/15 text-warning', punto: 'bg-warning' },
  BAJA: { etiqueta: 'Baja', fondo: 'bg-destructive/15 text-destructive', punto: 'bg-destructive' },
}

export function etiquetaEstadoBeca(estado: EstadoBeca): string {
  return ESTILOS_ESTADO_BECA[estado].etiqueta
}
