/** UUID v4 para el header `Idempotency-Key`; usa `crypto.randomUUID` y, si el navegador no lo trae, un respaldo. */
export function nuevaClaveIdempotencia(): string {
  const nativo = globalThis.crypto?.randomUUID?.()
  if (nativo) return nativo
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (letra) => {
    const azar = Math.floor(Math.random() * 16)
    return (letra === 'x' ? azar : (azar & 0x3) | 0x8).toString(16)
  })
}
