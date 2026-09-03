import type { LayoutUser } from '@/components/layout'

import type { MenuDia, PedidoHistorial, UsuarioReciente } from '../types'

export const mockAdminUser: LayoutUser = {
  nombre: 'Ana Pérez',
  rol: 'Administrador',
}

export const mockAlumnoUser: LayoutUser = {
  nombre: 'Juan Gómez',
  rol: 'Alumno',
}

export const mockMenuSemana: MenuDia[] = [
  { dia: 'Lunes', plato: 'Silpancho', estado: 'disponible', porciones: 34, porcionesTotal: 40 },
  { dia: 'Martes', plato: 'Milanesa con puré', estado: 'disponible', porciones: 28, porcionesTotal: 40 },
  { dia: 'Miércoles', plato: 'Pique a lo macho', estado: 'pocas-porciones', porciones: 6, porcionesTotal: 40 },
  { dia: 'Jueves', plato: 'Ensalada de quinoa', estado: 'disponible', porciones: 22, porcionesTotal: 30 },
  { dia: 'Viernes', plato: 'Charque con mote', estado: 'agotado', porciones: 0, porcionesTotal: 35 },
]

export const mockUsuariosRecientes: UsuarioReciente[] = [
  { nombre: 'Ana Pérez', rol: 'Administrador', correo: 'ana.perez@uagrm.edu.bo' },
  { nombre: 'Juan Gómez', rol: 'Alumno', correo: 'juan.gomez@uagrm.edu.bo' },
  { nombre: 'Rosa Fernández', rol: 'Alumno', correo: 'rosa.fernandez@uagrm.edu.bo' },
  { nombre: 'Diego Vaca', rol: 'Alumno', correo: 'diego.vaca@uagrm.edu.bo' },
]

export const mockPedidosPorCategoria = [
  { label: 'Ejecutivo', value: 148 },
  { label: 'Vegetariano', value: 62 },
  { label: 'Especial', value: 39 },
  { label: 'Snack', value: 71 },
  { label: 'Otros', value: 22 },
]

export const mockHistorialPedidos: PedidoHistorial[] = [
  { fecha: 'Hoy, 12:40', plato: 'Milanesa con puré', estado: 'entregado' },
  { fecha: 'Ayer, 13:05', plato: 'Silpancho', estado: 'entregado' },
  { fecha: 'Lun 01/09, 12:55', plato: 'Ensalada de quinoa', estado: 'pendiente' },
  { fecha: 'Vie 29/08, 12:30', plato: 'Pique a lo macho', estado: 'cancelado' },
]

export const mockConsumoSemanal = [
  { label: 'Lun', value: 620 },
  { label: 'Mar', value: 580 },
  { label: 'Mié', value: 710 },
  { label: 'Jue', value: 540 },
  { label: 'Vie', value: 690 },
]
