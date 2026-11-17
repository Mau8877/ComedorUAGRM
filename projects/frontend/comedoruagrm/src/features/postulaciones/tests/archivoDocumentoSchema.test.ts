import { describe, expect, it } from 'vitest'

import { crearArchivoDocumentoSchema, formatearTamano, nombresDeFormatos } from '../schemas'
import type { ReglasArchivo } from '../types'

const REGLAS: ReglasArchivo = {
  tamanoMaximoBytes: 1024,
  formatosPermitidos: ['application/pdf', 'image/jpeg', 'image/png'],
  maximoArchivosPorTipo: 5,
}

const archivo = (bytes: number, tipo: string, nombre = 'a.pdf') =>
  new File([new Uint8Array(bytes)], nombre, { type: tipo })

describe('crearArchivoDocumentoSchema', () => {
  const schema = crearArchivoDocumentoSchema(REGLAS)

  it('acepta un archivo permitido dentro del tamaño máximo', () => {
    expect(schema.safeParse({ archivo: archivo(100, 'application/pdf') }).success).toBe(true)
    expect(schema.safeParse({ archivo: archivo(1024, 'image/png', 'a.png') }).success).toBe(true)
  })

  it('rechaza un archivo que supera el tamaño máximo con el tamaño en el mensaje', () => {
    const r = schema.safeParse({ archivo: archivo(1025, 'application/pdf') })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toContain('tamaño máximo')
  })

  it('rechaza un formato no permitido y dice cuáles se aceptan', () => {
    const r = schema.safeParse({ archivo: archivo(10, 'text/plain', 'a.txt') })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toBe('Formato no permitido. Se aceptan: PDF, JPG o PNG')
  })

  it('rechaza un archivo vacío', () => {
    const r = schema.safeParse({ archivo: archivo(0, 'application/pdf') })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toBe('El archivo está vacío')
  })

  it('pide elegir un archivo cuando no hay ninguno', () => {
    const r = schema.safeParse({ archivo: null })

    expect(r.success).toBe(false)
    expect(r.error?.issues[0].message).toBe('Elige un archivo')
  })
})

describe('formatearTamano y nombresDeFormatos', () => {
  it('muestra el tamaño en KB o MB', () => {
    expect(formatearTamano(800 * 1024)).toBe('800 KB')
    expect(formatearTamano(5 * 1024 * 1024)).toBe('5 MB')
    expect(formatearTamano(1.2 * 1024 * 1024)).toBe('1.2 MB')
    expect(formatearTamano(10)).toBe('1 KB')
  })

  it('une los formatos con coma y «o»', () => {
    expect(nombresDeFormatos(['application/pdf'])).toBe('PDF')
    expect(nombresDeFormatos(['image/png', 'application/pdf'])).toBe('PDF o PNG')
    expect(nombresDeFormatos(['image/png', 'image/jpeg', 'application/pdf'])).toBe('PDF, JPG o PNG')
  })
})
