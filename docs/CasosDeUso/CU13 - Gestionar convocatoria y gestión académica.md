# Caso de uso

| Id   | Nombre                                      | Módulo                         | Prioridad | Estado      |
| ---- | ------------------------------------------- | ------------------------------ | --------- | ----------- |
| CU13 | Gestionar convocatoria y gestión académica  | P2. Convocatoria y postulación | Alta      | En progreso |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Crear, editar, abrir y cerrar las convocatorias de la Beca Comedor, y mantener las gestiones académicas a las que pertenecen, definiendo fechas, cupos y los documentos que se exigen, para controlar el periodo en que se reciben postulaciones.
**Plataforma:** Web
**RF asociados:** RF-08

**Descripción:**
El Administrador DUBSS entra a «Convocatorias» y ve la lista con la gestión académica, las fechas, los cupos y el estado de cada una (borrador, abierta o cerrada). Puede crear una convocatoria o editar una existente indicando su gestión académica, la fecha de apertura, la de cierre, el número de cupos y la lista de documentos obligatorios. También puede abrirla, para que reciba postulaciones, y cerrarla. El sistema valida que el cierre sea posterior a la apertura, que no haya otra convocatoria vigente en la misma gestión y que se haya definido al menos un documento obligatorio; si algo falla, lo informa y no guarda. Cada operación queda en la bitácora ([CU-25](CU25%20-%20Consultar%20bitácora%20de%20acciones.md)). Una convocatoria se cierra sola cuando llega su fecha de cierre, y una convocatoria con postulaciones nunca se elimina: solo se cierra.

Solo se puede recibir una postulación ([CU-02](CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md)) mientras la convocatoria está abierta. Este CU es prerequisito de CU-02 y [CU-18](CU18%20-%20Validar%20elegibilidad%20y%20duplicados.md). El [CU-05: Auditar y adjudicar becas](CU05%20-%20Auditar%20y%20adjudicar%20becas.md) exige una convocatoria **cerrada** con cupos y puntaje mínimo definidos, y usa la cantidad de finalistas que se configura acá.

> **Fuentes consultadas** (documentación de grado): `images/042` (CU), `055` (análisis), `125` (secuencia), `138` (mockup) y el detalle del CU-13 y el RF-08 en los capítulos 4, 5.4 y 7. Lo marcado como _(propuesta)_ no figura en ninguna de ellas y debe confirmarse. Los puntos en que las fuentes no coinciden entre sí están en [Decisiones tomadas y puntos abiertos](#decisiones-tomadas-y-puntos-abiertos).

---

## Precondiciones

- El Administrador DUBSS tiene sesión iniciada ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) y rol `ADMINISTRADOR_DUBSS`.
- Existe al menos una gestión académica para asociar la convocatoria (el propio CU permite crearla, ver decisión 1).

## Flujo principal

1. El Administrador accede a la opción «Convocatorias».
2. El sistema muestra la lista de convocatorias con su gestión académica, fechas de inicio y fin de postulación, cupos y estado.
3. El Administrador elige la acción: crear, editar, abrir, cerrar, eliminar o consultar una convocatoria, o administrar las gestiones académicas (listar, crear y editar).
4. Para crear o editar, ingresa el nombre, la gestión académica, la fecha de apertura, la de cierre, los cupos (por definir con la DUBSS), el puntaje mínimo y la cantidad de finalistas (ambos opcionales), y la lista de documentos obligatorios (tipos de documento y si son obligatorios).
5. El sistema valida que el cierre sea posterior a la apertura, que no exista otra convocatoria vigente para la misma gestión y que haya al menos un documento obligatorio.
6. El Administrador confirma.
7. El sistema guarda la convocatoria y sus documentos requeridos, actualiza su estado y registra la acción en la bitácora.
8. El sistema confirma la operación y muestra la lista actualizada.

## Flujos alternativos y excepciones

Los códigos A1 a A7 son los de las fuentes de grado; A8 en adelante son de este documento.

