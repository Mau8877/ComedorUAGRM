export type FieldError = 'required' | 'email' | null

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// Valida un campo del formulario de contacto. Es el único lugar con esta
// regla: el <script> de ContactForm la usa y los tests la cubren.
export function validateField(type: string, value: string): FieldError {
  const trimmed = value.trim()
  if (!trimmed) return 'required'
  if (type === 'email' && !EMAIL.test(trimmed)) return 'email'
  return null
}
