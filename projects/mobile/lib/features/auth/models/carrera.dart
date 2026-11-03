/// Carrera del catálogo público para el selector del registro.
class Carrera {
  final String codigo;
  final String nombre;
  final String facultad;

  const Carrera({required this.codigo, required this.nombre, required this.facultad});

  factory Carrera.fromJson(Map<String, dynamic> json) => Carrera(
        codigo: json['codigo'] as String,
        nombre: json['nombre'] as String,
        facultad: json['facultad'] as String,
      );
}
