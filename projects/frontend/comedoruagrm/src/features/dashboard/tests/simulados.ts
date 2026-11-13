// Simulacros compartidos por los tests de la pantalla del dashboard. Viven en su propio archivo (sin imports de
// la feature) para que las fábricas de `vi.mock` puedan importarlos sin ciclos.
import { vi } from 'vitest'

export const cliente = { get: vi.fn() }

export const cerrarSesion = vi.fn()

export const descargar = vi.fn()

export const avisos = {
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
  toastWarning: vi.fn(),
  toastInfo: vi.fn(),
}

/** Igual que `etiquetaRol` de features/auth (que se simula entera para no arrastrar el router). */
export const etiquetaRolSimulada = (rol: string) => (rol === 'DIRECTIVO' ? 'Directivo' : rol)
