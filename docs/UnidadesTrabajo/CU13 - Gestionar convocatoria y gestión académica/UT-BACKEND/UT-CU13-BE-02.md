# UT-CU13-BE-02 — `GestionAcademica` y `Convocatoria`: constructores y setters

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-02` |
| **Nombre** | `GestionAcademica` y `Convocatoria`: constructores y setters |
| **Caso de uso** | CU13-BE |
| **Depende de** | — |

## Antes de implementar

- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md#lombok) (`@Setter` por campo, nunca `@Data`)
- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

Los dos Models se crearon de solo lectura para el CU06. Este CU es el que crea y modifica gestiones y convocatorias, así que es el que les agrega cómo construirlas y qué campos se pueden cambiar.

## Objetivo

`GestionAcademica` y `Convocatoria` se pueden crear y modificar desde un `service`, sin exponer más setters de los necesarios.

## Alcance

- `B/features/convocatorias/model/GestionAcademica.java`: constructor público `GestionAcademica(String etiqueta, LocalDate fechaInicio, LocalDate fechaFin)` y `@Setter` **por campo** en `etiqueta`, `fechaInicio` y `fechaFin`. Se actualiza el comentario de clase («solo lectura por ahora»).
- `B/features/convocatorias/model/Convocatoria.java`: constructor público `Convocatoria(GestionAcademica gestion, Long creadoPor, String nombre, Instant fechaApertura, Instant fechaCierre, Integer cupos, BigDecimal puntajeMinimo, Integer cantidadFinalistas)` que deja `estado = BORRADOR`; `@Setter` por campo en `gestion`, `nombre`, `fechaApertura`, `fechaCierre`, `cupos`, `puntajeMinimo`, `cantidadFinalistas` y `estado`. **No** hay setter de `creadoPor` (lo fija el constructor). Se actualizan los comentarios que decían «solo lectura» y «solo tiene getter» (`cantidadFinalistas` ahora la escribe este CU).
- Se usa el `setEliminadoEn` que ya trae `BaseModel` para el borrado lógico; no se agrega otro.
- Tests (`T/features/convocatorias/model/`): el constructor deja `estado = BORRADOR`; los setters cambian solo el campo indicado.

Archivos esperados (crear/tocar): `GestionAcademica.java`, `Convocatoria.java` y su test.

## Fuera de alcance (no tocar)

- Los Models `TipoDocumento` y `ConvocatoriaDocumento` (`UT-CU13-BE-03`).
- Validar fechas o estados dentro del Model: las reglas viven en los `service`.
- El enum `EstadoConvocatoria` y las migraciones: no cambian.
- Quien ya lee estos Models (CU05, CU06, CU08, CU12, CU22) no se modifica.

## Resultado esperado

Un `service` puede hacer `new Convocatoria(...)` y cambiar sus campos editables; el resto del sistema sigue leyéndola igual.

## Validación

1. `./mvnw test` pasa, incluidos los tests de los CU que ya usan estos Models.
2. `./mvnw checkstyle:check` sin errores.
