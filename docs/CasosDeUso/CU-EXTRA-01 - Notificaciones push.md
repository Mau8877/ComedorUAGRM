# Caso de uso

| Id          | Nombre               | Módulo                      | Prioridad | Estado    |
| ----------- | -------------------- | --------------------------- | --------- | --------- |
| CU-EXTRA-01 | Notificaciones push  | Transversal (infraestructura) | Media     | Hecho |

**Actor principal:** Usuario del Sistema (el que tiene la app móvil instalada y recibe los avisos)
**Actores secundarios:** Firebase Cloud Messaging (entrega el push al teléfono); Servicio de correo electrónico (entrega el aviso por correo); los módulos del sistema que originan un aviso
**Objetivo:** Que el sistema pueda avisar a una persona de un evento por tres vías a la vez —push en su teléfono, correo y su bandeja de notificaciones—, de forma confiable y sin que cada módulo de negocio tenga que saber cómo se envía.
**Plataforma:** Móvil (push) y backend (push y correo). La web queda fuera.
**RF asociados:** — (caso de uso adicional, **no figura** en los 26 CU de la documentación de grado; es el mecanismo que usarán los CU que notifican, como los de adjudicación, publicación de resultados y comunicados)

**Descripción:**
Este CU deja listo el **servicio de notificaciones**: la infraestructura para avisar a un usuario de algo que pasó. Aún **no define ningún evento de negocio concreto**; cada CU que necesite avisar (por ejemplo, la publicación de resultados) agrega después su evento, sin tocar el mecanismo. Con la app instalada, el teléfono se **registra** en el sistema con su token de Firebase al iniciar sesión (y lo vuelve a registrar si Firebase lo renueva) y se **da de baja** al cerrar sesión. Cuando algún módulo pide notificar a un usuario, el sistema guarda el aviso en su bandeja, envía un **push a todos los teléfonos** registrados de esa persona y le manda además un **correo** con la plantilla del evento. Si un envío falla por un problema pasajero, el sistema **reintenta** unas pocas veces; si Firebase informa que un token ya no sirve (la app se desinstaló), el sistema lo elimina. Un fallo de envío nunca rompe la operación de negocio que originó el aviso. Para poder comprobar de punta a punta que todo funciona, el Administrador DUBSS puede mandarse un aviso de prueba a sí mismo.

Se apoya en el [CU-01: Registrarse y autenticarse](CU01%20-%20Registrarse%20y%20autenticarse.md) (hay que haber iniciado sesión para registrar el teléfono) y usa el correo del usuario, que se gestiona en el [CU-15: Recuperar contraseña y gestionar perfil](CU15%20-%20Recuperar%20contrase%C3%B1a%20y%20gestionar%20perfil.md).

---

## Precondiciones

- El usuario tiene la app móvil instalada, con conexión a Internet, y ha iniciado sesión ([CU-01](CU01%20-%20Registrarse%20y%20autenticarse.md)).
- Para recibir push reales, el proyecto de Firebase está configurado en el backend y en la app (ver las UT). Sin esa configuración el sistema funciona igual, pero el push solo se escribe en el registro del sistema y no se entrega.

## Flujo principal

### Flujo A: registrar el teléfono

1. El usuario inicia sesión en la app móvil.
2. La app pide permiso para mostrar notificaciones (A1) y obtiene de Firebase el token del teléfono.
3. La app envía el token al sistema.
4. El sistema guarda el token asociado al usuario y a la plataforma (Android) y confirma. Si el token ya existía, lo deja asociado a este usuario (A2) sin duplicarlo.
5. Si Firebase renueva el token mientras la sesión sigue abierta, la app lo envía de nuevo y se repite el paso 4.

### Flujo B: dar de baja el teléfono

6. El usuario cierra sesión en la app.
7. Antes de cerrar la sesión, la app avisa al sistema que ese token ya no debe recibir avisos de este usuario.
8. El sistema elimina el registro de ese token. Los demás teléfonos del usuario siguen registrados.

### Flujo C: notificar a un usuario

9. Un módulo del sistema pide notificar a un usuario indicando el evento (que fija el tipo, el título, el mensaje y el correo) y sus datos.
10. El sistema guarda el aviso en la bandeja del usuario y termina la operación de negocio sin esperar los envíos.
11. El sistema envía el push a **todos** los teléfonos registrados del usuario (A3, A4, A5) y el correo a su dirección (A6).
12. Cuando termina los envíos, el sistema marca el aviso con la fecha de envío.

### Flujo D: recibir el aviso en el teléfono

13. El teléfono muestra la notificación en la bandeja del sistema operativo si la app está en segundo plano o cerrada; si la app está abierta, la muestra dentro de la propia app.
14. El usuario toca la notificación y la app se abre.

### Flujo E: probar el servicio

15. El Administrador DUBSS pide un aviso de prueba.
16. El sistema ejecuta el flujo C con un evento de prueba dirigido al propio administrador (push a sus teléfonos y correo a su cuenta) y responde de inmediato.

## Flujos alternativos y excepciones

