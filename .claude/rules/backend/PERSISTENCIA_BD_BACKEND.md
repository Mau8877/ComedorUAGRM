# Persistencia / Base de Datos — Backend

## Fuente de verdad del modelo de datos

El modelo de datos de este proyecto **ya está diseñado** en la documentación
de grado (Taller de Grado I, carpeta fuera de este repositorio):

`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`

| Qué | Dónde (dentro de esa carpeta) | Rol |
| --- | --- | --- |
| Diseño lógico de la base de datos (31 tablas, columnas, tipos, UQ/FK, cardinalidades) | `images/091_Diseno_Logico_Base_de_Datos.jpg` | **Manda.** Es el diagrama vigente. |
| Propuesta textual del diseño (criterios, columnas comunes, índices, enums, preguntas abiertas) | `diseno_base_de_datos.md` | Borrador de apoyo: explica el porqué del diagrama y los índices. Si contradice a la imagen 091, gana la imagen. |
| Diagrama de clases / modelo de dominio | `images/090_Diagrama_de_Clases.jpg`, `images/018_Modelo_Dominio.png` | Contexto de negocio. |

[`docs/DiagramaConceptual.mermaid`](../../../docs/DiagramaConceptual.mermaid)
es la **transcripción en este repositorio** de la imagen 091 (para tenerla
a mano y versionada junto al código). Debe coincidir con la imagen: si el
diseño lógico cambia en la documentación de grado, se actualiza ese archivo
en el mismo cambio.

**Antes de crear un Model/tabla nuevo** (o de diseñar la Unidad de Trabajo
que lo hace, ver [UNIDADES_DE_TRABAJO.md](../UNIDADES_DE_TRABAJO.md)) se
revisa la entidad en ese diagrama: sus columnas, tipos, relaciones y
nombres exactos (`ci` y no `cedula`, `password_hash`, `eliminado_en`, etc.)
son los que se implementan, no una versión "razonable" inventada a partir
del nombre de la entidad o del caso de uso.

Si una entidad de negocio nueva **no** está en el diagrama, se agrega
primero a la documentación de grado y a `DiagramaConceptual.mermaid` (aunque
sea un boceto mínimo) y recién después se crea el Model — el diagrama no se
deja desactualizado "para después". Si algo contradice una decisión ya
tomada en otra rule, se resuelve la contradicción explícitamente
(actualizando el diagrama o la rule, lo que corresponda) antes de escribir
código contra cualquiera de los dos — nunca se asume en silencio cuál
"gana".

## Identificador de fila: `id` interno + `codigo_<tabla>` público

**Toda tabla** lleva dos identificadores con roles que no se mezclan:

| Campo | Tipo | Rol |
| --- | --- | --- |
| `id` | `bigint`, `GENERATED ALWAYS AS IDENTITY` | Clave primaria real. **Nunca** se expone en un DTO, una URL ni el cliente — es un detalle interno de persistencia. |
| `codigo_<tabla>` | `uuid` (UUID v7), `UNIQUE NOT NULL` | Identificador **público** de la fila: el único que viaja a la API y al frontend/mobile. Ej.: `codigo_usuario`, `codigo_postulacion`, `codigo_beca`. UUID v7 es ordenable por tiempo, lo que reduce la fragmentación del índice único. |

- El nombre de columna de `id` es siempre `id`. El de `codigo` **es
  específico de cada tabla** (`codigo_<tabla>`), porque `BaseModel` es una
  única clase compartida: cada Model fija el nombre real con
  `@AttributeOverride` sobre el campo heredado:

  ```java
  @Entity
  @Table(name = "usuario")
  @AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
  public class Usuario extends BaseModel {
      // ...
  }
  ```

- Las rutas y respuestas REST usan el código (`GET /api/v1/postulaciones/{codigo}`);
  el backend lo traduce a `id` al consultar. Así no se exponen secuencias
  ni se facilita enumerar recursos (IDOR).
- `JpaRepository<Entidad, Long>` — el tipo de ID del repositorio es siempre
  `Long` (el `id`), nunca `UUID`. Para buscar por el identificador público
  se agrega `findByCodigo(UUID codigo)` en el repositorio de cada Model; no
  se usa `findById(UUID)` para eso.
- **Tablas sin `id` propio.** El diagrama tiene tablas cuya clave es la FK
  (`postulante` → `usuario_id`; `respuesta_socioeconomica` →
  `postulacion_id`) o una clave compuesta (`convocatoria_documento` →
  `convocatoria_id` + `tipo_documento_id`). También llevan
  `codigo_<tabla>` y las columnas comunes de abajo, pero **no** extienden
  `BaseModel` tal cual (su PK no es un `id` generado): se mapean con
  `@MapsId` / `@EmbeddedId`, y la forma exacta se fija en la primera UT que
  cree una de ellas.
  **Ya resuelto para las tablas cuya PK es una FK (`postulante`):** el Model
  extiende `BaseModelSinId` (`common/model/`, mismas columnas comunes que
  `BaseModel` pero sin `id`), declara `@Id @Column(name = "usuario_id") Long`
  y la relación con `@MapsId @OneToOne`, y fija el nombre de la columna
  pública con `@AttributeOverride` (`codigo_postulante`). Una tabla con
  clave compuesta (`convocatoria_documento`) se resolverá en la primera UT
  que la cree.

## Columnas comunes a todas las tablas

Se agregan a **cada** tabla (el diagrama las muestra en todas):

| Columna | Tipo | Nota |
| --- | --- | --- |
| `creado_en` | `timestamptz NOT NULL DEFAULT now()` | No se modifica después de insertar. |
| `actualizado_en` | `timestamptz NOT NULL DEFAULT now()` | Se actualiza en cada `UPDATE` (`@UpdateTimestamp`/trigger). |
| `eliminado_en` | `timestamptz NULL` | `NULL` = fila activa; con valor = eliminada lógicamente. |

