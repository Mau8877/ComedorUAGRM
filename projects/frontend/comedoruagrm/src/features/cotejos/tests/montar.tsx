// Ayudas de los tests de la pantalla «Cotejo de asistencia»: montan la pantalla con un apiClient simulado y un
// QueryClient limpio, y responden cada ruta del backend con lo que se les pasa.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { CotejoScreen } from '../screens/CotejoScreen'
import type { Cotejo, DatosCotejo, RegistroDigital } from '../types'

import { cliente, convocatoriasPublicadas, planillasDelSelector } from './clienteSimulado'

export { cliente }

export const yo = {
  codigo: 'yo',
  nombres: 'Admin',
  apellidos: 'Principal',
  correo: 'admin@dubss.com',
  rol: 'ADMINISTRADOR_DUBSS',
}

export const PLANILLA = 'pl-7'

export function datos(extra: Partial<DatosCotejo> = {}): DatosCotejo {
  return {
    planilla: PLANILLA,
    convocatoria: 'conv-1',
    convocatoriaNombre: 'Beca 2026-II',
    fechaDesde: '2026-09-01',
    fechaHasta: '2026-09-30',
    totalBecados: 880,
    generadaEn: '2026-08-30T15:00:00Z',
    registrosDigitales: 861,
    becadosConAsistencia: 700,
    ...extra,
  }
}

export function registro(n: number, extra: Partial<RegistroDigital> = {}): RegistroDigital {
  return {
    nombres: `Nombre${n}`,
    apellidos: `Apellido${n}`,
    carrera: 'Ing. Sistemas',
    fecha: '2026-09-05',
    horaRegistro: '2026-09-05T16:30:00Z',
    ...extra,
  }
}

export function cotejo(n: number, extra: Partial<Cotejo> = {}): Cotejo {
  return {
    codigo: `ct-${n}`,
    planilla: PLANILLA,
    fechaCotejo: '2026-09-30',
    totalRegistros: 880,
    coincidencias: 861,
    discrepancias: 19,
    resultado: 'CON_DISCREPANCIAS',
    observaciones: 'Faltan firmas del 05/09',
    realizadoPor: 'Admin Prueba',
    creadoEn: '2026-10-01T15:00:00Z',
    ...extra,
  }
}

type Meta = Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }>

/** Responde cada ruta de GET con lo que se le pasa; lo que no se pasa responde con datos por defecto. */
export function responder(
  opciones: { datos?: DatosCotejo; registros?: RegistroDigital[]; cotejos?: Cotejo[]; meta?: Meta } = {},
) {
  const registros = opciones.registros ?? [registro(1), registro(2)]
  const cotejos = opciones.cotejos ?? [cotejo(1)]
  convocatoriasPublicadas.data = [{ codigo: 'conv-1', nombre: 'Beca 2026-II' }]
  planillasDelSelector.data = [{ codigo: PLANILLA, fechaDesde: '2026-09-01', fechaHasta: '2026-09-30', totalBecados: 880 }]
  const pagina = (data: unknown[]) => ({
    data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages: 1, ...opciones.meta } },
  })
  cliente.get.mockImplementation((ruta: string) => {
    if (ruta === `/planillas/${PLANILLA}/cotejos/datos`) return Promise.resolve({ data: opciones.datos ?? datos() })
    if (ruta === `/planillas/${PLANILLA}/cotejos/registros`) return Promise.resolve(pagina(registros))
    if (ruta === `/planillas/${PLANILLA}/cotejos`) return Promise.resolve(pagina(cotejos))
    return Promise.reject(new Error(`ruta sin simular: ${ruta}`))
  })
}

/** Parámetros de la última consulta a una ruta. */
export function ultimaConsulta(ruta: string): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === ruta)
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
  convocatoriasPublicadas.data = []
  planillasDelSelector.data = []
}

export function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <CotejoScreen titulo="Cotejo de asistencia" activeHref="/cotejo-asistencia" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { ...vista, queryClient, user: userEvent.setup() }
}
