import { useNavigate } from '@tanstack/react-router'
import { ArrowLeftIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'

import { mapearErrorPostulaciones, usePostulacion } from '../api'
import {
  BloqueFinalista,
  BloqueMotivo,
  BloqueObservaciones,
  BloqueResultado,
  EstadoPostulacionBadge,
  formatearFecha,
  LineaDeTiempo,
} from '../components'

export interface DetallePostulacionScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante). */
  usuarioActual: Usuario
  /** El código público de la postulación, de la ruta. */
  codigo: string
}

/**
 * El detalle de una postulación (CU04): su estado visible (tal cual lo manda el backend), la línea de tiempo con los
 * pasos que ocurrieron y los que faltan, y solo los bloques que corresponden al estado: observaciones, motivo,
 * «Eres finalista» y resultado. Una postulación inexistente o ajena se muestra como «no encontrada».
 */
export function DetallePostulacionScreen({ titulo, activeHref, usuarioActual, codigo }: DetallePostulacionScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const navigate = useNavigate()
  const consulta = usePostulacion(codigo)

  const irALaLista = () => void navigate({ to: '/mis-postulaciones' })
  const error = consulta.error ? mapearErrorPostulaciones(consulta.error) : null
  const postulacion = consulta.data

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-4">
        <div>
          <Button variant="outline" size="sm" onClick={irALaLista}>
            <ArrowLeftIcon data-icon="inline-start" />
            Volver a mis postulaciones
          </Button>
        </div>

        {consulta.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando postulación…</p>}

        {error?.noEncontrada && (
          <div className="flex flex-col items-center gap-3 py-8 text-center">
            <p role="alert" className="text-sm font-medium">
              Postulación no encontrada
            </p>
            <p className="text-sm text-muted-foreground">{error.mensaje}</p>
            <Button variant="outline" size="sm" onClick={irALaLista}>
              Ir a mis postulaciones
            </Button>
          </div>
        )}

        {error && !error.noEncontrada && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{error.mensaje}</span>
            <Button variant="outline" size="sm" onClick={() => void consulta.refetch()}>
              Reintentar
            </Button>
          </div>
        )}

        {postulacion && (
          <>
            <section aria-label="Postulación" className="rounded-2xl border border-border bg-card p-4">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <h2 className="font-heading text-lg">{postulacion.convocatoria.nombre}</h2>
                  <p className="text-sm text-muted-foreground">
                    Gestión {postulacion.gestionAcademica.nombre} · Enviada el {formatearFecha(postulacion.fechaEnvio)}
                  </p>
                </div>
                <EstadoPostulacionBadge estado={postulacion.estado} />
              </div>
            </section>

            <section aria-label="Recorrido" className="rounded-2xl border border-border bg-card p-4">
              <h3 className="mb-3 font-heading text-base">Recorrido de tu postulación</h3>
              <LineaDeTiempo pasos={postulacion.lineaDeTiempo} />
            </section>

            {postulacion.observaciones && (
              <BloqueObservaciones
                observaciones={postulacion.observaciones}
                puedeCorregir={postulacion.puedeCorregir}
              />
            )}
            {postulacion.motivo && <BloqueMotivo motivo={postulacion.motivo} estado={postulacion.estado} />}
            {postulacion.finalista && <BloqueFinalista finalista={postulacion.finalista} />}
            {postulacion.resultado && (
              <BloqueResultado
                resultado={postulacion.resultado}
                onVerResultados={() => void navigate({ to: '/resultados' })}
              />
            )}
          </>
        )}
      </div>
    </EstudianteLayout>
  )
}
