import type { ReactNode } from 'react'

import { AppHeader } from './AppHeader'
import { AppSidebar } from './AppSidebar'
import { AppFooter } from './AppFooter'
import type { LayoutUser, NavItem } from './types'

interface AppShellProps {
  appName: string
  title: string
  navItems: NavItem[]
  user: LayoutUser
  children: ReactNode
}

// * Esqueleto compartido por todos los layouts de rol (Admin, Alumno, y los
// * que se agreguen a futuro) -- no conoce roles, solo recibe navItems/user
// * por props. Cada layout de rol (src/layouts/) es el único que decide QUÉ
// * le pasa acá, nunca se bifurca por rol dentro de este componente.
export function AppShell({
  appName,
  title,
  navItems,
  user,
  children,
}: AppShellProps) {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <AppSidebar items={navItems} appName={appName} />
      <div className="flex min-w-0 flex-1 flex-col">
        <AppHeader title={title} user={user} />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
        <AppFooter />
      </div>
    </div>
  )
}
