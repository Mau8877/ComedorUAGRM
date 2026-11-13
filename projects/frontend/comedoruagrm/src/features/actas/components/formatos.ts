const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/La_Paz',
})

/** «13/11/2026, 11:30»: la fecha y hora de generación en la zona de La Paz. */
export function formatearFechaHora(iso: string): string {
  return FORMATO_FECHA_HORA.format(new Date(iso))
}
