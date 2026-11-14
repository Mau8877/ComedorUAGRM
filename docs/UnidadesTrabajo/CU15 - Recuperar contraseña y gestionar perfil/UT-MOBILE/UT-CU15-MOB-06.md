# UT-CU15-MOB-06 — Cambiar la contraseña desde el perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-06` |
| **Nombre** | Cambiar la contraseña desde el perfil |
| **Caso de uso** | CU15-MOB |
| **Depende de** | `UT-CU15-MOB-02`, `UT-CU15-MOB-04` |

## Antes de implementar

- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (dónde viven los tokens)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

El usuario cambia su contraseña confirmando la actual, sin perder la sesión del dispositivo en el que está.

## Objetivo

La sección «Cambiar contraseña» del perfil cambia la contraseña y deja abierta esta sesión.

## Alcance

- `M/features/perfil/widgets/cambiar_contrasena_form.dart`: `Form` con «Contraseña actual», «Contraseña nueva» y «Confirmar contraseña nueva» (ocultas, con opción de mostrar, `autofillHints` correctos), con el requisito visible («De 6 a 72 caracteres»). Validadores de `auth_validators.dart` (`contrasena` y `confirmacion`; la actual solo no vacía). Botón «Cambiar contraseña» con indicador de carga.
- Éxito → `SnackBar` «Contraseña actualizada. Cerramos tu sesión en los demás dispositivos.», el formulario se limpia y **la sesión sigue abierta**: no se navega ni se borra `TokenStorage`.
- La acción envía el refresh token guardado en el cuerpo (lo lee `perfilProvider` de `TokenStorage`, UT-02) para que el backend conserve esta sesión. El widget nunca ve ni muestra ese token.
- `ERR_AUTH_09` → error en «Contraseña actual» (A6); `ERR_SYS_01` → mensaje del backend; red → «Reintentar» (A10). Un `400` no cierra la sesión.
- Las contraseñas no se guardan fuera del estado del formulario ni se imprimen.
- `PerfilVista`: monta el formulario en su tercera sección.
- Tests: contraseña corta (5), de 6 y de 73 caracteres, y sin coincidir; la acción llama al servicio con el refresh token guardado; tras el éxito la sesión (`authSesionProvider`) no cambia; `ERR_AUTH_09` se mapea al campo de la contraseña actual.

Archivos esperados (crear): `cambiar_contrasena_form.dart` y su test de lógica. (Tocar): `perfil_vista.dart`.

## Fuera de alcance (no tocar)

- Cerrar la sesión local tras cambiar la contraseña.
- Forzar el cambio de la contraseña temporal del personal.
- Restablecer la contraseña olvidada (se hace en la web).

## Resultado esperado

El usuario cambia su contraseña desde la app y sigue en la misma sesión.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
