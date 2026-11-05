# Caso de uso

| Id   | Nombre                                    | Módulo               | Prioridad | Estado    |
| ---- | ----------------------------------------- | -------------------- | --------- | --------- |
| CU15 | Recuperar contraseña y gestionar perfil   | P1. Cuentas y acceso | Baja      | Pendiente |

**Actor principal:** Usuario del Sistema (actor abstracto que generaliza a Postulante, Administrador DUBSS, Directivo y Personal del Comedor Universitario)
**Actores secundarios:** Servicio de correo electrónico (envía el enlace de restablecimiento)
**Objetivo:** Que cualquier usuario recupere el acceso a su cuenta cuando olvida su contraseña, sin intervención del personal, y que mantenga al día sus datos de contacto y su contraseña desde su perfil.
**Plataforma:** Móvil y Web
**RF asociados:** RF-10

**Descripción:**
Este CU tiene dos funciones. La primera es **recuperar la contraseña**: desde la pantalla de inicio de sesión el usuario selecciona «¿Olvidó su contraseña?» e ingresa su correo. El sistema le envía a ese correo un enlace de restablecimiento de un solo uso y vigencia limitada, y le muestra siempre el mismo mensaje neutro («si la cuenta existe, enviamos un enlace»), exista o no el correo, para no revelar qué cuentas hay. Al abrir el enlace, el usuario define una nueva contraseña que cumpla la política; el sistema la guarda, invalida el enlace, cierra las sesiones abiertas de esa cuenta y le permite iniciar sesión con la contraseña nueva. La segunda es **gestionar el perfil**: el usuario autenticado entra a «Mi perfil», ve sus datos (el CI solo de lectura), actualiza sus datos de contacto y, si quiere, cambia su contraseña ingresando la actual y la nueva. Si el enlace venció o ya se usó, si la contraseña nueva no cumple la política, si la contraseña actual es incorrecta o si el correo nuevo ya pertenece a otra cuenta, el sistema lo informa sin cambiar nada.

Extiende al [CU-01: Registrarse y autenticarse](CU01%20-%20Registrarse%20y%20autenticarse.md) (alternativa A5 del CU-01, «¿Olvidó su contraseña?»). También es el CU con el que el personal cambia la contraseña temporal que le entregó el Administrador DUBSS en el [CU-14](CU14%20-%20Gestionar%20usuarios%20y%20roles.md), y con el que las cuentas que existían antes del cambio a la autenticación propia (que quedaron sin una contraseña válida) obtienen una.

---

## Precondiciones

- El usuario tiene acceso al portal web o a la app móvil y conexión a Internet.
- **Recuperar contraseña:** el usuario tiene una cuenta registrada con un correo electrónico al que puede acceder.
- **Gestionar perfil:** el usuario está autenticado en el sistema ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)) y su cuenta está activa.

## Flujo principal

### Flujo A: solicitar la recuperación de la contraseña

1. En la pantalla de inicio de sesión, el usuario selecciona «¿Olvidó su contraseña?».
2. El sistema le pide el correo electrónico de su cuenta.
3. El usuario ingresa su correo y confirma la solicitud.
4. Si el correo pertenece a una cuenta activa, el sistema genera un enlace de restablecimiento de un solo uso, válido por **60 minutos**, invalida cualquier enlace anterior de esa cuenta que siga pendiente y se lo envía a ese correo (A1).
5. El sistema muestra siempre el mismo mensaje neutro: si la cuenta existe, se envió un enlace al correo indicado y vence en 60 minutos.

### Flujo B: restablecer la contraseña con el enlace

6. El usuario abre el enlace recibido en su correo.
7. El sistema verifica que el enlace sea válido, no haya vencido y no se haya usado (A2), y le pide la nueva contraseña y su confirmación.
8. El usuario ingresa la nueva contraseña, la confirma y la guarda.
9. El sistema valida que la contraseña cumpla la política y coincida con su confirmación (A3).
10. El sistema guarda la nueva contraseña (solo su hash), marca el enlace como usado, reinicia el contador de intentos fallidos y el bloqueo temporal de la cuenta, y cierra todas las sesiones abiertas de esa cuenta en todos los dispositivos.
11. El sistema registra el restablecimiento en la bitácora, confirma el cambio y lleva al usuario a la pantalla de inicio de sesión para que entre con la contraseña nueva (CU-01).

