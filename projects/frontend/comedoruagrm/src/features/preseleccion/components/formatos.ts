const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'America/La_Paz',
})

/** «01/07/2026»: la fecha de un instante ISO en la zona de La Paz. */
export function formatearFecha(iso: string): string {
  return FORMATO_FECHA.format(new Date(iso))
}

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZone: 'America/La_Paz',
})

/** «16/07/2026 10:42»: fecha y hora de un instante ISO en la zona de La Paz; «—» si no hay. */
export function formatearFechaHora(iso: string | null): string {
  return iso ? FORMATO_FECHA_HORA.format(new Date(iso)).replace(',', '') : '—'
}
