# Casos de Uso

Esta regla define **cómo se escribe un Caso de Uso (CU)** en este
proyecto — el formato, las secciones obligatorias y el criterio de
granularidad. Se aplica siempre que se cree o edite un archivo dentro de
`docs/CasosDeUso/`.

Un CU documenta **un flujo de usuario coherente y acotado** — no es un
documento de diseño técnico (eso lo cubren las
[Unidades de Trabajo](UNIDADES_DE_TRABAJO.md), que se crean después y a
partir del CU ya escrito).

## Fuente de verdad: la documentación de grado

Los casos de uso de este proyecto **ya están definidos** en la
documentación de grado (Taller de Grado I), carpeta fuera de este
repositorio:

`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`

Un CU **no se inventa ni se redefine** al escribirlo: se parte de lo que
esa documentación dice y el sistema debe cumplirlo.

| Qué | Dónde (dentro de esa carpeta) |
| --- | --- |
| Lista vigente de **26 CU**, su numeración, paquetes, actores, relaciones (`include`/`extend`) y RF asociados (**fuente de verdad de la numeración**) | `casos_de_uso_filtrados.md` y `CLAUDE.md` §6 |
| Actores y contexto del dominio | `actores_por_cu.md`, `CLAUDE.md` §2–§3 |
| Diagrama general de CU | `images/019_Diagrama_General_CU.jpg` |
| Diagrama de cada CU | `images/038`–`050` y `images/064`–`076` (`..._CUNN_...`) |
| Análisis (clases de análisis) de cada CU | `images/051`–`063` y `images/077`–`089` (`Analisis_CUNN_...`) |
| Diagramas de **secuencia** de los CU (P4/P5/P6) | `images/092`–`104` (`Secuencia_CUNN_...`) |
| Mockups de pantallas | `images/105`–`117` (`Mockup_CUNN_...`) |
| Requisitos funcionales (RF-01…21) y no funcionales | `secciones/05_capitulo4_ieee830/03_requisitos_especificos.tex` |

Reglas:

- **Antes de escribir un CU**, se miran sus diagramas (CU, análisis,
  secuencia, mockup) y su RF. El flujo principal, los flujos alternativos y
  las pantallas del CU deben ser consistentes con esos diagramas; si el
  diagrama y la lista se contradicen, se avisa y no se elige en silencio.
- **No se inventan datos institucionales** (umbral mínimo de puntaje,
  número de cupos, formatos de documento, estados): lo que la documentación
  deja como "por definir con la DUBSS" se marca igual en el CU.
- Los casos de uso **descartados** que lista esa documentación no se
  reintroducen.

## Dónde vive y cómo se nombra

`docs/CasosDeUso/CU{NN} - {Nombre corto}.md` — el número y el nombre son los
de la documentación de grado (`CU01` es el «CU-01» de esa lista, etc.):
nunca se renumera ni se reasigna a otro CU aunque uno se descarte (mismo
criterio que los códigos de error de
[EXCEPCIONES_BACKEND.md](backend/EXCEPCIONES_BACKEND.md): un número usado
una vez no se reasigna a otra cosa). El nombre corto del archivo coincide
con el campo "Nombre" de la tabla interna.

## Granularidad: un CU, un flujo

Si una funcionalidad agrupa más de un flujo de usuario independiente, se
separa en CU distintos. Señal de que hay que separar: cada flujo tiene su
propio disparador (una acción de usuario distinta), su propio conjunto de
criterios de aceptación que no dependen de que el otro flujo se esté
ejecutando, y podría entregarse y probarse por separado.

