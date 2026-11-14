# Unidades de trabajo — CU04-MOB

**Caso de uso:** App móvil del Postulante: «Mis postulaciones» (lista con filtro por gestión), detalle de una postulación (estado visible, línea de tiempo e información asociada) y apertura del detalle al tocar un aviso push.
**Fuente CU:** [`CU04 - Consultar estado de postulación`](../../../CasosDeUso/CU04%20-%20Consultar%20estado%20de%20postulación.md) (plataforma «Móvil y Web»)
**Diagramas de grado:** CU-04 (maquetas de `images/134`–`146`, análisis y secuencia de `images/051`–`063` y `121`–`133`).
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md) (`GET /api/v1/postulaciones/mias` y `GET /api/v1/postulaciones/{codigo}`). Necesita `UT-CU04-BE-04` funcionando para la prueba completa.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/) — `ARQUITECTURA_MOBILE.md`, `ESTADO_GLOBAL_MOBILE.md`, `CONSUMO_API_MOBILE.md`, `NAVEGACION_MOBILE.md`, `WIDGETS_UI_MOBILE.md`, `FORMULARIOS_MOBILE.md` y `TESTING_MOBILE.md`. Estas fichas no las sustituyen: si hay conflicto, ganan las rules.

## Decisiones de diseño

`M/` = `projects/mobile/`. Feature nueva: `M/lib/features/postulaciones/` con `models/`, `services/`, `providers/`, `screens/` y `widgets/`.

- **Modelos Dart:** la rule de mobile aún no fija dónde viven los DTOs de la API; se sigue el precedente ya existente en `features/auth/models/` y `features/perfil/models/` (carpeta `models/` por feature, clases con `fromJson`). Es una convención de hecho, no escrita: si Mau la quiere fijar, se documenta en `CONSUMO_API_MOBILE.md`.
- **Solo lectura:** sin mutaciones, sin formularios. El `service` usa el `ApiClient` compartido (el interceptor ya desempaqueta el sobre y maneja el `401` con refresh).
- **Providers** (nombres de `ESTADO_GLOBAL_MOBILE.md`): `misPostulacionesListProvider` (lista paginada con el filtro de gestión) y `postulacionDetailProvider` (family por `codigo`). Estado de pantalla (filtro elegido) en `misPostulacionesUiProvider`.
- **Paginación en móvil:** scroll infinito (se pide la página siguiente al llegar al final), con el mismo `pageSize` por defecto del backend (20), sin selector de tamaño.
- **Navegación:** rutas `/mis-postulaciones` y `/mis-postulaciones/:codigo` en el `GoRouter` central (`core/routing/app_router.dart`); el chequeo de sesión ya lo hace el `redirect` global. La nueva sección «Mis postulaciones» entra en `EstudianteShell` (hoy tiene «Mi Menú», «Mis Pedidos» y «Perfil»); como el shell usa `NavigationBar` con pocas secciones, la lista se muestra **dentro** del shell y el detalle se abre como ruta aparte con botón de volver.
- **Estado visible:** se muestra tal cual lo devuelve el backend (no se recalcula en la app). Colores del `ColorScheme` y de `AppColors` (`success`, `warning`, `info`); sin hex sueltos.
- **Botón «Corregir y reenviar» e invitación a postular:** ocultos mientras no exista la pantalla del CU-02, igual que en la web.
- **Toque de un push (A6):** el payload del push debe traer `data.postulacionCodigo` (lo agrega `UT-CU04-BE-05`). Al tocar un aviso en segundo plano o con la app cerrada, o el aviso en primer plano, se navega a `/mis-postulaciones/{codigo}` pasando por el `redirect` global (si no hay sesión, primero el login). Sin ese dato, solo se abre la app (comportamiento actual de CU-EXTRA-01).
- **Dependencia entre features:** `notificaciones` solo emite el `codigo` hacia el router; no importa nada de `postulaciones/`.
- **Pruebas:** según `TESTING_MOBILE.md`, no se exige cubrir widgets triviales: se prueban el parseo de los modelos, los providers y la lógica de navegación del aviso.

## Contrato de datos / recurso HTTP

Mismo contrato que el backend (campos exactos de `MiPostulacionResumenResponse` y `MiPostulacionDetalleResponse`, ver el README de [`UT-BACKEND`](../UT-BACKEND/README.md)). Los bloques opcionales (`observaciones`, `motivo`, `finalista`, `resultado`) llegan `null` cuando no corresponden.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU04-MOB-01](./UT-CU04-MOB-01.md) | Modelos, servicio y providers | UT-CU04-BE-04 |
| [UT-CU04-MOB-02](./UT-CU04-MOB-02.md) | Pantalla «Mis postulaciones» | 01 |
| [UT-CU04-MOB-03](./UT-CU04-MOB-03.md) | Pantalla de detalle con línea de tiempo | 01 |
| [UT-CU04-MOB-04](./UT-CU04-MOB-04.md) | Rutas, sección del shell y apertura desde un push | 02, 03, UT-CU04-BE-05 |

**Cadena:** `01 → 02, 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo | UTs |
|---|---|
| Pasos 1–2 (lista) | 02, 04 |
| Pasos 3–6 (detalle) | 03, 04 |
| A1 (sin postulaciones), A2 (filtro) | 02 |
| A3, A4, A5 (observaciones, motivo, finalista, resultado) | 03 |
| A6 (el push abre el detalle) | 04 |
| A7, A8 (`401`/`403`) | 04 (`redirect` global e interceptor) |
| A9 (`404`) | 01, 03 |
| A10 (falla de conexión) | 02, 03 |
| CA01–CA13 (presentación) | 02, 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Habilitar «Corregir y reenviar» y la invitación a postular cuando exista el CU-02.
- Una bandeja de avisos dentro de la app (hoy no existe); cuando exista, enlazará al mismo detalle.
- Fijar en `CONSUMO_API_MOBILE.md` dónde viven los modelos Dart.

## Qué NO incluyen estas UT

- Cambios en el backend ni en la web (ver [`UT-BACKEND`](../UT-BACKEND/README.md) y [`UT-FRONTEND`](../UT-FRONTEND/README.md)).
- Enviar, corregir o reenviar postulaciones (CU-02) y guardar borrador (CU-17).
- Registro y permisos del push (CU-EXTRA-01): solo se agrega la navegación al tocar el aviso.
