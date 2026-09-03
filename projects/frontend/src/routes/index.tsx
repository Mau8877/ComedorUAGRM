import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: HomePage,
})

function HomePage() {
  return (
    <div className="min-h-screen bg-background text-foreground p-8 md:p-12 space-y-10">
      {/* Cabecera para probar Tipografías */}
      <header className="space-y-3">
        <h1 className="font-heading text-4xl md:text-5xl font-bold text-primary">ComedorUAGRM</h1>
        <p className="font-sans text-lg text-muted-foreground max-w-2xl">
          Esta es una vista previa para comprobar el contraste del tema, las fuentes (Familjen
          Grotesk para títulos y Jost para texto) y el sistema de colores.
        </p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 font-sans">
        {/* Tarjeta 1: Colores Principales */}
        <div className="bg-card text-card-foreground p-6 rounded-2xl border border-border shadow-sm space-y-5">
          <h2 className="font-heading text-2xl font-bold">Colores Base</h2>
          <div className="space-y-3">
            <div className="bg-primary text-primary-foreground p-3 rounded-lg text-center font-medium">
              Primary
            </div>
            <div className="bg-secondary text-secondary-foreground p-3 rounded-lg text-center font-medium">
              Secondary
            </div>
            <div className="bg-accent text-accent-foreground p-3 rounded-lg text-center font-medium">
              Accent
            </div>
          </div>
        </div>

        {/* Tarjeta 2: Colores de Estado */}
        <div className="bg-card text-card-foreground p-6 rounded-2xl border border-border shadow-sm space-y-5">
          <h2 className="font-heading text-2xl font-bold">Estados</h2>
          <div className="space-y-3">
            <div className="bg-success text-success-foreground p-3 rounded-lg text-center font-medium">
              Success
            </div>
            <div className="bg-destructive text-destructive-foreground p-3 rounded-lg text-center font-medium">
              Destructive
            </div>
            <div className="bg-muted text-muted-foreground p-3 rounded-lg text-center border border-border font-medium">
              Muted
            </div>
          </div>
        </div>

        {/* Tarjeta 3: Elementos UI y Gráficos */}
        <div className="bg-card text-card-foreground p-6 rounded-2xl border border-border shadow-sm space-y-5">
          <h2 className="font-heading text-2xl font-bold">Elementos UI</h2>
          <div className="space-y-4">
            {/* Prueba de Inputs y Rings */}
            <input
              type="text"
              placeholder="Escribe algo aquí..."
              className="w-full bg-background border border-input rounded-md px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring transition-shadow"
            />
            {/* Prueba de Botón */}
            <button className="w-full bg-primary text-primary-foreground py-2.5 rounded-xl hover:opacity-90 transition-opacity focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:ring-offset-background font-medium">
              Botón de Acción
            </button>
            {/* Prueba de colores de Chart */}
            <div className="pt-2">
              <p className="text-sm text-muted-foreground mb-2">Colores de Gráficos</p>
              <div className="flex h-6 w-full rounded-sm overflow-hidden">
                <div className="flex-1 bg-chart-1"></div>
                <div className="flex-1 bg-chart-2"></div>
                <div className="flex-1 bg-chart-3"></div>
                <div className="flex-1 bg-chart-4"></div>
                <div className="flex-1 bg-chart-5"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
