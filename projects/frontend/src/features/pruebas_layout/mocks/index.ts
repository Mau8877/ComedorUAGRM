import type { LayoutUser } from '@/components/layout'

import type { Ingrediente, MenuDia, PedidoHistorial, UsuarioReciente, UsuarioTabla } from '../types'

export const mockAdminUser: LayoutUser = {
  nombre: 'Ana Pérez',
  rol: 'Administrador',
}

export const mockEstudianteUser: LayoutUser = {
  nombre: 'Juan Gómez',
  rol: 'Estudiante',
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
  { nombre: 'Juan Gómez', rol: 'Estudiante', correo: 'juan.gomez@uagrm.edu.bo' },
  { nombre: 'Rosa Fernández', rol: 'Estudiante', correo: 'rosa.fernandez@uagrm.edu.bo' },
  { nombre: 'Diego Vaca', rol: 'Estudiante', correo: 'diego.vaca@uagrm.edu.bo' },
]

const nombresDemo = [
  'Ana Pérez', 'Juan Gómez', 'Rosa Fernández', 'Diego Vaca', 'Lucía Rojas',
  'Marco Suárez', 'Elena Choque', 'Pablo Mamani', 'Carla Justiniano', 'Iván Rivero',
  'Valeria Peña', 'Óscar Salvatierra', 'Noelia Rocha', 'Fabricio Antelo', 'Camila Áñez',
  'Bruno Chávez', 'Gabriela Ortiz', 'Ramiro Melgar', 'Daniela Roca', 'Tomás Barba',
  'Andrea Vargas', 'Sergio Cuéllar', 'Paola Landívar', 'Hugo Terceros',
]

export const mockUsuariosTabla: UsuarioTabla[] = nombresDemo.map((nombre, index) => ({
  id: `usuario-${index + 1}`,
  nombre,
  rol: index % 4 === 0 ? 'Administrador' : 'Estudiante',
  correo: `${nombre.toLowerCase().replace(/\s+/g, '.')}@uagrm.edu.bo`,
}))

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

// Fotos reales de Wikimedia Commons (licencia libre, URLs verificadas) --
// demo de components/ui/tanstack-table con imágenes en la celda de
// nombre, visibles tanto en DataTable como en DataCards (mismo cell).
export const mockIngredientes: Ingrediente[] = [
  {
    id: 'ing-1',
    nombre: 'Tomate',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/89/Tomato_je.jpg/330px-Tomato_je.jpg',
    categoria: 'Verdura',
    unidad: 'kg',
    stock: 42,
    stockMinimo: 10,
    precioUnitario: 6.5,
  },
  {
    id: 'ing-2',
    nombre: 'Cebolla',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/20/Harvested_vegetables%28Onions%29.jpg/330px-Harvested_vegetables%28Onions%29.jpg',
    categoria: 'Verdura',
    unidad: 'kg',
    stock: 8,
    stockMinimo: 10,
    precioUnitario: 4.2,
  },
  {
    id: 'ing-3',
    nombre: 'Papa',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a3/Potatoes%2C_Wirral_flower_and_vegetable_show_-_DSC08219.JPG/330px-Potatoes%2C_Wirral_flower_and_vegetable_show_-_DSC08219.JPG',
    categoria: 'Verdura',
    unidad: 'kg',
    stock: 65,
    stockMinimo: 15,
    precioUnitario: 3.8,
  },
  {
    id: 'ing-4',
    nombre: 'Zanahoria',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/3/32/Carrots_of_many_colors.jpg/330px-Carrots_of_many_colors.jpg',
    categoria: 'Verdura',
    unidad: 'kg',
    stock: 0,
    stockMinimo: 8,
    precioUnitario: 5.0,
  },
  {
    id: 'ing-5',
    nombre: 'Ajo',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Garlic_bulbs_and_cloves.jpg/330px-Garlic_bulbs_and_cloves.jpg',
    categoria: 'Condimento',
    unidad: 'kg',
    stock: 12,
    stockMinimo: 3,
    precioUnitario: 18.0,
  },
  {
    id: 'ing-6',
    nombre: 'Huevo',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dd/Eggs_in_basket_2020_G1.jpg/330px-Eggs_in_basket_2020_G1.jpg',
    categoria: 'Lácteo',
    unidad: 'docena',
    stock: 30,
    stockMinimo: 10,
    precioUnitario: 12.5,
  },
  {
    id: 'ing-7',
    nombre: 'Leche',
    foto: 'https://upload.wikimedia.org/wikipedia/commons/a/a8/Farm-Fresh_glass_of_milk.png',
    categoria: 'Lácteo',
    unidad: 'L',
    stock: 5,
    stockMinimo: 12,
    precioUnitario: 7.0,
  },
  {
    id: 'ing-8',
    nombre: 'Arroz',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bd/White_rice_on_a_brown_table.jpg/330px-White_rice_on_a_brown_table.jpg',
    categoria: 'Grano',
    unidad: 'kg',
    stock: 90,
    stockMinimo: 20,
    precioUnitario: 5.5,
  },
  {
    id: 'ing-9',
    nombre: 'Harina',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/66/Whole_wheat_grain_flour_being_scooped.jpg/330px-Whole_wheat_grain_flour_being_scooped.jpg',
    categoria: 'Grano',
    unidad: 'kg',
    stock: 25,
    stockMinimo: 10,
    precioUnitario: 4.9,
  },
  {
    id: 'ing-10',
    nombre: 'Pimiento',
    foto: 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/de/Capsicum_annuum_fruits_IMGP0049.jpg/330px-Capsicum_annuum_fruits_IMGP0049.jpg',
    categoria: 'Verdura',
    unidad: 'kg',
    stock: 3,
    stockMinimo: 5,
    precioUnitario: 9.0,
  },
]

export const mockConsumoSemanal = [
  { label: 'Lun', value: 620 },
  { label: 'Mar', value: 580 },
  { label: 'Mié', value: 710 },
  { label: 'Jue', value: 540 },
  { label: 'Vie', value: 690 },
]
