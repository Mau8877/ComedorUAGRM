# Caso de uso

| Id   | Nombre                     | Módulo               | Prioridad | Estado    |
| ---- | -------------------------- | -------------------- | --------- | --------- |
| CU01 | Registrarse y autenticarse | P1. Cuentas y acceso | Alta      | En progreso |

**Actor principal:** Usuario del Sistema (actor abstracto que generaliza a Postulante, Administrador DUBSS, Directivo y Personal del Comedor Universitario)
**Actores secundarios:** —
**Objetivo:** Que el Postulante cree su cuenta y que cualquier usuario del sistema inicie y cierre sesión para acceder, de forma segura, solo a lo que corresponde a su rol.
**Plataforma:** Móvil y Web
**RF asociados:** RF-01

**Descripción:**
Un Postulante (estudiante de la UAGRM) crea su cuenta desde el portal web o la app móvil con sus nombres, apellidos, CI, registro universitario, carrera, teléfono, correo electrónico y contraseña. Si el CI y el correo no están registrados y los datos son válidos, el sistema crea la cuenta con el rol Postulante, inicia su sesión y lo lleva a su módulo. Cualquier usuario con cuenta (Postulante, Administrador DUBSS, Directivo o Personal del Comedor Universitario) inicia sesión con su correo y su contraseña: el sistema verifica las credenciales y que la cuenta esté activa, abre la sesión y lo lleva al módulo de su rol. Cuando termina, el usuario cierra su sesión y el sistema lo devuelve a la pantalla de acceso. Las cuentas del personal **no se autorregistran**: las crea el Administrador DUBSS en el CU14. Si los datos de registro ya existen o son inválidos, si las credenciales son incorrectas, o si la cuenta está desactivada o bloqueada temporalmente, el sistema lo informa sin abrir sesión.

Este CU cubre el registro, el inicio de sesión y el cierre de sesión (este último no figura como CU propio en la lista de 26 de la documentación de grado; se documenta acá por decisión del equipo). Recuperar la contraseña es el CU15 (extiende este CU).

---

## Precondiciones

- El usuario tiene acceso al portal web o a la app móvil y conexión a Internet.
- Para registrarse: no existe una cuenta con su CI ni con su correo electrónico.
- Para iniciar sesión: el usuario tiene una cuenta registrada y activa (creada por registro propio si es Postulante, o por el Administrador DUBSS en el CU14 si es personal).
- Para cerrar sesión: el usuario tiene una sesión abierta.

## Flujo principal

### Flujo A: registro de un Postulante

1. El usuario selecciona «Crear cuenta» en la pantalla de acceso.
2. El sistema muestra el formulario de registro: nombres, apellidos, CI, registro universitario, carrera, teléfono, correo electrónico, contraseña y confirmación de contraseña.
3. El usuario completa los datos y confirma el registro.
4. El sistema valida que los campos obligatorios estén completos, que su formato sea correcto, que la contraseña cumpla la política y coincida con su confirmación, y que el CI, el registro universitario y el correo no existan en el sistema.
5. El sistema crea la cuenta con el rol Postulante, activa, sin guardar nunca la contraseña en texto plano.
6. El sistema informa que el registro fue exitoso, inicia la sesión del usuario y lo lleva al módulo del Postulante.

### Flujo B: inicio de sesión

7. El usuario accede a la pantalla de inicio de sesión del portal web o de la app móvil.
8. El usuario ingresa su correo electrónico y su contraseña.
9. El sistema verifica las credenciales.
10. El sistema verifica que la cuenta esté activa y no bloqueada, y obtiene el rol del usuario.
11. El sistema abre la sesión y registra el acceso en la bitácora.
12. El sistema lleva al usuario al módulo de su rol: el Postulante a su módulo de postulación, el Administrador DUBSS al panel de gestión, el Directivo al dashboard de indicadores y el Personal del Comedor al registro de asistencia.

### Flujo C: cierre de sesión

13. El usuario selecciona «Cerrar sesión» desde el menú de su cuenta.
14. El sistema cierra la sesión del usuario en ese dispositivo y deja de reconocerla.
15. El sistema lleva al usuario a la pantalla de acceso.

