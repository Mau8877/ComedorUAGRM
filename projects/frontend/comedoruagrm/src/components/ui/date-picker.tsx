"use client"

import * as React from "react"
import { CalendarIcon, ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

// * Selector de fecha propio del sistema de diseño: reemplaza al `<input type="date">` nativo,
// * cuyo calendario lo pinta el navegador y no se puede estilar. El valor viaja como texto
// * `YYYY-MM-DD` (el mismo que usa el backend), así que no hay conversiones de zona horaria.

const MESES = [
  "enero",
  "febrero",
  "marzo",
  "abril",
  "mayo",
  "junio",
  "julio",
  "agosto",
  "septiembre",
  "octubre",
  "noviembre",
  "diciembre",
]
const MESES_CORTOS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
const DIAS_SEMANA = ["L", "M", "X", "J", "V", "S", "D"]

type Vista = "dias" | "meses"

interface Fecha {
  anio: number
  mes: number // 0-11
  dia: number
}

function dosDigitos(n: number): string {
  return String(n).padStart(2, "0")
}

function aIso({ anio, mes, dia }: Fecha): string {
  return `${String(anio).padStart(4, "0")}-${dosDigitos(mes + 1)}-${dosDigitos(dia)}`
}

/** Lee `YYYY-MM-DD`; devuelve `null` si no es una fecha real (ej. `2026-02-31`). */
function deIso(valor: string | undefined): Fecha | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor ?? "")
  if (!m) return null
  const fecha = { anio: Number(m[1]), mes: Number(m[2]) - 1, dia: Number(m[3]) }
  const real = new Date(fecha.anio, fecha.mes, fecha.dia)
  return real.getFullYear() === fecha.anio && real.getMonth() === fecha.mes && real.getDate() === fecha.dia
    ? fecha
    : null
}

function hoy(): Fecha {
  const ahora = new Date()
  return { anio: ahora.getFullYear(), mes: ahora.getMonth(), dia: ahora.getDate() }
}

function diasDelMes(anio: number, mes: number): number {
  return new Date(anio, mes + 1, 0).getDate()
}

/** Cuántas celdas vacías van antes del día 1 (la semana empieza en lunes). */
function huecosIniciales(anio: number, mes: number): number {
  return (new Date(anio, mes, 1).getDay() + 6) % 7
}

function sumarDias(fecha: Fecha, dias: number): Fecha {
  const d = new Date(fecha.anio, fecha.mes, fecha.dia + dias)
  return { anio: d.getFullYear(), mes: d.getMonth(), dia: d.getDate() }
}

function sumarMeses(anio: number, mes: number, meses: number): { anio: number; mes: number } {
  const d = new Date(anio, mes + meses, 1)
  return { anio: d.getFullYear(), mes: d.getMonth() }
}

/** Texto que ve la persona en el botón: `17/11/2026`. */
function formatear(fecha: Fecha): string {
  return `${dosDigitos(fecha.dia)}/${dosDigitos(fecha.mes + 1)}/${fecha.anio}`
}

function etiquetaLarga(fecha: Fecha): string {
  return `${fecha.dia} de ${MESES[fecha.mes]} de ${fecha.anio}`
}

interface DatePickerProps {
  /** Fecha elegida como `YYYY-MM-DD`; vacío si no hay ninguna. */
  value?: string
  onChange: (valor: string) => void
  /** Se llama al cerrar el calendario (para marcar el campo como tocado en el formulario). */
  onBlur?: () => void
  /** Primer día elegible, `YYYY-MM-DD`. */
  min?: string
  /** Último día elegible, `YYYY-MM-DD`. */
  max?: string
  placeholder?: string
  disabled?: boolean
  /** Muestra «Borrar» en el pie para vaciar el campo. */
  clearable?: boolean
  id?: string
  className?: string
  "aria-label"?: string
  "aria-invalid"?: boolean
}

