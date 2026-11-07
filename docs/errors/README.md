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
- [`ERR_AUTH_08`](auth/ERRORES_AUTH.md#err_auth_08) — El enlace de restablecimiento no sirve (400)
- [`ERR_AUTH_09`](auth/ERRORES_AUTH.md#err_auth_09) — La contraseña actual es incorrecta o falta (400)

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

### Lista priorizada (`PRI`)

- [`ERR_PRI_01`](postulaciones/ERRORES_POSTULACIONES.md#err_pri_01) — La convocatoria no existe (404)
- [`ERR_PRI_02`](postulaciones/ERRORES_POSTULACIONES.md#err_pri_02) — `sort` o `filter` no soportado en la lista priorizada (400)
- [`ERR_PRI_03`](postulaciones/ERRORES_POSTULACIONES.md#err_pri_03) — No hay postulaciones para exportar (400)
- [`ERR_US_13`](usuarios/ERRORES_USUARIOS.md#err_us_13) — Solo el Postulante tiene teléfono (400)

<!--
  A medida que se agregan módulos de negocio (platos, menú, usuarios...),
  cada uno suma acá su propia sección "### Nombre (PREFIJO)" con la lista
  de sus códigos, igual que la sección de Sistema de arriba — ver
  GUIA_ERRORES_BACKEND.md para el detalle exacto de qué hacer al agregar
  un código nuevo.
-->

### Finalistas (`FIN`)

- [`ERR_FIN_01`](finalistas/ERRORES_FINALISTAS.md#err_fin_01) — El finalista no existe (404)
- [`ERR_FIN_02`](finalistas/ERRORES_FINALISTAS.md#err_fin_02) — La convocatoria no existe (404)
- [`ERR_FIN_03`](finalistas/ERRORES_FINALISTAS.md#err_fin_03) — La fecha de recepción es futura (400)
- [`ERR_FIN_04`](finalistas/ERRORES_FINALISTAS.md#err_fin_04) — El plazo de entrega no es posterior a hoy (400)
- [`ERR_FIN_05`](finalistas/ERRORES_FINALISTAS.md#err_fin_05) — `sort` o `filter` no soportado en finalistas (400)

### Comunicados (`COM`)

- [`ERR_COM_01`](comunicados/ERRORES_COMUNICADOS.md#err_com_01) — Destinatarios no soportados o sin destinatarios (400)
- [`ERR_COM_02`](comunicados/ERRORES_COMUNICADOS.md#err_com_02) — `FINALISTAS` sin convocatoria (400)

### Expedientes (`EXP`)

- [`ERR_EXP_01`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_01) — La entrega de la documentación física no está registrada (409)
- [`ERR_EXP_02`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_02) — La postulación ya no está «calificada» (409)
- [`ERR_EXP_03`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_03) — Observaciones obligatorias si es «Observado» o «No válido» (400)
- [`ERR_EXP_04`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_04) — Puntaje académico obligatorio en «Validado» o fuera de 0–30 (400)
- [`ERR_EXP_05`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_05) — El documento no existe o no es del finalista (404)
- [`ERR_EXP_06`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_06) — La clave de idempotencia ya se usó para otra auditoría (409)
- [`ERR_EXP_07`](expedientes/ERRORES_EXPEDIENTES.md#err_exp_07) — La clave de idempotencia no es un UUID (400)

### Notificaciones (`NOT`)

- [`ERR_NOT_01`](notificaciones/ERRORES_NOTIFICACIONES.md#err_not_01) — El aviso no existe o no es del usuario (404)
- [`ERR_NOT_02`](notificaciones/ERRORES_NOTIFICACIONES.md#err_not_02) — `sort` no soportado en la bandeja (400)