## Flujos alternativos y excepciones

- **A1 (paso 4):** El CI, el registro universitario o el correo electrónico ya están registrados → el sistema rechaza el registro con `409`, indica el campo en conflicto y no crea la cuenta.
- **A2 (paso 4):** Faltan campos obligatorios, el formato es inválido, la contraseña no cumple la política o no coincide con su confirmación → el sistema marca cada campo con su error y permite corregirlo, sin crear la cuenta.
- **A3 (paso 9):** El correo no existe o la contraseña es incorrecta → el sistema responde `401` con el mismo mensaje en ambos casos (sin revelar cuál dato falló) y permite reintentar.
- **A4 (paso 10):** La cuenta está desactivada → el sistema responde `403`, indica que debe comunicarse con la DUBSS y no abre sesión.
- **A5 (paso 8):** El usuario selecciona «¿Olvidó su contraseña?» → se ejecuta el CU15.
- **A6 (paso 9):** Tras varios intentos fallidos consecutivos la cuenta se bloquea temporalmente → el sistema niega el acceso, aunque la contraseña sea correcta, e informa que debe reintentar más tarde. Lo que se busca es bloquear a los 5 intentos, por 5 minutos la primera vez, y por más tiempo si el usuario sigue fallando; el alcance exacto depende de lo que permita el servicio de autenticación.
- **A7 (paso 4):** La petición de registro llega repetida (doble envío o reintento) → el sistema la procesa de forma idempotente y no crea dos cuentas.
- **A8 (pasos 11 y 12):** La sesión ya no es válida (expiró o fue invalidada) → el sistema lleva al usuario a la pantalla de inicio de sesión.
- **A9 (cualquier paso):** Falla la conexión con el servidor o un servicio del que depende → el sistema informa el error con un mensaje claro, no crea ni modifica nada y permite reintentar.
- **A10 (paso 14):** La sesión ya había expirado o se había cerrado antes de pedir el cierre → el sistema igual lleva al usuario a la pantalla de acceso, sin mostrar error.

## Reglas de negocio

- **Solo el Postulante se autorregistra.** La cuenta que crea el registro siempre tiene el rol Postulante; el usuario no elige su rol. Los demás roles (Administrador DUBSS, Directivo y Personal del Comedor) los crea el Administrador DUBSS desde el CU14.
- **Un usuario tiene un único rol** y de él depende a qué módulo se le lleva al iniciar sesión.
- **CI, registro universitario y correo únicos** entre las cuentas existentes (una cuenta eliminada no bloquea reutilizarlos). El CI no se modifica después del registro (RF-10, CU15). El registro universitario tiene exactamente **9 dígitos** (solo números) y no empieza por 0.
- **La carrera** se elige de la lista de carreras de la UAGRM; la lista inicial (carreras de la UAGRM con su facultad) viene cargada con el sistema; su mantenimiento posterior queda fuera de este CU.
- **El rechazo por credenciales es siempre el mismo mensaje**, exista o no el correo.
- **Política de contraseña:** al menos 6 caracteres, sin obligación de mayúsculas, números ni símbolos.
- **Bloqueo por intentos fallidos:** el objetivo es 5 intentos consecutivos, un primer bloqueo de 5 minutos y una duración mayor si se sigue fallando (la escala exacta queda por definir). Se implementa **hasta donde lo permita el servicio de autenticación**; lo que no se pueda cumplir tal cual se revisa y se ajusta esta regla. Un inicio de sesión correcto reinicia el contador.
- **Una cuenta desactivada** no puede iniciar sesión; se desactiva y reactiva desde el CU14.
- **Cerrar sesión** solo cierra la sesión del dispositivo desde el que se pide; no afecta a otros dispositivos.
- **El registro es idempotente** (RNF-03): repetir la misma petición no duplica la cuenta.
- **«Becado» no es un rol:** quien resulta adjudicado sigue siendo un Postulante, con una beca vigente.
- **Contraseñas y tokens de sesión nunca se escriben en los registros del sistema** ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **Quién verifica las credenciales:** el servicio de autenticación de Firebase (RF-01); el sistema solo comprueba el token y los datos propios de la cuenta ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- Los mensajes al usuario están en español y no exponen detalles técnicos.

