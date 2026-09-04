import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/es'

dayjs.extend(relativeTime)
dayjs.locale('es')

/**
 * Formatea una fecha ISO-8601 a relativo en español (ej. "hace 5 minutos",
 * "hace 2 días") -- pensado para timestamps de eventos recientes
 * (notificaciones, actividad reciente), no para fechas de negocio lejanas
 * (ahí una fecha absoluta es más útil que "hace 3 meses").
 */
export function formatRelativeDate(iso: string): string {
  return dayjs(iso).fromNow()
}
