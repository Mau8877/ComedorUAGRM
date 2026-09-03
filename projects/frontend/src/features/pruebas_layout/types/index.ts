export type EstadoDisponibilidad = 'disponible' | 'pocas-porciones' | 'agotado'

export interface MenuDia {
  dia: string
  plato: string
  estado: EstadoDisponibilidad
  porciones: number
  porcionesTotal: number
}

export interface UsuarioReciente {
  nombre: string
  rol: 'Administrador' | 'Estudiante'
  correo: string
}

// Dataset del demo de components/ui/tanstack-table -- misma forma que
// UsuarioReciente más un id estable para paginación/filas.
export interface UsuarioTabla {
  id: string
  nombre: string
  rol: 'Administrador' | 'Estudiante'
  correo: string
}

export type EstadoPedido = 'entregado' | 'pendiente' | 'cancelado'

export interface PedidoHistorial {
  fecha: string
  plato: string
  estado: EstadoPedido
}
