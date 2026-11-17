# Reparto de tareas — Persona B

Fuente: [plan_desarrollo_tareas.pdf](plan_desarrollo_tareas.pdf). Este archivo
lista **solo lo pendiente de la Persona B** (postulación, IA, becados y
bitácora). La Persona A (cuentas, auditoría/adjudicación, BI y notificaciones)
ya está terminada. Detalle funcional de cada CU: `docs/CasosDeUso/` (se crea el
archivo del CU al empezarlo, ver [CASOS_DE_USO.md](../.claude/rules/CASOS_DE_USO.md))
y la documentación de grado.

**Total: 15 tareas, 58 puntos.**

## Criterio de "terminado"

BD + API + pantallas + pruebas + criterios de aceptación del RF. Todo se prueba
en local (sin despliegue ni pruebas de rendimiento en la nube).

## Tareas, en orden de ejecución

Estado: `[ ]` pendiente · `[~]` en progreso · `[x]` hecha.
El orden respeta las dependencias y pone antes lo que destraba más trabajo.

| Est. | Orden | # | Tarea | CU | RF | Pts | Plataforma | Depende de |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [~] | 1 | B5 | Gestionar convocatoria y gestión académica (CRUD, validación de fechas, abierta/cerrada) | CU-13 | RF-08 | 3 | Web | — |
| [~] | 2 | B6 | Validar elegibilidad y duplicados (una postulación vigente por postulante y gestión) | CU-18 | RF-13 | 2 | Backend | B5 |
| [~] | 3 | B7 | Digitalizar y adjuntar documentos (cámara en Flutter, carga web, validación de tipo/tamaño, MinIO) | CU-16 | RF-11 | 5 | Web + Móvil | — |
| [x] | 4 | B1 | Generador de datos semilla: postulaciones con puntaje, adjudicados, becas y asistencias de prueba (volumen 20.000) | extra | todos | 2 | Backend | — |
| [ ] | 5 | B2 | IA: dataset de ajuste, entrenamiento y validación del modelo (variables socioeconómicas, Python) | CU-03 | RF-03 | 5 | IA | contrato de la interfaz de IA |
| [~] | 6 | B3 | IA: API del modelo y cálculo por lote (puntaje ≤ 5 s con fecha de cálculo, 20.000 postulaciones) | CU-03 | RF-03 | 3 | IA | B2 |
| [~] | 7 | B8 | Enviar postulación a la Beca Comedor (formulario socioeconómico largo; bloquea si faltan campos/documentos; invoca la API de B3) | CU-02 | RF-02 | 8 | Web + Móvil | B5, B6, B7, B3 (por contrato) |
| [~] | 8 | B9 | Guardar borrador de postulación (persistencia parcial, recuperar borrador) | CU-17 | RF-12 | 3 | Web + Móvil | B8 |
| [~] | 9 | B4 | Parametrizar el modelo y reprocesar puntajes (recálculo masivo en segundo plano) | CU-26 | RF-21 | 5 | Web + IA | B3 |
| [x] | 10 | B10 | Gestionar padrón de becados (alta desde adjudicados, baja, suspensión, reemplazo) | CU-23 | RF-18 | 5 | Web | adjudicaciones de CU-05 (ya hecho) |
| [x] | 11 | B15 | Consultar bitácora de acciones (filtros sobre la tabla `bitacora`, que ya existe) | CU-25 | RF-20 | 2 | Web | — |
| [x] | 12 | B11 | Generar planilla de asistencia imprimible (firma + fotocopia de carnet) | CU-09 | RF-05 | 3 | Web | B10 |
| [x] | 13 | B12 | Registrar asistencia digital: **app móvil** del Personal del Comedor, ingresa CI del becado y confirma; idempotente (RNF-03) | CU-10 | RF-05 | 5 | Móvil | B10 |
| [x] | 14 | B13 | Cotejar registro digital contra la planilla física | CU-11 | RF-05 | 5 | Web | B12 |
| [x] | 15 | B14 | Consultar historial de asistencia y estado de beca (solo lectura) | CU-24 | RF-19 | 2 | Web + Móvil | B12 |

