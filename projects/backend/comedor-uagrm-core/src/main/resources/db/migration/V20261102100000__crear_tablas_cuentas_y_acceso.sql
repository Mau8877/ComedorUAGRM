-- =====================================================================
-- Paquete P1 (cuentas y acceso) + bitacora
-- Tablas: rol, usuario, carrera, postulante, bitacora
-- Fuente: diseño lógico de la base de datos (docs/DiagramaConceptual.mermaid)
--
-- Todas las tablas llevan id interno (nunca sale a la API), codigo_<tabla>
-- público (UUID v7) y creado_en / actualizado_en / eliminado_en (borrado
-- lógico). Las unicidades son índices únicos parciales: una fila eliminada
-- lógicamente no bloquea reutilizar el dato.
-- =====================================================================

CREATE TABLE rol (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_rol     uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    nombre         varchar(50) NOT NULL,
    creado_en      timestamptz NOT NULL DEFAULT now(),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    eliminado_en   timestamptz
);

CREATE TABLE usuario (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_usuario uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    rol_id         bigint       NOT NULL REFERENCES rol (id),
    ci             varchar(20)  NOT NULL,
    correo         varchar(150) NOT NULL,
    firebase_uid   varchar(128) NOT NULL,
    nombres        varchar(100) NOT NULL,
    apellidos      varchar(100) NOT NULL,
    activo         boolean      NOT NULL DEFAULT true,
    creado_en      timestamptz  NOT NULL DEFAULT now(),
    actualizado_en timestamptz  NOT NULL DEFAULT now(),
    eliminado_en   timestamptz
);

CREATE TABLE carrera (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_carrera uuid         NOT NULL DEFAULT uuidv7() UNIQUE,
    nombre         varchar(150) NOT NULL,
    facultad       varchar(150) NOT NULL,
    creado_en      timestamptz  NOT NULL DEFAULT now(),
    actualizado_en timestamptz  NOT NULL DEFAULT now(),
    eliminado_en   timestamptz
);

-- La clave primaria de postulante es la FK a usuario (no tiene id propio).
CREATE TABLE postulante (
    usuario_id            bigint      PRIMARY KEY REFERENCES usuario (id),
    codigo_postulante     uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    carrera_id            bigint      NOT NULL REFERENCES carrera (id),
    registro_universitario varchar(20) NOT NULL,
    telefono              varchar(20) NOT NULL,
    creado_en             timestamptz NOT NULL DEFAULT now(),
    actualizado_en        timestamptz NOT NULL DEFAULT now(),
    eliminado_en          timestamptz
);

-- Append-only: la aplicación solo inserta (ver PERSISTENCIA_BD_BACKEND.md).
CREATE TABLE bitacora (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_bitacora  uuid        NOT NULL DEFAULT uuidv7() UNIQUE,
    usuario_id       bigint      REFERENCES usuario (id),
    accion           varchar(60) NOT NULL,
    entidad          varchar(60),
    entidad_id       bigint,
    detalle          jsonb,
    ip               inet,
    creado_en        timestamptz NOT NULL DEFAULT now(),
    actualizado_en   timestamptz NOT NULL DEFAULT now(),
    eliminado_en     timestamptz
);

-- Unicidad respetando el borrado lógico
CREATE UNIQUE INDEX uq_rol_nombre ON rol (nombre) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_carrera_nombre ON carrera (nombre) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_usuario_ci ON usuario (ci) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_usuario_correo ON usuario (correo) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_usuario_firebase_uid ON usuario (firebase_uid) WHERE eliminado_en IS NULL;
CREATE UNIQUE INDEX uq_postulante_registro_universitario
    ON postulante (registro_universitario) WHERE eliminado_en IS NULL;

-- Índices de las claves foráneas que se filtran con frecuencia
CREATE INDEX ix_usuario_rol_id ON usuario (rol_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_postulante_carrera_id ON postulante (carrera_id) WHERE eliminado_en IS NULL;
CREATE INDEX ix_bitacora_usuario_id ON bitacora (usuario_id);

-- Roles del sistema (un usuario tiene exactamente uno)
INSERT INTO rol (nombre) VALUES
    ('POSTULANTE'),
    ('ADMINISTRADOR_DUBSS'),
    ('DIRECTIVO'),
    ('PERSONAL_COMEDOR');
