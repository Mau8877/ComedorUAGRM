import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../formatos_mibeca.dart';
import '../mibeca_error_mapper.dart';
import '../models/mi_beca.dart';
import '../providers/mibeca_provider.dart';

/// Contenido de «Mi beca» (CU24): el estado de la beca (con el motivo si está suspendida o dada de baja) y el
/// historial de asistencia, de la más reciente a la más antigua, filtrable por rango de fechas. Es una vista sin
/// `Scaffold` para montarse dentro del shell del Estudiante. Solo lectura.
///
/// * Scroll infinito: al acercarse al final pide la página siguiente. Tirar hacia abajo recarga todo.
/// * Sin beca (`ERR_MBE_01`, A1) informa y, con [onVerPostulaciones], ofrece ir a «Mis postulaciones» (CU04).
class MiBecaVista extends ConsumerStatefulWidget {
  /// Lleva a la sección «Mis postulaciones» del shell; sin él, el aviso de «sin beca» no ofrece el botón.
  final VoidCallback? onVerPostulaciones;

  const MiBecaVista({super.key, this.onVerPostulaciones});

  @override
  ConsumerState<MiBecaVista> createState() => _MiBecaVistaState();
}

class _MiBecaVistaState extends ConsumerState<MiBecaVista> {
  final _scroll = ScrollController();

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
      ref.read(miBecaAsistenciasProvider.notifier).cargarSiguiente();
    }
  }

  Future<void> _recargar() async {
    ref.invalidate(miBecaProvider);
    ref.invalidate(miBecaAsistenciasProvider);
    // Un fallo de la recarga lo muestra la propia vista (estado de error); acá solo se espera a que termine.
    await ref.read(miBecaProvider.future).then<void>((_) {}, onError: (_) {});
  }

  Future<void> _elegirRango() async {
    final hoy = DateTime.now();
    final elegido = await showDateRangePicker(
      context: context,
      firstDate: DateTime(hoy.year - 3),
      lastDate: DateTime(hoy.year + 1, 12, 31),
      initialDateRange: ref.read(miBecaRangoUiProvider),
      helpText: 'Filtrar por fechas',
      saveText: 'Aplicar',
    );
    if (elegido != null) ref.read(miBecaRangoUiProvider.notifier).elegir(elegido);
  }

  @override
  Widget build(BuildContext context) {
    final beca = ref.watch(miBecaProvider);

    return beca.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) {
        final error = mapearErrorMiBeca(e);
        return Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: error.sinBeca
                ? _SinBeca(onVerPostulaciones: widget.onVerPostulaciones)
                : _AlertaMiBeca(error: error, onReintentar: () => ref.invalidate(miBecaProvider)),
          ),
        );
      },
      data: (datos) => RefreshIndicator(
        onRefresh: _recargar,
        child: ListView(
          controller: _scroll,
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            _TarjetaBeca(beca: datos),
            const SizedBox(height: 16),
            _Historial(onElegirRango: _elegirRango),
          ],
        ),
      ),
    );
  }
}

class _TarjetaBeca extends StatelessWidget {
  final MiBeca beca;

