/// Modelo de mock para la pantalla de prueba `/ingredientes` -- espejo de
/// `Ingrediente` en `projects/frontend/src/features/pruebas_layout/types/`.
/// No es un DTO real de una feature (ARQUITECTURA_MOBILE.md todavía no
/// define esa convención) -- vive acá solo mientras exista el mockup.
enum EstadoStock { disponible, bajoStock, agotado }

class Ingrediente {
  final String id;
  final String nombre;
  final String foto;
  final String categoria;
  final String unidad;
  final int stock;
  final int stockMinimo;
  final double precioUnitario;
  final String? descripcion;
  final String? proveedor;
  final String? ubicacionAlmacen;
  final String? fechaIngreso;
  final String? fechaVencimiento;

  const Ingrediente({
    required this.id,
    required this.nombre,
    required this.foto,
    required this.categoria,
    required this.unidad,
    required this.stock,
    required this.stockMinimo,
    required this.precioUnitario,
    this.descripcion,
    this.proveedor,
    this.ubicacionAlmacen,
    this.fechaIngreso,
    this.fechaVencimiento,
  });

  Ingrediente copyWith({
    String? nombre,
    String? foto,
    String? categoria,
    String? unidad,
    int? stock,
    int? stockMinimo,
    double? precioUnitario,
  }) {
    return Ingrediente(
      id: id,
      nombre: nombre ?? this.nombre,
      foto: foto ?? this.foto,
      categoria: categoria ?? this.categoria,
      unidad: unidad ?? this.unidad,
      stock: stock ?? this.stock,
      stockMinimo: stockMinimo ?? this.stockMinimo,
      precioUnitario: precioUnitario ?? this.precioUnitario,
      descripcion: descripcion,
      proveedor: proveedor,
      ubicacionAlmacen: ubicacionAlmacen,
      fechaIngreso: fechaIngreso,
      fechaVencimiento: fechaVencimiento,
    );
  }

  EstadoStock get estadoStock {
    if (stock <= 0) return EstadoStock.agotado;
    if (stock <= stockMinimo) return EstadoStock.bajoStock;
    return EstadoStock.disponible;
  }
}

const categoriasIngrediente = ['Verdura', 'Fruta', 'Lácteo', 'Carne', 'Grano', 'Condimento'];
const unidadesIngrediente = ['kg', 'L', 'unidad', 'docena'];