### Estado de B5 (CU-13)

Código y pruebas escritos (backend y web). Pasan: 98 tests unitarios del backend nuevos, checkstyle, lint, `tsc` y los 936 tests del frontend. **Pendiente para marcarla `[x]`:** correr con Docker los tests de base real (`ConvocatoriaRepositoryTest`, `ConvocatoriaIntegrationTest` y los controller con contexto de Spring, que no corren en esta máquina) y una prueba manual con el stack levantado. Detalle en [Implementacion/CU13](Implementacion/CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md).

### Estado de B1 (datos semilla de volumen)

`DatosPruebaVolumen` (perfil `dev`, `SEED_VOLUMEN_ENABLED`) completa los cargadores existentes: sobre los postulantes de prueba de `DatosPruebaListaPriorizada` crea una convocatoria propia con las **respuestas del formulario** (también las del formulario oficial en `datos_extra`), puntajes simulados de 0 a 70, **adjudicados**, **becas** (con suspensiones y bajas y su movimiento) y **asistencias**. Todo con SQL por conjuntos y una semilla fija. Pasan 10 tests con Postgres real (`DatosPruebaVolumenTest`: catálogos del CU02, topes de la IA, orden de la lista, una beca por adjudicado, asistencias solo de becas vigentes en días hábiles y sin repetir, e idempotencia). **Medido con 20.000 postulantes** (`SEED_PRIORIZACION_POSTULACIONES=20000`): la carga completa tarda 12 s (lista priorizada 7,8 s + volumen 4,2 s) y deja 20.000 postulaciones con respuestas, 1.500 decisiones, 1.000 becas, 137 movimientos y 17.263 asistencias. El puntaje es una simulación para tener datos con forma, no el de la IA real.

### Estado de B3 (CU-03, API de la IA)

`POST /api/v1/puntaje` en `comedor-uagrm-ia`: puntaje **ponderado y explicable** (0 a 70, con el desglose por variable en `detalle`) que aplica las ponderaciones del CU26 que recibe en cada solicitud, sin guardar estado. Pasan 49 tests de `pytest` (contrato con el core, rango 0-70, monotonía, parámetros y rechazos), `ruff check` y `ruff format --check`, y una prueba HTTP real contra el servicio levantado (un puntaje en unos 2 ms; el límite es 5 s). **Medido con datos de volumen (B1):** 18.500 puntajes calculados por el cliente real del core contra la IA real en 277 s, con p50 de 14 ms, p95 de 15 ms y un máximo de 103 ms por puntaje (el límite es 5 s), de forma secuencial. **Pendiente para marcarla `[x]`:** probarla con el stack completo levantado (contra el core con Postgres real ya se probó, ver «Pruebas con Postgres real»; el core registra `v1.0` al migrar con `V20261121100000__sembrar_modelo_preseleccion_v1_0.sql`), y que la DUBSS valide el criterio adoptado (necesidad económica y familiar; escala lineal de 100 a 70; promedio académico con peso 0 porque el auditor ya suma hasta 30; niveles por opción), descrito en el README del módulo. **No cubre B2:** no hay dataset ni modelo entrenado (no hay datos reales de la DUBSS). La IA puntúa diez variables: las seis originales y cuatro del formulario oficial de la DUBSS (dependencia económica, ocupación, discapacidad e hijos); el core las lee de `datos_extra` y la pantalla del CU26 ya pide las diez ponderaciones. Detalle en el [README del módulo](../projects/backend/comedor-uagrm-ia/README.md).

### Pruebas con Postgres real (2026-11-17)

Con un Postgres 18 descartable en Docker (`-Dtest.postgres.url=…` y, para los tests con contexto de Spring, también `-Dspring.datasource.url|username|password`):

