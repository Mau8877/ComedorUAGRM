import type { ReactNode } from 'react'
import { Link } from '@tanstack/react-router'
import { ArrowLeftIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { AdminLayout } from '@/layouts'

import { mapearErrorPreseleccion, usePrecalificacion } from '../api'
import { AporteVariables } from '../components/AporteVariables'
import { EstadoPrecalificacionBadge } from '../components/EstadoPrecalificacionBadge'
import { formatearFechaHora } from '../components/formatos'
import { PuntajeGauge } from '../components/PuntajeGauge'
import { TrazaProceso } from '../components/TrazaProceso'

export interface PrecalificacionScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
  /** El código público de la postulación. */
  codigo: string
}

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 sm:flex-row sm:justify-between sm:gap-4">
      <dt className="text-sm text-muted-foreground">{etiqueta}</dt>
      <dd className="text-sm font-medium text-foreground sm:text-right">{children}</dd>
    </div>
  )
}

/**
 * Precalificación con IA de una postulación (CU03, mockup img 136): el puntaje, cuánto aportó cada variable y la traza
 * del proceso. Solo lectura: no cambia nada. Se llega desde el detalle de la lista priorizada (CU06). Una postulación
 * que todavía no se calificó muestra su estado y, si la IA no pudo calcularla, la marca de revisión.
 *
 * * Diferencias con el mockup: el puntaje es «de 70» (los otros 30 los pone el auditor, CU07), son diez variables y
 * * no se muestra la duración del cálculo, que no se guarda.
 */
export function PrecalificacionScreen({ titulo, activeHref, usuarioActual, codigo }: PrecalificacionScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()
  const consulta = usePrecalificacion(codigo)
  const datos = consulta.data
  const error = consulta.error ? mapearErrorPreseleccion(consulta.error) : null

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      migas={[{ label: 'Lista priorizada', href: '/lista-priorizada' }, { label: titulo }]}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-4">
        <div>
          <Button variant="ghost" size="sm" nativeButton={false} render={<Link to="/lista-priorizada" />}>
            <ArrowLeftIcon /> Volver a la lista priorizada
          </Button>
        </div>

        {consulta.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando precalificación…</p>}

        {error && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{error.mensaje}</span>
            {error.red && (
              <Button variant="outline" size="sm" onClick={() => void consulta.refetch()}>
                Reintentar
              </Button>
            )}
          </div>
        )}

        {datos && (
          <>
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <h2 className="text-xl font-semibold text-foreground">
                  {datos.nombres} {datos.apellidos}
                </h2>
                <p className="text-sm text-muted-foreground">
                  CI {datos.ci} · {datos.convocatoria}
                </p>
              </div>
              <EstadoPrecalificacionBadge estado={datos.estado} enRevision={datos.marcadaParaRevision} />
            </div>

            {datos.marcadaParaRevision && (
              <div role="status" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-foreground">
                La IA no pudo calcular el puntaje de esta postulación después de {datos.intentos} intentos, así que
                ya no se reintenta sola. Si el postulante corrige y reenvía, vuelve a intentarse.
              </div>
            )}

            <div className="grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
              <section className="rounded-2xl border border-border bg-card p-4" aria-labelledby="titulo-puntaje">
                <h3 id="titulo-puntaje" className="mb-3 text-base font-semibold text-foreground">
                  Puntaje de vulnerabilidad
                </h3>
                {datos.puntaje !== null && datos.puntajeMaximo !== null ? (
                  <>
                    <PuntajeGauge puntaje={datos.puntaje} maximo={datos.puntajeMaximo} />
                    <dl className="mt-4 flex flex-col gap-2">
                      <Dato etiqueta="Calculado el">{formatearFechaHora(datos.fechaCalculo)}</Dato>
                      <Dato etiqueta="Modelo">{datos.modelo ?? '—'}</Dato>
                      <Dato etiqueta="Posición en la lista">{datos.posicion ?? '—'}</Dato>
                    </dl>
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground">Todavía no tiene puntaje calculado.</p>
                )}
              </section>

              <section className="rounded-2xl border border-border bg-card p-4" aria-labelledby="titulo-aporte">
                <h3 id="titulo-aporte" className="mb-3 text-base font-semibold text-foreground">
                  Aporte de cada variable al puntaje
                </h3>
                <AporteVariables variables={datos.variables} />
                {datos.advertencias.length > 0 && (
                  <ul className="mt-3 list-disc pl-5 text-xs text-muted-foreground">
                    {datos.advertencias.map((a) => (
                      <li key={a}>{a}</li>
                    ))}
                  </ul>
                )}
                <p className="mt-3 text-xs text-muted-foreground">
                  Las ponderaciones se ajustan en «Parámetros del modelo» (CU26).
                </p>
              </section>
            </div>

            <section className="rounded-2xl border border-border bg-card p-4" aria-labelledby="titulo-traza">
              <h3 id="titulo-traza" className="mb-3 text-base font-semibold text-foreground">
                Traza del proceso automático
              </h3>
              <TrazaProceso pasos={datos.traza} />
            </section>

            <p className="rounded-md border border-info/30 bg-info/10 p-3 text-sm text-foreground">
              El puntaje solo ordena los expedientes para auditoría; la adjudicación depende de la revisión de la DUBSS.
              Puntaje mínimo requerido: por definir con la DUBSS.
            </p>
          </>
        )}
      </div>
    </AdminLayout>
  )
}
