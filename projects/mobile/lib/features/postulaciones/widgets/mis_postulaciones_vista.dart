import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../enlaces_cu02.dart';
import '../formatos.dart';
import '../models/mi_postulacion_resumen.dart';
import '../postulaciones_error_mapper.dart';
import '../providers/mis_postulaciones_provider.dart';
import 'alerta_postulaciones.dart';
import 'estado_postulacion_chip.dart';

/// Contenido de «Mis postulaciones» (CU04): las postulaciones enviadas del postulante, de la más reciente a la más
/// antigua, con su gestión, su fecha de envío y su estado visible. Es una vista sin `Scaffold` para montarse dentro
/// del shell del Estudiante.
///
/// * Scroll infinito: al acercarse al final pide la página siguiente. Tirar hacia abajo recarga desde la página 1.
/// * El filtro por gestión ofrece las gestiones de las postulaciones ya cargadas (no hay un endpoint de gestiones
/// * para el postulante); al cambiarlo la lista se recarga desde la página 1.
class MisPostulacionesVista extends ConsumerStatefulWidget {
  const MisPostulacionesVista({super.key});

  @override
  ConsumerState<MisPostulacionesVista> createState() => _MisPostulacionesVistaState();
}

class _MisPostulacionesVistaState extends ConsumerState<MisPostulacionesVista> {
  final _scroll = ScrollController();

  /// Las gestiones vistas hasta ahora (código -> nombre): al filtrar, la lista solo trae una, pero las demás
  /// siguen disponibles para volver a elegirlas.
  final Map<String, String> _gestiones = {};

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_alDesplazar);
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _alDesplazar() {
    if (_scroll.hasClients && _scroll.position.extentAfter < 240) {
      ref.read(misPostulacionesListProvider.notifier).cargarSiguiente();
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(misPostulacionesListProvider, (_, siguiente) {
      final lista = siguiente.valueOrNull;
      if (lista == null) return;
      final nuevas = {
        for (final p in lista.items)
          if (!_gestiones.containsKey(p.gestionAcademica.codigo)) p.gestionAcademica.codigo: p.gestionAcademica.nombre,
      };
      if (nuevas.isNotEmpty) setState(() => _gestiones.addAll(nuevas));
    });

    final lista = ref.watch(misPostulacionesListProvider);
    final gestion = ref.watch(misPostulacionesUiProvider);

    return lista.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: AlertaPostulaciones(
            error: mapearErrorPostulaciones(e),
            onReintentar: () => ref.invalidate(misPostulacionesListProvider),
          ),
        ),
      ),
      data: (datos) {
        if (datos.items.isEmpty && gestion == null) return const _SinPostulaciones();
        return RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(misPostulacionesListProvider);
            await ref.read(misPostulacionesListProvider.future).catchError((_) => datos);
          },
          child: ListView(
            controller: _scroll,
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              if (_gestiones.isNotEmpty) _FiltroGestion(gestiones: _gestiones, elegida: gestion),
              if (datos.items.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 32),
                  child: Center(child: Text('No hay postulaciones en esa gestión')),
                ),
              for (final postulacion in datos.items) _TarjetaPostulacion(postulacion: postulacion),
              if (datos.cargandoMas)
                const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
              if (datos.errorSiguiente)
                Center(
                  child: Column(
                    children: [
                      const Text('No se pudieron cargar más postulaciones.'),
                      TextButton(
                        onPressed: () => ref.read(misPostulacionesListProvider.notifier).cargarSiguiente(),
                        child: const Text('Reintentar'),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}

class _FiltroGestion extends ConsumerWidget {
  final Map<String, String> gestiones;
  final String? elegida;

  const _FiltroGestion({required this.gestiones, required this.elegida});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ui = ref.read(misPostulacionesUiProvider.notifier);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Wrap(
        spacing: 8,
        children: [
          ChoiceChip(label: const Text('Todas'), selected: elegida == null, onSelected: (_) => ui.elegirGestion(null)),
          for (final entrada in gestiones.entries)
            ChoiceChip(
              label: Text(entrada.value),
              selected: elegida == entrada.key,
              onSelected: (_) => ui.elegirGestion(entrada.key),
            ),
        ],
      ),
    );
  }
}

class _TarjetaPostulacion extends StatelessWidget {
  final MiPostulacionResumen postulacion;

  const _TarjetaPostulacion({required this.postulacion});

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => context.push('/mis-postulaciones/${postulacion.codigo}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(postulacion.convocatoria.nombre, style: texto.titleMedium),
                    const SizedBox(height: 4),
                    Text(
                      'Gestión ${postulacion.gestionAcademica.nombre} · Enviada el ${formatearFecha(postulacion.fechaEnvio)}',
                      style: texto.bodySmall,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              EstadoPostulacionChip(estado: postulacion.estado),
            ],
          ),
        ),
      ),
    );
  }
}

class _SinPostulaciones extends StatelessWidget {
  const _SinPostulaciones();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Todavía no enviaste ninguna postulación', textAlign: TextAlign.center),
            // La invitación a postular solo existe cuando exista la pantalla del CU-02.
            if (rutaPostular != null) ...[
              const SizedBox(height: 12),
              FilledButton(onPressed: () => context.push(rutaPostular!), child: const Text('Postular')),
            ],
          ],
        ),
      ),
    );
  }
}
