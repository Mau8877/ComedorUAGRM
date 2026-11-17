import type { EstadoConvocatoria } from '../types'

// * Colores solo con tokens de tema (success / warning / info / muted), igual que EstadoCuentaBadge. Vive aparte del
// * componente del badge para que ese archivo exporte solo componentes (react-refresh).
export const ESTILOS_ESTADO: Record<EstadoConvocatoria, { etiqueta: string; fondo: string; punto: string }> = {
  BORRADOR: { etiqueta: 'Borrador', fondo: 'bg-muted text-muted-foreground', punto: 'bg-muted-foreground' },
  ABIERTA: { etiqueta: 'Abierta', fondo: 'bg-success/15 text-success', punto: 'bg-success' },
  CERRADA: { etiqueta: 'Cerrada', fondo: 'bg-warning/15 text-warning', punto: 'bg-warning' },
  PUBLICADA: { etiqueta: 'Publicada', fondo: 'bg-info/15 text-info', punto: 'bg-info' },
}

export function etiquetaEstado(estado: EstadoConvocatoria): string {
  return ESTILOS_ESTADO[estado].etiqueta
}
