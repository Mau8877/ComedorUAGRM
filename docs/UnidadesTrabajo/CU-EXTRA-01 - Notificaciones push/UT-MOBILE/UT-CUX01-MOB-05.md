# UT-CUX01-MOB-05 — Pruebas y verificación de punta a punta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CUX01-MOB-05` |
| **Nombre** | Pruebas y verificación de punta a punta |
| **Caso de uso** | CU-EXTRA-01-MOB |
| **Depende de** | UT-CUX01-MOB-03, UT-CUX01-MOB-04 |

## Antes de implementar

- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (se testea lo crítico, no cada pantalla)
- [`docs/README.md`](../../../README.md), sección «Notificaciones push (Firebase)» (`UT-CUX01-BE-01`)

## Solicitud de negocio

Confirmar que lo construido funciona de verdad con Firebase real, y que la parte con lógica queda cubierta por pruebas automáticas.

## Objetivo

Las pruebas automáticas están en verde y existe una verificación manual de punta a punta documentada, hecha con el aviso de prueba del backend.

## Alcance

- Revisar que las pruebas de `MOB-02`, `MOB-03` y `MOB-04` corren juntas con `flutter test` y que no dependen de Firebase ni de red.
- Verificación manual (se anota el resultado en el PR o en el commit, no en un archivo nuevo):
  1. Backend con `secrets/firebase-sa.json`; app con `google-services.json` del **mismo** proyecto de Firebase.
  2. Iniciar sesión como Administrador DUBSS en un teléfono o emulador con Google Play: se concede el permiso y aparece una fila en `dispositivo_fcm`.
  3. `POST /api/v1/notificaciones/prueba` con la sesión del administrador: llega el push (app abierta, en segundo plano y cerrada) y el correo en `http://localhost:8025`.
  4. Cerrar sesión: la fila de `dispositivo_fcm` queda eliminada y un nuevo aviso de prueba ya no llega a ese teléfono.
  5. Denegar el permiso: la app funciona, no hay fila.
- Corregir lo que la verificación muestre; no se agregan funcionalidades.

Archivos esperados: solo correcciones puntuales si la verificación las exige.

## Fuera de alcance (no tocar)

- Pruebas de widgets de pantallas triviales (no se exigen en mobile).
- Pruebas en iOS.

## Resultado esperado

Los 16 criterios de aceptación del CU relevantes para mobile se pueden dar por verificados.

## Validación

1. `flutter test` y `flutter analyze` pasan.
2. Los 5 pasos manuales anteriores se cumplen y se informa el resultado.
