# UT-CU06-BE-01 — Migración: gestión académica, convocatoria, postulación, modelo y puntaje

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-01` |
| **Nombre** | Migración: gestión académica, convocatoria, postulación, modelo y puntaje |
| **Caso de uso** | CU06-BE |
| **Depende de** | — |

## Antes de implementar

- [`PERSISTENCIA_BD_BACKEND.md`](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (identificadores, columnas comunes, borrado lógico, índices únicos parciales, formato de la migración).
- Como referencia: `R/db/migration/V20261102100000__crear_tablas_cuentas_y_acceso.sql`.
- El diseño lógico (`docs/DiagramaConceptual.mermaid` y `images/091` de la documentación de grado) y `diseno_base_de_datos.md` §4 y §5: **manda el diagrama**; los nombres y tipos se copian de ahí.

## Solicitud de negocio

La lista priorizada necesita dónde vivir los datos que ordena: convocatorias, postulaciones y sus puntajes.

## Objetivo

Una migración Flyway crea las cinco tablas con sus enums, claves e índices, tal como el diseño lógico.

## Alcance

- Una sola migración `R/db/migration/V{yyyyMMddHHmmss}__crear_tablas_convocatoria_postulacion_y_puntaje.sql` con fecha posterior a la última existente.
- Enums de PostgreSQL: `estado_convocatoria` (`BORRADOR`, `ABIERTA`, `CERRADA`) y `estado_postulacion` (`borrador`, `pendiente`, `devuelta`, `archivada`, `calificada`, `adjudicada`, `no_adjudicada`).
- Tablas, con `id` + `codigo_<tabla>` (uuid v7) + `creado_en`/`actualizado_en`/`eliminado_en` y **exactamente** las columnas del diagrama:
  - `gestion_academica` (`etiqueta`, `fecha_inicio`, `fecha_fin`).
  - `convocatoria` (`gestion_id` FK, `creado_por` FK→`usuario`, `nombre`, `fecha_apertura`, `fecha_cierre`, `cupos`, `puntaje_minimo` numeric(5,2), `estado`).
  - `postulacion` (`convocatoria_id` FK, `postulante_id` FK→`postulante.usuario_id`, `estado`, `fecha_envio` nula mientras es borrador, `numero_envios` smallint default 0).
  - `modelo_preseleccion` (`creado_por` FK→`usuario`, `version`, `descripcion`, `activo`).
  - `puntaje_postulacion` (`modelo_id` FK, `postulacion_id` FK, `puntaje_total` numeric(5,2) con `CHECK` 0 a 100, `detalle` jsonb, `fecha_calculo`, `vigente`).
- Índices (todos con `WHERE eliminado_en IS NULL` cuando son únicos):
  - `UNIQUE (postulante_id, convocatoria_id)` en `postulacion`.
  - `UNIQUE (gestion_id) WHERE estado = 'ABIERTA'` en `convocatoria`.
  - `UNIQUE (postulacion_id) WHERE vigente` en `puntaje_postulacion`.
  - `UNIQUE (activo) WHERE activo` en `modelo_preseleccion`.
  - De rendimiento de la lista: `(convocatoria_id, estado)` en `postulacion` y `(puntaje_total DESC) WHERE vigente` en `puntaje_postulacion`; más los índices de las FK que se joinean (`postulante_id`, `postulacion_id`).
- Si al comparar con el diagrama algo no coincide con esta ficha, **gana el diagrama** y se avisa.

Archivos esperados (crear): la migración. (Tocar): ninguno.

## Fuera de alcance (no tocar)

- El resto de las tablas de P2 a P6 (`respuesta_socioeconomica`, `nota_academica`, `documento_adjunto`, `postulacion_historial`, `parametro_modelo`, `lote_reprocesamiento`, `finalista`, etc.): las crea el CU que las usa.
- Datos de ejemplo (UT-03) y Models (UT-02).
- Cambiar tablas existentes.

## Resultado esperado

La base queda con las cinco tablas listas y la aplicación arranca con Flyway sin errores.

## Validación

1. `./mvnw test` pasa (el contexto levanta con la migración aplicada sobre una base real, ver `PostgresTestDatabase`).
2. En una base vacía, Flyway aplica todas las migraciones y las columnas de las cinco tablas coinciden con el diagrama.
3. Insertar dos postulaciones del mismo postulante en la misma convocatoria falla por el índice único; insertar dos puntajes vigentes de la misma postulación también.