### Flujo C: consultar el perfil

12. El usuario autenticado selecciona «Mi perfil» desde el menú de su cuenta.
13. El sistema muestra sus datos: nombres, apellidos, CI, correo electrónico, rol y estado de la cuenta y, si es Postulante, además su registro universitario, carrera y teléfono. El CI y los datos que no son de contacto se muestran solo de lectura (A4).

### Flujo D: actualizar los datos de contacto

14. El usuario modifica sus datos de contacto: el correo electrónico y, si es Postulante, el teléfono.
15. Si cambia el correo, el sistema le pide su contraseña actual para confirmar que es él (A7).
16. El usuario guarda los cambios.
17. El sistema valida el formato de los datos y que el correo nuevo no pertenezca a otra cuenta (A5), y que la contraseña actual sea correcta cuando se pidió (A6).
18. El sistema guarda los cambios, registra la acción en la bitácora y confirma la actualización. A partir de ese momento, las notificaciones al usuario se envían al correo y al teléfono nuevos.

### Flujo E: cambiar la contraseña desde el perfil

19. En «Mi perfil», el usuario ingresa su contraseña actual, la nueva y su confirmación, y guarda.
20. El sistema valida que la contraseña actual sea correcta (A6) y que la nueva cumpla la política y coincida con su confirmación (A3).
21. El sistema guarda la nueva contraseña (solo su hash) y cierra las sesiones de esa cuenta en los **demás** dispositivos; la sesión desde la que se hizo el cambio sigue abierta.
22. El sistema registra el cambio en la bitácora y confirma la actualización.

## Flujos alternativos y excepciones

- **A1 (paso 4):** El correo no está registrado, o pertenece a una cuenta desactivada → el sistema no envía ningún correo y muestra el mismo mensaje neutro del paso 5, para no revelar qué correos existen en el sistema ni en qué estado están.
- **A2 (paso 7):** El enlace venció, ya se usó, fue reemplazado por uno más reciente o no es válido → el sistema informa que el enlace ya no sirve y ofrece solicitar uno nuevo (vuelve al paso 1); no cambia la contraseña.
- **A3 (pasos 9 y 20):** La contraseña nueva no cumple la política o no coincide con su confirmación → el sistema responde `400`, muestra los requisitos de la contraseña y permite reintentar, sin cambiarla.
- **A4 (paso 13):** El usuario intenta modificar su CI (o algún otro dato que no es de contacto) → el sistema no lo permite: el campo se muestra bloqueado y, si igual llega un cambio de CI, lo ignora.
- **A5 (paso 17):** El correo nuevo ya pertenece a otra cuenta → el sistema responde `409`, indica el correo como campo en conflicto y no guarda los cambios.
- **A6 (pasos 17 y 20):** La contraseña actual ingresada es incorrecta → el sistema rechaza la operación (cambio de correo o de contraseña) sin cambiar nada y permite reintentar.
- **A7 (paso 15):** Se cambia el correo electrónico → el sistema exige la contraseña actual antes de guardarlo (reautenticación).
- **A8 (flujos C, D y E):** La sesión expiró o no es válida → `401` y el sistema lleva al usuario a la pantalla de inicio de sesión.
- **A9 (paso 4):** El servicio de envío de correo no está disponible → el sistema registra el fallo, muestra igual el mensaje neutro (para no revelar que la cuenta existe) y el usuario puede volver a solicitar el enlace más tarde.
- **A10 (cualquier paso):** Falla la conexión con el servidor o con la base de datos → el sistema informa el error con un mensaje claro, no cambia nada y permite reintentar.
- **A11 (paso 3):** El usuario pide el enlace varias veces seguidas → solo el último enlace enviado es válido; los anteriores dejan de servir (A2).