function DatePicker({
  value = "",
  onChange,
  onBlur,
  min,
  max,
  placeholder = "dd/mm/aaaa",
  disabled,
  clearable = false,
  id,
  className,
  "aria-label": ariaLabel,
  "aria-invalid": ariaInvalid,
}: DatePickerProps) {
  const [abierto, setAbierto] = React.useState(false)
  const seleccionada = deIso(value)
  const minimo = deIso(min)
  const maximo = deIso(max)
  const hoyFecha = hoy()
  const hoyIso = aIso(hoyFecha)

  const fueraDeRango = React.useCallback(
    (fecha: Fecha): boolean => {
      const iso = aIso(fecha)
      return (minimo !== null && iso < aIso(minimo)) || (maximo !== null && iso > aIso(maximo))
    },
    [minimo, maximo],
  )

  const [vista, setVista] = React.useState<Vista>("dias")
  const [mostrado, setMostrado] = React.useState({ anio: hoyFecha.anio, mes: hoyFecha.mes })
  // * Día que recibe el foco con el teclado (flechas); solo uno es tabulable a la vez.
  const [enfocado, setEnfocado] = React.useState<Fecha>(seleccionada ?? hoyFecha)
  const rejilla = React.useRef<HTMLDivElement>(null)
  const moverFoco = React.useRef(false)

  const cambiarAbierto = (siguiente: boolean) => {
    if (siguiente) {
      const inicial = seleccionada ?? hoyFecha
      setMostrado({ anio: inicial.anio, mes: inicial.mes })
      setEnfocado(inicial)
      setVista("dias")
    } else if (abierto) {
      onBlur?.()
    }
    setAbierto(siguiente)
  }

  const elegir = (fecha: Fecha) => {
    if (fueraDeRango(fecha)) return
    onChange(aIso(fecha))
    cambiarAbierto(false)
  }

  React.useEffect(() => {
    if (!abierto || vista !== "dias" || !moverFoco.current) return
    moverFoco.current = false
    rejilla.current?.querySelector<HTMLElement>(`[data-iso="${aIso(enfocado)}"]`)?.focus()
  }, [abierto, vista, enfocado])

  const irA = (fecha: Fecha) => {
    moverFoco.current = true
    setEnfocado(fecha)
    setMostrado({ anio: fecha.anio, mes: fecha.mes })
  }

  const alTeclear = (e: React.KeyboardEvent) => {
    const pasos: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
    if (e.key in pasos) {
      e.preventDefault()
      irA(sumarDias(enfocado, pasos[e.key]))
    } else if (e.key === "PageUp" || e.key === "PageDown") {
      e.preventDefault()
      const { anio, mes } = sumarMeses(enfocado.anio, enfocado.mes, e.key === "PageUp" ? -1 : 1)
      irA({ anio, mes, dia: Math.min(enfocado.dia, diasDelMes(anio, mes)) })
    } else if (e.key === "Home" || e.key === "End") {
      e.preventDefault()
      irA({ ...enfocado, dia: e.key === "Home" ? 1 : diasDelMes(enfocado.anio, enfocado.mes) })
    }
  }

  const huecos = huecosIniciales(mostrado.anio, mostrado.mes)
  const totalDias = diasDelMes(mostrado.anio, mostrado.mes)
  const hoyElegible = !fueraDeRango(hoyFecha)

  const mesAnterior = sumarMeses(mostrado.anio, mostrado.mes, -1)
  const mesSiguiente = sumarMeses(mostrado.anio, mostrado.mes, 1)
  const hayAnterior =
    vista === "meses"
      ? minimo === null || mostrado.anio - 1 >= minimo.anio
      : minimo === null ||
        aIso({ ...mesAnterior, dia: diasDelMes(mesAnterior.anio, mesAnterior.mes) }) >= aIso(minimo)
  const haySiguiente =
    vista === "meses"
      ? maximo === null || mostrado.anio + 1 <= maximo.anio
      : maximo === null || aIso({ ...mesSiguiente, dia: 1 }) <= aIso(maximo)

  const navegar = (sentido: -1 | 1) => {
    if (vista === "meses") {
      setMostrado((m) => ({ ...m, anio: m.anio + sentido }))
    } else {
      setMostrado((m) => sumarMeses(m.anio, m.mes, sentido))
    }
  }

  return (
    <Popover open={abierto} onOpenChange={cambiarAbierto}>
      <PopoverTrigger
        id={id}
        data-slot="date-picker-trigger"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-invalid={ariaInvalid}
        className={cn(
          "flex h-8 w-full items-center justify-between gap-1.5 rounded-2xl border border-transparent bg-input/50 px-3 text-sm whitespace-nowrap transition-[color,box-shadow] duration-200 outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0",
          className,
        )}
      >
        <span className={cn(!seleccionada && "text-muted-foreground")}>
          {seleccionada ? formatear(seleccionada) : placeholder}
        </span>
        <CalendarIcon className="size-4 text-muted-foreground" />
      </PopoverTrigger>

      <PopoverContent align="start" className="w-72 p-3" aria-label="Calendario">
        <div className="mb-2 flex items-center justify-between gap-1">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label={vista === "meses" ? "Año anterior" : "Mes anterior"}
            disabled={!hayAnterior}
            onClick={() => navegar(-1)}
          >
            <ChevronLeftIcon />
          </Button>
          <button
            type="button"
            aria-live="polite"
            onClick={() => setVista((v) => (v === "dias" ? "meses" : "dias"))}
            className="rounded-2xl px-3 py-1 font-heading text-sm font-medium capitalize outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/30"
          >
            {vista === "dias" ? `${MESES[mostrado.mes]} ${mostrado.anio}` : mostrado.anio}
          </button>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label={vista === "meses" ? "Año siguiente" : "Mes siguiente"}
            disabled={!haySiguiente}
            onClick={() => navegar(1)}
          >
            <ChevronRightIcon />
          </Button>
        </div>

        {vista === "meses" ? (
          <div className="grid grid-cols-3 gap-1" role="group" aria-label={`Meses de ${mostrado.anio}`}>
            {MESES_CORTOS.map((nombre, mes) => {
              const ultimo = { anio: mostrado.anio, mes, dia: diasDelMes(mostrado.anio, mes) }
              const primero = { anio: mostrado.anio, mes, dia: 1 }
              const sinDias = fueraDeRango(ultimo) && fueraDeRango(primero)
              const esActual = mes === mostrado.mes
              return (
                <button
                  key={nombre}
                  type="button"
                  disabled={sinDias}
                  aria-label={`${MESES[mes]} ${mostrado.anio}`}
                  aria-pressed={esActual}
                  onClick={() => {
                    setMostrado((m) => ({ ...m, mes }))
                    setEnfocado((f) => ({
                      anio: mostrado.anio,
                      mes,
                      dia: Math.min(f.dia, diasDelMes(mostrado.anio, mes)),
                    }))
                    setVista("dias")
                  }}
                  className={cn(
                    "h-9 rounded-2xl text-sm capitalize outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/30 disabled:pointer-events-none disabled:opacity-35",
                    esActual ? "bg-primary text-primary-foreground" : "hover:bg-muted",
                  )}
                >
                  {nombre}
                </button>
              )
            })}
          </div>
        ) : (
          <div role="grid" aria-label={`${MESES[mostrado.mes]} ${mostrado.anio}`} ref={rejilla} onKeyDown={alTeclear}>
            <div role="row" className="mb-1 grid grid-cols-7">
              {DIAS_SEMANA.map((dia, i) => (
                <span
                  key={i}
                  role="columnheader"
                  className="flex h-8 items-center justify-center text-xs font-medium text-muted-foreground"
                >
                  {dia}
                </span>
              ))}
            </div>
            <div className="grid grid-cols-7 gap-y-0.5">
              {Array.from({ length: huecos }, (_, i) => (
                <span key={`hueco-${i}`} aria-hidden />
              ))}
              {Array.from({ length: totalDias }, (_, i) => {
                const fecha = { anio: mostrado.anio, mes: mostrado.mes, dia: i + 1 }
                const iso = aIso(fecha)
                const esElegida = iso === value
                const esHoy = iso === hoyIso
                const bloqueada = fueraDeRango(fecha)
                const tabulable = iso === aIso(enfocado)
                return (
                  <button
                    key={iso}
                    type="button"
                    role="gridcell"
                    data-iso={iso}
                    aria-label={etiquetaLarga(fecha)}
                    aria-selected={esElegida}
                    aria-current={esHoy ? "date" : undefined}
                    disabled={bloqueada}
                    tabIndex={tabulable ? 0 : -1}
                    onClick={() => elegir(fecha)}
                    className={cn(
                      "mx-auto flex size-9 items-center justify-center rounded-full text-sm outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/40 disabled:pointer-events-none disabled:text-muted-foreground disabled:opacity-40",
                      esElegida
                        ? "bg-primary font-medium text-primary-foreground hover:bg-primary/90"
                        : "hover:bg-muted",
                      esHoy && !esElegida && "font-semibold text-accent ring-1 ring-accent/50",
                    )}
                  >
                    {fecha.dia}
                  </button>
                )
              })}
            </div>
          </div>
        )}

        {(clearable || hoyElegible) && (
          <div className="mt-2 flex items-center justify-between border-t border-border pt-2">
            {clearable ? (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={!seleccionada}
                onClick={() => {
                  onChange("")
                  cambiarAbierto(false)
                }}
              >
                Borrar
              </Button>
            ) : (
              <span />
            )}
            {hoyElegible && (
              <Button type="button" variant="ghost" size="sm" className="text-primary" onClick={() => elegir(hoyFecha)}>
                Hoy
              </Button>
            )}
          </div>
        )}
      </PopoverContent>
    </Popover>
  )
}

export { DatePicker }
export type { DatePickerProps }
