import { createFileRoute } from '@tanstack/react-router'
import { LayoutDashboardIcon, Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { PanelAdministradorScreen, PanelComedorScreen, PanelPostulanteScreen } from '@/features/panel'
import { AdminLayout, ComedorLayout, EstudianteLayout } from '@/layouts'

// * Sigue siendo la ruta hija que evita el conflicto del layout pathless vacío (ver _authenticated.tsx). Es la
// * pantalla de inicio del Administrador DUBSS y del Personal del Comedor (resumen con datos reales, features/panel)
// * y del Postulante (resumen de su convocatoria, postulación y beca); el Directivo aterriza en /dashboard.
export const Route = createFileRoute('/_authenticated/panel')({
  component: PanelPage,
})

function PanelPage() {
  const { usuario } = Route.useRouteContext()
  const cerrarSesion = useCerrarSesion()
  const alCerrarSesion = () => cerrarSesion.mutate()
  const acciones = useAccionesDeCuenta()

  const user = { nombre: `${usuario.nombres} ${usuario.apellidos}`, rol: etiquetaRol(usuario.rol) }

  if (usuario.rol === 'ADMINISTRADOR_DUBSS') {
    return (
      <AdminLayout title="Panel" icono={LayoutDashboardIcon} user={user} onCerrarSesion={alCerrarSesion} {...acciones}>
        <PanelAdministradorScreen nombres={usuario.nombres} />
      </AdminLayout>
    )
  }

  if (usuario.rol === 'PERSONAL_COMEDOR') {
    return (
      <ComedorLayout title="Panel" icono={LayoutDashboardIcon} user={user} onCerrarSesion={alCerrarSesion} {...acciones}>
        <PanelComedorScreen nombres={usuario.nombres} />
      </ComedorLayout>
    )
  }

  if (usuario.rol === 'POSTULANTE') {
    return (
      <EstudianteLayout title="Panel" icono={LayoutDashboardIcon} user={user} onCerrarSesion={alCerrarSesion} {...acciones}>
        <PanelPostulanteScreen nombres={usuario.nombres} />
      </EstudianteLayout>
    )
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-4 text-center text-foreground">
      <h1 className="font-heading text-2xl font-semibold">Hola, {usuario.nombres}</h1>
      <p className="text-muted-foreground">Tu módulo ({user.rol}) aún no está disponible.</p>
      <Button onClick={alCerrarSesion} disabled={cerrarSesion.isPending}>
        {cerrarSesion.isPending && <Loader2Icon className="animate-spin" />}
        Cerrar sesión
      </Button>
    </div>
  )
}
