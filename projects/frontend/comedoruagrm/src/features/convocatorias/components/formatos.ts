// Formatos de la feature. Las fechas del backend son `yyyy-MM-dd` (calendario de Bolivia): se reordenan como texto,
// sin pasar por `Date`, para que la zona horaria del navegador nunca corra un día.

const FECHA = /^(\d{4})-(\d{2})-(\d{2})$/

/** `2026-08-01` → `01/08/2026`. Lo que no es una fecha `yyyy-MM-dd` se devuelve tal cual. */
export function formatearFecha(fecha: string): string {
  const partes = FECHA.exec(fecha)
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : fecha
}

/** Un valor opcional vacío (`null`) se muestra como «—». */
export function formatearOpcional(valor: number | string | null | undefined): string {
  return valor === null || valor === undefined || valor === '' ? '—' : String(valor)
}
