const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', { day: '2-digit', month: '2-digit', year: 'numeric' })

/** `YYYY-MM-DD` → `dd/mm/aaaa` sin pasar por la zona horaria (es una fecha, no un instante). */
export function formatearFechaDia(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia))
}

export function formatearPuntaje(puntaje: number | null): string {
  return puntaje === null ? '—' : Number(puntaje).toFixed(2)
}
