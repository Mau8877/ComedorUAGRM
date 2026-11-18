// Formato de fechas de las pantallas de inicio. Se fija la zona de La Paz para que no dependa de la del navegador.

/** `2026-11-18T14:30:00Z` → `10:30`. */
export function formatearHora(iso: string): string {
  const partes = new Intl.DateTimeFormat('es-BO', {
    timeZone: 'America/La_Paz',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(iso))
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? ''
  return `${valor('hour')}:${valor('minute')}`
}

/** `2026-11-18` → `18/11`. */
export function formatearDiaCorto(fecha: string): string {
  const [, mes, dia] = fecha.split('-')
  return `${dia}/${mes}`
}

/** `2026-11-18` → `18/11/2026`. */
export function formatearFecha(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-')
  return `${dia}/${mes}/${anio}`
}

/** Días enteros que faltan hasta `iso` desde `ahora` (0 si ya pasó). */
export function diasHasta(iso: string, ahora: Date = new Date()): number {
  const ms = new Date(iso).getTime() - ahora.getTime()
  return ms <= 0 ? 0 : Math.ceil(ms / 86_400_000)
}
