# Errores — Sistema (`SYS`)

Códigos transversales, no ligados a ningún feature de negocio puntual (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)
para el criterio de cuándo un código va acá en vez de en el módulo de un
feature).

## `ERR_SYS_00`

- **HTTP:** 500
- **Excepción:** (catch-all, no es una `BusinessException`)
- **Significado:** Error interno no controlado — cualquier excepción no
  mapeada explícitamente, o una falla de infraestructura (base de datos
  caída, etc.).
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`

## `ERR_SYS_01`

- **HTTP:** 400
- **Excepción:** `ValidationException` (genérica, no de un módulo puntual)
- **Significado:** Falló una validación automática de `@Valid`/Bean
  Validation sobre un DTO de request (campo obligatorio faltante, formato
  inválido, etc.) — no una regla de negocio, esa se cataloga con su propio
  código en el módulo que corresponda.
- **Dónde se lanza:** `GlobalExceptionHandler.handleValidationException()`

## `ERR_SYS_03`

- **HTTP:** 404
- **Excepción:** ninguna de negocio — `NoResourceFoundException` del framework
- **Significado:** La ruta pedida no existe (ningún controller la atiende).
  No es el `NotFoundException` de un recurso de negocio inexistente.
- **Dónde se lanza:** `GlobalExceptionHandler.handleRutaInexistente()`

## `ERR_SYS_04`

- **HTTP:** 405
- **Excepción:** ninguna de negocio — `HttpRequestMethodNotSupportedException` del framework
- **Significado:** La ruta existe pero no admite el verbo HTTP usado
  (ej. `DELETE /api/v1/carreras`).
- **Dónde se lanza:** `GlobalExceptionHandler.handleMetodoNoPermitido()`

## `ERR_SYS_05`

- **HTTP:** 400
- **Excepción:** ninguna de negocio — `HttpMessageNotReadableException`,
  `MethodArgumentTypeMismatchException` o `MissingServletRequestParameterException`
- **Significado:** El request no se puede interpretar: JSON ilegible o
  vacío, un parámetro con tipo inválido (ej. `page=abc`) o un parámetro
  obligatorio ausente. Distinto de `ERR_SYS_01`, que es validación de
  campos de un DTO ya leído.
- **Dónde se lanza:** `GlobalExceptionHandler.handleRequestMalformado()`

## `ERR_SYS_02`

- **HTTP:** 429
- **Excepción:** ninguna — lo escribe `RateLimitFilter` directo, antes de
  que exista un `@RestControllerAdvice` que pueda intervenir (ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).
- **Significado:** Se superó el límite de requests por IP configurado en
  `app.ratelimit.capacity` / `app.ratelimit.refill-per-minute`.
- **Dónde se lanza:** `RateLimitFilter.writeTooManyRequests()`