- **A1 (paso 2):** El usuario no concede el permiso de notificaciones → la app sigue funcionando con normalidad, no registra el teléfono y no vuelve a insistir en cada inicio; el usuario puede activarlo después desde los ajustes del teléfono.
- **A2 (paso 4):** El token ya estaba registrado a nombre de otra cuenta (el teléfono se compartió) → el sistema lo reasigna al usuario actual; así los avisos de la cuenta anterior dejan de llegar a ese teléfono.
- **A3 (paso 11):** El usuario no tiene ningún teléfono registrado → no se envía push; se guarda el aviso y se manda el correo.
- **A4 (paso 11):** Firebase informa que un token ya no es válido (app desinstalada o token vencido) → el sistema elimina ese registro y sigue con los demás.
- **A5 (paso 11):** Firebase no responde o falla de forma pasajera → el sistema reintenta hasta 3 veces con esperas crecientes; si agota los intentos, lo registra y descarta ese envío. El aviso sigue en la bandeja.
- **A6 (paso 11):** El servicio de correo no está disponible → mismo tratamiento que A5: reintentos y, si no se logra, se registra y se descarta ese envío.
- **A7 (paso 11):** Firebase no está configurado en el backend → el push no se entrega: queda escrito en el registro del sistema (sin el token) y el resto del flujo sigue.
- **A8 (flujo B):** La baja llega sin sesión, con un token desconocido o repetido → el sistema responde igual de bien; la baja es idempotente. Si el cierre de sesión no tiene conexión, el cierre en la app se completa igual (CU-01, A10).
- **A9 (flujos A, B y E):** Sin sesión válida → `401`; un usuario sin el rol necesario en el flujo E → `403`.
- **A10 (paso 11):** El aviso lleva datos que la plantilla necesita y faltan → el sistema no envía nada y lo registra; no se guardan avisos con texto a medias.

## Reglas de negocio

- **Un usuario puede tener varios teléfonos registrados**, y un token pertenece a un solo usuario a la vez.
- **Una fila por aviso** en la bandeja del usuario, sin importar por cuántas vías se envíe. Los tipos de aviso son `SUCCESS`, `INFO`, `WARNING` y `ERROR`, el mismo vocabulario que usan los toasts de la web.
- **El envío es asíncrono y nunca rompe la operación de negocio** que notificó: si un push o un correo fallan, la operación original ya terminó bien.
- **Reintentos en memoria:** hasta 3 intentos por envío y por vía, con esperas crecientes. Si el sistema se reinicia en medio de un reintento, ese envío se pierde; el aviso sigue visible en la bandeja. Un mecanismo de reintento persistente queda fuera por ahora.
- **Plantilla por evento:** cada evento define su tipo, título, texto del push y correo. Los textos están en español.
- **Los tokens de Firebase nunca se escriben** en los registros del sistema ni en la bitácora (ver [LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)). Tampoco datos sensibles dentro del texto de un aviso.
- **Este CU no incluye** los avisos de seguridad con un enlace de un solo uso (como el correo de restablecimiento de contraseña del CU-15): esos no se guardan en la bandeja.
- **Fuera de este CU:** los endpoints para leer la bandeja (ver [NOTIFICACIONES_WEB.md](../pendientes/NOTIFICACIONES_WEB.md)), el push en la web, las preferencias por canal, iOS y el envío masivo a todos los postulantes de una gestión.
- **Pendiente de definir con la DUBSS:** qué eventos notifican y por qué vías, y el remitente y proveedor del correo real.

## Postcondiciones

- **Registro:** el teléfono queda asociado al usuario y recibirá sus avisos.
- **Baja:** el teléfono deja de recibir los avisos de ese usuario.
- **Notificación:** el aviso queda en la bandeja del usuario, con la fecha de envío una vez entregado, y el push y el correo se enviaron o se agotaron los reintentos sin afectar la operación de negocio.

## Criterios de aceptación

- **CA01:** Dado que el usuario inicia sesión en la app y concede el permiso, el sistema debe registrar el token de su teléfono asociado a su cuenta.
- **CA02:** Dado que se registra un token que ya existe, el sistema no debe duplicarlo: debe dejar un único registro asociado al usuario actual.
- **CA03:** Dado que Firebase renueva el token del teléfono, el sistema debe registrar el nuevo token.
- **CA04:** Dado que el usuario cierra sesión, el sistema debe eliminar el registro del token de ese teléfono y mantener los de sus otros teléfonos.
- **CA05:** Dado que el usuario no concede el permiso de notificaciones, la app debe seguir funcionando y el sistema no debe registrar el teléfono.
- **CA06:** Dado que un módulo notifica a un usuario con dos teléfonos registrados, el sistema debe guardar un aviso en su bandeja y enviar el push a ambos y el correo a su dirección.
- **CA07:** Dado que el usuario no tiene teléfonos registrados, el sistema debe guardar el aviso y enviar solo el correo, sin error.
- **CA08:** Dado que Firebase informa que un token no es válido, el sistema debe eliminar ese registro y seguir con los demás.
- **CA09:** Dado que el envío de push o de correo falla de forma pasajera, el sistema debe reintentar hasta 3 veces y, si no lo logra, registrarlo sin afectar la operación que originó el aviso.
- **CA10:** Dado que Firebase no está configurado, el sistema debe arrancar y funcionar con normalidad dejando el push solo en el registro del sistema.
- **CA11:** Dado que el teléfono recibe un push con la app cerrada o en segundo plano, debe mostrarse en la bandeja del sistema operativo y, al tocarlo, abrir la app.
- **CA12:** Dado que el teléfono recibe un push con la app abierta, la app debe mostrar el aviso sin cerrarse ni interrumpir lo que el usuario hacía.
- **CA13:** Dado que el Administrador DUBSS pide el aviso de prueba, el sistema debe enviarle el push y el correo de prueba y responder sin esperar la entrega.
- **CA14:** Dado que un usuario sin rol de Administrador DUBSS pide el aviso de prueba, el sistema debe responder `403`.
- **CA15:** Dado cualquier envío, los tokens de Firebase no deben aparecer en los registros del sistema.
- **CA16:** Dado un colaborador sin credenciales de Firebase, el sistema y la app deben compilar, arrancar y funcionar sin push real.

## Desarrollador

Mauro Gutierrez