## Reglas de negocio

- **Lo usa cualquier usuario del sistema** (Postulante, Administrador DUBSS, Directivo y Personal del Comedor). *(El RF-10 de la documentación de grado no menciona al Personal del Comedor en su «¿Quién?»; se toma el actor del CU-15, que lo incluye.)*
- **Mensaje neutro:** la solicitud de recuperación responde siempre igual, exista o no el correo y esté o no activa la cuenta.
- **El enlace de restablecimiento** es de un solo uso, vence a los **60 minutos** (valor de la maqueta de la documentación de grado; por confirmar con la DUBSS) y solo hay uno vigente por cuenta: pedir otro invalida el anterior. El sistema guarda solo un hash del token del enlace, nunca el token en claro. Lo genera y lo verifica el propio backend (RF-10); no se usa Firebase.
- **Política de contraseña:** la misma del CU-01: al menos 6 caracteres, sin obligación de mayúsculas, números ni símbolos, y debe coincidir con su confirmación.
- **Restablecer la contraseña cierra todas las sesiones** de la cuenta y reinicia su bloqueo temporal por intentos fallidos (si la cuenta estaba bloqueada, queda desbloqueada). *(Decisión del equipo: la documentación de grado no lo precisa.)*
- **Cambiar la contraseña desde el perfil cierra las sesiones de los demás dispositivos** y mantiene la actual. *(Decisión del equipo.)*
- **Una cuenta desactivada no recupera la contraseña:** no recibe enlace (y el mensaje sigue siendo neutro); se reactiva en el CU-14.
- **Datos editables desde el perfil:** el correo electrónico (todos los usuarios) y el teléfono (solo el Postulante, que es el único que lo tiene). **El CI no se modifica** (RF-10), y tampoco nombres, apellidos, registro universitario ni carrera. *(Decisión del equipo: la documentación de grado deja los campos «por definir» y dice «datos de contacto»; la maqueta 140 muestra nombres, apellidos y carrera como editables. Los nombres del personal los corrige el Administrador DUBSS en el CU-14.)*
- **Cambiar el correo exige la contraseña actual** (A7 de la documentación de grado, que lo deja «por definir»). *(Decisión del equipo.)* El correo sigue siendo único entre las cuentas.
- **Bitácora (CU-25):** se anotan el restablecimiento de la contraseña, el cambio de contraseña desde el perfil y la actualización del perfil (qué campos cambiaron, no sus contraseñas). La solicitud del enlace no se anota, para no dejar rastro de correos inexistentes.
- **Contraseñas, tokens del enlace y tokens de sesión nunca se escriben** en la bitácora ni en los registros del sistema ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)), ni el enlace completo.
- **El enlace del correo abre el portal web**, que es donde se define la nueva contraseña; la app móvil permite solicitar el enlace. *(Decisión del equipo.)*
- Los mensajes al usuario están en español y no exponen detalles técnicos.
- **Pendiente de definir con la DUBSS:** la vigencia definitiva del enlace, si el cambio de correo debe verificarse además con un enlace enviado al correo nuevo o avisarse al correo anterior, y si el personal debe estar obligado a cambiar la contraseña temporal del CU-14 en su primer acceso.

## Postcondiciones

- **Recuperar contraseña:** la cuenta tiene una contraseña nueva, el enlace usado ya no sirve, la cuenta no está bloqueada, no queda ninguna sesión abierta y el usuario puede iniciar sesión con la contraseña nueva; el restablecimiento quedó en la bitácora.
- **Actualizar el perfil:** el correo o el teléfono quedaron actualizados y se usan en las notificaciones al usuario; la acción quedó en la bitácora.
- **Cambiar la contraseña:** la cuenta tiene la contraseña nueva, solo sigue abierta la sesión desde la que se hizo el cambio y la acción quedó en la bitácora.

## Criterios de aceptación

