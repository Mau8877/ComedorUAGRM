import { useEffect, useRef, useState } from 'react'
import { EyeIcon, FileTextIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

import { mapearErrorActas, useActasPendientes, useVistaPreviaActa } from '../api'
import type { ErrorActas } from '../api'
import type { ConvocatoriaPendienteActa } from '../types'
import { GenerarActaDialog } from './GenerarActaDialog'

// El selector muestra todas las pendientes de una vez (una por convocatoria con resultados publicados).
const PENDIENTES = { page: 1, pageSize: 50 }

/** El PDF borrador que se está mostrando: su dirección de objeto local y a qué convocatoria pertenece. */
interface VistaPrevia {
  url: string
  convocatoria: string
}

/**
 * Panel «Generar acta» (CU08): el Administrador elige una convocatoria con resultados publicados y sin acta y ve
 * la vista previa del PDF antes de generarlo. No hay campo de número: lo asigna el backend al generar.
 *
 * * El PDF borrador y la convocatoria elegida son estado local de esta pantalla: el `Blob` se muestra en un
 * * `<iframe>` con una dirección de objeto que se libera (`URL.revokeObjectURL`) al pedir otra vista previa, al
 * * cambiar de convocatoria y al desmontar. Nunca pasa por la caché ni por un store.
 */
export function GenerarActaPanel() {
  const pendientes = useActasPendientes(PENDIENTES)
  const vistaPrevia = useVistaPreviaActa()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [vista, setVista] = useState<VistaPrevia | null>(null)
  const [error, setError] = useState<ErrorActas | null>(null)
  // La convocatoria que se está confirmando, tal como estaba al pulsar «Generar PDF»: si la lista de pendientes
  // se refresca mientras la ventana está abierta, la ventana sigue hablando de la misma convocatoria.
  const [confirmando, setConfirmando] = useState<ConvocatoriaPendienteActa | null>(null)
  // La dirección viva, para poder liberarla desde cualquier camino (cambio, nueva vista previa o desmontar).
  const urlViva = useRef<string | null>(null)

  const disponibles = pendientes.data?.data ?? []
  const convocatoria = disponibles.find((c) => c.codigo === elegida) ?? disponibles[0]
  const convocatoriaId = convocatoria?.codigo

  const liberar = () => {
    if (urlViva.current) {
      URL.revokeObjectURL(urlViva.current)
      urlViva.current = null
    }
  }

  // Si la convocatoria de la vista previa ya no es la elegida (cambio, o dejó de estar pendiente), se descarta:
  // el estado se ajusta al renderizar y la dirección de objeto se libera en el efecto de abajo.
  const vistaVigente = vista && vista.convocatoria === convocatoriaId ? vista : null
  if (vista && !vistaVigente) {
    setVista(null)
  }
  useEffect(() => {
    if (!vistaVigente) liberar()
  }, [vistaVigente])

  // Al desmontar se libera la dirección que siga viva.
  useEffect(() => liberar, [])

  const pedirVistaPrevia = () => {
    if (!convocatoriaId) return
    setError(null)
    vistaPrevia.mutate(convocatoriaId, {
      onSuccess: (pdf) => {
        liberar()
        const url = URL.createObjectURL(pdf)
        urlViva.current = url
        setVista({ url, convocatoria: convocatoriaId })
      },
      onError: (e) => {
        const mapeado = mapearErrorActas(e)
        liberar()
        setVista(null)
        if (mapeado.refrescar) void pendientes.refetch()
        if (mapeado.convocatoriaInexistente) {
          setElegida(undefined)
          void pendientes.refetch()
        }
        setError(mapeado)
      },
    })
  }

  const errorPendientes = pendientes.error ? mapearErrorActas(pendientes.error) : null
  const sinPendientes = pendientes.isSuccess && disponibles.length === 0
  const ocupado = vistaPrevia.isPending

  return (
    <section aria-label="Generar acta" className="rounded-2xl border border-border bg-card p-4">
      <h2 className="mb-3 font-heading text-lg">Generar acta</h2>

      {errorPendientes && (
        <div
          role="alert"
          className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{errorPendientes.mensaje}</span>
          <Button variant="outline" size="sm" onClick={() => void pendientes.refetch()}>
            Reintentar
          </Button>
        </div>
      )}

      {pendientes.isLoading && <p className="text-sm text-muted-foreground">Cargando convocatorias…</p>}

      {sinPendientes && (
        <p role="status" className="mb-3 text-sm text-muted-foreground">
          No hay convocatorias con resultados publicados pendientes de acta
        </p>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
          <Select
            value={convocatoriaId ?? ''}
            disabled={disponibles.length === 0}
            onValueChange={(valor) => {
              if (!valor) return
              setError(null)
              setElegida(valor)
            }}
          >
            <SelectTrigger size="sm" aria-label="Convocatoria" className="min-w-72">
              <SelectValue>
                {(valor: string | null) => {
                  const actual = disponibles.find((c) => c.codigo === valor)
                  return actual ? `${actual.nombre} (${actual.adjudicados} adjudicados)` : ''
                }}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {disponibles.map((c) => (
                <SelectItem key={c.codigo} value={c.codigo}>
                  {c.nombre} ({c.adjudicados} adjudicados)
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Button variant="outline" size="sm" disabled={!convocatoriaId || ocupado} onClick={pedirVistaPrevia}>
          <EyeIcon data-icon="inline-start" />
          {ocupado ? 'Generando vista previa…' : 'Vista previa'}
        </Button>
        <Button size="sm" disabled={!convocatoria || ocupado} onClick={() => setConfirmando(convocatoria ?? null)}>
          <FileTextIcon data-icon="inline-start" />
          Generar PDF
        </Button>
      </div>

      <p role="note" className="mt-2 text-xs text-muted-foreground">
        El número de acta lo asigna el sistema al generarla.
      </p>

      {error && (
        <div
          role="alert"
          className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{error.mensaje}</span>
          {error.red && (
            <Button variant="outline" size="sm" onClick={pedirVistaPrevia} disabled={ocupado}>
              Reintentar
            </Button>
          )}
        </div>
      )}

      <div className="mt-4">
        {vistaVigente ? (
          <iframe
            title="Vista previa del acta"
            src={vistaVigente.url}
            className="h-[70vh] w-full rounded-md border border-border"
          />
        ) : (
          <p className="rounded-md border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
            Elige una convocatoria y pulsa «Vista previa» para ver cómo quedará el acta.
          </p>
        )}
      </div>

      {/* Se monta solo mientras está abierta: al cerrar se descarta. */}
      {confirmando && (
        <GenerarActaDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setConfirmando(null)
          }}
          convocatoria={confirmando}
          onDesactualizado={() => void pendientes.refetch()}
          onConvocatoriaInexistente={() => {
            setElegida(undefined)
            void pendientes.refetch()
            setError({ mensaje: 'La convocatoria no existe o fue eliminada. Elige otra.', convocatoriaInexistente: true })
          }}
        />
      )}
    </section>
  )
}
