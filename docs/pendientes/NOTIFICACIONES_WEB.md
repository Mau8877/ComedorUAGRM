# Pendiente: Notificaciones (Web)

## Estado actual

El componente de UI **ya está construido y funcionando en el frontend**,
pero corriendo 100% sobre datos mock -- no existe todavía ningún endpoint
de `notificaciones` en el backend (no hay `features/notificaciones/` en
`projects/backend/src/main/java/com/comedoruagrm/backend/`). Este
documento es el contrato que el backend necesita implementar para que el
frontend deje de simular y pase a hablar con la API real.

**No implementar nada de esto especulativamente en mobile todavía** -- ese
apartado queda pendiente aparte, este documento cubre exclusivamente la
web (`projects/frontend`).

### Dónde vive la parte de frontend ya hecha

| Pieza | Archivo |
| --- | --- |
| Componente visual (campanita + panel) | [`src/components/layout/NotificationsMenu.tsx`](../../projects/frontend/src/components/layout/NotificationsMenu.tsx) |
| Tipos del contrato que el frontend espera | [`src/components/layout/types.ts`](../../projects/frontend/src/components/layout/types.ts) (`NotificationItem`, `NotificationsPageMeta`, `NotificationsController`) |
| Simulación actual (a reemplazar por un hook real de `api/`) | [`src/features/pruebas_layout/api/useMockNotifications.ts`](../../projects/frontend/src/features/pruebas_layout/api/useMockNotifications.ts) |
| Formateo de fecha relativa (ya resuelto, no depende del backend) | [`src/utils/formatRelativeDate.ts`](../../projects/frontend/src/utils/formatRelativeDate.ts) (`dayjs` + locale español) |

Cuando exista el endpoint real, el trabajo del lado frontend es acotado:
un hook nuevo en la feature que corresponda (`api/useNotificaciones.ts`,
con TanStack Query -- ver
[TANSTACK_QUERY_FRONTEND.md](../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md))
que arme un `NotificationsController` real y se lo pase a `AdminLayout`/
`EstudianteLayout` en vez de `useMockNotifications`. El componente visual
(`NotificationsMenu`) no necesita ningún cambio -- ya está escrito contra
este contrato.

## Comportamiento esperado (ya implementado en el frontend, referencia)

1. El panel **no** pide datos al montar el layout -- recién hace el primer
   `GET` cuando el usuario abre la campanita por primera vez.
