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
| [ ] | 4 | B1 | Generador de datos semilla: postulaciones con puntaje, adjudicados, becas y asistencias de prueba (volumen 20.000) | extra | todos | 2 | Backend | — |
| [ ] | 5 | B2 | IA: dataset de ajuste, entrenamiento y validación del modelo (variables socioeconómicas, Python) | CU-03 | RF-03 | 5 | IA | contrato de la interfaz de IA |
| [ ] | 6 | B3 | IA: API del modelo y cálculo por lote (puntaje ≤ 5 s con fecha de cálculo, 20.000 postulaciones) | CU-03 | RF-03 | 3 | IA | B2 |
| [ ] | 7 | B8 | Enviar postulación a la Beca Comedor (formulario socioeconómico largo; bloquea si faltan campos/documentos; invoca la API de B3) | CU-02 | RF-02 | 8 | Web + Móvil | B5, B6, B7, B3 (por contrato) |
| [ ] | 8 | B9 | Guardar borrador de postulación (persistencia parcial, recuperar borrador) | CU-17 | RF-12 | 3 | Web + Móvil | B8 |
| [ ] | 9 | B4 | Parametrizar el modelo y reprocesar puntajes (recálculo masivo en segundo plano) | CU-26 | RF-21 | 5 | Web + IA | B3 |
| [x] | 10 | B10 | Gestionar padrón de becados (alta desde adjudicados, baja, suspensión, reemplazo) | CU-23 | RF-18 | 5 | Web | adjudicaciones de CU-05 (ya hecho) |
| [x] | 11 | B15 | Consultar bitácora de acciones (filtros sobre la tabla `bitacora`, que ya existe) | CU-25 | RF-20 | 2 | Web | — |
| [ ] | 12 | B11 | Generar planilla de asistencia imprimible (firma + fotocopia de carnet) | CU-09 | RF-05 | 3 | Web | B10 |
| [ ] | 13 | B12 | Registrar asistencia digital: **app móvil** del Personal del Comedor, ingresa CI del becado y confirma; idempotente (RNF-03) | CU-10 | RF-05 | 5 | Móvil | B10 |
| [ ] | 14 | B13 | Cotejar registro digital contra la planilla física | CU-11 | RF-05 | 5 | Web | B12 |
| [ ] | 15 | B14 | Consultar historial de asistencia y estado de beca (solo lectura) | CU-24 | RF-19 | 2 | Web + Móvil | B12 |

### Estado de B5 (CU-13)

Código y pruebas escritos (backend y web). Pasan: 98 tests unitarios del backend nuevos, checkstyle, lint, `tsc` y los 936 tests del frontend. **Pendiente para marcarla `[x]`:** correr con Docker los tests de base real (`ConvocatoriaRepositoryTest`, `ConvocatoriaIntegrationTest` y los controller con contexto de Spring, que no corren en esta máquina) y una prueba manual con el stack levantado. Detalle en [Implementacion/CU13](Implementacion/CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md).

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
