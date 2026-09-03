import { useLocation } from '@tanstack/react-router'

import { cn } from '@/lib/utils'
import { useUiStore } from '@/store'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'

import type { NavItem } from './types'

interface SidebarNavProps {
  items: NavItem[]
  onNavigate?: () => void
}

// * Se usa <a> en vez del <Link> tipado de TanStack Router porque los
// * navItems de cada rol (src/layouts/) hoy apuntan a rutas que todavía no
// * existen (ej. /usuarios, /menus) -- son placeholders de la estructura del
// * menú, no rutas reales todavía. Cuando esas rutas se implementen, este
// * componente pasa a usar <Link to={item.href}> para navegación tipada.
function SidebarNav({ items, onNavigate }: SidebarNavProps) {
  const location = useLocation()

  return (
    <nav className="flex flex-col gap-1 p-3">
      {items.map((item) => {
        const Icon = item.icon
        const isActive = location.pathname.startsWith(item.href)
        return (
          <a
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cn(
              'flex items-center gap-2.5 rounded-xl px-3 py-2 text-sm font-medium text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
              isActive && 'bg-sidebar-primary text-sidebar-primary-foreground'
            )}
          >
            <Icon className="size-4 shrink-0" />
            <span>{item.label}</span>
          </a>
        )
      })}
    </nav>
  )
}

interface AppSidebarProps {
  items: NavItem[]
  appName: string
}

// * El sidebar tiene dos estados independientes en useUiStore:
// * `sidebarCollapsed` (desktop: ancho completo vs. solo íconos) y
// * `mobileSidebarOpen` (mobile: el Sheet/drawer abierto o cerrado). Son
// * conceptos distintos -- confundirlos haría que el sidebar apareciera
// * abierto por default en mobile solo porque en desktop no está colapsado.
export function AppSidebar({ items, appName }: AppSidebarProps) {
  const sidebarCollapsed = useUiStore((s) => s.sidebarCollapsed)
  const mobileSidebarOpen = useUiStore((s) => s.mobileSidebarOpen)
  const setMobileSidebarOpen = useUiStore((s) => s.setMobileSidebarOpen)

  return (
    <>
      <aside
        className={cn(
          'hidden shrink-0 border-r border-sidebar-border bg-sidebar transition-[width] duration-200 md:flex md:flex-col',
          sidebarCollapsed ? 'md:w-16' : 'md:w-60'
        )}
      >
        <div className="flex h-14 items-center border-b border-sidebar-border px-4">
          <span
            className={cn(
              'truncate font-heading text-sm font-medium text-sidebar-foreground',
              sidebarCollapsed && 'md:hidden'
            )}
          >
            {appName}
          </span>
        </div>
        <div className={cn(sidebarCollapsed && 'md:[&_span]:hidden')}>
          <SidebarNav items={items} />
        </div>
      </aside>

      <Sheet open={mobileSidebarOpen} onOpenChange={setMobileSidebarOpen}>
        <SheetContent side="left" className="p-0 md:hidden">
          <SheetHeader className="border-b border-sidebar-border">
            <SheetTitle>{appName}</SheetTitle>
          </SheetHeader>
          <SidebarNav
            items={items}
            onNavigate={() => setMobileSidebarOpen(false)}
          />
        </SheetContent>
      </Sheet>
    </>
  )
}
