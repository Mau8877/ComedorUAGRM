import { describe, expect, it } from 'vitest'
import { validateField } from '../lib/validate'

describe('validateField', () => {
  it('rechaza un campo vacío o con solo espacios', () => {
    expect(validateField('text', '')).toBe('required')
    expect(validateField('text', '   ')).toBe('required')
  })

  it('acepta un texto con contenido', () => {
    expect(validateField('text', 'Ana')).toBeNull()
  })

  it('rechaza un correo con formato inválido', () => {
    expect(validateField('email', 'sin-arroba')).toBe('email')
    expect(validateField('email', 'a@b')).toBe('email')
  })

  it('acepta un correo válido', () => {
    expect(validateField('email', 'ana@nexware.example')).toBeNull()
  })

  it('el correo vacío es "obligatorio", no "inválido"', () => {
    expect(validateField('email', '')).toBe('required')
  })
})
