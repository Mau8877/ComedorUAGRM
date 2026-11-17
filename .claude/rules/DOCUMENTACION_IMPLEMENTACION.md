---
globs: docs/DOCUMENTACION.md, docs/Implementacion/**/*
---

# Documentación de la implementación (del diagrama al código)

Los diagramas del documento de grado (análisis, secuencia, clases, mockup) se
hicieron **antes** de implementar, y al construir cada caso de uso (CU) parte
de lo que dibujaron ya no coincide con lo que hace el sistema. Esta
documentación cumple dos funciones distintas, y cada una vive en su lugar:

1. **Búsqueda rápida: del diagrama al código.** Ante una pregunta como «en el
   diagrama de secuencia aparece `registrarAuditoria()`, ¿dónde está en el
   código?», se abre `docs/DOCUMENTACION.md`, se busca el nombre tal como
   figura en el diagrama y se llega al archivo y método con un clic.
2. **Registro de diferencias: del código al diagrama.** Qué hace de verdad el
   sistema y en qué se aparta de los diagramas, para poder rehacerlos y
   actualizar la documentación de grado sin leer el código.

Se aplica a todo CU que se implemente o se cambie. La fuente de verdad del
comportamiento es el código y el CU en `docs/CasosDeUso/`; los diagramas son
lo que se corrige a partir de este registro.

## Estructura

```
docs/
├── DOCUMENTACION.md                         <- búsqueda rápida (función 1): un solo archivo con TODOS los CU
└── Implementacion/
    └── CU{NN} - {Nombre corto}.md           <- diferencias y flujo real (función 2), uno por CU
```

- **`DOCUMENTACION.md` se mantiene como un único archivo** con el mapa de
  código de todos los CU, para poder buscar con `Ctrl+F` sin abrir varios
  archivos. No se parte por CU.
- **`docs/Implementacion/CU{NN}...md` guarda lo que no es un mapa:** el flujo
  real, la tabla de diferencias y la lista de diagramas por rehacer. No repite
  los enlaces de código; remite a la sección del CU en `DOCUMENTACION.md`.
- El archivo de implementación se crea cuando el CU pasa a `Hecho` o cuando
  una UT cambia algo que un diagrama ya mostraba; no se deja uno vacío "para
  después". Los CU ya documentados antes de esta rule no lo tienen todavía: se
  les crea la próxima vez que se toquen.

## Cuándo se actualiza

**En el mismo cambio** que toca el código, como el resto de la documentación
(mismo criterio que `docs/errors/` en
[GUIA_ERRORES_BACKEND.md](backend/GUIA_ERRORES_BACKEND.md)). Al terminar la
última UT de un CU, y también cuando una UT posterior cambia su
comportamiento. Un CU `Hecho` sin su sección en `DOCUMENTACION.md`, o con una
que apunta a código que ya no existe, está incompleto.

Claude, al terminar un CU o cambiar su comportamiento, actualiza
`DOCUMENTACION.md` y el archivo de implementación sin que se lo pidan, y avisa
qué diferencias nuevas con los diagramas registró.

## Función 1: `docs/DOCUMENTACION.md`

Cada CU tiene su sección (ancla `cuNN`) con la tabla de contenido arriba, como
hasta ahora: caso de uso, UT, secciones e imágenes del documento de taller, y
las viñetas por diagrama (análisis, secuencia, clases, mockup, pruebas) con
enlaces a los archivos. A eso se agrega **una tabla de elementos del
diagrama**, que es lo que permite buscar por nombre:

```markdown
- **Elementos de los diagramas → código:**

  | En el diagrama | Dónde está en el código |
  | --- | --- |
  | `ExpedienteController` (análisis, img 079) | [`ExpedienteController.java`](ruta) |
  | `registrarAuditoria()` (secuencia, img 094) | [`ExpedienteService.registrar`](ruta#L120) |
  | `AuditoriaExpediente` (clases, img 090) | [`AuditoriaExpediente.java`](ruta) |
  | Botón «Guardar auditoría» (mockup, img 107) | [`AuditoriaForm.tsx`](ruta#L45) |
```

