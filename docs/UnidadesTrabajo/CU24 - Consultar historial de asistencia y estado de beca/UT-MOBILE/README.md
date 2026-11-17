# Unidades de trabajo — CU24-MOBILE

**Caso de uso:** sección «Mi beca» de la app del Postulante becado: estado de la beca y historial de asistencia con filtro por fechas.
**Fuente CU:** [`CU24 - Consultar historial de asistencia y estado de beca.md`](../../../CasosDeUso/CU24%20-%20Consultar%20historial%20de%20asistencia%20y%20estado%20de%20beca.md)
**Diagramas de grado:** mockup img 145 (es el móvil: «Mi beca» e «Historial de asistencia») · secuencia img 132 · análisis img 062
**Recurso HTTP / contrato:** el de [`UT-BACKEND`](../UT-BACKEND/README.md#contrato-de-datos--recurso-http).

> `M/` = `projects/mobile/lib` y `T/` = `projects/mobile/test`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): `ARQUITECTURA_MOBILE`, `mobile/CONSUMO_API_MOBILE`, `mobile/ESTADO_GLOBAL_MOBILE`, `mobile/NAVEGACION_MOBILE`, `mobile/WIDGETS_UI_MOBILE` y `mobile/TESTING_MOBILE`. Molde: la feature `postulaciones` (CU04, lista con scroll infinito).

## Decisiones de diseño

- **Feature `mibeca`** (`models/`, `services/`, `providers/`, `widgets/`). El modelo de datos (DTOs Dart) vive en `models/`, igual que `postulaciones`.
- **Nueva sección «Mi beca» en `EstudianteShell`** (antes de «Perfil», que pasa a ser la quinta): `MiBecaVista` es una vista sin `Scaffold`, como `MisPostulacionesVista`. El mockup la pone en la barra inferior (img 145).
- **El historial usa scroll infinito** (página de 20, el valor por defecto del backend) y el filtro de fechas un `showDateRangePicker`; cambiar el rango recarga desde la página 1.
- **A1:** `ERR_MBE_01` se muestra como «No cuentas con una beca» con «Ir a Mis postulaciones», que cambia a esa sección del shell (callback `onVerPostulaciones`).
- **Una sola pantalla de historial dentro de «Mi beca»**, no una segunda pantalla («Ver historial completo» del mockup): el historial ya está debajo del resumen y se pagina.
- **Fuera del mockup:** calendario mensual, «Faltas» y «Cumplimiento», «Posición en la lista» y turno (ver follow-ups de BACKEND).
- Hora de La Paz con un desfase fijo de UTC-4 (Bolivia no tiene horario de verano), igual que la feature `asistencia`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU24-MOB-01](./UT-CU24-MOB-01.md) | Modelos, servicio y traducción de errores | — |
| [UT-CU24-MOB-02](./UT-CU24-MOB-02.md) | Estado de la pantalla (providers) | MOB-01 |
| [UT-CU24-MOB-03](./UT-CU24-MOB-03.md) | Vista «Mi beca» y sección del shell | MOB-02 |
| [UT-CU24-MOB-04](./UT-CU24-MOB-04.md) | Pruebas | MOB-03 |

**Cadena:** `01 → 02 → 03 → 04`. Requiere el backend (UT-BACKEND) para probar contra la API real.

## Cobertura del caso de uso

| Paso / flujo | Descripción | UTs |
|---|---|---|
| Flujo principal 1 a 5 | Estado, resumen, historial y filtro | 02, 03, 04 |
| A1 a A3 | Sin beca, historial vacío, beca no vigente con motivo | 01, 03, 04 |
| A6 | Error de conexión con reintento | 01, 03, 04 |
| CA11 | Pantalla completa | 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Calendario mensual con asistió / falta.
- Aviso push cuando la beca cambia de estado (hoy solo se ve al abrir la sección).

## Qué NO incluyen estas UT

- El backend, la web y las demás secciones del shell.
