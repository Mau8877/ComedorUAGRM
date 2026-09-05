import dayjs from 'dayjs'

import type { LayoutUser, NotificationItem } from '@/components/layout'

import type { Ingrediente, MenuDia, PedidoHistorial, UsuarioReciente, UsuarioTabla } from '../types'

export const mockAdminUser: LayoutUser = {
  nombre: 'Ana Pérez',
  rol: 'Administrador',
}

export const mockEstudianteUser: LayoutUser = {
  nombre: 'Juan Gómez',
  rol: 'Estudiante',
}

// 23 notificaciones (no las 5-7 de antes) para poder ver la paginación real
// de a 10 + "Mostrar más" de NotificationsMenu -- ya ordenadas más nueva
// primero (el orden es responsabilidad de quien arma el array, no del
// componente). `minutosAtras` se resuelve a un ISO real al construir el
// array, así `formatRelativeDate` muestra un relativo de verdad ("hace 5
// minutos", "hace 2 días") en vez de un string ya formateado a mano.
const notificacionesSeed: {
  tipo: NotificationItem['tipo']
  titulo: string
  detalle?: string
  minutosAtras: number
  leida: boolean
}[] = [
  { tipo: 'warning', titulo: 'Stock bajo: Cebolla', detalle: 'Quedan 8 kg, por debajo del mínimo (10 kg).', minutosAtras: 5, leida: false },
  { tipo: 'info', titulo: 'Nuevo pedido recibido', detalle: 'Pedido #1042 -- Milanesa con puré.', minutosAtras: 32, leida: false },
  { tipo: 'error', titulo: 'Ingrediente agotado: Zanahoria', minutosAtras: 60, leida: false },
  { tipo: 'success', titulo: 'Menú de mañana publicado', detalle: 'Silpancho, Ensalada de quinoa, Charque con mote.', minutosAtras: 95, leida: false },
  { tipo: 'info', titulo: 'Nuevo usuario registrado', detalle: 'Rosa Fernández se unió como Estudiante.', minutosAtras: 140, leida: false },
  { tipo: 'warning', titulo: 'Stock bajo: Leche', detalle: 'Quedan 5 L, por debajo del mínimo (12 L).', minutosAtras: 210, leida: false },
  { tipo: 'error', titulo: 'Pedido cancelado', detalle: 'Pedido #1039 -- Pique a lo macho.', minutosAtras: 300, leida: false },
  { tipo: 'success', titulo: 'Pedido entregado', detalle: 'Pedido #1035 -- Silpancho.', minutosAtras: 480, leida: false },
  { tipo: 'info', titulo: 'Cambio de horario', detalle: 'El comedor cierra 30 minutos antes este viernes.', minutosAtras: 600, leida: false },
  { tipo: 'warning', titulo: 'Stock bajo: Pimiento', detalle: 'Quedan 3 kg, por debajo del mínimo (5 kg).', minutosAtras: 720, leida: true },
  { tipo: 'success', titulo: 'Nuevo ingrediente registrado', detalle: 'Arroz agregado al inventario.', minutosAtras: 900, leida: true },
  { tipo: 'info', titulo: 'Nuevo usuario registrado', detalle: 'Diego Vaca se unió como Estudiante.', minutosAtras: 1080, leida: true },
  { tipo: 'error', titulo: 'Ingrediente agotado: Harina', minutosAtras: 1300, leida: true },
  { tipo: 'success', titulo: 'Menú semanal publicado', detalle: 'Ya está disponible el menú de la próxima semana.', minutosAtras: 1500, leida: true },
  { tipo: 'warning', titulo: 'Stock bajo: Ajo', detalle: 'Quedan 2 kg, por debajo del mínimo (3 kg).', minutosAtras: 1800, leida: true },
  { tipo: 'info', titulo: 'Nuevo pedido recibido', detalle: 'Pedido #1020 -- Ensalada de quinoa.', minutosAtras: 2100, leida: true },
  { tipo: 'success', titulo: 'Pedido entregado', detalle: 'Pedido #1018 -- Charque con mote.', minutosAtras: 2600, leida: true },
  { tipo: 'error', titulo: 'Pedido cancelado', detalle: 'Pedido #1015 -- Milanesa con puré.', minutosAtras: 3000, leida: true },
  { tipo: 'info', titulo: 'Nuevo usuario registrado', detalle: 'Lucía Rojas se unió como Estudiante.', minutosAtras: 4000, leida: true },
  { tipo: 'warning', titulo: 'Stock bajo: Tomate', detalle: 'Quedan 9 kg, por debajo del mínimo (10 kg).', minutosAtras: 5200, leida: true },
  { tipo: 'success', titulo: 'Nuevo ingrediente registrado', detalle: 'Huevo agregado al inventario.', minutosAtras: 7000, leida: true },
  { tipo: 'info', titulo: 'Cambio de horario', detalle: 'El comedor abre 1 hora antes durante exámenes.', minutosAtras: 8500, leida: true },
  { tipo: 'success', titulo: 'Menú de mañana publicado', detalle: 'Pique a lo macho, Ensalada de quinoa.', minutosAtras: 10000, leida: true },
]

