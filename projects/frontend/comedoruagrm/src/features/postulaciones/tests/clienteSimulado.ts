// apiClient y utilidades simuladas de los tests de las pantallas de postulaciones. Vive en su propio archivo (sin
// imports de la feature) para que las fábricas de `vi.mock` puedan importarlo sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn() }

export const cerrarSesion = vi.fn()

/** El `navigate` de TanStack Router simulado: los tests miran a dónde se navegó. */
export const navegar = vi.fn()

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => (rol === 'POSTULANTE' ? 'Postulante' : rol)
