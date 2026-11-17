# Unidades de trabajo — CU10-MOBILE

**Caso de uso:** Pantalla «Registrar asistencia» de la app del Personal del Comedor: buscar al becado por CI y confirmar su asistencia de hoy.
**Fuente CU:** [`CU10 - Registrar asistencia digital.md`](../../../CasosDeUso/CU10%20-%20Registrar%20asistencia%20digital.md)
**Diagramas de grado:** contexto img 069 · análisis img 082 · secuencia img 097 · mockup img 110
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md) (`POST /api/v1/asistencias/buscar-becado` y `POST /api/v1/asistencias`).

> `M/` = `projects/mobile/lib` y `T/` = `projects/mobile/test`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_MOBILE`, `mobile/CONSUMO_API_MOBILE`, `mobile/ESTADO_GLOBAL_MOBILE`, `mobile/NAVEGACION_MOBILE`, `mobile/WIDGETS_UI_MOBILE`, `mobile/FORMULARIOS_MOBILE` y `mobile/TESTING_MOBILE`. Molde: la feature `perfil`.

## Decisiones de diseño

- **Una sola pantalla, sin shell**: `PERSONAL_COMEDOR` tiene un único caso de uso en la app, así que `rutaInicioPorRol` lo lleva a `/asistencia` (ya no a `/sin-modulo`). Si aparecen más pantallas para ese rol, se crea su shell entonces.
- **Feature `asistencia`** (`providers/`, `screens/`, `services/`, `models/`): modelos propios; nombres de provider según [`ESTADO_GLOBAL_MOBILE`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md).
- **El botón «Registrar» solo aparece** si la beca está vigente y no hay asistencia hoy; si no, se explica el motivo y se ofrece «Siguiente becado».
- **Reintentar es seguro**: registrar es idempotente en el backend, así que tras un error de red se puede pulsar de nuevo sin duplicar.
- **Fuera del mockup** (img 110): escaneo de carnet, foto del carnet y «Turno almuerzo» (ni el diseño lógico ni el backend los tienen).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU10-MOB-01](./UT-CU10-MOB-01.md) | Modelos, servicio y traducción de errores | — |
| [UT-CU10-MOB-02](./UT-CU10-MOB-02.md) | Estado de la pantalla (provider) | MOB-01 |
| [UT-CU10-MOB-03](./UT-CU10-MOB-03.md) | Pantalla, ruta y destino del rol | MOB-02 |

**Cadena:** `01 → 02 → 03`. Requiere el backend (UT-BACKEND) para probar contra la API real.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 1 a 9 | Buscar, confirmar y siguiente | 02, 03 |
| A1 a A5 | Errores traducidos y reintento | 01, 02 |
| CA11 a CA13 | Destino del rol y estados de la tarjeta | 03 |

## Follow-ups (no se implementan en estas UT)

- Escaneo del carnet (cámara) y turno, si la DUBSS los pide.
- Cola de registros sin conexión (hoy se reintenta a mano).

## Qué NO incluyen estas UT

- El historial del becado (CU-24 móvil) y el cotejo (CU-11, web).
