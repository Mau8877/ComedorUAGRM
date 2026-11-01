# Casos de Uso

Esta regla define **cómo se escribe un Caso de Uso (CU)** en este
proyecto — el formato, las secciones obligatorias y el criterio de
granularidad. Se aplica siempre que se cree o edite un archivo dentro de
`docs/CasosDeUso/`.

Un CU documenta **un flujo de usuario coherente y acotado** — no es un
documento de diseño técnico (eso lo cubren las
[Unidades de Trabajo](UNIDADES_DE_TRABAJO.md), que se crean después y a
partir del CU ya escrito).

## Dónde vive y cómo se nombra

`docs/CasosDeUso/CU{NN} - {Nombre corto}.md` — numeración secuencial de dos
dígitos (`CU01`, `CU02`, ..., `CU10`...), asignada cuando el CU se acuerda
con el equipo, sin reusar un número aunque un CU se descarte (mismo
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
