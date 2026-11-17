import { z } from 'zod'

import type { ReglasArchivo } from '../types'

const NOMBRES: Record<string, string> = {
  'application/pdf': 'PDF',
  'image/jpeg': 'JPG',
  'image/png': 'PNG',
}

/** «5 MB», «1.2 MB», «800 KB»: el tamaño de un archivo para mostrarlo. */
export function formatearTamano(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    const mb = bytes / (1024 * 1024)
    return `${Number.isInteger(mb) ? mb : mb.toFixed(1)} MB`
  }
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

/** «PDF, JPG o PNG»: los formatos permitidos para el mensaje. */
export function nombresDeFormatos(formatos: string[]): string {
  const nombres = [...formatos].sort().map((f) => NOMBRES[f] ?? f)
  if (nombres.length <= 1) return nombres.join('')
  return `${nombres.slice(0, -1).join(', ')} o ${nombres[nombres.length - 1]}`
}

/**
 * El archivo de un documento (CU16), validado con las reglas que manda el backend. Es solo para dar feedback
 * inmediato: el backend vuelve a validar y es el que decide, por el **contenido** del archivo (no por su extensión).
 */
export function crearArchivoDocumentoSchema(reglas: ReglasArchivo) {
  return z.object({
    archivo: z
      .file('Elige un archivo')
      .min(1, 'El archivo está vacío')
      .max(reglas.tamanoMaximoBytes, `El archivo supera el tamaño máximo de ${formatearTamano(reglas.tamanoMaximoBytes)}`)
      .mime(
        reglas.formatosPermitidos as [string, ...string[]],
        `Formato no permitido. Se aceptan: ${nombresDeFormatos(reglas.formatosPermitidos)}`,
      ),
  })
}