- **La columna izquierda lleva el nombre exacto que dibuja el diagrama**
  (clase, mensaje de la secuencia, atributo, botón del mockup), aunque el
  código lo llame distinto. Por eso incluye los nombres que no coinciden (el
  caso de `EstadoPostulacionController` → `MisPostulacionesController`); si el
  elemento del diagrama **no existe** en el código, la fila lo dice
  («no implementado: ...») en vez de omitirse.
- **La columna derecha apunta al método o a la línea** (`#L120`) cuando el
  elemento es una función, no solo al archivo. Se verifica que el enlace lleve
  a donde dice.
- Un elemento que el código tiene pero el diagrama no dibuja **no va acá**: va
  en la tabla de diferencias del archivo de implementación.
- Se listan los elementos que un docente puede preguntar: las clases de
  análisis, cada mensaje de la secuencia, las clases y atributos del diagrama
  de clases, y los controles del mockup. No se listan getters ni detalles
  triviales.

## Qué diagramas se revisan y qué cambio los afecta

Al implementar o cambiar un CU se revisan **siempre los seis tipos**, no solo
los que parecen obvios. Las imágenes son las de la documentación de grado
(ver [CASOS_DE_USO.md](CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)).

| Diagrama | Imágenes | Se corrige cuando el código... |
| --- | --- | --- |
| **Secuencia** | `092`–`104`, `121`–`133` | agrega, quita o reordena un paso o mensaje; agrega una validación o un flujo de error; usa un componente que el diagrama no tiene (job, caché, MinIO, IA, correo, FCM); hace algo en segundo plano |
| **Análisis (comunicación)** | `051`–`063`, `077`–`089` | agrega o quita una clase (vista, controlador, entidad), cambia sus nombres o con quién se comunica |
| **Mockup** | `105`–`117`, `134`–`146` | cambia campos, botones, textos, filtros, columnas o la forma de una pantalla |
| **Clases / dominio** | `090`, `018` | cambia un model, un enum, un atributo, un tipo o una relación entre clases |
| **Diseño lógico de la base de datos** | `091` (+ [DiagramaConceptual.mermaid](../../docs/DiagramaConceptual.mermaid)) | crea o cambia una tabla, columna, restricción, índice o enum en una migración |
| **Casos de uso** | `019` (general), `038`–`050`, `064`–`076` | cambia un actor, una relación `include`/`extend` o el alcance del CU |

Además, dos casos que no son un diagrama de CU pero se tratan igual:

- **Arquitectura** (`118`–`120`): solo cuando se suma una integración o pieza
  de infraestructura nueva (un servicio externo, un job programado, un
  módulo). No cambia por un CU común.
- **Texto de grado**: `casos_de_uso_filtrados.md` y los RF (§ de requisitos),
  si el cambio modifica el alcance de un CU o un requisito.

Reglas para decidirlo:

- **Se evalúa cada tipo y se anota el resultado**, incluso «sin cambios». Un
  tipo no evaluado no es lo mismo que uno sin cambios.