- **A1 (paso 5):** ya existe una convocatoria vigente para la misma gestión → el sistema rechaza con `409` e informa el conflicto.
- **A2 (paso 5):** fechas incoherentes (cierre igual o anterior a la apertura), datos obligatorios vacíos, valores fuera de rango o ningún documento obligatorio → `400` indicando cada error, para corregirlos.
- **A3 (paso 3):** al **abrir**, el sistema pasa la convocatoria a `ABIERTA` y habilita el envío de postulaciones desde esa fecha.
- **A4 (paso 3):** al **cerrar**, el sistema pasa la convocatoria a `CERRADA`, bloquea el envío de nuevas postulaciones y descarta o bloquea los borradores no enviados ([CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md)).
- **A5 (paso 3):** cuando se cumple la fecha de cierre, el sistema cierra la convocatoria automáticamente, con los mismos efectos que A4.
- **A6 (paso 4):** si la convocatoria ya recibió postulaciones, el sistema restringe la edición de los documentos obligatorios y de la fecha de apertura para no afectarlas (`409`). _(El alcance exacto está por definir con la DUBSS.)_
- **A7 (paso 3):** el sistema no permite eliminar una convocatoria con postulaciones asociadas; solo cerrarla (`409`).
- **A8:** gestión, convocatoria o tipo de documento inexistente o eliminado → `404`.
- **A9:** la operación no corresponde al estado actual _(propuesta: solo se edita una convocatoria en `BORRADOR` o `ABIERTA`; solo se abre una `BORRADOR`; solo se cierra una `ABIERTA`; una `CERRADA` o `PUBLICADA` no se modifica; solo se elimina una `BORRADOR`)_ → `409`.
- **A10:** no es Administrador DUBSS → `403`; sin sesión o con sesión vencida → `401`.

## Reglas de negocio

- **Una convocatoria vigente por gestión académica** (RF-08): no puede haber dos `ABIERTA` en la misma gestión (ya garantizado por un índice único parcial en la base). Ver decisión 3.
- **Fechas:** el cierre es posterior a la apertura.
- **Cupos:** entero positivo, opcional mientras la DUBSS no los defina; no se inventa un valor por defecto. **Puntaje mínimo:** entre 0 y 100, opcional por la misma razón. El CU-05 exige ambos para adjudicar.
- **Cantidad de finalistas:** entero positivo, opcional; se propone al seleccionar finalistas en el CU-05 y puede ser mayor que los cupos (suplentes). **Solo el Administrador DUBSS la ve**: ninguna consulta del postulante ni pública la devuelve. _(Propuesta del CU-05, por revisar: no menor que los cupos cuando ambos existen.)_
- **Documentos requeridos:** una convocatoria tiene su propia lista de tipos de documento, cada uno marcado como obligatorio o no; se exige al menos un obligatorio. El postulante adjunta esos documentos en [CU-16](CU16%20-%20Digitalizar%20y%20adjuntar%20documentos.md).
- **Estados:** `BORRADOR → ABIERTA → CERRADA`, sin vuelta atrás _(propuesta sobre las transiciones; los tres estados están en el diseño lógico y el mockup)_. Un cuarto estado, `PUBLICADA`, lo pone el [CU-22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md) cuando se publican los resultados: desde ahí la convocatoria ya no se modifica en este CU.
- **Gestiones académicas:** etiqueta única (no vacía, hasta 50 caracteres), fecha de inicio y fecha de fin con el fin posterior al inicio. Se pueden crear y editar; no se eliminan desde este CU.
- **Envío de postulaciones:** este CU solo fija el estado y las fechas. Que una postulación se acepte únicamente con la convocatoria `ABIERTA` y dentro de sus fechas lo comprueban los CU de postulación ([CU-02](CU02%20-%20Enviar%20postulación%20a%20la%20Beca%20Comedor.md), [CU-17](CU17%20-%20Guardar%20borrador%20de%20postulación.md)); un borrador de una convocatoria cerrada queda bloqueado porque ya no se puede enviar (A4).
- **Cierre automático (A5):** lo hace el sistema, no una persona; queda anotado en la bitácora como acción del sistema _(propuesta de cómo se registra)_.
- **Borrado lógico:** nunca se borra una fila; se marca `eliminado_en` ([PERSISTENCIA_BD_BACKEND.md](../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)).
- **Bitácora:** cada operación (crear, editar, abrir, cerrar) se registra ([AUDITORIA_BITACORA_BACKEND.md](../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md)).

## Postcondiciones

