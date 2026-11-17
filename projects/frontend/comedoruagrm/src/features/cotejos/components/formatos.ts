import type { ResultadoCotejo } from '../types'

const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'UTC',
})

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/La_Paz',
})

const FORMATO_HORA = new Intl.DateTimeFormat('es-BO', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZone: 'America/La_Paz',
})

/** «01/09/2026»: una fecha `AAAA-MM-DD` sin corrimiento de zona horaria. */
export function formatearFecha(fecha: string): string {
  return FORMATO_FECHA.format(new Date(`${fecha}T00:00:00Z`))
}

/** «01/09/2026 – 30/09/2026»: el periodo de una planilla. */
export function formatearPeriodo(desde: string, hasta: string): string {
  return `${formatearFecha(desde)} – ${formatearFecha(hasta)}`
}

/** «16/11/2026, 11:30»: fecha y hora en la zona de La Paz. */
export function formatearFechaHora(iso: string): string {
  return FORMATO_FECHA_HORA.format(new Date(iso))
}

/** «14:35»: la hora de un registro en la zona de La Paz. */
export function formatearHora(iso: string): string {
  return FORMATO_HORA.format(new Date(iso))
}

export function etiquetaResultado(resultado: ResultadoCotejo): string {
  return resultado === 'CONFORME' ? 'Conforme' : 'Con discrepancias'
}
