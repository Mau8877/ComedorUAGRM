# UT-CU15-FE-05 — Ruta `/perfil`, consulta del perfil y acceso desde el header

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-FE-05` |
| **Nombre** | Ruta `/perfil`, consulta del perfil y acceso desde el header |
| **Caso de uso** | CU15-FE |
| **Depende de** | `UT-CU15-FE-02` |

## Antes de implementar

- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- Como referencia: `F/routes/_authenticated/panel.tsx` (cómo se elige el layout por rol) y `F/components/layout/AppHeader.tsx`

## Solicitud de negocio

Cada usuario encuentra su perfil desde el menú de su cuenta y ve sus datos, con el CI como dato que no se puede modificar.

## Objetivo

`/perfil` muestra los datos del usuario dentro del layout de su rol, y el menú del header lleva hasta ahí.

## Alcance

- `F/routes/_authenticated/perfil.tsx`: ruta privada. Si el rol no tiene layout web (`DIRECTIVO`, `PERSONAL_COMEDOR`) redirige a `/panel`.
- `F/features/perfil/screens/PerfilScreen.tsx`: elige `AdminLayout` o `EstudianteLayout` según el rol (mismo criterio que `/panel`), usa `usePerfil` y muestra tres secciones: **Mis datos** (solo lectura: nombres, apellidos, CI, rol y estado y, si es Postulante, registro universitario y carrera con su facultad), y los espacios donde irán **Datos de contacto** y **Cambiar contraseña** (componentes de UT-06 y UT-07; mientras no existen, solo la primera sección). El CI se ve como texto bloqueado (campo `disabled` con una nota «El CI no se puede modificar», A4), nunca como un campo editable. Estados de carga y de error de red con «Reintentar» (A10); un `401` lo resuelve el flujo de sesión existente.
- `F/components/layout/AppHeader.tsx`: nuevas props opcionales `onPerfil` y `onCambiarContrasena`; los ítems «Mi perfil» y «Cambiar contraseña» del menú de usuario las ejecutan (hoy no hacen nada). `AdminLayout`/`EstudianteLayout` (y `AppShell` si hace falta) las pasan hacia abajo como ya hacen con `onCerrarSesion`.
- Las pantallas que arman el layout (`/panel` y `/perfil`) pasan `onPerfil` (`navigate('/perfil')`) y `onCambiarContrasena` (`navigate('/perfil', { hash: 'cambiar-contrasena' })`).
- `F/layouts/estudiante/estudianteNavItems.ts`: el ítem «Perfil» apunta a `/perfil` (hoy es `/perfil` sin ruta real).
- Tests: el perfil de un Postulante muestra los datos con registro y carrera; el de un Administrador no muestra esos campos; el CI no es editable; un Directivo es redirigido a `/panel`; elegir «Mi perfil» en el header navega a `/perfil`; error de red muestra el reintento.

Archivos esperados (crear): `routes/_authenticated/perfil.tsx`, `PerfilScreen.tsx`, el barrel `screens/index.ts` y su test. (Tocar): `AppHeader.tsx`, los layouts, `panel.tsx` y `estudianteNavItems.ts`.

## Fuera de alcance (no tocar)

- Los formularios de contacto y de contraseña (UT-06 y UT-07).
- Layout para Directivo y Personal del Comedor.
- Editar nombres, apellidos, registro universitario o carrera.

## Resultado esperado

Cualquier Administrador o Postulante llega a su perfil desde el menú y ve sus datos.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
