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
  rol: 'Administrador' | 'Alumno'
  correo: string
}

export type EstadoPedido = 'entregado' | 'pendiente' | 'cancelado'

export interface PedidoHistorial {
  fecha: string
  plato: string
  estado: EstadoPedido
}
