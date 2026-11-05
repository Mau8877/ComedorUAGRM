# UT-CU15-FE-06 — Actualizar los datos de contacto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-06` |
| **Nombre** | Actualizar los datos de contacto |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-01`, `UT-CU15-FE-02`, `UT-CU15-FE-05` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

El usuario mantiene al día su correo y, si es Postulante, su teléfono, para recibir las notificaciones; cambiar el correo exige confirmar que es él.

## Objetivo

La tarjeta «Datos de contacto» del perfil actualiza el correo y el teléfono con las protecciones del CU.

## Alcance

- `F/features/perfil/components/DatosContactoForm.tsx` (TanStack Form + `crearActualizarPerfilSchema`): campo «Correo electrónico» y, solo si el perfil tiene `postulante`, «Teléfono»; valores iniciales del perfil. Si el correo se edita y es distinto del actual aparece el campo «Contraseña actual» (obligatorio, `type="password"`, `autoComplete="current-password"`) con la nota «Por seguridad, confirma tu contraseña para cambiar el correo» (A7); si vuelve al valor original, desaparece. Botón «Guardar cambios» deshabilitado si no hay cambios o mientras guarda; manda solo lo que cambió (`aActualizarPerfilRequest`).
- Resultados: éxito → aviso «Datos actualizados» y los valores se refrescan con la respuesta (la contraseña actual se limpia); `ERR_US_02` → error en el campo correo (A5); `ERR_AUTH_09` → error en «Contraseña actual» (A6); `ERR_SYS_01`/`ERR_US_13` → mensaje del backend sobre el formulario; red → conexión con «Reintentar» (A10). Nada se pierde de lo escrito ante un error.
- Los errores inline solo aparecen en campos tocados.
- La contraseña actual no se guarda fuera del estado del formulario ni se escribe en la consola.
- `PerfilScreen.tsx`: monta el componente en la segunda sección.
- Tests: el Postulante ve teléfono y el Administrador no; cambiar solo el teléfono no pide contraseña y manda solo `telefono`; cambiar el correo muestra y exige la contraseña actual; `409` marca el correo; `ERR_AUTH_09` marca la contraseña actual; sin cambios el botón está deshabilitado; éxito invalida las claves del perfil.

Archivos esperados (crear): `DatosContactoForm.tsx` y su test. (Tocar): `PerfilScreen.tsx` y el barrel de `components`.

## Fuera de alcance (no tocar)

- Editar el CI, nombres, apellidos, registro universitario o carrera.
- Verificar el correo nuevo con un enlace.
- El formulario de contraseña (UT-07).

## Resultado esperado

El usuario cambia su teléfono y su correo y el sistema rechaza cada caso de A5 a A7 con un mensaje en el campo correcto.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
