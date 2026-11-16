import { useCallback, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import type { LayoutUser } from '@/components/layout'

import { AdminLayout } from '../admin/AdminLayout'
import { DirectivoLayout } from '../directivo/DirectivoLayout'
import { EstudianteLayout } from '../estudiante/EstudianteLayout'
import { ShellContext } from './shellContext'
import type { PaginaShell } from './shellContext'

const SIN_PAGINA: PaginaShell = { title: '' }

interface ShellDeRolProps {
  rol: string
  user: LayoutUser
  onCerrarSesion: () => void
  onPerfil: () => void
  onCambiarContrasena: () => void
  children: ReactNode
}

/** Marco persistente (header + sidebar) del rol; los roles sin layout web (PERSONAL_COMEDOR) no lo tienen. */
export function ShellDeRol({ rol, user, onCerrarSesion, onPerfil, onCambiarContrasena, children }: ShellDeRolProps) {
  const [pagina, setPagina] = useState<PaginaShell>(SIN_PAGINA)

  const registrar = useCallback((nueva: PaginaShell) => {
    setPagina(nueva)
    return () => setPagina((actual) => (actual === nueva ? SIN_PAGINA : actual))
  }, [])
  const valor = useMemo(() => ({ registrar }), [registrar])

  const Layout =
    rol === 'ADMINISTRADOR_DUBSS'
      ? AdminLayout
      : rol === 'POSTULANTE'
        ? EstudianteLayout
        : rol === 'DIRECTIVO'
          ? DirectivoLayout
          : null
  if (!Layout) return <>{children}</>

  return (
    <Layout
      title={pagina.title}
      activeHref={pagina.activeHref}
      icono={pagina.icono}
      migas={pagina.migas}
      user={user}
      onCerrarSesion={onCerrarSesion}
      onPerfil={onPerfil}
      onCambiarContrasena={onCambiarContrasena}
    >
      <ShellContext.Provider value={valor}>{children}</ShellContext.Provider>
    </Layout>
  )
}
