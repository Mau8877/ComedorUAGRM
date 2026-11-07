# Errores — Comunicados (`COM`)

Errores de la publicación de comunicados (CU21).

## `ERR_COM_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Los destinatarios no están soportados (`ADJUDICADOS`, todavía no habilitado) o el grupo elegido no tiene a nadie; no se publica nada.
- **Dónde se lanza:** `ComunicadoService.publicar()`

## `ERR_COM_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Un comunicado dirigido a `FINALISTAS` no indicó la convocatoria.
- **Dónde se lanza:** `ComunicadoService.publicar()`

