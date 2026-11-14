import type { ReactNode } from 'react'

import { Skeleton } from '@/components/ui/skeleton'
import { AlertaError, etiquetaRol, mapearError, useCerrarSesion, type Usuario } from '@/features/auth'
import { AdminLayout, EstudianteLayout } from '@/layouts'

import { usePerfil } from '../api/usePerfil'
import { CambiarContrasenaForm } from '../components/CambiarContrasenaForm'
import { DatosContactoForm } from '../components/DatosContactoForm'
import { MisDatos } from '../components/MisDatos'
import { useAccionesDeCuenta } from '../useAccionesDeCuenta'

/**
 * «Mi perfil» (CU15): dentro del layout del rol (Administrador DUBSS o Postulante; los demás roles todavía
 * no tienen layout web y la ruta los lleva a /panel). Tres secciones: Mis datos (solo lectura), datos de
 * contacto y cambiar contraseña.
 */
export function PerfilScreen({ usuario }: { usuario: Usuario }) {
  const perfil = usePerfil()
  const cerrarSesion = useCerrarSesion()
  const acciones = useAccionesDeCuenta()

  const layoutProps = {
    title: 'Mi perfil',
    user: { nombre: `${usuario.nombres} ${usuario.apellidos}`, rol: etiquetaRol(usuario.rol) },
    onCerrarSesion: () => cerrarSesion.mutate(),
    ...acciones,
  }

  let contenido: ReactNode
  if (perfil.isPending) {
    contenido = (
      <div className="flex flex-col gap-3" aria-busy="true">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    )
  } else if (perfil.isError) {
    const error = mapearError(perfil.error)
    contenido = (
      <AlertaError>
        <span>{error.mensaje}</span>
        {error.red && (
          <button type="button" className="w-fit underline" onClick={() => void perfil.refetch()}>
            Reintentar
          </button>
        )}
      </AlertaError>
    )
  } else {
    contenido = (
      <div className="flex max-w-3xl flex-col gap-8">
        <MisDatos perfil={perfil.data} />
        <DatosContactoForm perfil={perfil.data} />
        <CambiarContrasenaForm />
      </div>
    )
  }

  if (usuario.rol === 'ADMINISTRADOR_DUBSS') {
    return <AdminLayout {...layoutProps}>{contenido}</AdminLayout>
  }
  return (
    <EstudianteLayout {...layoutProps} activeHref="/perfil">
      {contenido}
    </EstudianteLayout>
  )
}
