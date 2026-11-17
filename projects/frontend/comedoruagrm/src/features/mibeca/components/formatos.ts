const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'America/La_Paz',
})

const FORMATO_HORA = new Intl.DateTimeFormat('es-BO', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZone: 'America/La_Paz',
})

/** «18/09/2026» a partir de AAAA-MM-DD (sin pasar por la zona horaria: ya es un día de calendario). */
export function formatearDia(dia: string): string {
  const [anio, mes, diaMes] = dia.split('-')
  return `${diaMes}/${mes}/${anio}`
}

/** «28/08/2026» de un instante ISO, en la zona de La Paz. */
export function formatearFechaInstante(iso: string): string {
  return FORMATO_FECHA.format(new Date(iso))
}

/** «12:30» de un instante ISO, en la zona de La Paz. */
export function formatearHora(iso: string): string {
  return FORMATO_HORA.format(new Date(iso))
}