- **Borrado lógico siempre.** Ninguna operación de la aplicación hace
  `DELETE` físico; se registra `eliminado_en`. Las consultas filtran
  `eliminado_en IS NULL` (en Hibernate, `@SQLRestriction("eliminado_en IS NULL")`
  en `BaseModel`). El borrado lógico "en cascada" lo hace el `service` en
  la misma transacción — no hay `ON DELETE CASCADE`.
- **Tablas append-only** (`bitacora`, `postulacion_historial`): llevan las
  columnas por uniformidad, pero `actualizado_en` = `creado_en` y
  `eliminado_en` queda siempre `NULL` (no hay `UPDATE` ni `DELETE` para el
  rol de la aplicación).
- Las restricciones de unicidad que deben respetar el borrado lógico son
  **índices únicos parciales** `... WHERE eliminado_en IS NULL` (por ejemplo
  `usuario.ci`, `usuario.correo`, `UNIQUE (postulante_id, convocatoria_id)`
  en `postulacion`). Los índices de rendimiento también se declaran con ese
  filtro.

## Nombres de migraciones Flyway

Formato obligatorio: **`V{yyyyMMddHHmmss}__descripcion.sql`**, ej.:

```
V20260915143022__crear_tablas_cuentas_y_acceso.sql
V20260916091500__agregar_estado_postulacion.sql
```

**No se usa versión secuencial simple** (`V1__`, `V2__`, `V3__...`).

### Por qué

Con versión secuencial, dos ramas paralelas que agregan cada una su propia
migración terminan generando el mismo número (`V5__`) de forma independiente.
Al mergear, Flyway ve dos migraciones con el mismo número de versión y
revienta (o, peor, una pisa el checksum esperado de la otra si alguien las
renombra a mano para "resolver" el conflicto). El timestamp evita la
colisión por diseño.

**No "corregir" esto a formato secuencial** aunque parezca más prolijo — es
una decisión deliberada para evitar conflictos de merge, no un descuido.

> Excepción histórica: `V1__init_schema.sql` (la migración inicial del
> scaffold, con una tabla de prueba `system_metadata`) ya fue aplicada y
> queda con su nombre actual — no se renombra una migración ya aplicada
> (invalidaría su checksum en `flyway_schema_history`). La primera migración
> real del dominio arranca con el formato timestamp.

## Convención de nombres: Models, tablas, columnas

- **Java (Models/campos)**: `camelCase`. Clases en `PascalCase` singular
  (`Usuario`, `PostulacionHistorial`).
- **Base de datos (tablas/columnas)**: `snake_case`, **en español y en
  singular** (`usuario`, `postulacion`, `convocatoria_documento`,
  `postulacion_historial`) — así están en el diagrama. No se pluralizan.
- La conversión `camelCase` → `snake_case` es **automática** vía la
  estrategia de nombrado de Spring Boot/Hibernate — no hace falta anotar
  `@Column(name = "...")` en cada campo solo para convertir el case
  (`creadoEn` → `creado_en`). Se usa `@Column(name = "...")` explícito
  únicamente cuando el nombre de columna debe diferir del derivado.
- Nombre de tabla **siempre explícito** (`@Table(name = "usuario")`), no
  derivado: el diagrama fija el nombre y no se deja a la estrategia por
  defecto.
- **Enums de negocio** (`estado_postulacion`, `estado_convocatoria`,
  `estado_beca`, `tipo_notificacion`, `tipo_movimiento_beca`,
  `resultado_adjudicacion`, `destinatarios_comunicado`): son tipos `enum` de
  PostgreSQL en el diagrama; en Java van como `enum` con
  `@Enumerated(EnumType.STRING)` (nunca `ORDINAL`) y se mapean al tipo
  nativo de PostgreSQL (`@JdbcType(PostgreSQLEnumJdbcType.class)` o
  equivalente). Los valores exactos están en `diseno_base_de_datos.md` §4.
- **Archivos**: los binarios viven en **MinIO** (almacenamiento de objetos compatible con S3); la base solo
  guarda la ruta (`storage_path`), el tipo (`mime_type`) y el tamaño
  (`tamano_bytes`). Nunca se guarda un binario en PostgreSQL.
- **Contraseñas**: solo existe su hash BCrypt (`usuario.password_hash`),
  nunca la contraseña en texto plano. Los refresh tokens tampoco se guardan:
  la tabla `sesion` guarda solo su hash SHA-256 (`token_hash`). Ver
  [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md).

## Claves foráneas e índices

- Nombre de columna FK: **`{tabla_referenciada}_id`** (`postulante_id`,
  `convocatoria_id`, `rol_id`). Cuando la FK apunta a `usuario` y expresa
  *quién hizo algo*, lleva el nombre del rol en la acción (`creado_por`,
  `decidido_por`, `generada_por`, `registrado_por`, `publicado_por`,
  `solicitado_por`, `realizado_por`, `auditor_id`) — así figuran en el
  diagrama; no se renombran a `usuario_id`.
- Toda FK lleva su índice si se va a filtrar/joinear por ella con
  frecuencia (Postgres no indexa FKs automáticamente). Los índices de
  rendimiento previstos (p. ej. `(convocatoria_id, estado)` en `postulacion`
  para ~20 000 postulaciones por gestión) están en `diseno_base_de_datos.md`.
- **Idempotencia** (RNF-03 de la documentación de grado): las restricciones
  únicas que evitan duplicar una operación repetida (`UNIQUE (beca_id, fecha)`
  en `asistencia`, `UNIQUE (postulante_id, convocatoria_id)` en
  `postulacion`) son parte del contrato y se crean como índices únicos
  parciales.
