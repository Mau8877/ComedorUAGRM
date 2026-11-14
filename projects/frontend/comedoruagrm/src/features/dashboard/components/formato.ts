// Formato de números y meses del dashboard (CU12). Se hace a mano (sin Intl) para que el resultado no dependa
// de los datos de idioma del navegador o de Node: miles con punto y decimales con coma.

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

/** `5230` → `5.230`. */
export function formatearNumero(valor: number): string {
  return String(Math.round(valor)).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

/** Porcentaje con un decimal como máximo: `87.5` → `87,5 %`, `90` → `90 %`. */
export function formatearPorcentaje(valor: number): string {
  const redondeado = Math.round(valor * 10) / 10
  return `${String(redondeado).replace('.', ',')} %`
}

/** `2026-03` → `Mar 2026`. Un valor que no es `yyyy-MM` se devuelve tal cual. */
export function formatearMes(mes: string): string {
  const coincidencia = /^(\d{4})-(\d{2})$/.exec(mes)
  const indice = coincidencia ? Number(coincidencia[2]) - 1 : -1
  return coincidencia && indice >= 0 && indice < 12 ? `${MESES[indice]} ${coincidencia[1]}` : mes
}

/** `2026-11-13T06:00:00Z` → `13/11/2026 02:00` (hora de La Paz). */
export function formatearFechaHora(iso: string): string {
  const partes = new Intl.DateTimeFormat('es-BO', {
    timeZone: 'America/La_Paz',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(iso))
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? ''
  return `${valor('day')}/${valor('month')}/${valor('year')} ${valor('hour')}:${valor('minute')}`
}