Ejemplo genérico: "iniciar sesión", "renovar la sesión" y "cerrar sesión"
son tres disparadores distintos con criterios independientes → son **tres
CU**, no uno. Cuando un CU depende del resultado de otro (ej. "cerrar
sesión" no tiene sentido sin "iniciar sesión"), se documenta en sus
**precondiciones** con un link cruzado, no fusionándolos.

## Formato del archivo (plantilla completa)

```markdown
# Caso de uso

| Id   | Nombre         | Módulo   | Prioridad       | Estado                      |
| ---- | -------------- | -------- | --------------- | --------------------------- |
| CUNN | {Nombre corto} | {Módulo} | Alta/Media/Baja | Pendiente/En progreso/Hecho |

**Actor principal:** {rol o tipo de usuario que dispara el CU}
**Actores secundarios:** {otros roles o sistemas involucrados, o "—"}
**Objetivo:** {qué busca lograr el actor, en una frase}
**Plataforma:** {Web, Móvil, o Móvil y Web}
**RF asociados:** {RF-NN de la documentación de grado}

**Descripción:**
{Párrafo narrativo: qué hace el actor, qué responde el sistema y qué pasa
en los casos alternativos (error, permiso, estado inválido). Si este CU
depende de otro o es prerequisito de uno futuro, se aclara acá con un link
relativo (`[CU-NN: Nombre](CU{NN}%20-%20Nombre.md)` — espacios codificados
como `%20` porque el nombre del archivo los tiene).}

---

## Precondiciones

- {Estado que debe cumplirse antes de ejecutar el CU. Si depende de otro
  CU, link relativo.}

## Flujo principal

1. {Paso numerado del camino feliz, desde la acción inicial del actor
   hasta el resultado final.}
2. ...

## Flujos alternativos y excepciones

- **A1 (paso N):** {condición alternativa o de error} → {qué hace el
  sistema, incluido el código HTTP/error si aplica}.

## Reglas de negocio

- {Cada regla no obvia que condiciona el comportamiento, una por bullet.
  Si remite a una decisión técnica ya tomada en otra rule, se linkea esa
  rule en vez de repetirla.}

## Postcondiciones

- {Estado en que queda el sistema al terminar con éxito.}

## Criterios de aceptación

- **CA01:** Dado que {contexto}, el sistema debe {resultado verificable}.
- **CA02:** Dado que {condición alternativa/error}, el sistema debe
  {resultado esperado, con código HTTP/mensaje si aplica}.

## Desarrollador

{Nombre de la persona responsable de implementar/dar seguimiento a este CU.}
```

## Reglas de contenido

- **Actor principal, Objetivo y Descripción** son obligatorios. La
  descripción cubre el camino feliz y menciona los errores principales (el
  detalle exacto va en los flujos alternativos y en los criterios).
- **Flujo principal** numerado y **flujos alternativos** referenciados al
  paso que bifurcan (`A1`, `A2`...). Si el CU es muy simple puede tener
  pocos alternativos, pero un CU sin ningún flujo de error documentado se
  revisa dos veces. Si el CU tiene una parte que continúa después del
  login (ej. mantener una sesión), los pasos siguen la numeración del
  flujo principal bajo un subtítulo, no reinician en 1.
- **Reglas de negocio**: no se dejan implícitas solo en la prosa.
- **Criterios de aceptación**: obligatorios siempre, numerados `CA01`,
  `CA02`... sin reusar un número si uno se elimina. Cada CA es verificable
  de forma binaria, formato "Dado que {contexto}, el sistema debe
  {resultado}" — nunca "el sistema debe funcionar bien".
- **Plataforma** indica dónde se ejecuta el CU; de ahí sale qué capas de
  UT (`UT-BACKEND`, `UT-FRONTEND`, `UT-MOBILE`) le corresponden.
- **RF asociados** y, cuando el CU cambia el estado de una postulación, los
  **estados de `postulacion` afectados** (ver el ciclo de vida en
  `diseno_base_de_datos.md` §4) se citan en el flujo o en las reglas de
  negocio.
- **Estado** se actualiza a mano a medida que avanza (`Pendiente` → `En
  progreso` → `Hecho`).
- Los links a otras rules o a otros CU usan rutas relativas correctas
  desde `docs/CasosDeUso/`.

## Qué NO va en un Caso de Uso

- Nombres de clases, endpoints exactos, esquemas de base de datos o
  cualquier detalle de implementación — eso es contenido de las
  [Unidades de Trabajo](UNIDADES_DE_TRABAJO.md). El CU describe
  **comportamiento observable por el actor**, no cómo se construye.
- Una lista de tareas técnicas ("crear el Model", "armar el controller").