2. El "circulito" con el número de no leídas en la campanita **no** sale
   del array paginado -- es un valor aparte (ver
   [`GET /notificaciones/me/resumen`](#2-get-apiv1notificacionesmeresumen----conteo-de-no-leídas)
   abajo). Así el badge puede estar actualizado sin necesidad de haber
   abierto el panel todavía.
3. El panel pagina de a **10** (`pageSize=10`), con un botón "Mostrar más"
   que pide la página siguiente y la agrega a lo ya cargado (nunca
   reemplaza lo anterior).
4. Marcar una notificación como leída **no la elimina** de la lista --
   sigue ahí, solo cambia de fondo gris (no leída) a blanco (leída). Toda
   la fila es clickeable, no un ícono aparte.
5. "Leer todo" marca todas como leídas (sin eliminarlas). "Eliminar
   notificaciones" es un **hard delete** real de todas las notificaciones
   del usuario -- el frontend ya pide confirmación antes de llamarlo
   (`AlertDialog`), así que el backend no necesita un soft-delete/papelera
   para este caso de uso.
6. Cada notificación tiene un `tipo` (`success`/`info`/`warning`/`error`)
   que el frontend usa para elegir ícono y color -- mismo vocabulario que
   ya usan los toasts de la app (ver
   [`components/ui/sonner.tsx`](../../projects/frontend/src/components/ui/sonner.tsx)).

## Endpoints necesarios

Todos bajo `/api/v1/notificaciones`, autenticados (JWT, `@CurrentUserId`)
-- una notificación siempre pertenece a un usuario, no hay notificaciones
públicas/anónimas. Formato de respuesta, códigos HTTP y paginación siguen
el contrato general ya definido en
[ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y
[RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md) --
acá solo se documenta lo específico de este módulo.

### 1. `GET /api/v1/notificaciones/me` -- listado paginado

```
GET /api/v1/notificaciones/me?page=1&pageSize=10&sort=-fecha
```

- El frontend siempre pide `pageSize=10` explícito (el default general del
  backend es 20, pero acá el panel está diseñado para 10 por página --
  nada especial del lado del backend, `pageSize` ya acepta cualquier
  entero positivo).
- `sort=-fecha` (más nueva primero) es el único orden que necesita este
  endpoint -- `fecha` tiene que estar documentada como campo ordenable
  (ver [el formato de `sort`](../../.claude/rules/backend/ENDPOINTS_BACKEND.md#formato-de-sort)).
  Si no se manda `sort`, el default del endpoint debería ser el mismo
  orden (`-fecha`) -- no tiene sentido ningún otro default acá.
- Devuelve **todas** las notificaciones del usuario (leídas y no leídas
  mezcladas, en orden cronológico) -- no hay un `filter[leida]` necesario
  para el caso de uso actual del frontend (podría agregarse a futuro si
  hace falta un filtro "solo no leídas" en la UI, pero hoy no se pidió).

Response:

```json
{
  "status": "success",
  "data": [
    {
      "id": "9f1c1e2a-...",
      "tipo": "WARNING",
      "titulo": "Stock bajo: Cebolla",
      "detalle": "Quedan 8 kg, por debajo del mínimo (10 kg).",
      "fecha": "2026-09-03T14:32:10Z",
      "leida": false
    }
  ],
  "message": "Listado obtenido correctamente",
  "timestamp": "2026-09-03T15:00:00Z",
  "meta": { "page": 1, "pageSize": 10, "totalItems": 23, "totalPages": 3 }
}
```

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | string (UUID) | |
| `tipo` | string, uno de `SUCCESS`/`INFO`/`WARNING`/`ERROR` | Enum Java `TipoNotificacion`, persistido `@Enumerated(EnumType.STRING)` -- nunca `ORDINAL` (mismo criterio que `Role` en `SEGURIDAD_AUTH_BACKEND.md`). El frontend lo pasa a minúscula (`tipo.toLowerCase()`) al mapear la response a `NotificationType` |
| `titulo` | string | |
| `detalle` | string \| `null` | Opcional -- no toda notificación tiene texto secundario |
| `fecha` | string, ISO-8601 UTC | Momento en que se generó el evento, no en que se consultó |
| `leida` | boolean | |

### 2. `GET /api/v1/notificaciones/me/resumen` -- conteo de no leídas

```
GET /api/v1/notificaciones/me/resumen
```

Endpoint liviano, separado del listado paginado, pensado para el
"circulito" de la campanita -- se puede pollear con más frecuencia que el
listado completo sin pagar el costo de traer notificaciones enteras cada
vez.

```json
{
  "status": "success",
  "data": { "noLeidas": 8 },
  "message": "Resumen obtenido correctamente",
  "timestamp": "2026-09-03T15:00:00Z"
}
```

**Estrategia de refresco: polling, no WebSocket.** El hook real de
`api/` pide este endpoint con `refetchInterval` de TanStack Query (cada
30-60s es un punto de partida razonable, ajustable sin tocar nada del
backend). Se descarta WebSocket para este caso de uso, deliberadamente:

- El backend hoy no tiene ninguna infraestructura de WebSocket (nada de
  STOMP/SockJS en `pom.xml`, ni mención en las rules de backend) --
  agregarla solo para este contador sería meter una pieza de
  infraestructura nueva para un dominio (comedor universitario) donde no
  hace falta que el número se actualice al milisegundo.
- Con más de una instancia del backend, un WebSocket necesita algo como
  Redis pub/sub para que un evento generado en la instancia A le llegue a
  un cliente conectado a la instancia B -- mismo problema ya documentado y
  aceptado como pendiente para el rate limit (ver
  [SEGURIDAD_AUTH_BACKEND.md#rate-limit](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).
- El polling reusa el mismo mecanismo que ya usa toda la app (TanStack
  Query) -- cero infraestructura nueva.

Si en algún momento el proyecto necesita real-time de verdad (chat,
pedidos en vivo para cocina, etc.), ahí sí se justifica meter WebSocket
como pieza transversal -- y esta feature podría subirse a esa
infraestructura ya existente en vez de justificarla ella sola. Hasta que
eso pase, no se implementa de antemano (mismo criterio de "no anticipar"
que ya usa el resto del proyecto).

### 3. `POST /api/v1/notificaciones/{id}/leer` -- marcar una como leída

Acción que no es CRUD directo → sub-recurso verbo en infinitivo (mismo
patrón que `POST /pedidos/{id}/aprobar` de
[ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).
Idempotente en la práctica (marcar como leída una que ya está leída no
debería fallar, solo no cambia nada) aunque el verbo sea `POST` por no ser
un reemplazo de recurso completo.

- `404` (`NotFoundException`) si el `id` no existe **o no pertenece al
  usuario autenticado** -- no se distingue entre "no existe" y "es de otro
  usuario" en el mensaje de error, para no filtrar información de otros
  usuarios (mismo criterio que cualquier recurso propio de un usuario).
- `data: null` en éxito (no hay nada que devolver -- el frontend no
  necesita la notificación actualizada de vuelta, ya tiene el `id`).

### 4. `POST /api/v1/notificaciones/leer-todo` -- marcar todas como leídas

Sin body. Marca como leídas **todas** las notificaciones del usuario
autenticado, no solo las que el frontend tiene cargadas en ese momento
(el frontend puede tener cargada solo la página 1 de 3, pero "Leer todo"
tiene que limpiar el contador real completo, no solo lo visible).

`data: null` en éxito.

### 5. `DELETE /api/v1/notificaciones` -- hard delete de todas

Sin body, sin `{id}` en la URL -- borra **todas** las notificaciones del
usuario autenticado. Hard delete real (`DELETE FROM`, no un
`eliminada_en`/soft-delete) -- el frontend ya exige confirmación explícita
antes de llamar a este endpoint, así que no hace falta una papelera de
reciclaje ni una ventana de "deshacer" del lado del backend para este caso
de uso tal como está planteado hoy.

`data: null` en éxito.

> **Pendiente de decidir, no bloqueante para la v1**: ¿se necesita también
> un `DELETE /api/v1/notificaciones/{id}` (eliminar una sola)? El frontend
> hoy no tiene esa acción en la UI (por diseño: "leer" ya cubre el caso de
> "ya la vi", eliminar es solo la acción masiva). Si en algún momento se
> agrega un botón de eliminar por fila, se suma ese endpoint en su momento
> -- no se implementa de antemano sin un caso de uso real que lo pida (ver
> el criterio de "no anticipar" que ya usa el resto del proyecto).

## Modelo / persistencia (sugerido)

Siguiendo [ARQUITECTURA_BACKEND.md](../../.claude/rules/ARQUITECTURA_BACKEND.md)
y [CONVENCIONES_JAVA_BACKEND.md](../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md):

```
features/notificaciones/
├── controller/NotificacionController.java
├── service/NotificacionService.java
├── repository/NotificacionRepository.java
├── dto/NotificacionResponse.java   (record)
└── model/
    ├── Notificacion.java
    └── TipoNotificacion.java       (enum: SUCCESS, INFO, WARNING, ERROR)
```

Tabla `notificaciones` (ver
[PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)):

| Columna | Tipo | Notas |
| --- | --- | --- |
| `id` | UUID, PK | |
| `usuario_id` | FK → `users.id` | Con índice -- se filtra por esta columna en cada request de este módulo |
| `tipo` | varchar | `TipoNotificacion` como `STRING` |
| `titulo` | varchar | |
| `detalle` | text, nullable | |
| `leida` | boolean, default `false` | |
| `created_at` | timestamp | Es la `fecha` que expone el DTO -- no hace falta una columna separada, `created_at` ya es el momento del evento |

**Quién crea las notificaciones**: este documento cubre solo el contrato
de **lectura/consumo** (lo que necesita `NotificationsMenu`). Quién
dispara la creación de una notificación nueva (¿qué eventos de negocio la
generan -- stock bajo, pedido nuevo, etc.?) es una decisión de diseño
aparte, probablemente resuelta desde el `service` de cada módulo que
origina el evento (ej. `IngredienteService` crea una notificación `WARNING`
cuando el stock cruza el mínimo) -- no está en el alcance de este
documento, que es específicamente el caso de uso "Notificaciones (Web)"
del lado de quien las recibe.

## Errores

Catalogar en `docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md` cuando
se implemente de verdad (ver
[GUIA_ERRORES_BACKEND.md](../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)
para el proceso obligatorio -- no se documenta acá de antemano un código
que todavía no existe en código real). Prefijo de módulo sugerido: `NOT`
(no colisiona con ningún prefijo ya usado, ver
[`docs/errors/README.md`](../errors/README.md)).

Mínimo esperable:

| Caso | HTTP | Excepción |
| --- | --- | --- |
| `POST .../{id}/leer` con un `id` que no existe o no es del usuario | `404` | `NotFoundException` |

## Checklist para cerrar este pendiente

- [ ] `features/notificaciones/` en el backend (model, migración Flyway,
      repository, service, controller, DTO) según lo de arriba.
- [ ] Los 5 endpoints implementados y probados (tests de `service` +
      `controller`, ver
      [TESTING_BACKEND.md](../../.claude/rules/backend/TESTING_BACKEND.md)).
- [ ] Códigos de error catalogados en `docs/errors/notificaciones/` (si
      aplica alguno más allá del `404` de arriba).
- [ ] Definir **quién y cómo** crea notificaciones nuevas desde cada
      módulo de negocio (fuera del alcance de este documento, ver nota
      arriba).
- [ ] Frontend: reemplazar `useMockNotifications` por un hook real de
      `api/` con TanStack Query, siguiendo
      [TANSTACK_QUERY_FRONTEND.md](../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md)
      (queryKey `['notificaciones', 'list', { page, pageSize }]` para el
      listado, `['notificaciones', 'resumen']` para el conteo, con
      `refetchInterval` para el polling -- ver sección 2 -- e invalidando
      ambos tras cualquier mutation de leer/eliminar).
