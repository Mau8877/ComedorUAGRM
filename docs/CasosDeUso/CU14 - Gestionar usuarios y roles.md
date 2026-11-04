# Caso de uso

| Id   | Nombre                    | Módulo               | Prioridad | Estado    |
| ---- | ------------------------- | -------------------- | --------- | --------- |
| CU14 | Gestionar usuarios y roles | P1. Cuentas y acceso | Alta      | En progreso |

**Actor principal:** Administrador DUBSS
**Actores secundarios:** —
**Objetivo:** Que el Administrador DUBSS cree y mantenga las cuentas del personal (Administrador DUBSS, Directivo y Personal del Comedor) y les asigne el rol que les corresponde, para que cada persona acceda solo a las funciones de su rol.
**Plataforma:** Web
**RF asociados:** RF-09

**Descripción:**
El Administrador DUBSS entra a la gestión de usuarios y ve la lista de las cuentas del personal con su nombre, CI, correo, rol y estado (activa o desactivada), que puede filtrar por rol y por estado y buscar por CI o nombre. Desde ahí crea la cuenta de un funcionario (nombres, apellidos, CI, correo y uno de los tres roles del personal), edita sus datos, le cambia el rol, la desactiva o la reactiva. Al crear una cuenta, el sistema genera una contraseña temporal que muestra una sola vez al Administrador para que se la entregue al funcionario. Las cuentas no se eliminan: se desactivan, para conservar la trazabilidad de lo que hicieron. Desactivar una cuenta le impide el acceso y cierra sus sesiones activas. El sistema impide que el Administrador desactive su propia cuenta o la del último Administrador activo. Cada operación queda anotada en la bitácora; los cambios de rol conservan el rol anterior, el nuevo, la fecha y el responsable. Si el CI o el correo ya están registrados, si los datos son inválidos o si la operación no está permitida, el sistema lo informa sin cambiar nada.

**Los Postulantes no se gestionan en este CU:** se registran por sí mismos ([CU-01: Registrarse y autenticarse](CU01%20-%20Registrarse%20y%20autenticarse.md)) y no aparecen en la lista. Este CU es la fuente de las cuentas con las que el personal inicia sesión en el CU-01; el cambio de contraseña posterior a la entrega de la contraseña temporal lo resuelve el CU-15.

---

## Precondiciones

- El Administrador DUBSS está autenticado en el sistema ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) y su cuenta está activa.
- El usuario autenticado tiene el rol Administrador DUBSS.

## Flujo principal

### Flujo A: consultar la lista de cuentas

1. El Administrador accede a la gestión de usuarios. En la web el menú ofrece una vista por rol (Administradores DUBSS, Directivos y Personal del Comedor); cada una es la misma lista filtrada por ese rol.
2. El sistema muestra la lista paginada de cuentas del personal con su nombre, CI, correo, rol y estado, con filtros por rol y por estado y búsqueda por CI o nombre.
3. El Administrador selecciona la acción deseada: crear, editar, cambiar rol, desactivar o reactivar una cuenta.

### Flujo B: crear una cuenta

4. El Administrador ingresa los datos del funcionario: nombres, apellidos, CI, correo electrónico y rol (Administrador DUBSS, Directivo o Personal del Comedor).
5. El sistema valida que los datos obligatorios estén completos y con formato válido, que el rol sea uno de los tres del personal y que el CI y el correo no estén registrados.
6. El Administrador confirma la operación.
7. El sistema genera una contraseña temporal, crea la cuenta activa con el rol indicado y le muestra al Administrador la contraseña temporal una sola vez, para que se la entregue al funcionario.
8. El sistema registra la acción en la bitácora y muestra la lista actualizada.

### Flujo C: editar los datos de una cuenta

9. El Administrador selecciona una cuenta y modifica sus nombres, apellidos o correo electrónico.
10. El sistema valida el formato de los datos y que el correo no pertenezca a otra cuenta.
11. El Administrador confirma; el sistema actualiza la cuenta, registra la acción en la bitácora y muestra la lista actualizada.

### Flujo D: cambiar el rol de una cuenta

12. El Administrador selecciona una cuenta y elige el nuevo rol entre los tres del personal.
13. El sistema valida que no sea su propia cuenta y que el cambio no deje al sistema sin un Administrador activo (A5).
14. El Administrador confirma; el sistema actualiza el rol, cierra las sesiones activas de esa cuenta, registra el cambio en la bitácora (rol anterior, rol nuevo, fecha y responsable) y muestra la lista actualizada. El nuevo rol rige desde el siguiente inicio de sesión de esa persona (A3).

### Flujo E: desactivar una cuenta

15. El Administrador selecciona «Desactivar» en una cuenta activa y el sistema le pide confirmar.
16. El Administrador confirma la baja.
17. El sistema valida que no sea su propia cuenta ni la del último Administrador activo (A5).
18. El sistema marca la cuenta como desactivada, invalida sus sesiones activas (A4), registra la acción en la bitácora y muestra la lista actualizada.

### Flujo F: reactivar una cuenta