  const _TarjetaBeca({required this.beca});

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final colores = Theme.of(context).colorScheme;
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${beca.convocatoria} · Gestión ${beca.gestion}', style: texto.bodySmall),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(child: Text('Estado de la beca', style: texto.titleMedium)),
                Chip(
                  key: const Key('estado-beca'),
                  label: Text(etiquetaEstadoBeca(beca.estado)),
                  backgroundColor: beca.vigente ? colores.primaryContainer : colores.errorContainer,
                ),
              ],
            ),
            if (!beca.vigente && beca.motivo != null) ...[
              const SizedBox(height: 4),
              Text('Motivo: ${beca.motivo}', style: texto.bodyMedium),
            ],
            const SizedBox(height: 8),
            Text(
              beca.fechaAdjudicacion == null
                  ? 'Inicio de la beca: ${formatearDiaBeca(beca.fechaInicio)}'
                  : 'Adjudicada el ${formatearFechaInstanteBeca(beca.fechaAdjudicacion!)}',
              style: texto.bodySmall,
            ),
            const SizedBox(height: 12),
            Text('${beca.totalAsistencias}', style: texto.headlineMedium),
            Text('Asistencias registradas', style: texto.bodySmall),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: colores.secondaryContainer, borderRadius: BorderRadius.circular(12)),
              child: Text(
                'El registro lo realiza el personal del comedor. Si detectas un error, comunícate con la DUBSS.',
                style: texto.bodySmall,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Historial extends ConsumerWidget {
  final VoidCallback onElegirRango;

  const _Historial({required this.onElegirRango});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final texto = Theme.of(context).textTheme;
    final rango = ref.watch(miBecaRangoUiProvider);
    final lista = ref.watch(miBecaAsistenciasProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Historial de asistencia', style: texto.titleMedium),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            OutlinedButton.icon(
              onPressed: onElegirRango,
              icon: const Icon(Icons.date_range_outlined),
              label: Text(rango == null
                  ? 'Filtrar por fechas'
                  : '${formatearDiaBeca(rango.start)} - ${formatearDiaBeca(rango.end)}'),
            ),
            if (rango != null)
              TextButton(
                onPressed: () => ref.read(miBecaRangoUiProvider.notifier).elegir(null),
                child: const Text('Quitar filtro'),
              ),
          ],
        ),
        const SizedBox(height: 8),
        lista.when(
          loading: () => const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator())),
          error: (e, _) => _AlertaMiBeca(
            error: mapearErrorMiBeca(e),
            onReintentar: () => ref.invalidate(miBecaAsistenciasProvider),
          ),
          data: (datos) {
            if (datos.items.isEmpty) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 24),
                child: Center(
                  child: Text(
                    rango == null
                        ? 'Todavía no hay asistencias registradas'
                        : 'No hay asistencias en ese rango de fechas',
                    textAlign: TextAlign.center,
                  ),
                ),
              );
            }
            return Column(
              children: [
                for (final asistencia in datos.items)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(formatearDiaBeca(asistencia.fecha)),
                    trailing: Text(formatearHoraBeca(asistencia.horaRegistro)),
                  ),
                if (datos.cargandoMas)
                  const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
                if (datos.errorSiguiente)
                  Column(
                    children: [
                      const Text('No se pudieron cargar más asistencias.'),
                      TextButton(
                        onPressed: () => ref.read(miBecaAsistenciasProvider.notifier).cargarSiguiente(),
                        child: const Text('Reintentar'),
                      ),
                    ],
                  ),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _SinBeca extends StatelessWidget {
  final VoidCallback? onVerPostulaciones;

  const _SinBeca({this.onVerPostulaciones});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text('No cuentas con una beca', style: Theme.of(context).textTheme.titleMedium, textAlign: TextAlign.center),
        const SizedBox(height: 8),
        const Text('No figuras en el padrón de becados. Revisa el estado de tu postulación.',
            textAlign: TextAlign.center),
        if (onVerPostulaciones != null) ...[
          const SizedBox(height: 12),
          FilledButton(onPressed: onVerPostulaciones, child: const Text('Ir a Mis postulaciones')),
        ],
      ],
    );
  }
}

class _AlertaMiBeca extends StatelessWidget {
  final ErrorMiBeca error;
  final VoidCallback? onReintentar;

  const _AlertaMiBeca({required this.error, this.onReintentar});

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: colores.errorContainer, borderRadius: BorderRadius.circular(12)),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.warning_amber_rounded, color: colores.onErrorContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(error.mensaje, style: TextStyle(color: colores.onErrorContainer)),
                  if (error.red && onReintentar != null)
                    TextButton(onPressed: onReintentar, child: const Text('Reintentar')),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
