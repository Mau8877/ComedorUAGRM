import { createContext, useContext } from 'react'
import type { LucideIcon } from 'lucide-react'

import type { MigaPagina } from '@/components/layout'

export interface PaginaShell {
  title: string
  activeHref?: string
  icono?: LucideIcon
  migas?: MigaPagina[]
}

export interface ShellContextValue {
  /** Registra el título/ítem activo de la pantalla montada; devuelve la función que lo retira al desmontarla. */
  registrar: (pagina: PaginaShell) => () => void
}

export const ShellContext = createContext<ShellContextValue | null>(null)

/** `null` fuera del shell persistente (pantallas de prueba, tests): ahí el layout dibuja su propio marco. */
export function useShellPersistente() {
  return useContext(ShellContext)
}