## Postcondiciones

- **Registro:** existe una cuenta nueva, activa y con rol Postulante, y el usuario tiene una sesión abierta.
- **Inicio de sesión:** el usuario tiene una sesión activa con los permisos de su rol, está en el módulo que le corresponde y su acceso quedó registrado en la bitácora (se consulta en el CU25).
- **Cierre de sesión:** el dispositivo ya no tiene sesión y cualquier pantalla privada lleva a la pantalla de acceso.

## Criterios de aceptación

- **CA01:** Dado que los datos del formulario son válidos y ni el CI ni el correo están registrados, el sistema debe crear la cuenta con rol Postulante, abrir la sesión y llevar al usuario a su módulo.
- **CA02:** Dado que el CI ya está registrado, el sistema debe responder `409` indicando el CI como campo en conflicto y no crear la cuenta.
- **CA03:** Dado que el correo ya está registrado, el sistema debe responder `409` indicando el correo como campo en conflicto y no crear la cuenta.
- **CA04:** Dado que falta un campo obligatorio o su formato es inválido, el sistema debe marcar ese campo con su error y no crear la cuenta.
- **CA05:** Dado que la contraseña tiene menos de 6 caracteres, o no coincide con su confirmación, el sistema debe marcar el error en esos campos y no crear la cuenta.
- **CA06:** Dado que la persona intenta registrarse con otro rol que no sea Postulante, el sistema debe ignorar esa elección y crear la cuenta siempre como Postulante.
- **CA07:** Dado que la cuenta está activa y las credenciales son correctas, el sistema debe abrir la sesión y llevar al usuario al módulo de su rol (Postulante, Administrador DUBSS, Directivo o Personal del Comedor).
- **CA08:** Dado que el correo no existe o la contraseña es incorrecta, el sistema debe responder `401` con el mismo mensaje en ambos casos, sin abrir sesión.
- **CA09:** Dado que la cuenta está desactivada, el sistema debe responder `403`, indicar que se comunique con la DUBSS y no abrir sesión.
- **CA10:** Dado que la cuenta acumula varios intentos fallidos consecutivos, el sistema debe bloquearla temporalmente y negar el acceso, incluso con la contraseña correcta, hasta que termine el bloqueo (objetivo: 5 intentos y 5 minutos; ver la regla de bloqueo).
- **CA11:** Dado que el usuario sigue fallando al volver a intentar después de un bloqueo, el sistema debe aumentar la duración del siguiente bloqueo, hasta donde lo permita el servicio de autenticación.
- **CA12:** Dado que se envía dos veces la misma petición de registro, el sistema debe crear una sola cuenta.
- **CA13:** Dado que el usuario selecciona «¿Olvidó su contraseña?», el sistema debe iniciar el CU15.
- **CA14:** Dado que el usuario se registra, inicia o cierra sesión desde la web o desde la app móvil, el sistema debe ofrecer el mismo comportamiento en ambas.
- **CA15:** Dado que la sesión expiró o ya no es válida, el sistema debe llevar al usuario a la pantalla de inicio de sesión.
- **CA16:** Dado que un usuario del personal quiere acceder, el sistema no debe ofrecerle crear su cuenta por sí mismo.
- **CA17:** Dado que el usuario tiene una sesión abierta y selecciona «Cerrar sesión», el sistema debe cerrarla y llevarlo a la pantalla de acceso.
- **CA18:** Dado que el usuario cerró sesión, el sistema no debe permitirle ver pantallas privadas hasta que vuelva a iniciar sesión.
- **CA19:** Dado que el usuario tiene sesión abierta en otro dispositivo, el sistema debe mantenerla al cerrar sesión en este.
- **CA20:** Dado cualquier registro o inicio de sesión, ni la contraseña ni los tokens de sesión deben aparecer en los registros del sistema, y todo inicio de sesión exitoso debe quedar anotado en la bitácora.
- **CA21:** Dado que el registro universitario ya está registrado, el sistema debe responder `409` indicando el registro universitario como campo en conflicto y no crear la cuenta.

## Desarrollador

Mauro Gutierrez