export const mockNotificaciones: NotificationItem[] = notificacionesSeed.map((seed, index) => ({
  id: `notif-${index + 1}`,
  tipo: seed.tipo,
  titulo: seed.titulo,
  detalle: seed.detalle,
  fecha: dayjs().subtract(seed.minutosAtras, 'minute').toISOString(),
  leida: seed.leida,
}))

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
    descripcion: 'Tomate fresco de mesa, usado en ensaladas y salsas del menú diario.',
    proveedor: 'Agropecuaria Santa Cruz',
    ubicacionAlmacen: 'Cámara fría A - Estante 2',
    fechaIngreso: '2026-08-28',
    fechaVencimiento: '2026-09-10',
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
    descripcion: 'Cebolla blanca, base de aderezos y guisos.',
    proveedor: 'Mercado Los Pozos',
    ubicacionAlmacen: 'Depósito seco - Estante 1',
    fechaIngreso: '2026-08-25',
    fechaVencimiento: '2026-09-20',
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
    descripcion: 'Papa holandesa, insumo principal de los platos ejecutivos.',
    proveedor: 'Agropecuaria Santa Cruz',
    ubicacionAlmacen: 'Depósito seco - Estante 3',
    fechaIngreso: '2026-08-20',
    fechaVencimiento: '2026-10-05',
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
    descripcion: 'Zanahoria naranja, usada en ensaladas y guarniciones.',
    proveedor: 'Mercado Los Pozos',
    ubicacionAlmacen: 'Cámara fría A - Estante 1',
    fechaIngreso: '2026-08-15',
    fechaVencimiento: '2026-09-01',
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
    descripcion: 'Ajo entero, condimento base de la mayoría de los platos salados.',
    proveedor: 'Distribuidora Condimentos SRL',
    ubicacionAlmacen: 'Depósito seco - Estante 4',
    fechaIngreso: '2026-08-18',
    fechaVencimiento: '2026-11-18',
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
    descripcion: 'Huevo de granja, usado en milanesas, postres y desayunos.',
    proveedor: 'Granja Los Andes',
    ubicacionAlmacen: 'Cámara fría B - Estante 1',
    fechaIngreso: '2026-08-30',
    fechaVencimiento: '2026-09-25',
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
    descripcion: 'Leche entera pasteurizada, usada en desayunos y postres.',
    proveedor: 'Granja Los Andes',
    ubicacionAlmacen: 'Cámara fría B - Estante 2',
    fechaIngreso: '2026-09-01',
    fechaVencimiento: '2026-09-08',
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
    descripcion: 'Arroz grano de oro, guarnición principal del menú ejecutivo.',
    proveedor: 'Molinos del Oriente',
    ubicacionAlmacen: 'Depósito seco - Estante 5',
    fechaIngreso: '2026-08-10',
    fechaVencimiento: '2027-02-10',
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
    descripcion: 'Harina de trigo, usada para empanizados y masas.',
    proveedor: 'Molinos del Oriente',
    ubicacionAlmacen: 'Depósito seco - Estante 5',
    fechaIngreso: '2026-08-12',
    fechaVencimiento: '2027-01-12',
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
    descripcion: 'Pimiento rojo, usado en salteados y guarniciones.',
    proveedor: 'Mercado Los Pozos',
    ubicacionAlmacen: 'Cámara fría A - Estante 2',
    fechaIngreso: '2026-08-27',
    fechaVencimiento: '2026-09-12',
  },
]

export const mockConsumoSemanal = [
  { label: 'Lun', value: 620 },
  { label: 'Mar', value: 580 },
  { label: 'Mié', value: 710 },
  { label: 'Jue', value: 540 },
  { label: 'Vie', value: 690 },
]
