# Caso de uso

| Id   | Nombre                                                  | Módulo                      | Prioridad | Estado      |
| ---- | ------------------------------------------------------- | --------------------------- | --------- | ----------- |
| CU26 | Parametrizar modelo de preselección y reprocesar puntajes | P3. Preselección con IA   | Media     | En progreso |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** Módulo de IA
**Objetivo:** Que el Administrador ajuste las ponderaciones que usa el modelo de preselección y recalcule los puntajes de las postulaciones de una convocatoria.
**Plataforma:** Web
**RF asociados:** RF-21 (extiende al CU-03 (precalificación con IA) del RF-03)

**Descripción:**
El Administrador DUBSS entra a «Parámetros del modelo» y ve la versión vigente con la ponderación (puntos máximos) de cada variable: ingresos familiares, dependientes, procedencia, patrimonio, estado de vulnerabilidad y promedio académico. Escribe las nuevas ponderaciones; el sistema las valida (deben sumar 100) y, al guardar, crea una **versión nueva** que pasa a ser la vigente y conserva las anteriores. Después puede elegir una convocatoria y pedir «Reprocesar puntajes»: el sistema le muestra cuántas postulaciones calificadas se recalcularán, pide confirmación y las recalcula **en segundo plano** con la versión vigente, mostrando el avance. El puntaje anterior de cada postulación se conserva como historial. Si resultan postulaciones que no se pudieron calcular, las lista y permite reintentarlas. No se reprocesa una convocatoria con resultados ya publicados ni mientras haya otro reprocesamiento en curso. Quien calcula cada puntaje es el módulo de IA (CU-03 (precalificación con IA)); este CU solo decide con qué parámetros y a qué postulaciones.

---

## Precondiciones

- El Administrador está autenticado ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) con el rol `ADMINISTRADOR_DUBSS` y su cuenta está activa.
- Para reprocesar: hay una versión vigente del modelo y la convocatoria tiene postulaciones en estado `calificada` (CU-03 (precalificación con IA)).
- Para que el recálculo termine: el módulo de IA está disponible.

## Flujo principal

1. El Administrador selecciona la opción «Parámetros del modelo».
2. El sistema muestra la versión vigente con las ponderaciones de cada variable y el historial de versiones.
3. El Administrador modifica las ponderaciones que necesita (el valor «Nuevo» de cada variable).
4. El sistema muestra la suma de las ponderaciones y la valida mientras se escribe.
5. El Administrador pulsa «Guardar versión» y el sistema crea la versión siguiente (`v{M}.{n+1}`), que pasa a ser la vigente, y archiva la anterior.
6. El Administrador elige una convocatoria y pulsa «Reprocesar puntajes».
7. El sistema muestra el resumen (convocatoria, versión con la que se recalcula y cantidad de postulaciones por recalcular) y pide confirmación.
8. El Administrador confirma.
9. El sistema crea el reprocesamiento y lo ejecuta en segundo plano, invocando el cálculo del CU-03 (precalificación con IA) para cada postulación, y muestra el avance.
10. Por cada postulación, el sistema registra el puntaje nuevo con su fecha de cálculo y conserva el anterior como historial.
11. Al terminar, el sistema muestra el resultado (procesadas y fallidas) y el acceso a la lista priorizada ([CU-06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md)); la acción queda en la bitácora ([CU-25](CU25%20-%20Consultar%20bit%C3%A1cora%20de%20acciones.md)).

## Flujos alternativos y excepciones

- **A1 (paso 4 o 5):** una ponderación está fuera de 0–100, es repetida o desconocida, falta una variable o la suma no da 100 → el botón «Guardar» queda bloqueado y, si el servidor las recibe, `400` `ERR_MOD_01` con lo que falla; no se guarda nada.
- **A2 (paso 5):** el Administrador guarda sin reprocesar → la versión nueva se aplica solo a las postulaciones que se precalifiquen desde ese momento; los puntajes existentes no cambian.
- **A3 (paso 6 u 8):** la convocatoria ya tiene resultados publicados ([CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicaci%C3%B3n.md)) → se bloquea el reprocesamiento: `409` `ERR_MOD_04`.
- **A4 (paso 6 u 8):** ya hay un reprocesamiento en curso para esa convocatoria → `409` `ERR_MOD_03`; la pantalla muestra el avance del que está en curso.
- **A5 (paso 9):** el cálculo falla para algunas postulaciones → el sistema continúa con las demás, cuenta las fallidas y, al volver a reprocesar, toma solo las que faltan. Si el módulo de IA no está disponible, el reprocesamiento se corta y queda `FALLIDO`.
- **A6 (paso 7):** el Administrador cancela antes de confirmar → no se crea nada ni se modifica ningún puntaje.
- **A7 (paso 6 u 8):** todavía no hay versión vigente → `409` `ERR_MOD_02`: primero se guardan los parámetros.
- **A8 (paso 6 u 8):** no hay postulaciones calificadas por recalcular con la versión vigente → `409` `ERR_MOD_06`.
- **A9 (paso 5):** otra versión se guardó al mismo tiempo → `409` `ERR_MOD_07`; hay que volver a abrir los parámetros.
- **A10 (cualquier paso):** el usuario no tiene el rol Administrador DUBSS → `403` `ERR_AUTH_07`. La sesión vencida da `401` y el cliente renueva o lleva al login ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)).
- **A11 (al reiniciar el servidor):** un reprocesamiento que quedó en curso con el servidor apagado pasa a `FALLIDO`; volver a reprocesar continúa con las que faltan.

