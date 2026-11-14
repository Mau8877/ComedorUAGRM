import { Link } from '@tanstack/react-router'
import { LogInIcon, MoonIcon, SunIcon } from 'lucide-react'

import { runThemeTransition } from '@/utils/runThemeTransition'
import { useUiStore } from '@/store'
import { Button } from '@/components/ui/button'

import { ComedoruLogo } from './ComedoruLogo'

interface PublicHeaderProps {
  appName: string
}

// * Mismo look que AppHeader (bg-header, logo, botón de tema) pero sin sidebar,
// * notificaciones ni menú de usuario: es el header de las páginas públicas.
const headerButtonClass =
  'text-header-foreground hover:bg-header-foreground/15 hover:text-header-foreground focus-visible:ring-header-foreground/40'

export function PublicHeader({ appName }: PublicHeaderProps) {
  const theme = useUiStore((s) => s.theme)
  const toggleTheme = useUiStore((s) => s.toggleTheme)

  return (
    <header className="flex h-16 shrink-0 items-center gap-3 bg-header px-4 text-header-foreground">
      <Link to="/" className="flex items-center gap-2.5">
        <ComedoruLogo className="size-9" />
        <span className="font-heading text-lg font-semibold">{appName}</span>
      </Link>

      <div className="ml-auto flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon-sm"
          className={headerButtonClass}
          onClick={(event) => runThemeTransition(event.clientX, event.clientY, toggleTheme)}
        >
          {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          <span className="sr-only">Cambiar tema</span>
        </Button>
        <Button nativeButton={false} render={<Link to="/login" />}>
          <LogInIcon />
          Iniciar sesión
        </Button>
      </div>
    </header>
  )
}
