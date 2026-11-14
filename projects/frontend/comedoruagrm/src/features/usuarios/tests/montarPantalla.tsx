// Ayudas compartidas por los tests de la pantalla de listado y de sus modales:
// montan la pantalla con un apiClient simulado y un QueryClient limpio.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'

import { UsuariosPersonalScreen } from '../screens/UsuariosPersonalScreen'
import type { UsuarioPersonal } from '../types'

import { cliente } from './clienteSimulado'

export { cliente }

export const yo = { codigo: 'yo', nombres: 'Admin', apellidos: 'Principal', correo: 'admin@uagrm.edu.bo', rol: 'ADMINISTRADOR_DUBSS' }

export function usuario(n: number, extra: Partial<UsuarioPersonal> = {}): UsuarioPersonal {
  return {
    codigo: `u${n}`,
    nombres: `Nombre${n}`,
    apellidos: `Apellido${n}`,
    ci: `${1000 + n}`,
    correo: `u${n}@uagrm.edu.bo`,
    rol: 'DIRECTIVO',
    activo: true,
    ...extra,
  }
}

export function pagina(data: UsuarioPersonal[], meta: Partial<{ page: number; pageSize: number; totalItems: number; totalPages: number }> = {}) {
  return { data: { data, meta: { page: 1, pageSize: 20, totalItems: data.length, totalPages: 1, ...meta } } }
}

/** Parámetros de la última consulta de la lista. */
export function ultimaConsulta(): Record<string, unknown> {
  const llamadas = cliente.get.mock.calls.filter((c) => c[0] === '/usuarios')
  return llamadas[llamadas.length - 1][1].params
}

export function reiniciarCliente() {
  Object.values(cliente).forEach((fn) => fn.mockReset())
}

export function montar(
  ui: ReactNode = <UsuariosPersonalScreen rol="DIRECTIVO" titulo="Directivos" activeHref="/usuarios/directivos" usuarioActual={yo} />,
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const vista = render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
  return { ...vista, queryClient, user: userEvent.setup() }
}
