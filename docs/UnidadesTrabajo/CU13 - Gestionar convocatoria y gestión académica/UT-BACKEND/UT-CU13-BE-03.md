# UT-CU13-BE-03 — Models `TipoDocumento` y `ConvocatoriaDocumento` (clave compuesta)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-BE-03` |
| **Nombre** | Models `TipoDocumento` y `ConvocatoriaDocumento` (clave compuesta) |
| **Caso de uso** | CU13-BE |
| **Depende de** | `UT-CU13-BE-02` |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#identificador-de-fila-id-interno--codigo_tabla-público) (tablas sin `id` propio y clave compuesta)
- [`CONVENCIONES_JAVA_BACKEND.md`](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md)
- Migración `V20261107110000__crear_auditoria_expediente_y_tablas_del_expediente.sql` (las dos tablas)

## Solicitud de negocio

Cada convocatoria exige su propia lista de documentos. Esa lista vive en dos tablas que existen pero que ningún código mapea todavía.

## Objetivo

Existen los Models de `tipo_documento` y de `convocatoria_documento` (clave `(convocatoria_id, tipo_documento_id)`), y queda documentada en la rule la forma de mapear una tabla con clave compuesta.

## Alcance

- `B/features/convocatorias/model/TipoDocumento.java`: extiende `BaseModel`, `@Table(name = "tipo_documento")`, `@AttributeOverride(name = "codigo", column = @Column(name = "codigo_tipo_documento"))`, campos `nombre` (100) y `descripcion` (255, nullable). Solo `@Getter` (el catálogo no se modifica en este CU).
- `B/features/convocatorias/model/ConvocatoriaDocumentoId.java`: `@Embeddable`, `Serializable`, con `Long convocatoriaId` y `Long tipoDocumentoId` (columnas `convocatoria_id` y `tipo_documento_id`) y `equals`/`hashCode` sobre los dos.
- `B/features/convocatorias/model/ConvocatoriaDocumento.java`: extiende `BaseModelSinId`, `@Table(name = "convocatoria_documento")`, `@AttributeOverride(name = "codigo", column = @Column(name = "codigo_convocatoria_documento"))`, `@EmbeddedId ConvocatoriaDocumentoId id`, `@ManyToOne(fetch = LAZY) @MapsId("convocatoriaId") Convocatoria convocatoria`, `@ManyToOne(fetch = LAZY) @MapsId("tipoDocumentoId") TipoDocumento tipoDocumento` y `boolean obligatorio` (`@Setter`). Constructor `ConvocatoriaDocumento(Convocatoria, TipoDocumento, boolean obligatorio)` que arma el `id`. Hereda `eliminadoEn` y su setter de `BaseModelSinId`.
- `.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md`: en el apartado «Tablas sin `id` propio», reemplaza «Una tabla con clave compuesta (`convocatoria_documento`) se resolverá en la primera UT que la cree» por la forma resuelta: `BaseModelSinId` + `@EmbeddedId` + `@MapsId` por cada parte, con `codigo_<tabla>` fijado por `@AttributeOverride`; y deja anotado que **la clave compuesta incluye las filas eliminadas lógicamente**, así que volver a agregar un par ya eliminado se hace **reactivando** la fila (`UT-CU13-BE-04`), no insertando otra.
- Tests (`@DataJpaTest` con `support/PostgresTestDatabase`): se guarda y se lee un `ConvocatoriaDocumento` con su clave; al marcar `eliminadoEn` deja de aparecer en la consulta pero la fila sigue en la tabla (consulta nativa).

Archivos esperados (crear/tocar): `TipoDocumento.java`, `ConvocatoriaDocumentoId.java`, `ConvocatoriaDocumento.java`, `PERSISTENCIA_BD_BACKEND.md` y sus tests.

## Fuera de alcance (no tocar)

- Repositorios y consultas (`UT-CU13-BE-04`).
- Crear o editar tipos de documento (el catálogo se carga en otro CU) y la tabla `documento_adjunto` (CU16).
- Migraciones: las tablas ya existen.
- Las consultas nativas del CU07 que ya leen estas tablas no se tocan.

## Resultado esperado

El código puede leer y escribir la lista de documentos de una convocatoria con los Models, y la rule de persistencia dice cómo se mapea una clave compuesta.

## Validación

1. `./mvnw test` pasa (con Docker disponible el test de base real corre; si no, queda `SKIPPED`).
2. `./mvnw checkstyle:check` sin errores.
3. El texto nuevo de `PERSISTENCIA_BD_BACKEND.md` ya no dice «se resolverá».
