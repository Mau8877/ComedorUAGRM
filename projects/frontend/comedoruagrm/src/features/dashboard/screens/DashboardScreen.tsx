import { useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { toastWarning } from '@/components/ui/toast'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { DirectivoLayout } from '@/layouts'

import { mapearErrorDashboard, useBi, useFiltrosDashboard, useIndicadores } from '../api'
import {
  AsistenciaPorDiaSemana,
  CoberturaCupos,
  ComparacionConvocatorias,
  DescargarDashboardButton,
  DescargarExcelButton,
  EmbudoProceso,
  HistogramaPuntajes,
  RankingFacultades,
  TendenciaGestiones,
  FiltrosDashboard,
  formatearFechaHora,
  formatearNumero,
  formatearPorcentaje,
  GraficoAsistenciaPorMes,
  GraficoPostulacionesPorMes,
  TarjetaIndicador,
} from '../components'
import type { SeleccionDashboard } from '../types'

export interface DashboardScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Directivo). */
  usuarioActual: Usuario
}

/**
 * «Dashboard de indicadores» (CU12): el Directivo ve postulaciones recibidas, finalistas, becas adjudicadas y
 * asistencia, con gráficos por mes y un análisis BI (embudo, facultades, convocatorias, puntajes, cobertura,
 * asistencia y tendencia); filtra por gestión y convocatoria y descarga el PDF o el Excel.
 *
 * * La consulta usa la selección APLICADA (`aplicada`); los selectores editan otra copia y solo cambian la
 * * consulta al pulsar «Aplicar». Sin selección aplicada el backend muestra la gestión vigente.
 */