19. El Administrador selecciona «Reactivar» en una cuenta desactivada y confirma.
20. El sistema marca la cuenta como activa (recupera su rol anterior, A6), registra la acción en la bitácora y muestra la lista actualizada.

## Flujos alternativos y excepciones

- **A1 (paso 5):** El CI o el correo electrónico ya están registrados → el sistema rechaza el alta con `409`, indica el campo en conflicto y no crea la cuenta.
- **A2 (pasos 5 y 10):** Faltan datos obligatorios o su formato es inválido (incluido un rol que no es del personal) → el sistema responde `400`, marca cada campo con su error y permite corregirlo, sin crear ni modificar la cuenta.
- **A3 (paso 14):** Se cambia el rol de una cuenta → el sistema aplica los nuevos permisos desde el siguiente inicio de sesión de esa persona y conserva el registro del cambio (rol anterior, rol nuevo, fecha y responsable).
- **A4 (paso 18):** Se desactiva una cuenta → el sistema impide su acceso e invalida sus sesiones activas; la cuenta no se elimina, de modo que se preserve la trazabilidad de sus acciones.
- **A5 (pasos 13 y 17):** La cuenta es la del propio Administrador o la del último Administrador activo → el sistema responde `403`, no realiza la operación (desactivar o cambiar el rol) y muestra un aviso.
- **A6 (paso 20):** El Administrador reactiva una cuenta desactivada → recupera su rol anterior y puede volver a iniciar sesión.
- **A7 (cualquier paso):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario a la pantalla de inicio de sesión. El usuario no tiene el rol Administrador DUBSS → `403` y el sistema no muestra ni ejecuta la gestión.
- **A8 (pasos 9, 12, 15 y 19):** La cuenta indicada no existe, o es la de un Postulante → el sistema responde `404` y no cambia nada.
- **A9 (pasos 15 y 19):** La cuenta ya está en el estado pedido (desactivar una desactivada, reactivar una activa) → el sistema responde `409` e informa el estado actual.
- **A10 (cualquier paso):** Falla la conexión con el servidor o con la base de datos → el sistema informa el error con un mensaje claro, no crea ni modifica nada y permite reintentar.

## Reglas de negocio

- **Solo el Administrador DUBSS gestiona cuentas.** Cualquier otro rol recibe `403`; la verificación se hace en el backend, no solo ocultando la pantalla.
- **Se gestionan solo las cuentas del personal:** Administrador DUBSS, Directivo y Personal del Comedor. Los Postulantes quedan fuera: no aparecen en la lista ni se pueden editar, desactivar ni reactivar desde este CU, y el rol Postulante no se puede asignar.
- **Un usuario tiene un único rol.** Al crear la cuenta se elige uno de los tres roles del personal.
- **CI y correo únicos** entre las cuentas existentes. El CI se ingresa solo al crear la cuenta y **no se modifica** después (RF-10); en la edición se pueden cambiar nombres, apellidos y correo, y el rol por su propia acción. *(Decisión del equipo: la documentación de grado no precisa qué campos se editan.)*
- **No se elimina ninguna cuenta:** se desactiva, para preservar la trazabilidad. Una cuenta desactivada no puede iniciar sesión (CU-01) y desaparece de las sesiones activas; reactivarla le devuelve el acceso con el rol que tenía.
- **Protección de los administradores:** el Administrador no puede desactivar su propia cuenta ni la del último Administrador activo. *(Decisión del equipo: la misma protección aplica al cambio de rol, para que nadie deje al sistema sin Administrador activo ni se quite a sí mismo el rol; el diagrama de secuencia solo la muestra en la desactivación.)*
- **Contraseña temporal:** la genera el sistema al crear la cuenta; se le muestra al Administrador **una sola vez** y se guarda únicamente su hash (nunca en texto plano, ni en la bitácora ni en los registros del sistema). *(Decisión del equipo: se elige la contraseña temporal y no el enlace de activación por correo, porque el envío de correos y la recuperación de contraseña son del CU-15.)* El funcionario cambia esa contraseña desde su perfil (CU-15).
- **Cambio de rol:** se aplica desde el siguiente inicio de sesión y deja registro (rol anterior, nuevo, fecha y responsable). *(Decisión del equipo: el cambio de rol cierra las sesiones activas de esa cuenta, así la persona tiene que iniciar sesión de nuevo y aterriza en el módulo del rol nuevo. El diagrama de secuencia no muestra este cierre de sesiones.)*
- **Desactivar invalida las sesiones activas** de la cuenta; las sesiones no se restauran al reactivarla: la persona inicia sesión de nuevo.
- **Bitácora (CU-25):** toda operación (crear, editar, cambiar rol, desactivar y reactivar) queda anotada con quién la hizo, qué cambió, fecha y hora. La bitácora nunca contiene contraseñas.
- **Lista:** solo cuentas del personal, paginada, con filtros por rol y por estado y búsqueda por CI o nombre ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).
- Los mensajes al usuario están en español y no exponen detalles técnicos.
- **Pendiente de definir con la DUBSS:** si el funcionario debe estar obligado a cambiar la contraseña temporal en su primer acceso (hoy el sistema no lo fuerza) y cuál es la política de vigencia de esa contraseña temporal.

