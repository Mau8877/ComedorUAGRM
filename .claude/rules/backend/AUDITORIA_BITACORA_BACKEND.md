# Auditoría / Bitácora — Backend

La bitácora (`bitacora`, append-only) registra los eventos de negocio que se
quieren poder auditar (CU25 los consulta). Un evento se anota **una sola vez**,
en la capa de `service`, igual que el logging (ver
[LOGGING_BACKEND.md](LOGGING_BACKEND.md#dónde-se-loguea)).

## Cómo se anota: `@Auditar`

Se anota el método de `service` que produce el evento:

```java
@Transactional
@Auditar(accion = BitacoraAcciones.INICIO_SESION, entidad = "usuario")
public SesionAbierta abrir(Usuario usuario) { ... }
```

- El método tiene que devolver un `Auditable` (`common/auditoria/`): de ahí
  salen el actor, la entidad afectada (`entidadId()`, por defecto el actor) y
  el `detalle()`. Lo normal es que el tipo de retorno interno del service (un
  `record`) implemente la interfaz; un DTO de respuesta no, porque no lleva
  ids internos.
- `auditar()` devuelve `false` cuando esta vez no hay nada que anotar (ej. un
  registro repetido que no crea la cuenta).
- `AuditoriaAspect` (`features/bitacora/`) anota solo si el método **termina
  bien**, dentro de la transacción de quien llama (un rollback deshace la
  fila), y toma la IP del request en curso: **no se pasa `ip` por parámetro**.
- La acción es una constante de `BitacoraAcciones`; no se escriben strings
  sueltos.

## Cuándo NO sirve la anotación

- **Llamadas dentro de la misma clase.** Spring AOP intercepta por proxy, solo
  las llamadas entre beans. Un método que llama a otro de su propia clase no
  lo audita (por eso `SesionService.renovar` → `abrir` no anota un acceso nuevo,
  y es lo que se quiere). Se anota el método **público** que otro bean invoca.
- El método no devuelve un `Auditable`, o el evento necesita datos que no
  están en el resultado. En ese caso se llama a mano:
  `bitacoraService.registrar(usuario, accion, entidad, entidadId, detalle, ip)`.

## Cuándo agregar otra anotación

Si un patrón de auditoría se repite en 2 o más métodos y `@Auditar` no lo
expresa bien (ej. anotar sobre un id que viene en el argumento y no en el
resultado, o anotar también cuando el método falla), se agrega una anotación
nueva en vez de repetir llamadas manuales o torcer `@Auditar`:

- La anotación va en `common/auditoria/` y su advice en `features/bitacora/`
  (el mismo `AuditoriaAspect` o un aspecto hermano), siempre delegando en
  `BitacoraService`: ningún otro código inserta filas en `bitacora`.
- Se documenta acá con su regla de cuándo usarla, y se cubre con su propio
  test unitario (igual que `AuditoriaAspectTest`) más uno de integración.
- No se crea de forma anticipada: se agrega cuando aparece el segundo caso
  real, no "por si hace falta".
- Se agrega en la UT del caso de uso que la necesita, no dentro de otro CU.

## Qué no se anota ni se guarda

- Los intentos fallidos de inicio de sesión no van a la bitácora: se cuentan
  en `usuario.intentos_fallidos` (CU01).
- Nunca contraseñas, tokens ni sus hashes en `detalle` (ver
  [LOGGING_BACKEND.md](LOGGING_BACKEND.md#nunca-loguear)).

## Tests

- Un método nuevo con `@Auditar` se prueba con una base real (el test de
  integración del CU): que la fila quede con la acción, el actor, la entidad,
  el detalle y la IP esperados, y que **no** quede cuando el método falla.
- El comportamiento general del aspecto lo cubre `AuditoriaAspectTest`; no se
  repite en cada service.
