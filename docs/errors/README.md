# `docs/errors/` — Catálogo de códigos de error

Esta carpeta documenta, para humanos (soporte, quien consume la API desde
el frontend/mobile, o el propio equipo de backend meses después), qué
significa cada código de error (`ERR_{MODULO}_{NUMERO}`) que la API puede
devolver. El código en sí (la constante Java, `XxxErrorCodes`) le sirve al
`service` que lo lanza; esto de acá le sirve a la persona que lo recibe y
no quiere ponerse a leer código fuente para entender qué pasó.

El **proceso** de cuándo y cómo se documenta un código nuevo (obligatorio
en el mismo cambio que lo introduce) vive en
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
Este `README.md` es solo el índice — no repite esas reglas.

## Cómo está organizada

Una carpeta por feature/módulo (`system/`, y a futuro `platos/`, `menu/`,
`usuarios/`, etc. — se crean recién cuando ese módulo tiene su primer
error real, no antes), y dentro un único archivo `ERRORES_{MODULO}.md` con
todos los códigos de ese módulo, cada uno en su propia sección para poder
enlazarlo directo:

```
docs/errors/
├── README.md                          <- este archivo, el índice
├── system/
│   └── ERRORES_SISTEMA.md
├── platos/
│   └── ERRORES_PLATOS.md              (cuando exista el primer error de platos)
└── usuarios/
    └── ERRORES_USUARIOS.md            (cuando exista el primer error de usuarios)
```

## Índice de errores

### Sistema (`SYS`)

- [`ERR_SYS_00`](system/ERRORES_SISTEMA.md#err_sys_00) — Error interno no controlado (500)
- [`ERR_SYS_01`](system/ERRORES_SISTEMA.md#err_sys_01) — Validación de Bean Validation fallida (400)
- [`ERR_SYS_02`](system/ERRORES_SISTEMA.md#err_sys_02) — Límite de requests por IP superado (429)
- [`ERR_SYS_03`](system/ERRORES_SISTEMA.md#err_sys_03) — La ruta no existe (404)
- [`ERR_SYS_04`](system/ERRORES_SISTEMA.md#err_sys_04) — Método HTTP no permitido (405)
- [`ERR_SYS_05`](system/ERRORES_SISTEMA.md#err_sys_05) — Request mal formado o con parámetros inválidos (400)

### Autenticación (`AUTH`)

- [`ERR_AUTH_01`](auth/ERRORES_AUTH.md#err_auth_01) — Token de acceso ausente, inválido o vencido (401)
- [`ERR_AUTH_02`](auth/ERRORES_AUTH.md#err_auth_02) — Token válido de un usuario que ya no existe (401)
- [`ERR_AUTH_03`](auth/ERRORES_AUTH.md#err_auth_03) — Cuenta desactivada (403)
- [`ERR_AUTH_04`](auth/ERRORES_AUTH.md#err_auth_04) — Correo o contraseña incorrectos (401)
- [`ERR_AUTH_05`](auth/ERRORES_AUTH.md#err_auth_05) — Cuenta bloqueada temporalmente por intentos fallidos (403)
- [`ERR_AUTH_06`](auth/ERRORES_AUTH.md#err_auth_06) — Refresh token inválido, vencido o revocado (401)
- [`ERR_AUTH_07`](auth/ERRORES_AUTH.md#err_auth_07) — El usuario no tiene el rol necesario (403)

### Usuarios (`US`)

- [`ERR_US_01`](usuarios/ERRORES_USUARIOS.md#err_us_01) — El CI ya está registrado (409)
- [`ERR_US_02`](usuarios/ERRORES_USUARIOS.md#err_us_02) — El correo ya está registrado (409)
- [`ERR_US_03`](usuarios/ERRORES_USUARIOS.md#err_us_03) — El registro universitario ya está registrado (409)
- [`ERR_US_04`](usuarios/ERRORES_USUARIOS.md#err_us_04) — La carrera indicada no existe (404)
- [`ERR_US_05`](usuarios/ERRORES_USUARIOS.md#err_us_05) — Retirado: registro simultáneo de la misma cuenta de Firebase (409)
- [`ERR_US_06`](usuarios/ERRORES_USUARIOS.md#err_us_06) — La cuenta del personal no existe o es de un Postulante (404)
- [`ERR_US_07`](usuarios/ERRORES_USUARIOS.md#err_us_07) — La cuenta ya está en el estado pedido (409)
- [`ERR_US_08`](usuarios/ERRORES_USUARIOS.md#err_us_08) — No se puede operar sobre la propia cuenta (403)
- [`ERR_US_09`](usuarios/ERRORES_USUARIOS.md#err_us_09) — La operación dejaría al sistema sin Administrador activo (403)
- [`ERR_US_10`](usuarios/ERRORES_USUARIOS.md#err_us_10) — Rol no válido para el personal (400)
- [`ERR_US_11`](usuarios/ERRORES_USUARIOS.md#err_us_11) — `sort` o `filter` no soportado en el listado (400)
- [`ERR_US_12`](usuarios/ERRORES_USUARIOS.md#err_us_12) — La cuenta ya tiene ese rol (409)

<!--
  A medida que se agregan módulos de negocio (platos, menú, usuarios...),
  cada uno suma acá su propia sección "### Nombre (PREFIJO)" con la lista
  de sus códigos, igual que la sección de Sistema de arriba — ver
  GUIA_ERRORES_BACKEND.md para el detalle exacto de qué hacer al agregar
  un código nuevo.
-->
