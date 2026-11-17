interface PuntajeGaugeProps {
  puntaje: number
  maximo: number
}

const RADIO = 52
const CIRCUNFERENCIA = 2 * Math.PI * RADIO

/**
 * El puntaje como un anillo: lo lleno es la parte del máximo que alcanzó. Colores solo con tokens del tema. El valor
 * también va en texto (no depende del color ni del dibujo).
 */
export function PuntajeGauge({ puntaje, maximo }: PuntajeGaugeProps) {
  const parte = maximo > 0 ? Math.min(Math.max(puntaje / maximo, 0), 1) : 0
  return (
    <div className="relative mx-auto size-44" role="img" aria-label={`Puntaje ${puntaje.toFixed(1)} de ${maximo}`}>
      <svg viewBox="0 0 120 120" className="size-full -rotate-90">
        <circle cx="60" cy="60" r={RADIO} fill="none" strokeWidth="12" className="stroke-muted" />
        <circle
          cx="60"
          cy="60"
          r={RADIO}
          fill="none"
          strokeWidth="12"
          strokeLinecap="round"
          className="stroke-primary"
          strokeDasharray={`${CIRCUNFERENCIA * parte} ${CIRCUNFERENCIA}`}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-4xl font-semibold text-foreground">{puntaje.toFixed(1)}</span>
        <span className="text-xs text-muted-foreground">de {maximo}</span>
      </div>
    </div>
  )
}
