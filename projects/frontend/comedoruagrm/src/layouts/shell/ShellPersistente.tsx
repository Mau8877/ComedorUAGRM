import { useLayoutEffect } from 'react'
import type { ReactNode } from 'react'

import type { PaginaShell, ShellContextValue } from './shellContext'

interface ContenidoEnShellProps extends PaginaShell {
  shell: ShellContextValue
  children: ReactNode
}

// * Header y sidebar se montan UNA sola vez (en `routes/_authenticated.tsx`) y no se desmontan al navegar:
// * lo único que cambia entre pantallas es el contenido y el título. Las pantallas siguen declarando
// * `<AdminLayout title=...>` (o el de su rol); dentro del shell esos layouts no dibujan el marco otra vez,
// * solo le avisan al shell su título y su ítem activo y devuelven el contenido.
export function ContenidoEnShell({ shell, title, activeHref, children }: ContenidoEnShellProps) {
  // * Layout effect (no useEffect): el título queda puesto antes del primer pintado, sin parpadeo.
  useLayoutEffect(() => shell.registrar({ title, activeHref }), [shell, title, activeHref])
  return <>{children}</>
}