export function DashboardScreen({ titulo, activeHref, usuarioActual }: DashboardScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  const [gestionEditada, setGestionEditada] = useState<string | undefined>(undefined)
  const [convocatoriaEditada, setConvocatoriaEditada] = useState<string | undefined>(undefined)
  const [aplicada, setAplicada] = useState<SeleccionDashboard>({})

  const filtros = useFiltrosDashboard()
  const indicadores = useIndicadores(aplicada)
  const bi = useBi(aplicada)

  const gestiones = filtros.data?.gestiones ?? []
  const gestionVigente = gestiones.find((g) => g.vigente)?.codigo
  const gestionDelSelector = gestionEditada ?? gestionVigente

  const volverALaVigente = () => {
    setGestionEditada(undefined)
    setConvocatoriaEditada(undefined)
    setAplicada({})
    void filtros.refetch()
  }

  const error = indicadores.error ? mapearErrorDashboard(indicadores.error) : null
  const seleccionInexistente = Boolean(error?.seleccionInexistente)
  // Con ERR_DAS_01 la selección ya no sirve: se vuelve a la vigente (ajuste de estado durante el render, una vez
  // por selección fallida) y un efecto avisa.
  const [fallida, setFallida] = useState<SeleccionDashboard | null>(null)
  if (seleccionInexistente && fallida !== aplicada) {
    setFallida(aplicada)
    setGestionEditada(undefined)
    setConvocatoriaEditada(undefined)
    setAplicada({})
  }
  useEffect(() => {
    if (fallida) {
      toastWarning(
        'Selección no disponible',
        'La gestión o la convocatoria elegida ya no existe. Se muestra la vigente.'
      )
    }
  }, [fallida])

  // Elegir una convocatoria en la comparación filtra todo el dashboard por ella.
  const elegirConvocatoria = (codigo: string) => {
    const gestion = aplicada.gestion ?? gestionVigente
    setGestionEditada(gestion)
    setConvocatoriaEditada(codigo)
    setAplicada({ gestion, convocatoria: codigo })
  }

  const datos = indicadores.data
  const analisis = bi.data
  const cargando = (filtros.isPending || indicadores.isPending) && !datos
  const errorGeneral = indicadores.isError && !datos && !seleccionInexistente ? error : null

  const leyenda = !datos
    ? ''
    : datos.actualizadoEn === null
      ? 'Aún no se calculan indicadores'
      : `Gestión ${datos.gestion?.etiqueta ?? '—'} · actualizado el ${formatearFechaHora(datos.actualizadoEn)}`

  return (
    <DirectivoLayout
      title={titulo}
      activeHref={activeHref}
      user={{
        nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`,
        rol: etiquetaRol(usuarioActual.rol),
      }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <div className="flex flex-col gap-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="font-heading text-xl">Dashboard de indicadores</h2>
            {leyenda && (
              <p role="status" className="text-sm text-muted-foreground">
                {leyenda}
              </p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <DescargarExcelButton
              seleccion={aplicada}
              deshabilitado={!analisis || analisis.sinDatos || bi.isPlaceholderData}
              onSeleccionInexistente={volverALaVigente}
            />
            <DescargarDashboardButton
              seleccion={aplicada}
              deshabilitado={!datos || datos.sinDatos || indicadores.isPlaceholderData}
              onSeleccionInexistente={volverALaVigente}
            />
          </div>
        </div>

        <FiltrosDashboard
          gestiones={gestiones}
          gestion={gestionDelSelector}
          convocatoria={convocatoriaEditada}
          onGestionChange={(codigo) => {
            setGestionEditada(codigo)
            setConvocatoriaEditada(undefined)
          }}
          onConvocatoriaChange={setConvocatoriaEditada}
          onAplicar={() =>
            setAplicada({ gestion: gestionDelSelector, convocatoria: convocatoriaEditada })
          }
          deshabilitado={filtros.isPending || gestiones.length === 0}
        />

        {errorGeneral && (
          <div
            role="alert"
            className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
          >
            <span>{errorGeneral.mensaje}</span>
            <Button size="sm" variant="outline" onClick={() => void indicadores.refetch()}>
              Reintentar
            </Button>
          </div>
        )}

        {cargando && (
          <div aria-label="Cargando indicadores" className="flex flex-col gap-4">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {[0, 1, 2, 3].map((n) => (
                <Skeleton key={n} className="h-28" />
              ))}
            </div>
            <div className="grid gap-4 lg:grid-cols-2">
              <Skeleton className="h-72" />
              <Skeleton className="h-72" />
            </div>
          </div>
        )}

        {datos && (
          <>
            {datos.sinDatos && (
              <p
                role="status"
                className="rounded-md border bg-muted/40 p-3 text-sm text-muted-foreground"
              >
                {datos.gestion
                  ? 'No hay datos para la selección elegida todavía.'
                  : 'Todavía no hay gestiones académicas registradas.'}
              </p>
            )}

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <TarjetaIndicador
                titulo="Postulaciones recibidas"
                valor={formatearNumero(datos.totales.postulacionesRecibidas)}
              />
              <TarjetaIndicador
                titulo="Finalistas"
                valor={formatearNumero(datos.totales.finalistas)}
              />
              <TarjetaIndicador
                titulo="Becas adjudicadas"
                valor={formatearNumero(datos.totales.becasAdjudicadas)}
              />
              <TarjetaIndicador
                titulo="Asistencia promedio"
                valor={
                  datos.totales.asistenciaPromedio === null
                    ? 'Sin datos'
                    : formatearPorcentaje(datos.totales.asistenciaPromedio)
                }
              />
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <GraficoPostulacionesPorMes datos={datos.porMes} />
              <GraficoAsistenciaPorMes datos={datos.porMes} />
            </div>

            {bi.isError && !analisis && (
              <div
                role="alert"
                className="flex items-center justify-between gap-2 rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
              >
                <span>No se pudo cargar el análisis BI.</span>
                <Button size="sm" variant="outline" onClick={() => void bi.refetch()}>
                  Reintentar
                </Button>
              </div>
            )}

            {analisis && !analisis.sinDatos && (
              <>
                <h3 className="font-heading text-lg">Análisis BI</h3>
                <div className="grid gap-4 lg:grid-cols-2">
                  <EmbudoProceso embudo={analisis.embudo} />
                  <RankingFacultades facultades={analisis.facultades} />
                </div>
                <ComparacionConvocatorias
                  convocatorias={analisis.convocatorias}
                  onElegir={elegirConvocatoria}
                />
                <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
                  <HistogramaPuntajes
                    tramos={analisis.puntaje}
                    promedio={analisis.puntajePromedio}
                  />
                  <CoberturaCupos cobertura={analisis.cobertura} />
                  <AsistenciaPorDiaSemana asistencia={analisis.asistencia} />
                </div>
                <TendenciaGestiones tendencia={analisis.tendencia} />
              </>
            )}
          </>
        )}
      </div>
    </DirectivoLayout>
  )
}