- **Pasan los 33 tests de repositorio** de B4 a B9 (`Precalificacion…`, `ModeloPreseleccion…`, `Reprocesamiento…`, `EnvioPostulacion…`), que nunca se habían ejecutado, y la migración `V20261121100000` (el modelo `v1.0` con diez ponderaciones que suman 100, idempotente).
- **Suite completa del core: 1926 tests, 4 fallos, 0 errores.** Los 4 no son de este trabajo: `ConvocatoriaIntegrationTest.sinSesion_401_yConRolDePostulante_403ErrAuth07` (400 en vez de 403) y `ValidacionPostulacionConsultaRepositoryTest.convocatoria_devuelveLaGestionYElEstado` («G-VAL1» en vez de «VAL1») ya estaban anotados; `SecurityConfigTest.get_conOrigenPermitido…` da 503 porque `/health` necesita Redis, que no estaba levantado; y `BorradorPostulacionControllerTest.miBorrador_sinBorrador_da200ConDataNull` espera `data` nulo pero `ApiResponse` omite los nulos (`@JsonInclude(NON_NULL)`): hay que decidir si el test o el sobre tiene la razón (RESPONSES_BACKEND dice «`data`: `null`»).
- **De punta a punta:** una postulación `pendiente` con `datos_extra` → `PrecalificacionService` → IA real → puntaje guardado (57,54 de 70 con el modelo `v1.0`) → `calificada`. El cliente HTTP del core también se probó contra la IA real (con y sin modelo, y el `422` por una variable desconocida).
- **Errores que salieron al probar y ya se corrigieron:** (1) `V20261119100000__crear_respuesta_socioeconomica.sql` creaba una tabla que `V20261107110000` (CU07) ya crea, así que **el core no arrancaba en ninguna base nueva**; ahora solo agrega los `CHECK` de rango. (2) La semilla de `v1.0` rompía la limpieza (`DELETE FROM modelo_preseleccion`) de 16 clases de test por la clave foránea de `parametro_modelo`; ahora esas clases borran primero `parametro_modelo`.
- **Todavía sin hacer:** probar a mano en la web y en la app con el stack completo levantado, y el rendimiento de 20.000 postulaciones (B1).

### Prueba manual de la web con el stack levantado (2026-11-17)

Postgres, Redis y MinIO en Docker (puertos libres), y el core, la IA y la web (Vite) desde el código; un Chromium sin interfaz (Playwright) hizo de usuario y se revisaron las capturas. **Móvil: no se probó** (no hay emulador).

- **Funciona:** registro, login, asistente de postulación en 3 pasos con el formulario oficial completo (5 secciones, grupo familiar, vivienda, infraestructura), subida de documentos, confirmación y comprobante; «Mis postulaciones» pasa a **Calificada** (la IA puntuó desde la interfaz real: 58,8 de 70, posición 1); borrador (guardar, ver en «Mis postulaciones» y retomar con los valores); lista priorizada → detalle → pantalla «Precalificación con IA» (anillo, 10 variables con «puntos / máximo», traza, posición); «Parámetros del modelo» con 10 variables, guardar versión (`v1.1`) y reprocesar (3 de 3).
- **A1 de CU03 en el stack real:** con la IA caída la postulación queda `pendiente` y **no** cuenta intentos; con datos que la IA rechaza, a los 3 intentos queda marcada para revisión, el contador se detiene y la pantalla lo avisa.
- **Errores que salieron al probar y ya se corrigieron:** (1) una versión vigente **sin parámetros** (la crean los seeds `[PRUEBA] v2`; también datos anteriores al CU26) hacía que el core mandara `parametros: {}` y la IA respondiera `422` a **toda** postulación: ahora no se manda la versión y la IA usa `v1.0`; (2) la web daba el error de consola `Query data cannot be undefined` en `mi-borrador` porque el sobre omite `data` si no hay borrador: ahora lo normaliza a `null` (y el test del backend `miBorrador_sinBorrador` espera que `data` no exista, lo que cerró ese fallo); (3) el desglose de la pantalla de precalificación salía en orden arbitrario (el `jsonb` no conserva el orden): ahora sigue el orden del modelo; (4) la traza repetía «Precalificación automática» como paso y como resultado.
- **Detalles que quedan:** en «Parámetros del modelo» la vista previa dice «Guardar versión v1.0» aunque el servidor asigna `v1.1` (pasa cuando la versión vigente no sigue el formato `vN.M`, como las del seed); y el bucket de MinIO lo crea el servicio `minio-init` del compose, así que al levantar MinIO a mano hay que crearlo o la subida de documentos da `500`.
- **Sin probar todavía:** la app móvil, y reenviar una postulación devuelta (que reinicia los intentos; solo lo cubre un test unitario).

