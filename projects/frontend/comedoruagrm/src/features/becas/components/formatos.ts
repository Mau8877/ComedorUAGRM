// Las fechas del backend son `yyyy-MM-dd` (calendario de Bolivia): se reordenan como texto, sin pasar por `Date`,
// para que la zona horaria del navegador nunca corra un día.

const FECHA = /^(\d{4})-(\d{2})-(\d{2})$/

/** `2026-11-10` → `10/11/2026`. Lo que no es una fecha `yyyy-MM-dd` se devuelve tal cual. */
export function formatearFecha(fecha: string): string {
  const partes = FECHA.exec(fecha)
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : fecha
}
