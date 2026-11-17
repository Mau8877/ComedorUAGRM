import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'

import { mapearErrorPreseleccion, useModeloVigente } from '../api'
import { FormularioParametros, HistorialVersiones, ReprocesarPanel } from '../components'

export interface ParametrosModeloScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Parámetros del modelo de preselección» (CU26): el Administrador ve las ponderaciones vigentes, escribe las nuevas y
 * guarda una versión (que pasa a ser la vigente), reprocesa los puntajes de una convocatoria y consulta el historial de
 * versiones. Solo el Administrador DUBSS.
 */
export function ParametrosModeloScreen({ titulo, activeHref, usuarioActual }: ParametrosModeloScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const vigente = useModeloVigente()
  const [hayCambios, setHayCambios] = useState(false)

  const error = vigente.isError && vigente.data === undefined ? mapearErrorPreseleccion(vigente.error) : null
  const modelo = vigente.data ?? null

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-4">
        <header className="flex flex-wrap items-start justify-between gap-2">
          <p className="text-sm text-muted-foreground">
            Ajuste los pesos del puntaje de vulnerabilidad y reprocese las postulaciones.
          </p>
          <span
            role="status"
            aria-label="Versión vigente"
            className="rounded-full bg-primary/10 px-3 py-1 text-sm font-medium text-primary"
          >
            {modelo ? `Versión vigente ${modelo.version}` : 'Sin versión vigente'}
          </span>
        </header>

        {vigente.isLoading && <p className="text-sm text-muted-foreground">Cargando parámetros…</p>}
        {error && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{error.mensaje}</span>
            <Button variant="outline" size="sm" onClick={() => void vigente.refetch()}>
              Reintentar
            </Button>
          </div>
        )}

        {vigente.isSuccess && (
          <div className="grid gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <FormularioParametros
              key={modelo?.codigo ?? 'sin-version'}
              vigente={modelo}
              onCambios={setHayCambios}
              onDesactualizado={() => void vigente.refetch()}
            />
            <ReprocesarPanel vigente={modelo} hayCambiosSinGuardar={hayCambios} />
          </div>
        )}

        <HistorialVersiones />
      </div>
    </AdminLayout>
  )
}
