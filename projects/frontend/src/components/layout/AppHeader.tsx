import { MenuIcon, PanelLeftIcon, LogOutIcon, UserIcon } from 'lucide-react'

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
  title: string
  user: LayoutUser
}

export function AppHeader({ title, user }: AppHeaderProps) {
  const toggleSidebarCollapsed = useUiStore((s) => s.toggleSidebarCollapsed)
  const setMobileSidebarOpen = useUiStore((s) => s.setMobileSidebarOpen)

  return (
    <header className="flex h-14 shrink-0 items-center gap-2 border-b border-border bg-card px-4">
      <Button
        variant="ghost"
        size="icon-sm"
        className="hidden md:inline-flex"
        onClick={toggleSidebarCollapsed}
      >
        <PanelLeftIcon />
        <span className="sr-only">Colapsar sidebar</span>
      </Button>
      <Button
        variant="ghost"
        size="icon-sm"
        className="md:hidden"
        onClick={() => setMobileSidebarOpen(true)}
      >
        <MenuIcon />
        <span className="sr-only">Abrir sidebar</span>
      </Button>

      <h1 className="truncate font-heading text-sm font-medium text-foreground">
        {title}
      </h1>

      <div className="ml-auto">
        <DropdownMenu>
          <DropdownMenuTrigger className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-3 focus-visible:ring-ring/30">
            <Avatar size="sm">
              <AvatarFallback>{iniciales(user.nombre)}</AvatarFallback>
            </Avatar>
            <span className="hidden text-left text-sm sm:block">
              <span className="block leading-tight font-medium text-foreground">
                {user.nombre}
              </span>
              <span className="block text-xs leading-tight text-muted-foreground">
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