- La convocatoria queda registrada o actualizada con su gestión, fechas, cupos y documentos requeridos.
- Su estado determina si el sistema acepta o no nuevas postulaciones.
- La operación queda en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que soy Administrador DUBSS con datos válidos, cuando creo una convocatoria, el sistema debe guardarla en estado `BORRADOR` con sus documentos requeridos y mostrarla en la lista.
- **CA02:** Dado que el cierre es igual o anterior a la apertura, el sistema debe responder `400` y no guardar.
- **CA03:** Dado que no hay ningún documento obligatorio, el sistema debe responder `400` y no guardar.
- **CA04:** Dado que cupos, puntaje mínimo o cantidad de finalistas están fuera de rango, el sistema debe responder `400`.
- **CA05:** Dado que ya hay una convocatoria `ABIERTA` en la misma gestión, cuando intento abrir otra, el sistema debe responder `409`.
- **CA06:** Dado que una convocatoria está en `BORRADOR` y no hay otra abierta en su gestión, cuando la abro, el sistema debe pasarla a `ABIERTA`.
- **CA07:** Dado que una convocatoria está `ABIERTA`, cuando la cierro, el sistema debe pasarla a `CERRADA`, rechazar nuevas postulaciones y bloquear los borradores no enviados.
- **CA08:** Dado que llega la fecha de cierre de una convocatoria `ABIERTA`, el sistema debe cerrarla automáticamente con los mismos efectos que CA07.
- **CA09:** Dado que la convocatoria ya tiene postulaciones, cuando intento cambiar sus documentos obligatorios o su fecha de apertura, el sistema debe responder `409`.
- **CA10:** Dado que la convocatoria tiene postulaciones, cuando intento eliminarla, el sistema debe responder `409`; solo puede cerrarse.
- **CA11:** Dado que una convocatoria está `CERRADA`, cuando intento abrirla o editarla, el sistema debe responder `409`.
- **CA12:** Dado que quien llama no es Administrador DUBSS, el sistema debe responder `403`; sin sesión, `401`.
- **CA13:** Dado que consulta un postulante o alguien sin sesión, el sistema no debe devolver la cantidad de finalistas.
- **CA14:** Dado que se crea, edita, abre, cierra o elimina una convocatoria (también el cierre automático) o se crea o edita una gestión académica, el sistema debe anotarlo en la bitácora.
- **CA15:** Dado que ingreso etiqueta, fecha de inicio y fecha de fin válidas, cuando creo una gestión académica, el sistema debe guardarla y ofrecerla al crear convocatorias.
- **CA16:** Dado que la etiqueta ya existe, o la fecha de fin no es posterior a la de inicio, el sistema debe responder `409` o `400` respectivamente y no guardar.
- **CA17:** Dado que una convocatoria está en `BORRADOR` y no tiene postulaciones, cuando la elimino, el sistema debe quitarla de la lista (borrado lógico).

## Decisiones tomadas y puntos abiertos

Decididas con el Desarrollador (propuestas aceptadas):

1. **Gestión académica: sí se crea desde este CU.** El título y el RF-08 la mencionan, pero el flujo, la secuencia y el mockup solo la muestran como selector y hoy ninguna pantalla la crea. Se incluye el alta y la edición de gestiones (etiqueta, inicio y fin). _El mockup y la secuencia no la dibujan: es una diferencia con los diagramas que se registra en `docs/Implementacion/`._
2. **Abrir una convocatoria: acción propia.** El mockup solo tiene «Editar» y «Cerrar» y, en el formulario, un selector de estado. Se agrega una acción «Abrir» en las convocatorias en borrador y el estado se cambia solo con las acciones «Abrir» y «Cerrar» (el formulario no lleva el selector de estado que dibuja el mockup). _Diferencia con el mockup._
3. **«Una abierta a la vez»: por gestión.** El RF-08 y la secuencia dicen por gestión; el mensaje del mockup dice «a la vez». Se implementa por gestión (lo que ya garantiza la base).
4. **Estado inicial:** `BORRADOR` (el flujo escrito dice «programada»; el diseño lógico y el mockup dicen «borrador»).
5. **Fechas:** la apertura rige desde las 00:00 y el cierre hasta las 23:59 del día, en hora de Bolivia (`America/La_Paz`); el mockup muestra solo la fecha y la base guarda hora.
6. **Campos que el mockup no muestra** y se agregan al formulario: puntaje mínimo, cantidad de finalistas y documentos requeridos.
7. **Eliminar** (que A7 da por existente pero el flujo del grado no lista): solo una convocatoria en `BORRADOR` y sin postulaciones; borrado lógico.

Abiertos, por definir con la DUBSS (no se inventan; se dejan parametrizados):

- El número de **cupos** y el **puntaje mínimo**: pueden quedar vacíos hasta que los defina.
- El alcance exacto de la edición cuando ya hay postulaciones (A6): hoy se bloquean los documentos y la fecha de apertura.

## Desarrollador

Leonel Barrios
