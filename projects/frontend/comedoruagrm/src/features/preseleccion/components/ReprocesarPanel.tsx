import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { RefreshCwIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { toastSuccess, toastWarning } from '@/components/ui/toast'
import { useConvocatorias } from '@/features/convocatorias'

import {
  estaEnCurso,
  mapearErrorPreseleccion,
  useLoteReprocesamiento,
  useRefrescarResumenes,
  useResumenReprocesamiento,
} from '../api'
import type { LoteReprocesamiento, ModeloPreseleccion } from '../types'
import { ReprocesarDialog } from './ReprocesarDialog'

// El selector muestra las convocatorias más recientes de una vez.
const CONVOCATORIAS = { page: 1, pageSize: 50, sort: '-fechaApertura' }

interface ReprocesarPanelProps {
  /** La versión vigente: con ella se recalcula. `null` si todavía no se guardó ninguna. */
  vigente: ModeloPreseleccion | null
  /** Hay valores sin guardar en el formulario: primero se guarda la versión. */
  hayCambiosSinGuardar: boolean
}

function AvanceLote({ lote }: { lote: LoteReprocesamiento }) {
  const enCurso = estaEnCurso(lote.estado)
  return (
    <div aria-label="Avance del reprocesamiento" className="mt-3 flex flex-col gap-2 text-sm">
      <progress
        className="w-full"
        max={Math.max(lote.total, 1)}
        value={lote.procesadas}
        aria-label="Postulaciones procesadas"
      />
      <p>
        {lote.procesadas} de {lote.total} procesadas
        {lote.fallidas > 0 && <span className="text-destructive"> · {lote.fallidas} con error</span>}
      </p>
      {enCurso && (
        <p role="status" className="text-muted-foreground">
          {lote.estado === 'PENDIENTE' ? 'Esperando para empezar…' : 'Reprocesando con la versión ' + lote.modelo + '…'}
        </p>
      )}
      {lote.estado === 'COMPLETADO' && (
        <p role="status" className="text-success">
          Reprocesamiento terminado con la versión {lote.modelo}.
        </p>
      )}
      {lote.estado === 'COMPLETADO' && lote.fallidas > 0 && (
        <p role="alert" className="text-warning">
          {lote.fallidas === 1 ? '1 postulación no se pudo calcular' : `${lote.fallidas} postulaciones no se pudieron calcular`}
          . Vuelve a reprocesar para reintentarlas: solo se calculan las que faltan.
        </p>
      )}
      {lote.estado === 'FALLIDO' && (
        <p role="alert" className="text-destructive">
          El reprocesamiento se interrumpió (el módulo de IA no respondió o el servidor se reinició). Los puntajes ya
          recalculados se conservan: vuelve a reprocesar para continuar con las que faltan.
        </p>
      )}
    </div>
  )
}

/**
 * «Reprocesar puntajes» (CU26): el Administrador elige una convocatoria, ve cuántas postulaciones calificadas se
 * recalcularán con la versión vigente y confirma. El lote corre en segundo plano; el panel muestra su avance y, al
 * terminar, las que fallaron y el acceso a la lista priorizada. Bloquea con aviso si los resultados ya se publicaron
 * (A3), si hay un lote en curso (A4), si hay cambios sin guardar o si no hay nada por recalcular.
 */
export function ReprocesarPanel({ vigente, hayCambiosSinGuardar }: ReprocesarPanelProps) {
  const convocatorias = useConvocatorias(CONVOCATORIAS)
  const refrescarResumenes = useRefrescarResumenes()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [iniciado, setIniciado] = useState<string | undefined>(undefined)
  const [confirmando, setConfirmando] = useState(false)

  const disponibles = convocatorias.data?.data ?? []
  const convocatoria = disponibles.find((c) => c.codigo === elegida) ?? disponibles[0]
  const resumen = useResumenReprocesamiento(convocatoria?.codigo)

  // El lote que se sigue: el que acaba de arrancar este panel o, al volver a la pantalla, el que ya estaba en curso.
  const loteCodigo = iniciado ?? resumen.data?.loteEnCurso?.codigo
  const lote = useLoteReprocesamiento(loteCodigo)
  const estadoLote = lote.data?.estado

  // Al terminar el lote cambian los pendientes de recalcular: se vuelve a pedir el resumen una sola vez.
  useEffect(() => {
    if (estadoLote === 'COMPLETADO') {
      toastSuccess('Reprocesamiento terminado', 'Los puntajes ya reflejan la versión vigente.')
      refrescarResumenes()
    } else if (estadoLote === 'FALLIDO') {
      toastWarning('Reprocesamiento interrumpido', 'Vuelve a reprocesar para continuar.')
      refrescarResumenes()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [estadoLote, loteCodigo])

  const datos = resumen.data
  const enCurso = estaEnCurso(estadoLote) || Boolean(datos?.loteEnCurso)
  const motivoBloqueo = !vigente
    ? 'Todavía no hay una versión vigente: guarda los parámetros primero.'
    : hayCambiosSinGuardar
      ? 'Hay cambios sin guardar: guarda la versión antes de reprocesar.'
      : datos?.resultadosPublicados
        ? 'Los resultados de esta convocatoria ya fueron publicados: no se pueden reprocesar los puntajes.'
        : enCurso
          ? 'Hay un reprocesamiento en curso para esta convocatoria.'
          : datos && datos.porReprocesar === 0
            ? 'No hay postulaciones calificadas por reprocesar con la versión vigente.'
            : null
  const puedeReprocesar = Boolean(convocatoria && datos && vigente && !motivoBloqueo)

  const errorCarga =
    (convocatorias.isError && !convocatorias.data ? mapearErrorPreseleccion(convocatorias.error) : null) ??
    (resumen.isError && !resumen.data ? mapearErrorPreseleccion(resumen.error) : null)

  return (
    <section aria-label="Reprocesar puntajes" className="rounded-2xl border border-border bg-card p-4">
      <h2 className="font-heading text-lg">Reprocesar puntajes</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        {vigente
          ? `Recalcula con la versión ${vigente.version} las postulaciones calificadas de una convocatoria. El puntaje anterior se conserva en la traza.`
          : 'Guarda una versión de los parámetros para poder reprocesar.'}
      </p>

      {convocatorias.isLoading && <p className="mt-3 text-sm text-muted-foreground">Cargando convocatorias…</p>}
      {convocatorias.isSuccess && disponibles.length === 0 && (
        <p role="status" className="mt-3 text-sm text-muted-foreground">
          Todavía no hay convocatorias.
        </p>
      )}
      {errorCarga && (
        <div
          role="alert"
          className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{errorCarga.mensaje}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              void convocatorias.refetch()
              void resumen.refetch()
            }}
          >
            Reintentar
          </Button>
        </div>
      )}

      {convocatoria && (
        <>
          <div className="mt-3 flex flex-col gap-1">
            <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
            <Select
              value={convocatoria.codigo}
              onValueChange={(valor) => {
                if (!valor) return
                setElegida(valor)
                setIniciado(undefined)
              }}
            >
              <SelectTrigger size="sm" aria-label="Convocatoria a reprocesar" className="w-full">
                <SelectValue>
                  {(valor: string | null) => disponibles.find((c) => c.codigo === valor)?.nombre ?? ''}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {disponibles.map((c) => (
                  <SelectItem key={c.codigo} value={c.codigo}>
                    {c.nombre}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {datos && (
            <dl aria-label="Resumen del reprocesamiento" className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm">
              <div className="flex gap-1.5">
                <dt className="text-muted-foreground">Calificadas:</dt>
                <dd className="font-medium">{datos.calificadas}</dd>
              </div>
              <div className="flex gap-1.5">
                <dt className="text-muted-foreground">Por recalcular:</dt>
                <dd className="font-medium">{datos.porReprocesar}</dd>
              </div>
            </dl>
          )}

          <p role="note" className="mt-3 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
            Cambiará la posición en la lista priorizada de algunos postulantes.
          </p>

          {motivoBloqueo && datos && (
            <p role="status" className="mt-3 text-sm text-muted-foreground">
              {motivoBloqueo}
            </p>
          )}

          <Button className="mt-3" disabled={!puedeReprocesar} onClick={() => setConfirmando(true)}>
            <RefreshCwIcon data-icon="inline-start" />
            Reprocesar puntajes
          </Button>

          {lote.data && <AvanceLote lote={lote.data} />}
          {lote.data?.estado === 'COMPLETADO' && (
            <Button variant="outline" size="sm" className="mt-3" nativeButton={false} render={<Link to="/lista-priorizada" />}>
              Ver la lista priorizada
            </Button>
          )}
        </>
      )}

      {convocatoria && datos && vigente && (
        <ReprocesarDialog
          open={confirmando}
          onOpenChange={setConfirmando}
          convocatoria={{ codigo: convocatoria.codigo, nombre: convocatoria.nombre }}
          modelo={vigente.version}
          porReprocesar={datos.porReprocesar}
          onIniciado={(nuevo) => setIniciado(nuevo.codigo)}
        />
      )}
    </section>
  )
}
