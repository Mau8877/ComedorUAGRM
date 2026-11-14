import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { ListOrderedIcon, MegaphoneIcon, UsersRoundIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { useConvocatoriasPriorizables } from '@/features/priorizacion'
import { AdminLayout } from '@/layouts'

import { mapearErrorAdjudicacion, usePanelAdjudicacion } from '../api'
import { avisosDelPanel, SeleccionarFinalistasDialog } from '../components'

export interface AdjudicacionScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

const FORMATO_PUNTAJE = new Intl.NumberFormat('es-BO', { minimumFractionDigits: 0, maximumFractionDigits: 2 })

/** Un valor del panel; `null` es «Sin definir»: nunca se inventa un número. */
function valor(numero: number | null, formato: (n: number) => string = String): string {
  return numero === null ? 'Sin definir' : formato(numero)
}

function Tarjeta({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">{titulo}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="font-heading text-2xl">{texto}</p>
      </CardContent>
    </Card>
  )
}

/**
 * «Auditoría y adjudicación» (CU05): el panel de cupos de la convocatoria elegida y la selección de
 * finalistas. La decisión de cada finalista se toma desde su expediente (CU07); esta pantalla enlaza con la
 * lista priorizada (CU06) y la de finalistas (CU21). Solo el Administrador DUBSS ve la cantidad de finalistas.
 */
export function AdjudicacionScreen({ titulo, activeHref, usuarioActual }: AdjudicacionScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [seleccionando, setSeleccionando] = useState(false)
  // Convocatorias que el backend dijo que ya no existen: se quitan del selector hasta que la lista se actualice.
  const [inexistentes, setInexistentes] = useState<Set<string>>(new Set())
  const [aviso, setAviso] = useState<string | null>(null)

  const convocatorias = useConvocatoriasPriorizables()
  const disponibles = (convocatorias.data ?? []).filter((c) => !inexistentes.has(c.codigo))
  const convocatoriaId =
    elegida && disponibles.some((c) => c.codigo === elegida) ? elegida : disponibles[0]?.codigo

  const panelConsulta = usePanelAdjudicacion(convocatoriaId)
  const panel = panelConsulta.data
  const errorConvocatorias = convocatorias.error ? mapearErrorAdjudicacion(convocatorias.error) : null
  const errorPanel = panelConsulta.error ? mapearErrorAdjudicacion(panelConsulta.error) : null

  const convocatoriaInexistente = () => {
    if (!convocatoriaId) return
    setInexistentes((previas) => new Set(previas).add(convocatoriaId))
    setElegida(undefined)
    setAviso('La convocatoria ya no existe. Elige otra.')
    void convocatorias.refetch()
  }

  // El `404` de la convocatoria al pedir el panel: se la quita del selector y se avisa (una sola vez por
  // convocatoria). Es el patrón de React para ajustar el estado al renderizar a partir de una consulta.
  if (errorPanel?.convocatoriaInexistente && convocatoriaId && !inexistentes.has(convocatoriaId)) {
    setInexistentes((previas) => new Set(previas).add(convocatoriaId))
    setElegida(undefined)
    setAviso('La convocatoria ya no existe. Elige otra.')
  }
  const panelInexistente = Boolean(errorPanel?.convocatoriaInexistente)
  useEffect(() => {
    // Solo vuelve a pedir la lista de convocatorias: ya no debería traer la que no existe.
    if (panelInexistente) void convocatorias.refetch()
    // `refetch` es estable en TanStack Query.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [panelInexistente, convocatoriaId])

  const avisos = panel ? avisosDelPanel(panel) : []
  const sinPostulaciones = panel !== undefined && panel.totalFinalistas === 0 && panel.calificadasSinSeleccionar === 0
  const bloqueoSeleccion = panel
    ? panel.calificadasSinSeleccionar === 0
      ? 'No hay postulaciones calificadas sin seleccionar.'
      : avisos[0] ?? null
    : 'Cargando el panel…'

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {errorConvocatorias && (
        <div
          role="alert"
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{errorConvocatorias.mensaje}</span>
          <Button variant="outline" size="sm" onClick={() => void convocatorias.refetch()}>
            Reintentar
          </Button>
        </div>
      )}

      {aviso && (
        <div role="alert" className="mb-4 rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning">
          {aviso}
        </div>
      )}

      {convocatorias.isLoading && (
        <p className="py-8 text-center text-sm text-muted-foreground">Cargando convocatorias…</p>
      )}
      {convocatorias.isSuccess && disponibles.length === 0 && (
        <p className="py-8 text-center text-sm text-muted-foreground">Todavía no hay convocatorias registradas.</p>
      )}

      {convocatoriaId && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
              <Select
                value={convocatoriaId}
                onValueChange={(valor) => {
                  if (valor) {
                    setAviso(null)
                    setElegida(valor)
                  }
                }}
              >
                <SelectTrigger size="sm" aria-label="Convocatoria" className="min-w-72">
                  <SelectValue>
                    {(valor: string | null) => {
                      const actual = disponibles.find((c) => c.codigo === valor)
                      return actual ? `${actual.nombre} (${actual.gestion})` : ''
                    }}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {disponibles.map((c) => (
                    <SelectItem key={c.codigo} value={c.codigo}>
                      {c.nombre} ({c.gestion})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Button variant="outline" size="sm" nativeButton={false} render={<Link to="/lista-priorizada" />}>
                <ListOrderedIcon data-icon="inline-start" />
                Ver lista priorizada
              </Button>
              <Button variant="outline" size="sm" nativeButton={false} render={<Link to="/finalistas" />}>
                <MegaphoneIcon data-icon="inline-start" />
                Ver finalistas
              </Button>
              <Button size="sm" disabled={bloqueoSeleccion !== null} onClick={() => setSeleccionando(true)}>
                <UsersRoundIcon data-icon="inline-start" />
                Seleccionar finalistas
              </Button>
            </div>
          </div>

          {bloqueoSeleccion && panel && (
            <p role="note" className="text-right text-xs text-muted-foreground">
              {bloqueoSeleccion}
            </p>
          )}

          {panelConsulta.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Cargando panel…</p>}

          {errorPanel && !errorPanel.convocatoriaInexistente && (
            <div
              role="alert"
              className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
            >
              <span>{errorPanel.mensaje}</span>
              <Button variant="outline" size="sm" onClick={() => void panelConsulta.refetch()}>
                Reintentar
              </Button>
            </div>
          )}

          {panel && (
            <>
              {avisos.map((texto) => (
                <p
                  key={texto}
                  role="status"
                  className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm text-warning"
                >
                  {texto}
                </p>
              ))}
              {sinPostulaciones && (
                <p className="rounded-md border border-border bg-muted p-3 text-sm text-muted-foreground">
                  No hay postulaciones para auditar
                </p>
              )}

              <section aria-label="Panel de adjudicación" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <Tarjeta titulo="Cupos" texto={valor(panel.cupos)} />
                <Tarjeta titulo="Adjudicados" texto={String(panel.adjudicados)} />
                <Tarjeta titulo="No adjudicados" texto={String(panel.noAdjudicados)} />
                <Tarjeta titulo="Cupos disponibles" texto={valor(panel.cuposDisponibles)} />
                <Tarjeta titulo="Puntaje mínimo" texto={valor(panel.puntajeMinimo, FORMATO_PUNTAJE.format)} />
                <Tarjeta titulo="Cantidad de finalistas" texto={valor(panel.cantidadFinalistas)} />
                <Tarjeta titulo="Finalistas" texto={String(panel.totalFinalistas)} />
                <Tarjeta titulo="Sin seleccionar" texto={String(panel.calificadasSinSeleccionar)} />
              </section>
            </>
          )}
        </div>
      )}

      {/* Se monta solo mientras está abierta: al cerrar se descarta lo escrito. */}
      {seleccionando && panel && (
        <SeleccionarFinalistasDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setSeleccionando(false)
          }}
          panel={panel}
          onDesactualizado={() => void panelConsulta.refetch()}
          onConvocatoriaInexistente={convocatoriaInexistente}
        />
      )}
    </AdminLayout>
  )
}
