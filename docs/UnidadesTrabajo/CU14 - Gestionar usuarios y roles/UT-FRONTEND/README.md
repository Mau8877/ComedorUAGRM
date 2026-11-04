# Unidades de trabajo — CU14-FE

**Caso de uso:** Pantallas web de la gestión de cuentas del personal para el Administrador DUBSS: una vista por rol (Administradores DUBSS, Directivos y Personal del Comedor) con lista, búsqueda, filtro por estado, alta con contraseña temporal, edición, cambio de rol, desactivación y reactivación.
**Fuente CU:** [`CU14 - Gestionar usuarios y roles`](../../../CasosDeUso/CU14%20-%20Gestionar%20usuarios%20y%20roles.md)
**Diagramas de grado:** del PDF de la documentación de grado, la **Figura 79 (análisis: `UsuarioView`)** y la **Figura 111 (secuencia)**; la maqueta (Figura 137) no se usa: manda el sistema de diseño ya hecho del panel (header, sidebar y contenedor).
**Recurso HTTP / contrato:** Rutas `/usuarios/administradores`, `/usuarios/directivos` y `/usuarios/personal-comedor`; consume `GET /usuarios`, `POST /usuarios`, `PATCH /usuarios/{codigo}` y `POST /usuarios/{codigo}/cambiar-rol`, `/desactivar` y `/reactivar` (ver [`UT-BACKEND`](../UT-BACKEND/README.md)).

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_FRONTEND.md`, `FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`, `TANSTACK_QUERY_FRONTEND.md`, `TANSTACK_TABLE_FRONTEND.md`, `RUTAS_NAVEGACION_FRONTEND.md`, `ESTADO_GLOBAL_FRONTEND.md`, `TAILWIND_STYLES_FRONTEND.md` y `TESTING_FRONTEND.md`. Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

Todo el código va en `projects/frontend/comedoruagrm/src/` (en estas fichas, `F/`); la feature nueva es `features/usuarios/`, con la forma de [`ARQUITECTURA_FRONTEND.md`](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md#estructura-de-un-feature-srcfeaturesfeature).

- **Una vista por rol, un solo listado.** El sidebar del Administrador muestra tres entradas (Administradores DUBSS, Directivos y Personal del Comedor). Las tres rutas montan **la misma pantalla** (`UsuariosPersonalScreen`) con el rol fijo en `filter[rol]`; no hay tres implementaciones. No hay entrada para Postulantes (el CU no los gestiona).
- **Sin store.** Los datos de la lista vienen de TanStack Query (`ESTADO_GLOBAL_FRONTEND.md`); la página, el tamaño, la búsqueda, el filtro de estado y el orden viven en `useState` de la pantalla, y los modales, en estado local del componente. No se copia nada del backend a Zustand.
- **Tabla 100% server-side** con el paquete `components/ui/tanstack-table` (`DataTable` para la vista ancha, `DataCards` para la angosta, `DataTableToolbar`, `DataTablePagination` y `RowActionButton`); no se arma una tabla a mano. Tamaño de página 10/20/50 (inicial 20). La verificación contra el backend que pide la rule se resume abajo.
- **Guard por rol.** `_authenticated` solo comprueba que hay sesión; para estas rutas se agrega un `beforeLoad` que exige `ADMINISTRADOR_DUBSS` y, si no lo es, lleva al usuario al módulo de su rol (`rutaInicioPorRol`). Es solo la capa de interfaz: el backend es quien niega con `403` (`ERR_AUTH_07`).
- **Contraseña temporal.** Tras crear la cuenta, el modal muestra la contraseña **una sola vez**, con un botón para copiarla y el aviso de que no se vuelve a mostrar. Vive solo en el estado de ese modal: no se guarda en `localStorage`, `sessionStorage`, un store, la URL ni la caché de TanStack Query (la mutación no la deja en caché, se descarta al cerrar), y no se escribe en la consola.
- **Acciones sobre la propia cuenta.** La fila de la cuenta del usuario conectado (`codigo` de `GET /auth/usuario-actual`) no ofrece «Desactivar» ni «Cambiar rol». Es solo una ayuda de interfaz: el backend responde `403` `ERR_US_08` igual.
- **Errores del backend → mensajes** (el cliente solo los muestra; las reglas las aplica el backend): `ERR_US_01` marca el campo CI y `ERR_US_02` el campo correo (A1); `ERR_SYS_01` y `ERR_US_10` muestran el mensaje del backend (A2); `ERR_US_06` («la cuenta ya no existe») y `ERR_US_07` («ya estaba en ese estado») refrescan la lista (A8, A9); `ERR_US_08` y `ERR_US_09` se muestran como aviso sin cambiar la cuenta (A5); `ERR_AUTH_07` lleva al módulo del rol (A7); error de red o `5xx` → mensaje de conexión con opción de reintentar (A10). Un `401` lo resuelve el flujo de sesión ya existente (renovar o ir a `/login`).
- **Confirmaciones.** Desactivar pide confirmación (el sistema de modales ya trae `modal-destructive`); reactivar y cambiar el rol, también.
- **Textos** en español neutro; errores de formulario **inline debajo del campo** y solo si el campo fue tocado.
- **Sin variables de entorno nuevas.**

## Contrato de datos / recurso HTTP

**Rutas nuevas**

| Ruta | Acceso | Qué muestra |
|------|--------|-------------|
| `/usuarios` | Solo `ADMINISTRADOR_DUBSS` | Redirige a `/usuarios/administradores` |
| `/usuarios/administradores` | Solo `ADMINISTRADOR_DUBSS` | Cuentas con rol Administrador DUBSS |
| `/usuarios/directivos` | Solo `ADMINISTRADOR_DUBSS` | Cuentas con rol Directivo |
| `/usuarios/personal-comedor` | Solo `ADMINISTRADOR_DUBSS` | Cuentas con rol Personal del Comedor |

**Verificación contra el backend** (`TANSTACK_TABLE_FRONTEND.md`):

| Qué | Valor real del backend |
|-----|------------------------|
| Campos de la fila (`accessorKey`) | `codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo` |
| `search` | nombres, apellidos y CI |
| `filter[...]` | `filter[rol]` (fijo por vista) y `filter[activo]` (`true` / `false`) |
| `sort` (un solo campo, `campo` o `-campo`) | `apellidos`, `ci`, `correo`, `rol`, `creadoEn`; por defecto `-creadoEn` |
| `meta` | `page`, `pageSize`, `totalItems`, `totalPages` |

**Formularios** (schemas de Zod, mismas reglas que el backend; el servidor vuelve a validar): `crearUsuarioPersonalSchema` (`nombres` y `apellidos` hasta 100 caracteres, `ci` hasta 20, `correo` hasta 150 con formato de correo, `rol` uno de los tres), `editarUsuarioPersonalSchema` (`nombres`, `apellidos`, `correo`) y `cambiarRolSchema` (`rol`). Todos obligatorios.

**Respuestas:** `UsuarioPersonal` (`codigo`, `nombres`, `apellidos`, `ci`, `correo`, `rol`, `activo`) y, solo al crear, `UsuarioCreado` (lo mismo más `contrasenaTemporal`).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU14-FE-01](./UT-CU14-FE-01.md) | Tipos, roles del personal y schemas de Zod | — |
| [UT-CU14-FE-02](./UT-CU14-FE-02.md) | Llamadas a la API, hooks de TanStack Query y mapeo de errores | 01 |
| [UT-CU14-FE-03](./UT-CU14-FE-03.md) | Navegación del Administrador y guard por rol | — |
| [UT-CU14-FE-04](./UT-CU14-FE-04.md) | Pantalla de listado por rol y sus rutas | 02, 03 |
| [UT-CU14-FE-05](./UT-CU14-FE-05.md) | Crear cuenta y mostrar la contraseña temporal | 01, 02, 04 |
| [UT-CU14-FE-06](./UT-CU14-FE-06.md) | Editar los datos y cambiar el rol de una cuenta | 01, 02, 04 |
| [UT-CU14-FE-07](./UT-CU14-FE-07.md) | Desactivar y reactivar una cuenta | 02, 04 |

**Cadena:** `01 → 02`; `02, 03 → 04`; `01, 02, 04 → 05, 06`; `02, 04 → 07`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo A (consultar la lista) | Entradas del sidebar, tabla/cards, búsqueda, filtro de estado, orden y paginación | 02, 03, 04 |
| Flujo B (crear) | Formulario, validación, contraseña temporal mostrada una vez | 01, 02, 05 |
| Flujo C (editar) | Formulario de edición | 01, 02, 06 |
| Flujo D (cambiar rol) | Selector de rol con confirmación | 01, 02, 06 |
| Flujos E y F (desactivar y reactivar) | Confirmación y estado en la lista | 02, 07 |
| A1, A2 | Conflicto de CI o correo marcado en el campo; errores de formulario | 01, 02, 05, 06 |
| A5 | Propia cuenta (sin acción) y último Administrador (aviso del backend) | 06, 07 |
| A7 | Sin rol de Administrador: fuera de las rutas | 03 |
| A8, A9 | Cuenta inexistente o estado repetido: aviso y lista refrescada | 02, 06, 07 |
| A10 | Error de conexión con reintento | 02, 04, 05, 06, 07 |
| CA01, CA04, CA05 | Alta, validaciones y rol del personal | 01, 05 |
| CA02, CA03, CA09 | Conflictos de CI y correo | 02, 05, 06 |
| CA06, CA07 | Lista solo del personal, con filtros y búsqueda | 03, 04 |
| CA08, CA10 | Edición sin poder cambiar el CI | 06 |
| CA11, CA16 | Cambio de rol y sin cambiar el propio | 06 |
| CA12–CA15, CA17, CA18 | Desactivar, reactivar y protecciones | 07 |
| CA19, CA20 | Sin el rol o sin sesión | 03 |

## Follow-ups (no se implementan en estas UT)

- Obligar al funcionario a cambiar la contraseña temporal en su primer acceso (por definir con la DUBSS; también necesita el CU15 y cambios en el backend).
- Layouts de `DIRECTIVO` y `PERSONAL_COMEDOR` (hoy no tienen módulo): los crean los CU de esos roles.

## Qué NO incluyen estas UT

- La gestión de Postulantes: no hay entrada ni vista para ellos.
- Las pantallas de perfil y de cambio o recuperación de contraseña (CU15), y la consulta de la bitácora (CU25).
- Mobile: este CU es solo web.
- Eliminar una cuenta: no existe esa acción, solo desactivar.
