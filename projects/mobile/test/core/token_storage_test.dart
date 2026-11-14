import 'package:comedor_uagrm_mobile/core/storage/token_storage.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  setUp(() => FlutterSecureStorage.setMockInitialValues({}));

  test('guarda el refresh token y lo devuelve; el token de acceso queda en memoria', () async {
    final tokens = TokenStorage();

    await tokens.guardarSesion(accessToken: 'acceso', refreshToken: 'refresh');

    expect(tokens.accessToken, 'acceso');
    expect(await tokens.leerRefreshToken(), 'refresh');
  });

  test('el token de acceso nunca se persiste: una instancia nueva solo recupera el refresh', () async {
    await TokenStorage().guardarSesion(accessToken: 'acceso', refreshToken: 'refresh');

    final otraInstancia = TokenStorage();

    expect(otraInstancia.accessToken, isNull);
    expect(await otraInstancia.leerRefreshToken(), 'refresh');
  });

  test('borrarSesion limpia los dos tokens', () async {
    final tokens = TokenStorage();
    await tokens.guardarSesion(accessToken: 'acceso', refreshToken: 'refresh');

    await tokens.borrarSesion();

    expect(tokens.accessToken, isNull);
    expect(await tokens.leerRefreshToken(), isNull);
  });

  test('sin sesión guardada no hay refresh token', () async {
    expect(await TokenStorage().leerRefreshToken(), isNull);
  });
}
