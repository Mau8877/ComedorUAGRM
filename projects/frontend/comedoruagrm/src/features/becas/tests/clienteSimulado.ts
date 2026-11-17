// apiClient simulado de los tests del padrón de becados. Vive en su propio archivo (sin imports de la feature) para
// que las fábricas de `vi.mock` puedan importarlo sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn(), post: vi.fn() }

export const cerrarSesion = vi.fn()

/** Las convocatorias con resultados publicados que ofrece el selector (las cambia cada test). */
export const convocatoriasPublicadas: { data: { codigo: string; nombre: string }[] } = { data: [] }

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => (rol === 'ADMINISTRADOR_DUBSS' ? 'Administrador DUBSS' : rol)