### Estado de B8 (CU-02)

Código y pruebas escritos (backend, web y móvil). Pasan: los tests unitarios del backend nuevos (`EnvioPostulacionServiceTest`, `PrecalificacionServiceTest`), checkstyle, lint, `tsc` y los tests de `features/postulaciones` del frontend, y `flutter analyze` y `flutter test` de la app. **Pendiente para marcarla `[x]`:** correr con Docker `EnvioPostulacionConsultaRepositoryTest` (la migración y el `UPSERT` de `respuesta_socioeconomica`, que no se ejecutaron) y una prueba manual con el stack levantado. **B3 (la API del modelo de IA) no existe todavía:** hasta que exponga `POST /api/v1/puntaje` toda postulación queda `pendiente` y el job la reintenta cada 5 minutos; el contrato está en [Implementacion/CU02](Implementacion/CU02%20-%20Enviar%20postulaci%C3%B3n%20a%20la%20Beca%20Comedor.md). No incluye «Guardar borrador» (B9). Detalle en esa misma ficha.

### Estado de B9 (CU-17)

Código y pruebas escritos (backend, web y móvil). Pasan: los tests unitarios del backend (`BorradorPostulacionServiceTest`, y `EnvioPostulacionServiceTest` tras extraer las reglas de valor), checkstyle, lint, `tsc` y los 114 tests de `features/postulaciones` del frontend, y `flutter analyze` y los tests de `test/features/postulaciones` de la app. **Pendiente para marcarla `[x]`:** correr con Docker `EnvioPostulacionConsultaRepositoryTest` (`guardadoEn_…` y `borradorAbierto_…`) y `BorradorPostulacionControllerTest` (compilan, pero necesitan Postgres y no se ejecutaron), y una prueba manual con el stack levantado. No hay migración nueva. **No incluye** «Eliminar» el borrador (botón del mockup que el CU no describe) ni limpieza de borradores de convocatorias cerradas (quedan bloqueados). Detalle en [Implementacion/CU17](Implementacion/CU17%20-%20Guardar%20borrador%20de%20postulaci%C3%B3n.md).

### Estado de B4 (CU-26)

Código y pruebas escritos (backend y web; el lado de Python de la IA queda para el final, como en B8). Pasan: los tests unitarios del backend nuevos (`ModeloPreseleccionServiceTest`, `VersionModeloTest`, `ReprocesamientoServiceTest`, `ReprocesamientoEjecucionServiceTest` y `PrecalificacionServiceTest` actualizado), checkstyle, `tsc`, ESLint y los tests de `features/preseleccion` y `adminNavItems` del frontend. **Pendiente para marcarla `[x]`:** correr con Docker `ModeloPreseleccionConsultaRepositoryTest`, `ReprocesamientoConsultaRepositoryTest` y `PrecalificacionConsultaRepositoryTest` (la migración `V20261120100000`, el índice único de un lote en curso y las consultas, que no se ejecutaron) y una prueba manual con el stack levantado. **Depende de la IA (B3), que no existe todavía:** su `POST /api/v1/puntaje` debe aplicar los `modelo` y `parametros` que ahora recibe, repetir la versión en la respuesta y devolver el puntaje normalizado a 0–70 (las ponderaciones suman 100 pero la tabla admite hasta 70; regla por definir con la DUBSS). Contrato y diferencias en [Implementacion/CU26](Implementacion/CU26%20-%20Parametrizar%20modelo%20de%20preselecci%C3%B3n%20y%20reprocesar%20puntajes.md). El puntaje mínimo para ser finalista no se implementa aquí (es de la convocatoria, CU13).

