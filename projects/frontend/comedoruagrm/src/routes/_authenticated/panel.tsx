import { createFileRoute } from '@tanstack/react-router'
import { Loader2Icon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout, EstudianteLayout } from '@/layouts'

// * Sigue siendo la ruta hija que evita el conflicto del layout pathless vacío
// * (ver _authenticated.tsx). Hasta que los CU de cada rol tengan sus
// * módulos, todos aterrizan acá: el Administrador DUBSS y el Postulante dentro
// * de su layout, y los otros dos roles con un aviso (todavía no tienen layout).
export const Route = createFileRoute('/_authenticated/panel')({
  component: PanelPage,
})

function PanelPage() {
  const { usuario } = Route.useRouteContext()
  const cerrarSesion = useCerrarSesion()
  const alCerrarSesion = () => cerrarSesion.mutate()
  const acciones = useAccionesDeCuenta()

  const user = { nombre: `${usuario.nombres} ${usuario.apellidos}`, rol: etiquetaRol(usuario.rol) }
  const saludo = <p className="text-muted-foreground">Hola, {usuario.nombres}. Tu módulo se irá habilitando pronto.</p>

  if (usuario.rol === 'ADMINISTRADOR_DUBSS') {
    return (
      <AdminLayout title="Panel" user={user} onCerrarSesion={alCerrarSesion} {...acciones}>
        {saludo}
      </AdminLayout>
    )
  }

  if (usuario.rol === 'POSTULANTE') {
    return (
      <EstudianteLayout title="Panel" user={user} onCerrarSesion={alCerrarSesion} {...acciones}>
        {saludo}
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
