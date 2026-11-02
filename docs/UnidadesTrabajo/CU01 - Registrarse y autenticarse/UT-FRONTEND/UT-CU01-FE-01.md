# UT-CU01-FE-01 — SDK de Firebase, configuración y variables de entorno

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-01` |
| **Nombre** | SDK de Firebase, configuración y variables de entorno |
| **Caso de uso** | CU01-FE |
| **Depende de** | — |

## Antes de implementar

- [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)
- [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)
- [`SEGURIDAD_AUTH_BACKEND.md`](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

El inicio de sesión lo hace Firebase: la web necesita su SDK inicializado con la configuración del proyecto.

## Objetivo

Existe `getFirebaseAuth()` listo para usar y las variables `VITE_FIREBASE_*` documentadas.

## Alcance

- `pnpm add firebase` en `projects/frontend/comedoruagrm` (actualiza `pnpm-lock.yaml`).
- `src/features/auth/api/firebase.ts`: inicializa la app una sola vez con `import.meta.env.VITE_FIREBASE_*` y exporta `auth` (`getAuth`). Si falta alguna variable, lanza un error claro en español que nombre la variable.
- `src/features/auth/index.ts` (barrel, por ahora exporta solo lo que necesite el resto de la app).
- `.env.example`: las cuatro variables `VITE_FIREBASE_*` con un comentario de dónde se obtienen (consola de Firebase, "Configuración del proyecto").
- `src/vite-env.d.ts` (o equivalente): tipos de las cuatro variables.
- Test `src/features/auth/tests/firebase.test.ts`: con variables simuladas inicializa; sin ellas lanza el error esperado.

Archivos esperados (crear/tocar): `package.json`, `pnpm-lock.yaml`, `firebase.ts`, `auth/index.ts`, `.env.example`, tipos de env, test.

## Fuera de alcance (no tocar)

- Funciones de registro, login y logout (`UT-CU01-FE-05`).
- Crear el proyecto de Firebase.
- Cualquier uso de Firebase Storage, FCM o Analytics.

## Resultado esperado

El resto de la feature puede importar `auth` de Firebase ya configurado.

## Validación

1. `pnpm test`, `pnpm lint` y `pnpm build` pasan.
2. Sin las variables en `.env`, `pnpm dev` muestra un error que dice cuál falta.
