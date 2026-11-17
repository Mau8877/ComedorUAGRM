import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/services.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:image_picker/image_picker.dart';

import '../models/archivo_local.dart';

/// El usuario no dio permiso para usar la cámara (A1 del CU16): la pantalla le ofrece elegir el documento de la
/// galería o de sus archivos.
class PermisoCamaraDenegado implements Exception {
  const PermisoCamaraDenegado();
}

/// Captura o elige un documento en el teléfono (CU16): foto con la cámara, imagen de la galería, PDF de los archivos, y
/// recorte/giro de una imagen. Envuelve a los plugins para que las pantallas no dependan de ellos y los tests lo
/// sustituyan (`capturaDocumentoServiceProvider`).
///
/// * Las fotos se reducen al elegirlas (ancho máximo y calidad JPEG) para que pesen menos que el máximo permitido.
/// * Cada método devuelve `null` si la persona cancela.
class CapturaDocumentoService {
  static const _anchoMaximo = 2200.0;
  static const _calidad = 85;

  final ImagePicker _picker;
  final ImageCropper _cropper;

  CapturaDocumentoService({ImagePicker? picker, ImageCropper? cropper})
      : _picker = picker ?? ImagePicker(),
        _cropper = cropper ?? ImageCropper();

  /// Una foto con la cámara del teléfono. Sin permiso de cámara lanza [PermisoCamaraDenegado].
  Future<ArchivoLocal?> tomarFoto() async {
    try {
      final foto = await _picker.pickImage(
        source: ImageSource.camera,
        maxWidth: _anchoMaximo,
        imageQuality: _calidad,
      );
      return foto == null ? null : await _deRuta(foto.path, foto.name);
    } on PlatformException catch (e) {
      if (e.code == 'camera_access_denied') throw const PermisoCamaraDenegado();
      rethrow;
    }
  }

  /// Una imagen de la galería.
  Future<ArchivoLocal?> elegirDeGaleria() async {
    final imagen = await _picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: _anchoMaximo,
      imageQuality: _calidad,
    );
    return imagen == null ? null : _deRuta(imagen.path, imagen.name);
  }

  /// Un PDF de los archivos del teléfono.
  Future<ArchivoLocal?> elegirPdf() async {
    final archivo = await FilePicker.pickFile(type: FileType.custom, allowedExtensions: const ['pdf']);
    final ruta = archivo?.path;
    return ruta == null ? null : _deRuta(ruta, archivo!.name);
  }

  /// Abre el recortador (con giro) sobre una imagen. Devuelve la imagen recortada, o `null` si se cancela.
  Future<ArchivoLocal?> recortar(ArchivoLocal imagen) async {
    final recortada = await _cropper.cropImage(
      sourcePath: imagen.ruta,
      compressQuality: _calidad,
      uiSettings: [
        AndroidUiSettings(toolbarTitle: 'Recortar documento', lockAspectRatio: false),
        IOSUiSettings(title: 'Recortar documento'),
      ],
    );
    return recortada == null ? null : _deRuta(recortada.path, imagen.nombre);
  }

  Future<ArchivoLocal> _deRuta(String ruta, String nombre) async {
    final tamano = await File(ruta).length();
    final nombreFinal = nombre.isEmpty ? ruta.split(RegExp(r'[\\/]')).last : nombre;
    return ArchivoLocal(
      ruta: ruta,
      nombre: nombreFinal,
      mimeType: ArchivoLocal.mimePorNombre(nombreFinal),
      tamanoBytes: tamano,
    );
  }
}
