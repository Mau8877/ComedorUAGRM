import {
  MenuIcon,
  UtensilsCrossedIcon,
  SunIcon,
  MoonIcon,
  LogOutIcon,
  UserIcon,
} from 'lucide-react'

import { cn } from '@/lib/utils'
import { useUiStore } from '@/store'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import type { LayoutUser } from './types'

function iniciales(nombre: string) {
  return nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join('')
}

interface AppHeaderProps {
  appName: string
  user: LayoutUser
}

// * Un solo botón de menú controla el sidebar en ambos tamaños de pantalla
// * (desktop: colapsa/expande el ancho; mobile: abre/cierra el drawer) --
// * se decide en runtime con matchMedia, igual que el breakpoint `md` de
// * Tailwind, para que el control visible sea uno solo como en la
// * referencia de diseño, en vez de dos botones distintos.
function toggleSidebar() {
  const isDesktop = window.matchMedia('(min-width: 768px)').matches
  const { toggleSidebarCollapsed, mobileSidebarOpen, setMobileSidebarOpen } =
    useUiStore.getState()

  if (isDesktop) {
    toggleSidebarCollapsed()
  } else {
    setMobileSidebarOpen(!mobileSidebarOpen)
  }
}

// * Clase compartida por los botones del header: sobre fondo navy sólido
// * (bg-header), el hover/foco por default de Button (pensado para fondos
// * claros) queda invisible -- se pisa acá con blanco translúcido.
const headerButtonClass =
  'text-header-foreground hover:bg-header-foreground/15 hover:text-header-foreground focus-visible:ring-header-foreground/40'

export function AppHeader({ appName, user }: AppHeaderProps) {
  const theme = useUiStore((s) => s.theme)
  const toggleTheme = useUiStore((s) => s.toggleTheme)

  return (
    <header className="flex h-16 shrink-0 items-center gap-3 bg-header px-4 text-header-foreground">
      <Button
        variant="ghost"
        size="icon-sm"
        className={headerButtonClass}
        onClick={toggleSidebar}
      >
        <MenuIcon />
        <span className="sr-only">Alternar sidebar</span>
      </Button>

      <div className="flex items-center gap-2.5">
        <span className="flex size-9 items-center justify-center rounded-xl bg-accent">
          <UtensilsCrossedIcon className="size-5 text-accent-foreground" />
        </span>
        <span className="font-heading text-lg font-semibold">{appName}</span>
      </div>

      <div className="ml-auto flex items-center gap-1">
        <Button
          variant="ghost"
          size="icon-sm"
          className={headerButtonClass}
          onClick={toggleTheme}
        >
          {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          <span className="sr-only">Cambiar tema</span>
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger
            className={cn(
              'ml-1 flex items-center gap-2 rounded-full px-1.5 py-1 outline-none transition-colors hover:bg-header-foreground/15 focus-visible:ring-3 focus-visible:ring-header-foreground/40'
            )}
          >
            <Avatar size="sm">
              <AvatarFallback className="bg-accent text-accent-foreground">
                {iniciales(user.nombre)}
              </AvatarFallback>
            </Avatar>
            <span className="hidden text-left text-sm sm:block">
              <span className="block leading-tight font-medium">
                {user.nombre}
              </span>
              <span className="block text-xs leading-tight text-header-foreground/70">
                {user.rol}
              </span>
            </span>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuGroup>
              <DropdownMenuLabel>{user.nombre}</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <UserIcon />
                Mi perfil
              </DropdownMenuItem>
              <DropdownMenuItem variant="destructive">
                <LogOutIcon />
                Cerrar sesión
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
