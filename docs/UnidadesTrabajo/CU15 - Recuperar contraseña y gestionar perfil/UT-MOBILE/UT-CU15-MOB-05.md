# UT-CU15-MOB-05 — Actualizar los datos de contacto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-05` |
| **Nombre** | Actualizar los datos de contacto |
| **Caso de uso** | CU15-MOB |
| **Depende de** | `UT-CU15-MOB-02`, `UT-CU15-MOB-04` |

## Antes de implementar

- [`FORMULARIOS_MOBILE.md`](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md)
- [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md)
- Como referencia: `M/features/auth/auth_validators.dart`

## Solicitud de negocio

El usuario mantiene al día su correo y, si es Postulante, su teléfono; cambiar el correo exige confirmar que es él.

## Objetivo

La sección «Datos de contacto» del perfil actualiza correo y teléfono con las protecciones del CU.

## Alcance

- `M/features/perfil/widgets/datos_contacto_form.dart`: `Form` con «Correo electrónico» y, solo si el perfil tiene `postulante`, «Teléfono» (obligatorio, hasta 20 caracteres), con los valores del perfil. Si el correo se edita y es distinto del actual aparece «Contraseña actual» (obligatoria, oculta, `autofillHints` de contraseña) con la nota «Por seguridad, confirma tu contraseña para cambiar el correo» (A7); si vuelve al valor original desaparece. «Guardar cambios» está deshabilitado sin cambios y mientras guarda; manda solo lo que cambió.
- Resultados: éxito → `SnackBar` «Datos actualizados» y el formulario toma los valores nuevos (la contraseña actual se limpia); `ERR_US_02` → error en el campo correo (A5); `ERR_AUTH_09` → error en «Contraseña actual» (A6); `ERR_SYS_01`/`ERR_US_13` → mensaje del backend sobre el formulario; red → conexión con «Reintentar» (A10). Lo escrito no se pierde ante un error.
- Los validadores de correo y teléfono salen de `auth_validators.dart` (mismas reglas que en el registro y en la web).
- La contraseña actual no se guarda fuera del estado del formulario ni se imprime.
- `PerfilVista`: monta el formulario en su segunda sección.
- Tests: lógica de «qué cambió» (solo teléfono → no pide contraseña; correo distinto → la exige; correo igual al original → no); el cuerpo enviado omite lo que no cambió; el mapeo marca el campo correcto para cada error.

Archivos esperados (crear): `datos_contacto_form.dart` y su test de lógica. (Tocar): `perfil_vista.dart`.

## Fuera de alcance (no tocar)

- Editar el CI, nombres, apellidos, registro universitario o carrera.
- Verificar el correo nuevo con un enlace.

## Resultado esperado

El usuario cambia su teléfono y su correo, y la app rechaza cada caso de A5 a A7 con un mensaje en el campo correcto.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
