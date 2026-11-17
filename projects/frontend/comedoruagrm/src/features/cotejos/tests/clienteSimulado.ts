// apiClient simulado de los tests de la pantalla «Cotejo de asistencia». Vive en su propio archivo (sin imports de la
// feature) para que las fábricas de `vi.mock` puedan importarlo sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn(), post: vi.fn() }

export const cerrarSesion = vi.fn()

/** Las convocatorias con resultados publicadas que ofrece el selector (las cambia cada test). */
export const convocatoriasPublicadas: { data: { codigo: string; nombre: string }[] } = { data: [] }

/** Las planillas de la convocatoria elegida que ofrece el selector (las cambia cada test). */
export const planillasDelSelector: {
  data: { codigo: string; fechaDesde: string; fechaHasta: string; totalBecados: number }[]
} = { data: [] }

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => (rol === 'ADMINISTRADOR_DUBSS' ? 'Administrador DUBSS' : rol)
