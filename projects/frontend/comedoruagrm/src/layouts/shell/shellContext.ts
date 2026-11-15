import { createContext, useContext } from 'react'

export interface PaginaShell {
  title: string
  activeHref?: string
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
