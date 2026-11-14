import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Guarda la sesión del dispositivo: el refresh token en el almacenamiento
/// seguro del sistema (Keystore en Android, Keychain en iOS) y el token de
/// acceso solo en memoria (nunca se persiste).
///
/// * Es el único lugar de la app que toca `flutter_secure_storage` (ver
/// * SEGURIDAD_AUTH_BACKEND.md). Al abrir la app se restaura la sesión
/// * canjeando el refresh token por un token de acceso nuevo.
/// ! Ninguno de los dos tokens se loguea.
class TokenStorage {
  static const _claveRefresh = 'comedoruagrm.refresh_token';

  /// Instancia compartida de la app; `core/network` la usa sin depender de `features/`.
  static final TokenStorage instance = TokenStorage();

  final FlutterSecureStorage _storage;

  TokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  /// Token de acceso vigente, o `null` si no hay sesión abierta en memoria.
  String? accessToken;

  Future<String?> leerRefreshToken() => _storage.read(key: _claveRefresh);

  Future<void> guardarSesion({required String accessToken, required String refreshToken}) async {
    this.accessToken = accessToken;
    await _storage.write(key: _claveRefresh, value: refreshToken);
  }

  Future<void> borrarSesion() async {
    accessToken = null;
    await _storage.delete(key: _claveRefresh);
  }
}
