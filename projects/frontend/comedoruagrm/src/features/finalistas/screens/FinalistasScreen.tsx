import { useCallback, useState } from 'react'
import { BellRingIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { ComunicadoForm } from '@/features/comunicados'
import { useAccionesDeCuenta } from '@/features/perfil'
import { useConvocatoriasPriorizables } from '@/features/priorizacion'
import { AdminLayout } from '@/layouts'

import { mapearErrorFinalistas } from '../api'
import { NotificarFinalistasDialog, RegistrarEntregaPanel, TablaFinalistas } from '../components'
import type { Finalista } from '../types'

export interface FinalistasScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Administrador DUBSS). */
  usuarioActual: Usuario
}

/**
 * «Finalistas: notificación y documentación física» (CU21): en una sola pantalla, la tabla de finalistas de
 * la convocatoria elegida (con el estado de su aviso y de su entrega) y, al costado, el registro de la
 * entrega física del finalista elegido y el formulario de un nuevo comunicado. No cambia el estado de
 * ninguna postulación.
 */
export function FinalistasScreen({ titulo, activeHref, usuarioActual }: FinalistasScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [elegida, setElegida] = useState<string | undefined>(undefined)
  const [seleccionados, setSeleccionados] = useState<Set<string>>(new Set())
  const [activo, setActivo] = useState<Finalista | undefined>(undefined)
  const [notificando, setNotificando] = useState(false)
  // Convocatorias que el backend dijo que ya no existen (A9): se quitan del selector hasta que la lista se actualice.
  const [inexistentes, setInexistentes] = useState<Set<string>>(new Set())
  const [aviso, setAviso] = useState<string | null>(null)

  const convocatorias = useConvocatoriasPriorizables()

  // La más reciente (la primera) viene preseleccionada; si la elegida ya no está en la lista, también.
  const disponibles = (convocatorias.data ?? []).filter((c) => !inexistentes.has(c.codigo))
  const convocatoriaId =
    elegida && disponibles.some((c) => c.codigo === elegida) ? elegida : disponibles[0]?.codigo

  const error = convocatorias.error ? mapearErrorFinalistas(convocatorias.error) : null

  const convocatoriaInexistente = useCallback(
    (codigo: string) => {
      setInexistentes((previas) => new Set(previas).add(codigo))
      setElegida(undefined)
      setSeleccionados(new Set())
      setActivo(undefined)
      setAviso('La convocatoria ya no existe. Elige otra.')
      void convocatorias.refetch()
    },
    // `refetch` es estable en TanStack Query.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  )

  const elegirConvocatoria = (valor: string) => {
    setAviso(null)
    setElegida(valor)
    setSeleccionados(new Set())
    setActivo(undefined)
  }

  return (
    <AdminLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      {error && (
        <div
          role="alert"
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <span>{error.mensaje}</span>
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
        <div className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <section aria-label="Finalistas" className="rounded-2xl border border-border bg-card p-4">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Convocatoria</span>
                <Select
                  value={convocatoriaId}
                  onValueChange={(valor) => {
                    if (valor) elegirConvocatoria(valor)
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

              <Button disabled={seleccionados.size === 0} onClick={() => setNotificando(true)}>
                <BellRingIcon data-icon="inline-start" />
                Notificar seleccionados{seleccionados.size > 0 ? ` (${seleccionados.size})` : ''}
              </Button>
            </div>

            <div className="mt-4">
              {/* La clave reinicia la búsqueda y la página al cambiar de convocatoria. */}
              <TablaFinalistas
                key={convocatoriaId}
                convocatoriaId={convocatoriaId}
                seleccionados={seleccionados}
                onSeleccionChange={setSeleccionados}
                activoCodigo={activo?.codigo}
                onElegirEntrega={setActivo}
                onConvocatoriaInexistente={() => convocatoriaInexistente(convocatoriaId)}
              />
            </div>
          </section>

          <div className="flex flex-col gap-4">
            <RegistrarEntregaPanel finalista={activo} onRegistrada={setActivo} />
            <ComunicadoForm convocatoriaId={convocatoriaId} />
          </div>
        </div>
      )}

      {/* Se monta solo mientras está abierto: al cerrar se descarta lo escrito. */}
      {notificando && (
        <NotificarFinalistasDialog
          open
          onOpenChange={(abierto) => {
            if (!abierto) setNotificando(false)
          }}
          finalistas={[...seleccionados]}
          onNotificados={() => setSeleccionados(new Set())}
        />
      )}
    </AdminLayout>
  )
}
