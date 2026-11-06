/**
 * Dispara en el navegador la descarga de un archivo que ya está en memoria (un `Blob`).
 *
 * * Crea un enlace temporal, lo cliquea y lo revoca enseguida: no deja nada colgado en la página.
 */
export function descargarArchivo(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}
