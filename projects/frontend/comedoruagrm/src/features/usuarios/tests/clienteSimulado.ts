// apiClient simulado de los tests de pantalla. Vive en su propio archivo (sin imports de la
// feature) para que las fábricas de `vi.mock` puedan importarlo sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn(), post: vi.fn(), patch: vi.fn() }

export const cerrarSesion = vi.fn()

const ETIQUETAS: Record<string, string> = {
  ADMINISTRADOR_DUBSS: 'Administrador DUBSS',
  DIRECTIVO: 'Directivo',
  PERSONAL_COMEDOR: 'Personal del comedor',
}

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => ETIQUETAS[rol] ?? rol
