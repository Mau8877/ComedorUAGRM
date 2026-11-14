const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-BO', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const FORMATO_NUMERO = new Intl.NumberFormat('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** «12/11/2026, 10:30»: fecha y hora de una publicación. */
export function formatearFechaHora(iso: string): string {
  return FORMATO_FECHA_HORA.format(new Date(iso))
}

/** El puntaje o la nota con dos decimales; `null` es «—»: nunca se inventa un número. */
export function formatearPuntaje(valor: number | null): string {
  return valor === null ? '—' : FORMATO_NUMERO.format(valor)
}