## Postcondiciones

- **Crear:** existe una cuenta activa con el rol indicado y su contraseña temporal fue mostrada una vez al Administrador; la acción quedó en la bitácora.
- **Editar:** la cuenta tiene los datos nuevos; la acción quedó en la bitácora.
- **Cambiar rol:** la cuenta tiene el nuevo rol y sus sesiones anteriores fueron cerradas; el cambio quedó en la bitácora con el rol anterior y el nuevo.
- **Desactivar:** la cuenta no puede acceder, sus sesiones fueron invalidadas y el registro se conserva; la acción quedó en la bitácora.
- **Reactivar:** la cuenta vuelve a estar activa con su rol anterior; la acción quedó en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que el Administrador DUBSS ingresa datos válidos y ni el CI ni el correo están registrados, el sistema debe crear la cuenta activa con el rol elegido (Administrador DUBSS, Directivo o Personal del Comedor) y mostrarle una contraseña temporal una sola vez.
- **CA02:** Dado que el CI ya está registrado, el sistema debe responder `409` indicando el CI como campo en conflicto y no crear la cuenta.
- **CA03:** Dado que el correo ya está registrado, el sistema debe responder `409` indicando el correo como campo en conflicto y no crear la cuenta.
- **CA04:** Dado que falta un dato obligatorio o su formato es inválido, el sistema debe responder `400`, marcar ese campo con su error y no crear ni modificar la cuenta.
- **CA05:** Dado que se intenta crear una cuenta con el rol Postulante o con un rol inexistente, el sistema debe rechazar la operación con `400` y no crear la cuenta.
- **CA06:** Dado que el Administrador consulta la lista, el sistema debe mostrar solo cuentas del personal, con nombre, CI, correo, rol y estado, paginadas, y permitir filtrar por rol y por estado y buscar por CI o nombre.
- **CA07:** Dado que existen cuentas de Postulantes, el sistema no debe incluirlas en la lista ni permitir editarlas, desactivarlas, reactivarlas ni cambiarles el rol (`404`).
- **CA08:** Dado que el Administrador edita nombres, apellidos o correo con datos válidos, el sistema debe actualizar la cuenta y dejar la acción en la bitácora.
- **CA09:** Dado que el correo nuevo ya pertenece a otra cuenta, el sistema debe responder `409` y no modificar la cuenta.
- **CA10:** Dado que el Administrador intenta modificar el CI de una cuenta existente, el sistema no debe permitirlo.
- **CA11:** Dado que el Administrador cambia el rol de una cuenta, el sistema debe aplicar el nuevo rol, cerrar las sesiones activas de esa cuenta, registrar en la bitácora el rol anterior, el nuevo, la fecha y el responsable, y aplicar los nuevos permisos desde su siguiente inicio de sesión.
- **CA12:** Dado que el Administrador desactiva una cuenta distinta de la suya y que no es la del último Administrador activo, el sistema debe marcarla como desactivada, invalidar sus sesiones activas, conservar el registro y dejar la acción en la bitácora.
- **CA13:** Dado que la cuenta está desactivada, el sistema no debe permitirle iniciar sesión ni usar una sesión anterior (`403` en el inicio de sesión, CU-01).
- **CA14:** Dado que el Administrador intenta desactivar su propia cuenta, el sistema debe responder `403`, mostrar un aviso y no modificarla.
- **CA15:** Dado que el Administrador intenta desactivar o cambiar el rol de la cuenta del último Administrador activo, el sistema debe responder `403`, mostrar un aviso y no modificarla.
- **CA16:** Dado que el Administrador intenta cambiar su propio rol, el sistema debe responder `403` y no modificarlo.
- **CA17:** Dado que el Administrador reactiva una cuenta desactivada, el sistema debe marcarla como activa con el rol que tenía, permitirle iniciar sesión de nuevo y dejar la acción en la bitácora.
- **CA18:** Dado que la cuenta ya está en el estado pedido (desactivar una desactivada o reactivar una activa), el sistema debe responder `409` y no cambiar nada.
- **CA19:** Dado que el usuario autenticado no tiene el rol Administrador DUBSS, el sistema debe responder `403` en cualquier operación de este CU, sin mostrar ni modificar cuentas.
- **CA20:** Dado que la sesión expiró o no es válida, el sistema debe responder `401` y llevar al usuario a la pantalla de inicio de sesión.
- **CA21:** Dado cualquier crear, editar, cambiar rol, desactivar o reactivar, el sistema debe dejar la acción anotada en la bitácora (quién, qué cambió, fecha y hora), y ni la contraseña temporal ni ninguna otra contraseña deben aparecer en la bitácora ni en los registros del sistema.
- **CA22:** Dado que la operación se repite por doble envío o reintento (crear la misma cuenta), el sistema debe crear una sola cuenta (el segundo intento recibe el `409` del CI o correo ya registrado).
- **CA23:** Dado que falla la conexión con el servidor o la base de datos, el sistema debe informar el error con un mensaje claro, no crear ni modificar nada y permitir reintentar.

## Desarrollador

Mauro Gutierrez
