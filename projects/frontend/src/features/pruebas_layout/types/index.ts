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

// Dataset del demo de components/ui/tanstack-table -- gestión de
// ingredientes del comedor (ejemplo más realista que UsuarioTabla).
export type CategoriaIngrediente = 'Verdura' | 'Fruta' | 'Lácteo' | 'Carne' | 'Grano' | 'Condimento'
export type UnidadIngrediente = 'kg' | 'L' | 'unidad' | 'docena'
export type EstadoStock = 'disponible' | 'bajo-stock' | 'agotado'

export interface Ingrediente {
  id: string
  nombre: string
  foto: string
  categoria: CategoriaIngrediente
  unidad: UnidadIngrediente
  stock: number
  stockMinimo: number
  precioUnitario: number
  // Campos que solo se muestran en el detalle (no en la tabla ni en el
  // form de crear/editar) -- opcionales porque un ingrediente creado desde
  // el mockup de la demo no los completa.
  descripcion?: string
  proveedor?: string
  ubicacionAlmacen?: string
  fechaIngreso?: string
  fechaVencimiento?: string
}
