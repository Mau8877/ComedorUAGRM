# UT-CU01-FE-06 — Pantalla de registro (/registro)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-06` |
| **Nombre** | Pantalla de registro (/registro) |
| **Caso de uso** | CU01-FE |
| **Depende de** | `UT-CU01-FE-02`, `UT-CU01-FE-03`, `UT-CU01-FE-05` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`RUTAS_NAVEGACION_FRONTEND.md`](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Un estudiante debe poder crear su cuenta de Postulante desde la web.

## Objetivo

La ruta `/registro` muestra el formulario de registro y lleva al Postulante a su módulo al terminar.

## Alcance

- `src/features/auth/screens/RegistroScreen.tsx`: `PublicHeader` + tarjeta con el formulario (TanStack Form + `registroSchema`), con los campos del README; `carreraCodigo` es un `Select` de shadcn alimentado por `useCarreras` (con estado de carga y de error); botón «Crear cuenta» deshabilitado mientras envía (A7); enlace «¿Ya tienes cuenta? Inicia sesión».
- Errores **inline y solo en campos tocados**; los conflictos del backend (`ci`, `correo`, `registroUniversitario`) y los de Firebase se muestran en su campo; errores de red con mensaje y reintento (A9).
- Al terminar bien: `navigate` a `rutaInicioPorRol(rol)` (`UT-CU01-FE-05`).
- `src/routes/registro.tsx`: monta la pantalla; si ya hay sesión, redirige al módulo (se completa en `UT-CU01-FE-08`).
- Sin selector de rol ni ninguna forma de crear cuentas del personal (CA16).
- Estilos solo con tokens del tema, sin valores arbitrarios.
- Test de componente (Vitest + Testing Library, acciones simuladas): muestra errores solo tras tocar el campo; los conflictos aparecen en su campo; el botón se deshabilita al enviar.

Archivos esperados (crear): `RegistroScreen.tsx`, `screens/index.ts`, `routes/registro.tsx` y el test.

## Fuera de alcance (no tocar)

- La pantalla de login (`UT-CU01-FE-07`).
- Pedir o validar el formato del CI y del registro universitario.
- Verificación de correo.

## Resultado esperado

Un Postulante puede registrarse desde la web y entrar a su módulo.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Con el stack y Firebase configurados: completar el formulario crea la cuenta, entra a `/panel` y existe la fila en `usuario` y `postulante`.
3. Repetir con el mismo CI muestra el error en el campo CI sin crear otra cuenta.