## Reglas de negocio

- Las variables y su orden son las del mockup más las del formulario oficial de la DUBSS (dependencia económica, ocupación de quien sostiene, discapacidad e hijos); la ponderación de cada una es un número de 0 a 100 con hasta 2 decimales y las diez **suman 100**. Los valores y rangos definitivos están **por definir con la DUBSS**.
- Cada versión es inmutable: guardar crea una nueva (`v{M}.{n+1}`, `v1.0` la primera) y archiva la anterior; solo una es la vigente.
- «Borrador» es únicamente lo que se escribe en pantalla sin guardar; no existe como versión.
- Reprocesar usa siempre la versión **vigente** y toma las postulaciones `calificada` de la convocatoria que no tienen un puntaje vigente de esa versión (por eso repetirlo reintenta solo las fallidas). No cambia el estado de la postulación.
- Cada recálculo conserva el puntaje anterior (`puntaje_postulacion` es un historial, solo uno vigente por postulación; RF-03, RF-21).
- Un solo reprocesamiento en curso por convocatoria.
- Lo que el módulo de IA devuelve como puntaje debe estar entre 0 y 70 (el resto del total, hasta 100, lo completa el auditor en el [CU-07](CU07%20-%20Auditar%20expediente%20finalista.md)): cómo se traducen las ponderaciones de 100 puntos a ese tope está **por definir con la DUBSS**.
- El puntaje mínimo para ser finalista es de cada convocatoria ([CU-13](CU13%20-%20Gestionar%20convocatoria%20y%20gesti%C3%B3n%20acad%C3%A9mica.md)), no del modelo.
- La bitácora anota la nueva versión (`NUEVA_VERSION_MODELO`) y el reprocesamiento (`REPROCESAMIENTO_PUNTAJES`) con códigos, versión y cantidades; nunca datos de postulantes. Ver [AUDITORIA_BITACORA_BACKEND.md](../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- Las lecturas (versión vigente, historial, resumen, avance) no se anotan en la bitácora.

## Postcondiciones

- Los nuevos parámetros quedan guardados como la versión vigente y las versiones anteriores se conservan.
- Los puntajes reprocesados quedan con su fecha de recálculo y el puntaje anterior se conserva; la lista priorizada refleja el nuevo orden.
- El reprocesamiento queda registrado con su estado y su avance, y la acción queda en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que hay una versión vigente, el sistema debe mostrar sus ponderaciones, su versión y el historial de versiones con fecha y estado.
- **CA02:** Dado que las ponderaciones no suman 100 o alguna está fuera de rango, el sistema debe rechazar el guardado con `400` `ERR_MOD_01` y no crear ninguna versión.
- **CA03:** Dado que las ponderaciones son válidas, el sistema debe crear la versión siguiente como vigente y conservar la anterior como archivada.
- **CA04:** Dado que se guarda una versión sin reprocesar, el sistema no debe modificar ningún puntaje existente.
- **CA05:** Dado que se confirma el reprocesamiento de una convocatoria, el sistema debe recalcular en segundo plano las postulaciones `calificada` sin puntaje de la versión vigente, conservar el puntaje anterior de cada una y mostrar el avance.
- **CA06:** Dado que los resultados de la convocatoria ya fueron publicados, el sistema debe responder `409` con `ERR_MOD_04` y no reprocesar.
- **CA07:** Dado que ya hay un reprocesamiento en curso para la convocatoria, el sistema debe responder `409` con `ERR_MOD_03`.
- **CA08:** Dado que el cálculo falla para algunas postulaciones, el sistema debe continuar con las demás, informar cuántas fallaron y, al reprocesar de nuevo, tomar solo las que faltan.
- **CA09:** Dado que el Administrador cancela antes de confirmar, el sistema no debe modificar ningún puntaje.
- **CA10:** Dado que el usuario no es Administrador DUBSS, el sistema debe responder `403`.
- **CA11:** Dado que se guarda una versión o se reprocesa, el sistema debe anotar la acción en la bitácora.

## Desarrollador

Leonel Barrios
