// Ayudas de los tests de la pantalla «Planillas»: montan la pantalla con un apiClient simulado y un QueryClient
// limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { PlanillasScreen } from '../screens/PlanillasScreen'
import type { Planilla, ResumenPlanilla } from '../types'

import { cliente, convocatoriasPublicadas } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export const CONVOCATORIA = 'conv-1'

export function planilla(n: number, extra: Partial<Planilla> = {}): Planilla {
  return {
    codigo: `pl-${n}`,
    convocatoria: CONVOCATORIA,
    fechaDesde: '2026-09-01',
    fechaHasta: '2026-09-30',
    totalBecados: 880,
    generadaEn: '2026-08-30T15:00:00Z',
    generadaPor: 'Admin Prueba',
    ...extra,
  }
}

export function resumen(extra: Partial<ResumenPlanilla> = {}): ResumenPlanilla {
  return { becadosVigentes: 880, gestion: '2026-II', gestionDesde: '2026-07-01', gestionHasta: '2026-12-31', ...extra }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde con datos por defecto. */
export function responder(opciones: { planillas?: Planilla[]; meta?: Meta; resumen?: ResumenPlanilla } = {}) {
  const planillas = opciones.planillas ?? [planilla(7), planilla(6, { totalBecados: 884, fechaDesde: '2026-08-01' })]
  convocatoriasPublicadas.data = [{ codigo: CONVOCATORIA, nombre: 'Beca 2026-II' }]
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === `/convocatorias/${CONVOCATORIA}/planillas`) {
      return Promise.resolve({
        data: {
          data: planillas,
          meta: { page: 1, pageSize: 20, totalItems: planillas.length, totalPages: 1, ...opciones.meta },
        },
      })
    }
    if (ruta === `/convocatorias/${CONVOCATORIA}/planillas/resumen`) {
      return Promise.resolve({ data: opciones.resumen ?? resumen() })
    }
    if (ruta === `/convocatorias/${CONVOCATORIA}/planillas/vista-previa`) {
      return Promise.resolve({ data: new Blob(['%PDF'], { type: 'application/pdf' }) })
    }
    if (/^\/planillas\/[^/]+\/descarga$/.test(ruta)) {
      return Promise.resolve({ data: { url: 'https://minio.test/firmada', expiraEn: '2026-11-16T15:05:00Z' } })
    }
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta del listado de planillas. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === `/convocatorias/${CONVOCATORIA}/planillas`)
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
  convocatoriasPublicadas.data = []
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <PlanillasScreen titulo="Planillas de asistencia" activeHref="/planillas" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