### Por qué este orden

- **B5 → B6 → B7 primero:** no dependen de nada pendiente y son la base de B8
  (la tarea más grande, 8 pts).
- **B1 (semilla) en 4.º lugar:** se arranca con lo que ya existe
  (convocatoria, postulación y puntaje) y se amplía con becas y asistencias
  cuando existan B10 y B12.
- **B2 → B3 (IA) antes de B8:** B8 invoca la API de B3 al enviar la
  postulación. Se puede alternar con B5–B7 si se quiere.
- **B15 en el medio:** no depende de nada y es corto; sirve de pausa entre la
  parte de postulación y la de becados.
- **B10 → B12 → B13/B14 al final:** la cadena de becados y asistencia es
  independiente de la de postulación.

## Relaciones UML que quedan dentro de esta lista

- CU-02 «include» CU-03, CU-16, CU-18
- CU-17 «extend» CU-02
- CU-26 «extend» CU-03
- CU-11 «extend» CU-10

## Tablas que escribe la Persona B

`postulante`, `convocatoria`, `gestion_academica`, `postulacion`,
`documento_adjunto`, `borrador`, `puntaje_ia`, `lote_puntaje`,
`parametro_modelo`, `becado`, `planilla_asistencia`, `registro_asistencia`,
`cotejo`.

Los nombres son orientativos: se ajustan a las tablas reales del diseño lógico
(`images/091_Diseno_Logico_Base_de_Datos.jpg` y
[DiagramaConceptual.mermaid](DiagramaConceptual.mermaid)). Algunas ya existen
por migraciones de la Persona A (convocatoria, postulación, puntaje), así que
antes de crear una tabla se revisa `db/migration/`.

La Persona A solo **lee** esas tablas. Excepción acordada: los CU-19, CU-20 y
CU-05 ya cambian el **estado** de `postulacion` con una operación definida.

## Contratos con la Persona A

| # | Contrato | Quién escribe | Quién lee |
| --- | --- | --- | --- |
| C1 | `postulacion` + estado + `puntaje_ia` | B (B3, B8) | A |
| C2 | Devolución / rechazo / archivo (con observaciones) | A | B (reenvío en B8) |
| C3 | Documentos adjuntos (ruta y metadatos en `documento_adjunto`) | B (B7) | A |
| C4 | Adjudicados → padrón de becados (estado `adjudicada` + fecha) | A (hecho) | B (B10) |
| C6 | Asistencia y becas (lectura para BI) | B (B10, B12) | A (dashboard) |
| C7 | Tabla `bitacora` | A (hecho) | B (B15) |

## Estados de la postulación (decisión del plan)

Usar el enum del modelo de dominio (`pendiente`, `devuelta`, `archivada`,
`calificada`, `adjudicada`, `no adjudicada`) y agregar solo `borrador`
(CU-17). Flujo: `borrador → pendiente → calificada → (devuelta | archivada |
adjudicada | no adjudicada)`; `devuelta` vuelve a `pendiente` al reenviarse.

## Pendientes de la DUBSS (no inventar; se parametrizan)

Umbral mínimo de puntaje, número de cupos y pesos del modelo.

## Hitos de integración

- **H2:** B8 y B3 listos → la Persona A reemplaza los datos semilla por
  postulaciones reales.
- **H4:** B12 listo → la Persona A usa asistencia real en el dashboard.
- **H6:** prueba end-to-end local con los dos: postular → puntuar → priorizar →
  auditar → adjudicar → asistencia → BI.
