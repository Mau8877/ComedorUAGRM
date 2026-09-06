# UT-HU02-BE-01 — Código de error `ERR_AUTH_04`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-HU02-BE-01` |
| **Nombre** | Código de error `ERR_AUTH_04` (sesión expirada/refresh inválido) |
| **Historia** | HU02-BE |
| **Depende de** | `features/auth/AuthErrorCodes.java` (ya existe, creado por HU01-BE) |

## Antes de implementar

Leer [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
completo.

## Solicitud de negocio

El endpoint de refresh necesita un código de error propio para "sesión
expirada" — no corresponde reusar `ERR_AUTH_01` (credenciales inválidas),
son situaciones semánticamente distintas.

## Objetivo

Agregar `ERR_AUTH_04` al catálogo existente del módulo `AUTH`, en Java y
en `docs/errors/`.

## Alcance

### `AuthErrorCodes` — **editar** `features/auth/AuthErrorCodes.java`

Agregar una constante nueva al archivo que ya existe (no se recrea el
archivo entero):

```java
String SESION_EXPIRADA = "ERR_AUTH_04";
```

### Documentación — **editar** `docs/errors/auth/ERRORES_AUTH.md`

Agregar la sección nueva al final del archivo existente:

```markdown
## `ERR_AUTH_04`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** El refresh token está ausente, tiene un formato
  inválido, no existe/fue revocado en Redis, o pertenece a una cuenta que
  ya no está activa.
- **Dónde se lanza:** `AuthService.refresh()`
```

Y agregar el link a `docs/errors/README.md`, en la misma sección
`### Autenticación (AUTH)` que ya creó HU01-BE (no se crea una sección
nueva, se le suma un ítem).

### Archivos esperados (editar — ambos ya existen)

- `features/auth/AuthErrorCodes.java`
- `docs/errors/auth/ERRORES_AUTH.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Uso real de este código (UT-HU02-BE-03 — ahí se **lanza**, no se define)
- `ERR_AUTH_01`/`02`/`03` (ya existentes, no se tocan)

## Resultado esperado

`AuthErrorCodes.SESION_EXPIRADA` disponible, documentado en `docs/errors/`.

## Validación

1. `ERR_AUTH_04` aparece en `docs/errors/auth/ERRORES_AUTH.md` con las 4
   líneas obligatorias.
2. `docs/errors/README.md` enlaza a la nueva sección.
3. Los códigos `ERR_AUTH_01`-`03` de HU01-BE siguen intactos en ambos
   archivos.
