// apiClient simulado de los tests de las pantallas de resultados. Vive en su propio archivo (sin imports de
// la feature) para que las fábricas de `vi.mock` puedan importarlo sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn(), post: vi.fn() }

export const cerrarSesion = vi.fn()

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => (rol === 'ADMINISTRADOR_DUBSS' ? 'Administrador DUBSS' : rol)
