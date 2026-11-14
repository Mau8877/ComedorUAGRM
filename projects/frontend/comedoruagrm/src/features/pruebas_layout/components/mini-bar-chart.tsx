import { cn } from '@/lib/utils'

interface MiniBarChartDatum {
  label: string
  value: number
}

// * Un color de `--chart-1`..`--chart-5` por barra, en orden -- si la
// * pantalla necesita más de 5 categorías, se repiten (no hay un chart-6).
const chartColorClasses = [
  'bg-chart-1',
  'bg-chart-2',
  'bg-chart-3',
  'bg-chart-4',
  'bg-chart-5',
]

export function MiniBarChart({ data }: { data: MiniBarChartDatum[] }) {
  const max = Math.max(...data.map((d) => d.value))

  return (
    <div className="flex items-end gap-4">
      {data.map((item, i) => (
        <div key={item.label} className="flex flex-1 flex-col items-center gap-2">
          <span className="text-xs font-medium text-foreground">{item.value}</span>
          <div className="flex h-24 w-full items-end justify-center">
            <div
              className={cn(
                'w-full max-w-9 rounded-t-md',
                chartColorClasses[i % chartColorClasses.length]
              )}
              style={{ height: `${(item.value / max) * 100}%` }}
            />
          </div>
          <span className="text-xs text-muted-foreground">{item.label}</span>
        </div>
      ))}
    </div>
  )
}
