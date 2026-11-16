import { useRouterState } from '@tanstack/react-router'

// * Barra fina fija arriba que aparece apenas el router empieza a cargar la ruta destino (chunk lazy,
// * guards o loaders) y desaparece al resolverla. No depende de ninguna pantalla: vive en la raíz.
export function NavigationProgress() {
  const cargando = useRouterState({ select: (s) => s.status === 'pending' })
  if (!cargando) return null
  return (
    <div
      role="progressbar"
      aria-label="Cargando página"
      className="pointer-events-none fixed inset-x-0 top-0 z-[100] h-0.5 overflow-hidden bg-primary/20"
    >
      <div className="h-full w-1/3 animate-[navprogress_1s_ease-in-out_infinite] rounded-full bg-accent" />
    </div>
  )
}
