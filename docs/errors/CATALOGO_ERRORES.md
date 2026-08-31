# Catálogo de códigos de error

Registro vivo de todos los códigos de error (`ERR_{MODULO}_{NUMERO}`) que
la API puede devolver, con su significado y de dónde salen. El **proceso**
para agregar o actualizar una fila acá vive en
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
— este archivo es solo el resultado, no repite las reglas de ese proceso.

Formato del código y jerarquía de excepciones asociada:
[EXCEPCIONES_BACKEND.md](../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Sistema (`SYS`)

| Código       | HTTP | Excepción                                        | Significado                                                                                                                              | Dónde se lanza                                       |
| ------------ | ---- | ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `ERR_SYS_00` | 500  | (catch-all)                                      | Error interno no controlado — cualquier excepción no mapeada explícitamente, o falla de infraestructura (DB, etc.)                       | `GlobalExceptionHandler.handleUnexpectedException()` |
| `ERR_SYS_01` | 400  | `ValidationException` (genérica)                 | Falló una validación automática de `@Valid`/Bean Validation sobre un DTO de request (campo obligatorio faltante, formato inválido, etc.) | `GlobalExceptionHandler.handleValidationException()` |
| `ERR_SYS_02` | 429  | (ninguna — lo escribe `RateLimitFilter` directo) | Se superó el límite de requests por IP configurado en `app.ratelimit.*`                                                                  | `RateLimitFilter.writeTooManyRequests()`             |

## Módulos de negocio

Todavía no hay ningún módulo de negocio implementado
