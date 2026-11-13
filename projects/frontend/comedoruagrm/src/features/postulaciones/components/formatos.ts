const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/La_Paz',
})

const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'America/La_Paz',
})

/** «13/11/2026, 11:30»: fecha y hora en la zona de La Paz (los pasos de la línea de tiempo, las observaciones). */
export function formatearFechaHora(iso: string): string {
  return FORMATO_FECHA_HORA.format(new Date(iso))
}

/** «13/11/2026»: solo la fecha (el listado, el plazo de entrega). */
export function formatearFecha(iso: string): string {
  return FORMATO_FECHA.format(new Date(iso))
}

/** Un plazo de entrega («2026-11-20», sin hora) como «20/11/2026», sin correrlo de día por la zona horaria. */
export function formatearDia(dia: string): string {
  const [anio, mes, diaDelMes] = dia.split('-')
  return `${diaDelMes}/${mes}/${anio}`
}
