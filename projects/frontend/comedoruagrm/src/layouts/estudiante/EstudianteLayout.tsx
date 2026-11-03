import { AppShell } from '@/components/layout'

import { estudianteNavItems } from './estudianteNavItems'
import type { EstudianteLayoutProps } from './estudianteTypes'

export function EstudianteLayout({
  title,
  user,
  notifications,
  onCerrarSesion,
  activeHref,
  children,
}: EstudianteLayoutProps) {
  return (
    <AppShell
      appName="ComedorU"
      title={title}
      navItems={estudianteNavItems}
      activeHref={activeHref}
      user={user}
      notifications={notifications}
      onCerrarSesion={onCerrarSesion}
    >
      {children}
    </AppShell>
  )
}
