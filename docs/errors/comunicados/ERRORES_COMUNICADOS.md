# Errores — Comunicados (`COM`)

Errores de la publicación de comunicados (CU21).

## `ERR_COM_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El grupo de destinatarios elegido no tiene a nadie; no se publica nada.
- **Dónde se lanza:** `ComunicadoService.publicar()`

## `ERR_COM_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Un comunicado dirigido a `FINALISTAS` o a `ADJUDICADOS` no indicó la convocatoria.
- **Dónde se lanza:** `ComunicadoService.publicar()`