- **El diseño lógico no espera.** Si una migración cambia el modelo de datos,
  `DiagramaConceptual.mermaid` se actualiza en ese mismo cambio (ya lo exige
  [PERSISTENCIA_BD_BACKEND.md](backend/PERSISTENCIA_BD_BACKEND.md#fuente-de-verdad-del-modelo-de-datos));
  la imagen `091` es lo que queda por rehacer.
- **Cuándo se revisa:** al cerrar cada UT que toca código se anotan los
  cambios que ya se ven, y al terminar el CU se hace la pasada completa de los
  seis tipos. Así las diferencias se registran mientras se recuerdan, no al
  final.
- Un mismo cambio puede afectar varios diagramas (un campo nuevo suele tocar
  mockup, clases, diseño lógico y secuencia). Se lista en cada uno.

## Función 2: `docs/Implementacion/CU{NN} - {Nombre}.md`

```markdown
# CU{NN} — {Nombre}

- **Caso de uso:** [enlace](../CasosDeUso/CU{NN}%20-%20Nombre.md) · **Mapa de código:** [DOCUMENTACION.md](../DOCUMENTACION.md#cu{NN})
- **Diagramas de grado:** CU img NNN · análisis img NNN · secuencia img NNN · mockup img NNN (y sus secciones §)
- **Estado frente a los diagramas:** Coincide | Difiere (ver tabla) | Diagrama por rehacer

## Flujo real implementado

Paso a paso, en el orden en que ocurre de verdad. Cada paso nombra el
componente, y las llamadas HTTP con su método, ruta y código de respuesta:

1. `Pantalla X` → `POST /api/v1/...` (`201`).
2. `XController` → `XService.metodo()`: valida ..., consulta ... .
3. ...

Incluye los flujos de error y las tareas en segundo plano (jobs, cierres
automáticos) que el diagrama no mostraba.

## Diferencias con los diagramas

| Diagrama | Lo que dibuja | Lo que hace la implementación | Por qué |
| --- | --- | --- | --- |
| Secuencia (img NNN) | ... | ... | ... |

Una fila por diferencia. Si no hay ninguna, se escribe "Sin diferencias" (no se
deja la tabla vacía). Tipos de diferencia a buscar: componentes que el
diagrama no tiene (o que sobran), nombres de clase distintos, pasos
agregados o reordenados, validaciones nuevas, campos que el mockup no
muestra, estados o tablas distintas, decisiones que cambiaron el flujo.

## Para rehacer los diagramas

Una fila por cada uno de los seis tipos de
[la tabla de arriba](#qué-diagramas-se-revisan-y-qué-cambio-los-afecta), siempre
las seis aunque no cambien:

| Diagrama | Imagen | ¿Cambia? | Qué corregir | Hecho |
| --- | --- | --- | --- | --- |
| Secuencia | 125 | Sí | Agregar el paso X entre Y y Z; quitar W | [ ] |
| Análisis | 055 | Sí | Agregar la clase `ConvocatoriaDocumento` | [ ] |
| Mockup | 138 | Sí | Agregar los campos puntaje mínimo y documentos | [ ] |
| Clases / dominio | 090 | No | — | — |
| Diseño lógico BD | 091 | Sí | Agregar columna `cantidad_finalistas` | [ ] |
| Casos de uso | 042 | No | — | — |

Cuando se rehace el diagrama se marca `[x]` y, si ya no queda ninguno
pendiente, se actualiza **Estado frente a los diagramas** (y la fila del
índice). Si el CU toca arquitectura o el texto de grado, se agrega una fila.

### Especificación de cada diagrama afectado

La tabla de arriba dice **qué** corregir; esta sección deja **el diagrama
completo, listo para dibujarse**. El objetivo es que quien rehace los
diagramas (a mano o con una herramienta como el skill de Enterprise
Architect, que recibe elementos y relaciones) trabaje solo con este archivo,
sin abrir el código ni explorar el proyecto.

Por cada diagrama con «¿Cambia? = Sí» se escribe su **contenido final
completo**, no solo la diferencia, marcando cada ítem como `[nuevo]`,
`[cambia]` o `[igual]` respecto de la imagen vigente. Así se puede rehacer
desde cero o parchear la imagen, a elección. Los nombres son los **del código
real** (clases, métodos, campos), salvo que se indique lo contrario, y se
verifican contra el código antes de escribirlos. Todo en el idioma del
proyecto (español).

Contenido mínimo según el tipo:

- **Secuencia.** Líneas de vida **de izquierda a derecha**, cada una con su
  tipo (`Actor`, `Boundary` = pantalla, `Control` = controller/service,
  `Entity` = model o tabla, `Object` = otro) y su nombre. Mensajes **en
  orden**, numerados, con `de → a`, el nombre del método con sus parámetros y
  lo que devuelve; las llamadas HTTP con método, ruta y código de respuesta.
  Las alternativas (`alt`/`opt`/error) y lo que ocurre en segundo plano se
  escriben como bloques marcados.
- **Análisis (comunicación).** Las clases con su estereotipo (`boundary`,
  `control`, `entity`) y el actor; los **enlaces** (qué par se comunica) y,
  sobre cada enlace, los mensajes numerados (`1`, `1.1`, `2`) con `de → a` y
  nombre. Se conservan los números y nombres que no cambian.
- **Clases / dominio.** Cada clase con atributos (visibilidad, nombre, tipo) y
  operaciones; enumeraciones con sus valores; relaciones con su tipo
  (asociación, composición, herencia), multiplicidades y navegabilidad.
  Composición solo cuando la parte no existe sin el todo.
- **Diseño lógico de la BD.** Cada tabla con sus columnas (nombre, tipo,
  nulabilidad, `PK`/`FK`/`UQ`), restricciones, índices únicos parciales y
  enums; las relaciones con su cardinalidad. Se remite a la migración y se
  verifica que coincida con
  [DiagramaConceptual.mermaid](../../docs/DiagramaConceptual.mermaid).
- **Casos de uso.** Actores (primarios a la izquierda, sistemas externos a la
  derecha), casos de uso con verbo en infinitivo, y las relaciones
  (`Association`, `Include`, `Extend` con su dirección: `from` es el CU que
  incluye o extiende).
- **Mockup.** Una descripción por pantalla o diálogo: cada control (campo,
  botón, filtro, columna) con su etiqueta exacta, tipo, obligatoriedad, el
  estado vacío y los mensajes de error. Un mockup no se dibuja con UML, así
  que se entrega como lista, no como esquema de elementos.

Formato sugerido para los diagramas de elementos y relaciones (un bloque por
diagrama):

```markdown
#### Secuencia — img 125 (rehacer completo)

**Líneas de vida (izquierda → derecha):** `Actor` Administrador · `Boundary` ConvocatoriasScreen · `Control` ConvocatoriaController · `Control` ConvocatoriaService · `Entity` Convocatoria

| N.º | De → a | Mensaje | Estado |
| --- | --- | --- | --- |
| 1 | Administrador → ConvocatoriasScreen | clic «Guardar» | [igual] |
| 2 | ConvocatoriasScreen → ConvocatoriaController | `POST /api/v1/convocatorias` → `201` | [cambia] |
| 3 | ConvocatoriaController → ConvocatoriaService | `crear(request)` | [nuevo] |
```

Si un diagrama es demasiado grande para dejarlo en el mismo archivo, se
mueve a `docs/Implementacion/CU{NN} - {Nombre}/` como un `.md` por diagrama
(`Secuencia.md`, `Analisis.md`...), enlazado desde la fila de la tabla.
```

## Fila del índice (tabla de contenido de `DOCUMENTACION.md`)

La tabla de arriba del archivo gana una columna con el estado frente a los
diagramas y el enlace al detalle:

```markdown
| CU | Nombre | Diagramas | Detalle |
| --- | --- | --- | --- |
| 13 | [Gestionar convocatoria y gestión académica](#cu13) | Difiere | [diferencias](Implementacion/CU13%20-%20Gestionar%20convocatoria%20y%20gestión%20académica.md) |
```

## Reglas

- **Se documenta lo que hace el código, no lo que se quiso hacer.** Antes de
  escribir el flujo real o un enlace se verifica contra el código (nombres de
  clase, métodos, rutas, estados), no contra el CU ni contra la UT.
- **No se inventa ni se promete.** Lo que el código todavía no hace no va en
  "Flujo real"; va como pendiente en el CU o en los follow-ups de la UT.
- **No se editan los diagramas ni la documentación de grado desde este
  repositorio.** El archivo de implementación es el insumo para rehacerlos; su
  sección "Para rehacer los diagramas" dice exactamente qué corregir.
- Si el diagrama y el código se contradicen y no está claro cuál es el
  correcto, se avisa a la persona antes de registrar la diferencia como
  decisión (mismo criterio que
  [CASOS_DE_USO.md](CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)).
- Los enlaces son rutas relativas con los espacios y tildes del nombre del
  archivo codificados (`%20`), igual que en los CU.
