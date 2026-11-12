import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import { AdjudicadaBadge, ArchivadaBadge, DevueltaBadge, NoAdjudicadaBadge } from '@/features/finalistas'

import type { Expediente } from '../types'
import { formatearPuntos } from './formatos'

const FORMATO_FECHA = new Intl.DateTimeFormat('es-BO', { day: '2-digit', month: '2-digit', year: 'numeric' })

function fechaDia(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia))
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="text-sm">{valor}</dd>
    </div>
  )
}

/** Datos del finalista, su postulación y las respuestas socioeconómicas, en solo lectura (CU07, paso 3). */
export function ExpedienteResumen({ expediente }: { expediente: Expediente }) {
  const { postulante, convocatoria, finalista, respuestaSocioeconomica: respuestas } = expediente
  const entrega = finalista.entrega

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          {postulante.nombres} {postulante.apellidos}
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <Dato etiqueta="CI" valor={postulante.ci} />
          <Dato etiqueta="Carrera" valor={postulante.carrera ?? '—'} />
          <Dato etiqueta="Convocatoria" valor={convocatoria.nombre} />
          <Dato etiqueta="Posición" valor={String(finalista.posicion)} />
          <Dato etiqueta="Estado de la postulación" valor={expediente.estadoPostulacion} />
          {expediente.estadoPostulacion === 'devuelta' && (
            <div className="flex flex-col">
              <dt className="text-xs text-muted-foreground">Devolución</dt>
              <dd>
                <DevueltaBadge />
              </dd>
            </div>
          )}
          {expediente.estadoPostulacion === 'adjudicada' && (
            <div className="flex flex-col">
              <dt className="text-xs text-muted-foreground">Decisión</dt>
              <dd>
                <AdjudicadaBadge />
              </dd>
            </div>
          )}
          {(expediente.estadoPostulacion === 'archivada' || expediente.estadoPostulacion === 'no_adjudicada') && (
            <div className="flex flex-col">
              <dt className="text-xs text-muted-foreground">Cierre</dt>
              <dd>{expediente.estadoPostulacion === 'archivada' ? <ArchivadaBadge /> : <NoAdjudicadaBadge />}</dd>
            </div>
          )}
          <Dato
            etiqueta="Documentación física"
            valor={
              entrega.estado === 'RECIBIDA' && entrega.fecha ? `Recibida el ${fechaDia(entrega.fecha)}` : 'Pendiente'
            }
          />
          {finalista.plazoEntrega && <Dato etiqueta="Plazo de entrega" valor={fechaDia(finalista.plazoEntrega)} />}
        </dl>

        <section aria-label="Respuestas socioeconómicas" className="flex flex-col gap-2">
          <h3 className="text-sm font-medium">Respuestas socioeconómicas</h3>
          {respuestas ? (
            <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <Dato etiqueta="Ingresos familiares" valor={formatearPuntos(respuestas.ingresosFamiliares)} />
              <Dato etiqueta="Dependientes" valor={respuestas.dependientes === null ? '—' : String(respuestas.dependientes)} />
              <Dato etiqueta="Patrimonio" valor={respuestas.patrimonio ?? '—'} />
              <Dato etiqueta="Procedencia" valor={respuestas.procedencia ?? '—'} />
              <Dato etiqueta="Promedio académico" valor={formatearPuntos(respuestas.promedioAcademico)} />
              <Dato etiqueta="Vulnerabilidad" valor={respuestas.estadoVulnerabilidad ?? '—'} />
            </dl>
          ) : (
            <p className="text-sm text-muted-foreground">El postulante no registró respuestas socioeconómicas.</p>
          )}
        </section>
      </CardContent>
    </Card>
  )
}
