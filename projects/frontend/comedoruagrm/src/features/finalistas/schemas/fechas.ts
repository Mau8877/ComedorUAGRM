/** Hoy como `YYYY-MM-DD` en la zona del navegador (la misma que ve quien llena el formulario). */
export function hoyIso(ahora: Date = new Date()): string {
  const mes = String(ahora.getMonth() + 1).padStart(2, '0')
  const dia = String(ahora.getDate()).padStart(2, '0')
  return `${ahora.getFullYear()}-${mes}-${dia}`
}

/** Mañana como `YYYY-MM-DD`: lo más pronto que puede ser un plazo de entrega. */
export function mananaIso(ahora: Date = new Date()): string {
  const manana = new Date(ahora)
  manana.setDate(manana.getDate() + 1)
  return hoyIso(manana)
}