- **CA01:** Dado que el usuario solicita la recuperación con el correo de una cuenta activa, el sistema debe enviar a ese correo un enlace de restablecimiento de un solo uso y mostrar el mensaje neutro.
- **CA02:** Dado que el correo no está registrado o pertenece a una cuenta desactivada, el sistema debe mostrar el mismo mensaje neutro y no enviar ningún correo.
- **CA03:** Dado que el usuario abre un enlace vigente y define una contraseña válida que coincide con su confirmación, el sistema debe guardarla, invalidar el enlace y permitirle iniciar sesión con la contraseña nueva (y no con la anterior).
- **CA04:** Dado que el enlace venció (más de 60 minutos), el sistema debe informar que ya no sirve, ofrecer solicitar uno nuevo y no cambiar la contraseña.
- **CA05:** Dado que el enlace ya se usó una vez, el sistema debe rechazar un segundo uso y no cambiar la contraseña.
- **CA06:** Dado que el usuario solicitó dos enlaces seguidos, el sistema debe aceptar solo el último y rechazar el anterior.
- **CA07:** Dado que la contraseña nueva tiene menos de 6 caracteres o no coincide con su confirmación, el sistema debe responder `400`, mostrar los requisitos y no cambiar la contraseña (tanto al restablecer como al cambiarla desde el perfil).
- **CA08:** Dado que el usuario restablece su contraseña, el sistema debe cerrar todas las sesiones de esa cuenta en todos los dispositivos.
- **CA09:** Dado que la cuenta estaba bloqueada temporalmente por intentos fallidos, el sistema debe desbloquearla al restablecer la contraseña.
- **CA10:** Dado que el usuario autenticado entra a «Mi perfil», el sistema debe mostrar sus datos con el CI solo de lectura y, si es Postulante, su registro universitario, carrera y teléfono.
- **CA11:** Dado que el usuario intenta modificar su CI, el sistema no debe permitirlo ni guardar ese cambio.
- **CA12:** Dado que el Postulante actualiza su teléfono con un valor válido, el sistema debe guardarlo y dejar la acción en la bitácora.
- **CA13:** Dado que el usuario cambia su correo e ingresa correctamente su contraseña actual, el sistema debe guardarlo, usarlo desde entonces para iniciar sesión y para las notificaciones, y dejar la acción en la bitácora.
- **CA14:** Dado que el correo nuevo ya pertenece a otra cuenta, el sistema debe responder `409` indicando el correo como campo en conflicto y no guardar los cambios.
- **CA15:** Dado que el usuario cambia su correo o su contraseña y la contraseña actual que ingresa es incorrecta, el sistema debe rechazar la operación sin cambiar nada.
- **CA16:** Dado que el usuario cambia su contraseña desde el perfil con la contraseña actual correcta, el sistema debe guardar la nueva, cerrar sus sesiones en los demás dispositivos y mantener abierta la actual.
- **CA17:** Dado que la sesión expiró o no es válida, el sistema debe responder `401` en cualquier operación del perfil y llevar al usuario a la pantalla de inicio de sesión.
- **CA18:** Dado que el servicio de correo no está disponible al pedir el enlace, el sistema debe mostrar el mensaje neutro, registrar el fallo y permitir volver a solicitarlo.
- **CA19:** Dado cualquier restablecimiento, cambio de contraseña o actualización del perfil, el sistema debe dejarlo anotado en la bitácora, y ni las contraseñas ni los tokens del enlace o de sesión deben aparecer en la bitácora ni en los registros del sistema.
- **CA20:** Dado que el usuario recupera la contraseña o gestiona su perfil desde la web o desde la app móvil, el sistema debe ofrecer el mismo comportamiento en ambas (salvo que el enlace del correo se abre en el portal web).
- **CA21:** Dado que el usuario selecciona «¿Olvidó su contraseña?» en la pantalla de inicio de sesión del CU-01, el sistema debe llevarlo a la solicitud de recuperación de este CU.

## Desarrollador

Mauro Gutierrez
