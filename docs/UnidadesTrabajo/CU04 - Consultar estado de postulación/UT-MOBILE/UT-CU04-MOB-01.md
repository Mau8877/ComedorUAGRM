# UT-CU04-MOB-01 — Modelos, servicio y providers

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-01` |
| **Nombre** | Modelos, servicio y providers |
| **Caso de uso** | CU04-MOB |
| **Depende de** | `UT-CU04-BE-04` |

## Antes de implementar

- [`CONSUMO_API_MOBILE.md`](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (`ApiClient`, servicios por feature), [`ESTADO_GLOBAL_MOBILE.md`](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (nombres de providers), [`TESTING_MOBILE.md`](../../../../.claude/rules/mobile/TESTING_MOBILE.md).
- Como referencia: `M/lib/features/perfil/` (`models/`, `services/`, `providers/`).

## Solicitud de negocio

La app necesita leer las postulaciones del usuario sin armar llamadas a mano en cada pantalla.

## Objetivo

Existen los modelos Dart, el servicio y los providers de la feature `postulaciones`.

## Alcance

- `M/lib/features/postulaciones/models/`: `MiPostulacionResumen`, `MiPostulacionDetalle`, `PasoLineaTiempo` (`situacion`: completado o pendiente), `Observaciones`, `FinalistaPostulante` y `EstadoPostulacion` (los seis visibles), con `fromJson`; los bloques opcionales son nulables. Archivos en `snake_case`.
- `M/lib/features/postulaciones/services/postulaciones_service.dart`: `listarMias({gestionAcademica, page, pageSize, sort})` (devuelve los elementos y el `meta`) y `obtener(codigo)`, sobre el `ApiClient`.
- `M/lib/features/postulaciones/providers/`: `postulacionesServiciosProvider`, `misPostulacionesListProvider` (`AsyncNotifier` con paginación acumulada: `cargarSiguiente()` y recarga al cambiar el filtro), `misPostulacionesUiProvider` (gestión elegida) y `postulacionDetailProvider` (family por `codigo`, `autoDispose`).
- Traducción del error `ERR_POS_01` a un mensaje en español (con un mapper propio de la feature, igual criterio que `auth_error_mapper.dart`).
- Tests: parseo de cada modelo (con y sin bloques opcionales), el servicio con un `ApiClient` simulado y el provider de la lista (acumula páginas y se reinicia al cambiar el filtro).

Archivos esperados (crear): modelos, servicio, providers, mapper de errores y tests.

## Fuera de alcance (no tocar)

- Pantallas y rutas (UT-02 a UT-04).
- `ApiClient` y el router.
- Mutaciones de envío o corrección (CU-02).

## Resultado esperado

Las pantallas leen la lista y el detalle desde providers ya armados.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
