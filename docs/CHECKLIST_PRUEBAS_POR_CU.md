# Checklist de pruebas manuales por caso de uso

Para verificar a mano, caso de uso por caso de uso, que lo codificado en la rama `Mauro` cumple lo que dicen los md de `docs/CasosDeUso/`. Cada ítem tiene su casilla `[ ]`: márcala con `[x]` si el resultado fue el esperado, o déjala y anota qué pasó en la columna/línea «Obs.» si falló.

> Basado en el código y los md reales del repo (rutas del frontend en `src/routes/`, controllers del backend, cargadores de datos de prueba en `config/DatosPrueba*.java`, catálogo `docs/errors/`). Los CA citados (`CA05`, etc.) son los de cada md de CU: si algo falla, ahí está el criterio exacto.

---

## 0. Resumen de estado

Llena la columna «Resultado» al terminar cada CU (`OK` / `Falla` / `Parcial`).

| CU | Nombre | Plataforma | Estado en el md | UT (BE/FE/MOB) | Resultado | Obs. |
| --- | --- | --- | --- | --- | --- | --- |
| CU01 | Registrarse y autenticarse | Web + móvil | Hecho | 16 / 9 / 10 | OK | |
| CU15 | Recuperar contraseña y gestionar perfil | Web + móvil | Hecho | 14 / 7 / 6 | OK | |
| CU14 | Gestionar usuarios y roles | Web | Hecho | 13 / 7 / – | OK | |
| CU-EXTRA-01 | Notificaciones push (bandeja + push + correo) | Backend + móvil + bandeja web | Hecho | 12 / – / 5 | OK | Probado con Firebase real (push al Android) |
| CU06 | Visualizar lista priorizada | Web | Hecho | 10 / 5 / – | OK | |
| CU21 | Notificar finalistas y publicar comunicados | Web | Hecho | 12 / 7 / – | OK | Estado «Enviando…» añadido; resultado del envío cuenta avisos registrados |
| CU07 | Auditar expediente finalista | Web | Hecho | 12 / 5 / – | OK | Los 10 fallos previos del frontend ya no ocurren (167 pruebas en verde) |
| CU19 | Devolver postulación para corrección | Web | Hecho | 9 / 4 / – | OK | «Corregir y reenviar» oculto hasta que exista el CU02 |
| CU20 | Archivar o rechazar postulación | Web | Hecho | 8 / 4 / – | OK | Doble clic cubierto por prueba de integración (dos peticiones a la vez) |
| CU05 | Auditar y adjudicar becas | Web | Hecho | 11 / 7 / – | OK | Probado a mano: panel, adjudicar, cupos agotados y no adjudicar. Seleccionar finalistas, ERR_ADJ_02/03, concurrencia y doble clic: cubiertos por las pruebas de integración |
| CU22 | Publicar resultados de adjudicación | Web + móvil (consulta) | Hecho | 11 / 5 / – | OK | «Fallo de envío» y «sin adjudicados» cubiertos por pruebas de integración; 2026-II quedó publicada |
| CU08 | Generar acta de adjudicación | Web | Hecho | 10 / 4 / – | | |
| CU04 | Consultar estado de postulación | Web + móvil | Hecho | 7 / 4 / 4 | | |
| CU12 | Dashboard de indicadores BI | Web | Hecho | 9 / 4 / – | | |

Orden de prueba recomendado (cada uno prepara datos o confianza para el siguiente): **CU01 → CU15 → CU14 → CU-EXTRA-01 → CU06 → CU21 → CU07 → CU19 → CU20 → CU05 → CU22 → CU08 → CU04 → CU12**.

### Notas conocidas (no son fallos nuevos)

- `SecurityConfigTest` falla si no hay **Redis local** levantado (es ajeno a los CU). Levanta Redis (sección 1) antes de correr `./mvnw test`.
- **UT del frontend del CU07:** había 10 pruebas fallando de antes; el 2026-11-17 las 167 de `expedientes` pasan, así que si vuelven a salir en rojo ya es una regresión.
- **CU22:** su Swagger y sus pantallas ya se verificaron (2026-11-17). Ojo: `[PRUEBA] Beca Comedor 2026-II` quedó **publicada**; CU08 usa esa convocatoria.
- **Los CU de origen no existen:** CU02 (postular), CU03 (IA), CU13 (convocatorias) los hace otro integrante. Todos los datos (convocatorias, postulaciones, puntajes, finalistas, expedientes) vienen de **cargadores de datos de prueba** que solo corren con el perfil `dev`.
- El menú del Administrador muestra **«Menús» y «Pedidos»**, y el del estudiante **«Mi Menú» y «Mis Pedidos»**: son placeholders del scaffold, no pertenecen a ningún CU. Ignóralos.
- El módulo de IA (`comedor-uagrm-ia`) solo tiene `/health`; los puntajes son de prueba.

---

## 1. Preparación del entorno (hazla una sola vez)

### 1.1 Requisitos

- [x] Docker Desktop corriendo (lo usan Postgres, Redis, MinIO, Mailpit y los tests de integración con Testcontainers).
- [x] Archivos `.env` creados a partir de sus `.env.example`: raíz del repo, `projects/backend/comedor-uagrm-core/` y `projects/frontend/comedoruagrm/`.
- [x] En el `.env` del backend: `JWT_SECRET` de **al menos 32 caracteres** (si no, el backend no arranca), `COOKIE_SECURE=false` (desarrollo por http).
- [x] Para probar correos (CU15, CU21, CU-EXTRA-01) con Mailpit, en el `.env`: `MAIL_HOST=comedor-uagrm-mailpit`, `MAIL_PORT=1025`, `MAIL_SMTP_AUTH=false`, `MAIL_SMTP_STARTTLS=false`, `MAIL_FROM=no-responder@comedoru.local`, `FRONTEND_URL=http://localhost` (el enlace de restablecer abre el portal).

### 1.2 Levantar todo

Opción A, todo en Docker (desde la raíz del repo):

```bash
docker compose up
```

Opción B, solo la infraestructura en Docker y backend/frontend en tu máquina:

```bash
docker compose up -d postgres redis minio minio-init mailpit
cd projects/backend/comedor-uagrm-core && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# en otra terminal
cd projects/frontend/comedoruagrm && pnpm install && pnpm dev
```

- [x] Backend arriba: `http://localhost:8080/health` responde.
- [x] Swagger: `http://localhost:8080/swagger-ui/index.html` abre.
- [x] Frontend: `http://localhost:5173` (o `http://localhost` si pasas por el proxy nginx).
- [x] Mailpit (bandeja de correos de desarrollo): `http://localhost:8025`.
- [x] MinIO (consola): `http://localhost:9001` (los PDF de expedientes y actas viven ahí).
- [x] En la consola del backend, al arrancar, aparecen los cargadores `DatosPrueba*` (si no, verifica que el perfil sea `dev` y que ningún `SEED_*_ENABLED=false` los apague).

### 1.3 Cuentas de prueba (perfil `dev`, contraseña `123123123`)

| Rol | Correo | Para qué CU |
| --- | --- | --- |
| Administrador DUBSS | `admin@dubss.com` | CU14, CU06, CU21, CU07, CU19, CU20, CU05, CU22, CU08 |
| Directivo | `directivo@dubss.com` | CU12 |
| Postulante finalista | `postulante.finalista@prueba.local` | CU04, CU15 (perfil), bandeja de avisos (CU21/CU22) |
| Postulante nuevo | lo creas tú en CU01 | CU01, CU15 |

Los otros ~60 postulantes de prueba (`postulante.NN@prueba.local`) **no tienen acceso** (sin contraseña usable): solo aparecen en listas.

### 1.4 Cómo llamar la API (Swagger)

- [x] En Swagger, ejecuta `POST /api/v1/auth/login` con `{"correo":"admin@dubss.com","contrasena":"123123123"}`. La sesión viaja en **cookies HttpOnly** (`access_token`, `refresh_token`): después de iniciar sesión en Swagger (o en la web, mismo host `localhost`), los demás endpoints ya van autenticados. Para probar `401`/`403` usa una ventana de incógnito o cierra sesión con `POST /auth/logout`.
- [x] Cada respuesta debe tener el sobre `{status, data, message, timestamp, error?}`. Un error trae `status:"failed"` y `error:"ERR_..."` (ver `docs/errors/README.md`).

### 1.5 Encontrar datos de prueba por estado (SQL)

Conéctate a Postgres (`localhost:5432`, usuario/base según tu `.env`) con DBeaver o `psql`. Útil para elegir un finalista en el estado que necesita cada prueba:

```sql
-- Finalistas con su convocatoria, postulación y estado (los «calificada» son los que admiten devolver/archivar/rechazar/decidir)
SELECT c.nombre AS convocatoria, f.posicion, f.codigo_finalista, p.estado,
       f.notificado_en IS NOT NULL AS notificado, f.documentacion_recibida_en IS NOT NULL AS entrega_recibida
FROM finalista f
JOIN postulacion p ON p.id = f.postulacion_id
JOIN convocatoria c ON c.id = p.convocatoria_id
WHERE f.eliminado_en IS NULL
ORDER BY c.nombre, f.posicion;

-- Convocatorias, estado, cupos y mínimo
SELECT nombre, estado, cupos, puntaje_minimo FROM convocatoria WHERE eliminado_en IS NULL ORDER BY nombre;
```

> **Ojo:** los datos de prueba del CU22 dejan **decididos** a todos los finalistas de la convocatoria `[PRUEBA]` principal. Para probar acciones sobre «calificada» (devolver, archivar, rechazar, adjudicar) busca finalistas «calificada» con la consulta de arriba (típicamente en `[PRUEBA] Con pendientes` y en las dos convocatorias de casos límite del CU05). Si gastas todos, borra la base y vuelve a levantar el backend con el perfil `dev` para regenerar los datos (`docker compose down -v` borra los volúmenes: úsalo solo en desarrollo).

### 1.6 Pruebas automáticas (UT) — comandos generales

```bash
# Backend (necesita Docker y Redis arriba para los de integración / SecurityConfigTest)
cd projects/backend/comedor-uagrm-core
./mvnw test                                      # toda la suite
./mvnw test -Dtest='NombreTest'                  # una clase
./mvnw test -Dtest='Adjudicacion*Test,CierrePostulacion*Test'   # varias por patrón
./mvnw checkstyle:check                          # estilo (lo exige el CI)

# Frontend
cd projects/frontend/comedoruagrm
pnpm test                                        # Vitest, una pasada
pnpm test -- src/features/expedientes            # una carpeta
pnpm lint && pnpm build                          # lo que corre el CI

# Móvil
cd projects/mobile
flutter pub get && flutter analyze && flutter test
```

- [x] `./mvnw test` verde (salvo `SecurityConfigTest` si no hay Redis).
- [x] `pnpm test` verde (salvo las 10 conocidas del CU07).
- [x] `pnpm lint` y `pnpm build` sin errores.
- [x] `flutter analyze` y `flutter test` verdes.

---

## CU01 — Registrarse y autenticarse

**Plataforma:** web (`/registro`, `/login`) y móvil. **Roles:** todos. **Md:** `CU01 - Registrarse y autenticarse.md`.

### Registro (flujo A)

- [x] Abre `/registro`. Registra un postulante nuevo con datos válidos (registro universitario de **9 dígitos**, contraseña ≥ 6 caracteres y confirmación igual). **Esperado:** cuenta creada, sesión abierta y te lleva al módulo del Postulante (`/mis-postulaciones`). (CA01)
- [x] Repite el registro con el **mismo CI** → mensaje de conflicto señalando el CI, `409`, no se crea. (CA02)
- [x] Repite con el **mismo correo** → conflicto en correo. (CA03)
- [x] Repite con el **mismo registro universitario** → conflicto en ese campo. (CA21)
- [x] Deja un campo obligatorio vacío, correo mal formado, contraseña de 5 caracteres, o contraseña ≠ confirmación → cada campo muestra su error inline, no se crea. (CA04, CA05)
- [x] En Swagger `POST /auth/registro` manda además `"rol":"ADMINISTRADOR_DUBSS"` → la cuenta se crea igual como **POSTULANTE**. (CA06, CA16)
- [x] Doble clic rápido en «Crear cuenta» (o repite el mismo `POST` idéntico) → una sola cuenta; la repetición devuelve `200`. (CA12)
- [x] BD: el nuevo usuario tiene `password_hash` (BCrypt, nunca la contraseña) y hay filas en `bitacora` por el alta y el acceso. (CA20)

### Inicio de sesión (flujo B)

- [x] Entra con `admin@dubss.com` → te lleva al panel del Administrador (`/panel`). (CA07)
- [x] Entra con `directivo@dubss.com` → te lleva al dashboard (`/dashboard`). (CA07)
- [x] Entra con el postulante → `/mis-postulaciones`. (CA07)
- [x] Contraseña incorrecta y correo inexistente → **mismo mensaje** en ambos casos (`401`, `ERR_AUTH_04`). (CA08)
- [x] Cuenta desactivada (desactívala en CU14) → `403`, mensaje «comuníquese con la DUBSS». (CA09)
- [x] **Bloqueo:** 5 contraseñas incorrectas seguidas → al 6.º intento, incluso con la contraseña correcta, `403` `ERR_AUTH_05` con los minutos que faltan (5 min la primera vez). (CA10)
- [x] Tras el bloqueo, espera/ajusta el tiempo y falla otra vez 5 veces → el siguiente bloqueo es **más largo** (15 min). (CA11)

### Cierre y continuidad de sesión (flujos C y D)

- [x] «Cerrar sesión» → vuelves a `/login`; si escribes a mano `/panel`, te devuelve a `/login`. (CA17, CA18)
- [x] Inicia sesión en dos navegadores, cierra en uno → el otro **sigue** con sesión. (CA19)
- [x] Con sesión abierta, **recarga la página** → sigues dentro sin pedir credenciales (consulta `GET /auth/usuario-actual`). (CA23)
- [x] Sesión que expira: baja `JWT_ACCESS_TTL_MINUTES` a 1, espera y navega → se renueva sola (`POST /auth/refresh`); si borras las cookies, te manda a `/login`. (CA15, CA22)
- [x] Logs del backend: no aparecen contraseñas ni tokens. (CA20)

### Móvil (si lo vas a probar)

- [x] `flutter run`: registro, login, logout y reapertura de la app con sesión vigente se comportan igual que en la web. (CA14)

**UT:** `./mvnw test -Dtest='Auth*Test,Sesion*Test,Registro*Test,TokenEnlaceTest'` · `pnpm test -- src/features/auth` · `flutter test`.

---

## CU15 — Recuperar contraseña y gestionar perfil

**Plataforma:** web (`/recuperar-contrasena`, `/restablecer-contrasena`, `/perfil`) y móvil. **Md:** `CU15 - Recuperar contraseña y gestionar perfil.md`.

### Recuperar contraseña

- [x] En `/login`, enlace «¿Olvidó su contraseña?» → abre `/recuperar-contrasena`. (CA13 del CU01)
- [x] Pide el enlace con el correo de una cuenta real → mensaje neutro («si la cuenta existe…, vence en 60 minutos»). **Mailpit** (`:8025`) recibe el correo con el enlace.
- [x] Pide el enlace con un correo que **no existe** y con el de una cuenta **desactivada** → **el mismo mensaje**, y Mailpit no recibe nada.
- [x] Pide el enlace **dos veces** seguidas → solo sirve el **último**; el primero da «el enlace ya no sirve» (`400` `ERR_AUTH_08`).
- [x] Abre el enlace bueno → define contraseña nueva (política: 6–72 caracteres + confirmación). **Esperado:** confirma y te lleva a `/login`; entras con la nueva; la vieja ya no sirve.
- [x] Reutiliza el mismo enlace → `ERR_AUTH_08` (un solo uso).
- [x] Contraseña nueva distinta de su confirmación → `400`, errores inline.
- [x] Tenías sesión abierta en otro navegador → al restablecer, esa sesión se **cierra**.
- [x] Una cuenta que estaba **bloqueada** por intentos fallidos queda desbloqueada tras restablecer.
- [x] Enlace vencido (cambia `app.auth.reset-ttl-minutes` o espera 60 min) → `ERR_AUTH_08`.

### Perfil

- [x] `/perfil` muestra nombres, apellidos, **CI (bloqueado, solo lectura)**, correo, rol y estado; el postulante además ve registro universitario, carrera y teléfono.
- [x] Cambia el teléfono (solo postulante) → se guarda.
- [x] Cambia el **correo** sin escribir contraseña actual → pide/rechaza (`ERR_AUTH_09`); con la contraseña actual correcta → se guarda; con una incorrecta → rechaza sin cambiar nada.
- [x] Cambia el correo a uno que ya usa otra cuenta → `409` en correo.
- [x] Manda `ci` en `PATCH /api/v1/perfil` por Swagger → se **ignora**, el CI no cambia.
- [x] Un admin (no postulante) intenta guardar teléfono por Swagger → `400` `ERR_US_13`.
- [x] **Cambiar contraseña:** con la actual correcta + nueva válida → ok; tus **otras** sesiones se cierran y la actual sigue abierta. Con la actual incorrecta → `400` `ERR_AUTH_09` (no `401`: no te saca de la sesión).
- [x] BD: filas en `bitacora` por restablecimiento, cambio de perfil y de contraseña.

**UT:** `./mvnw test -Dtest='RecuperacionContrasena*Test,RestablecimientoContrasenaServiceTest,CorreoRestablecimientoServiceTest,Perfil*Test,ActualizarPerfilRequestTest,ContrasenaRequestsTest'` · `pnpm test -- src/features/perfil src/features/auth` · `flutter test`.

---

## CU14 — Gestionar usuarios y roles

**Plataforma:** web. **Rol:** Administrador. **Rutas:** `/usuarios/administradores`, `/usuarios/directivos`, `/usuarios/personal-comedor`. **Md:** `CU14 - Gestionar usuarios y roles.md`.

- [x] Entra como `admin@dubss.com`. El menú muestra «Administradores DUBSS», «Directivos» y «Personal del Comedor»; cada una lista solo ese rol, paginada (10/20/50), con filtros por estado y búsqueda por CI o nombre. **Los postulantes no aparecen.**
- [x] **Crear** una cuenta (nombres, apellidos, CI, correo, rol): el sistema muestra una **contraseña temporal una sola vez**. Entra con esa cuenta y comprueba que funciona y llega a su módulo.
- [x] Crear con CI repetido o correo repetido → `409` indicando el campo. Con rol `POSTULANTE` o inválido → `400` `ERR_US_10`.
- [x] **Editar** nombres/apellidos/correo; correo de otra cuenta → `409`.
- [x] **Cambiar rol** de una cuenta: tras el cambio sus sesiones se cierran y el rol nuevo rige al volver a entrar. Mismo rol → `409` `ERR_US_12`.
- [x] **Desactivar** una cuenta: no puede entrar (`403`) y su sesión abierta se corta. Desactivar una ya desactivada → `409` `ERR_US_07`.
- [x] **Reactivar** → recupera su rol anterior y puede entrar.
- [x] No puedes **desactivarte ni cambiarte el rol a ti mismo** (`403` `ERR_US_08`).
- [x] No puedes dejar al sistema **sin administrador activo** (si solo queda uno: `403` `ERR_US_09`).
- [x] Con un `POSTULANTE` o un código inexistente en la URL → `404` `ERR_US_06`.
- [x] Un usuario sin rol admin (directivo/postulante) llamando a `/api/v1/usuarios` → `403` `ERR_AUTH_07`; sin sesión → `401`.
- [x] `sort`/`filter` no soportado por Swagger → `400` `ERR_US_11`.
- [x] BD: cada acción deja fila en `bitacora` (cambio de rol con rol anterior y nuevo).

**UT:** `./mvnw test -Dtest='Usuario*Test,GestionUsuariosRequestTest,GeneradorContrasenaTemporalTest,CatalogosRepositoryTest'` · `pnpm test -- src/features/usuarios`.

---

## CU-EXTRA-01 — Notificaciones push (bandeja + push + correo)

No es una pantalla propia: se prueba con la campana de la web, Swagger y el móvil. **Md:** `CU-EXTRA-01 - Notificaciones push.md`.

- [x] Como admin, `POST /api/v1/notificaciones/prueba` → `200`; el aviso de prueba aparece en **tu bandeja** (campana del encabezado) y en Mailpit (correo a tu cuenta).
- [x] La campana muestra el contador de no leídos (`GET /notificaciones/me/resumen`); abre un aviso → «marcar como leído» (`POST /{codigo}/leer`), «Leer todo» y «Eliminar notificaciones» funcionan.
- [x] Un usuario no ve ni puede marcar avisos de otro (`404` `ERR_NOT_01`).
- [x] **Sin Firebase configurado** todo sigue funcionando; el push solo queda en el log del backend (sin el token). Anota si lo probaste con Firebase real o no. _Probado con Firebase real: el push llegó a un Android._
- [x] Móvil: al iniciar sesión se registra el teléfono (`POST /dispositivos-fcm`); al cerrar sesión se da de baja (`POST /dispositivos-fcm/baja`, idempotente). BD: tabla `dispositivo_fcm`.
- [x] Token compartido con otra cuenta → se reasigna al usuario actual, sin duplicarse.
- [x] Un fallo de envío (apaga Mailpit un momento) **no** deshace la operación que originó el aviso; el aviso queda en la bandeja.
- [x] Sin sesión → `401`; endpoint de prueba con un rol que no es admin → `403`.

**UT:** `./mvnw test -Dtest='*Notificacion*Test,DispositivoFcm*Test,CanalPush*Test,Despacho*Test,ReintentosTest,Eventos*PlantillaTest'` · `pnpm test -- src/features/notificaciones` · `flutter test`.

---

## CU06 — Visualizar lista priorizada de postulantes

**Plataforma:** web, ruta `/lista-priorizada`, menú «Lista priorizada». **Rol:** Administrador. **Md:** `CU06 - Visualizar lista priorizada de postulantes.md`.

- [x] Abre la pantalla: queda seleccionada la **convocatoria más reciente**. La tabla muestra posición, nombre, CI, carrera, puntaje y fecha de cálculo, de **mayor a menor puntaje**, y arriba el total y cuántas siguen pendientes de precalificar. (CA01)
- [x] Solo aparecen postulaciones «calificada»; las devueltas/archivadas/adjudicadas/no adjudicadas/borrador/pendientes **no**. (CA03)
- [x] Cada postulación aparece **una sola vez**, con su puntaje vigente. (CA02)
- [x] Paginación: 10/20/50 (20 por defecto); en la página 2 con 20 filas la primera posición es **21**. (CA05, CA06)
- [x] Busca por **nombre** y por **CI**; filtra por **carrera**: solo salen los que cumplen y cada uno **conserva su posición real** de la lista completa. (CA07)
- [x] Recorre todas las páginas con empates de puntaje: ninguna se repite ni falta. (CA08)
- [x] Convocatoria sin calificadas → mensaje «No hay postulaciones calificadas…» y cuántas pendientes; sin tabla. (CA04)
- [x] **Exportar PDF** con la lista completa: descarga un PDF con convocatoria, fecha/hora, quién lo generó, **toda** la lista (no solo la página), mismo orden y posiciones, sin datos socioeconómicos. (CA16, CA18)
- [x] Exporta con una búsqueda/filtro activo → el PDF trae solo los filtrados e indica los filtros usados. (CA17)
- [x] Exporta con la lista vacía (filtro sin resultados) → no genera PDF, avisa «no hay postulaciones» (`400` `ERR_PRI_03`). (CA19)
- [x] BD: la exportación dejó una fila en `bitacora` (usuario, convocatoria, filtros). La consulta normal **no** deja nada. (CA14, CA20)
- [x] Un código de convocatoria inexistente en `GET /lista-priorizada?convocatoriaCodigo=...` → `404` `ERR_PRI_01`. (CA13)
- [x] Postulante/directivo llamando al endpoint → `403`; sin sesión → `401`. (CA11, CA12)
- [x] Rendimiento (opcional): con `SEED_PRIORIZACION_POSTULACIONES=20000` cada página responde en ≤ 3 s. (CA09)

**UT:** `./mvnw test -Dtest='ListaPriorizada*Test'` · `pnpm test -- src/features/priorizacion`.

---

## CU21 — Notificar finalistas, entrega física y comunicados

**Plataforma:** web, ruta `/finalistas`, menú «Finalistas». **Rol:** Administrador. **Md:** `CU21 - Notificar finalistas y publicar comunicados.md`.

### A. Notificar

- [x] Elige una convocatoria con finalistas (queda seleccionada la más reciente). La tabla muestra posición, nombre, puntaje, notificación («Sin enviar»/«Enviada»/«Fallida») y documentación («Pendiente»/«Recibida»).
- [x] Selecciona dos finalistas, define **plazo (posterior a hoy)**, **lugar y horario** y **documentos físicos**, y confirma el envío. **Esperado:** resultado «Se está enviando el aviso a N finalistas» (y, si algún aviso no se pudo registrar, cuántos); esos finalistas pasan a «Enviada» en la tabla (unos segundos después). (BD: `finalista.notificado_en`.)
- [x] Plazo de hoy o anterior → `400` `ERR_FIN_04`. Faltan lugar/documentos o ningún finalista seleccionado → no envía, indica los campos.
- [x] Entra con `postulante.finalista@prueba.local`: el aviso aparece en su **bandeja** de la web; ábrelo y márcalo como leído. Mailpit recibe el correo.
- [x] Reintento: simula fallo (apaga Mailpit), notifica, verifica «Fallida», vuelve a notificar solo a esos: el resto **no** recibe el aviso dos veces.
- [x] Convocatoria sin finalistas → «Todavía no hay finalistas en esta convocatoria», sin tabla.

### B. Entrega física

- [x] Busca un finalista por CI o nombre y marca «documentación recibida» con fecha (por defecto hoy) y observaciones opcionales → pasa a «Recibida».
- [x] Fecha **futura** → `400` `ERR_FIN_03`.
- [x] Corrige la fecha ya registrada → se guarda la nueva y la anterior queda en la `bitacora`.

### C. Comunicados

- [x] «Nuevo aviso o comunicado»: título, contenido y destinatarios (todos los postulantes o finalistas); **vista previa**; confirma. Muestra a cuántos llegó y cada destinatario recibe el aviso.
- [x] Faltan título/contenido/destinatarios → no publica. Grupo sin destinatarios → `400` `ERR_COM_01`.
- [x] Este CU **no cambia el estado** de ninguna postulación (verifica `postulacion.estado`).
- [x] Rol no admin → `403`; sin sesión → `401`.

**UT:** `./mvnw test -Dtest='Finalista*Test,SeleccionFinalistas*Test,RequestsFinalistasValidacionTest,Comunicado*Test,PublicarComunicadoRequestValidacionTest,FinalistasComunicadosIntegrationTest'` · `pnpm test -- src/features/finalistas src/features/comunicados`.

---

## CU07 — Auditar expediente finalista

**Plataforma:** web, ruta `/finalistas/{codigo}` (botón «Auditar» en la tabla de Finalistas). **Rol:** Administrador. **Md:** `CU07 - Auditar expediente finalista.md`.

Datos de prueba del expediente (cargados al arrancar): finalista 1 validado, 2 observado, 3 con entrega y sin auditar, 4 observado y luego validado, 5 no válido, 6–10 con entrega pendiente.

- [x] Desde «Finalistas», pulsa **Auditar** en el finalista 3. El expediente muestra: datos del postulante, **respuestas socioeconómicas**, documentos con estado («Recibido»/«Falta»), puntaje de la IA vigente y las auditorías previas.
- [x] Abre un documento: se obtiene un **acceso temporal** (no un enlace público); un documento de otro finalista → `404` `ERR_EXP_05`. Si MinIO está caído, error claro y el resto del expediente sigue visible.
- [x] Registra **Validado** con puntaje académico 0–30 → el expediente muestra la auditoría y el **puntaje total = IA + académico**; el postulante recibe un aviso con el resultado. (BD: `auditoria_expediente`, bitácora.)
- [x] **Observado** o **No válido** **sin observaciones** → `400` `ERR_EXP_03`; observaciones > 500 caracteres → `400`.
- [x] **Validado sin puntaje académico** o con puntaje fuera de 0–30 → `400` `ERR_EXP_04`.
- [x] Finalista con la **entrega pendiente** (6–10): no deja auditar, `409` `ERR_EXP_01` («primero registra la entrega»).
- [x] Reaudita un finalista (p. ej. el 4): se agrega una auditoría nueva y las anteriores se conservan; vale la última.
- [x] Doble clic en guardar → se registra **una sola** auditoría (idempotencia; clave repetida con otro contenido → `409` `ERR_EXP_06`).
- [x] Postulación que ya no es «calificada» (devuelta/archivada/decidida) → `409` `ERR_EXP_02`, el expediente se actualiza.
- [x] Finalista inexistente en la URL → `404` y vuelve a la lista. Rol no admin → `403`; sin sesión → `401`.

**UT:** `./mvnw test -Dtest='Expediente*Test,AuditoriaExpedienteRepositoryTest,RegistrarAuditoriaRequestValidacionTest'` · `pnpm test -- src/features/expedientes` (**10 fallos previos conocidos**: anota cuáles son y si cambió el número).

---

## CU19 — Devolver postulación para corrección

**Plataforma:** web, dentro del expediente (`/finalistas/{codigo}`), botón «Devolver para corrección». **Md:** `CU19 - Devolver postulación para corrección.md`. Sin idempotencia por decisión tuya (una segunda petición da `409`).

Necesitas un finalista **«calificada» con al menos una auditoría** (usa la consulta 1.5; el 2 o el 4 del expediente de prueba si siguen calificadas).

- [x] Pulsa «Devolver para corrección»: ventana con nombre y convocatoria y campo **motivo** (obligatorio, ≤ 500).
- [x] **Cancela** la ventana → no cambia nada (estado sigue «calificada»).
- [x] Motivo vacío, solo espacios o de más de 500 caracteres → error inline; por Swagger `400` `ERR_SYS_01`.
- [x] Confirma con un motivo válido → el expediente pasa a «Devuelta» y **ya no permite** auditar ni devolver otra vez. (BD: `postulacion.estado='devuelta'`; fila en `postulacion_historial` con estado anterior/nuevo, usuario, fecha y motivo; fila en `bitacora`.)
- [x] El postulante recibe aviso con las observaciones (bandeja, correo en Mailpit, push en log).
- [x] La postulación **sale de la lista priorizada** (CU06).
- [x] Devolver un finalista **sin auditoría** → `409` `ERR_DEV_02`.
- [x] Devolver uno que ya no está calificada (ya devuelto/archivado/…) → `409` `ERR_DEV_01`.
- [x] Finalista inexistente → `404` `ERR_FIN_01`. Rol no admin → `403`; sin sesión → `401`.
- [x] (Cruce con CU04) Entra como el postulante: su postulación devuelta muestra el estado «Devuelta», el paso en el recorrido y las **observaciones**. «Corregir y reenviar» **no** aparece: está oculto mientras no exista el CU02 (otro integrante), por diseño del CU04.

**UT:** `./mvnw test -Dtest='Devolucion*Test,DevolverPostulacionRequestValidacionTest'` · `pnpm test -- src/features/expedientes`.

---

## CU20 — Archivar o rechazar postulación

**Plataforma:** web, dentro del expediente, botones «Archivar» y «Rechazar». **Md:** `CU20 - Archivar o rechazar postulación.md`. Requiere finalista **«calificada»** y convocatoria **sin resultados publicados**.

- [x] **Archivar:** botón → ventana con nombre y convocatoria y **motivo obligatorio** (≤ 500). Cancelar no cambia nada.
- [x] Confirma → estado **«archivada»**; el expediente queda «Archivada» sin más acciones; fila en historial (con motivo) y en bitácora; aviso al postulante con el motivo. (CA de CU20)
- [x] **Rechazar** (con otro finalista) → estado **«no adjudicada»** (aviso con el texto de rechazo). Verifica en BD que **no** se creó fila en `adjudicacion` (es distinto de «No adjudicar» del CU05).
- [x] Motivo vacío/solo espacios/> 500 → `400` `ERR_SYS_01`.
- [x] Sobre una postulación que ya no es «calificada» (ya archivada, devuelta, adjudicada…) → `409` `ERR_ARC_01`.
- [x] En una convocatoria con resultados **ya publicados** (`[PRUEBA] Publicada`) → `409` `ERR_ARC_01`.
- [x] Doble clic/dos peticiones iguales → solo una surte efecto, la otra `409`.
- [x] Postulaciones archivadas/rechazadas salen de la lista priorizada.
- [x] Finalista inexistente → `404`; rol no admin → `403`; sin sesión → `401`.
- [x] (Cruce con CU04) El postulante ve el **motivo** en su detalle.

**UT:** `./mvnw test -Dtest='CierrePostulacion*Test,EventosCierrePostulacionPlantillaTest'` · `pnpm test -- src/features/expedientes`.

---

## CU05 — Auditar y adjudicar becas

**Plataforma:** web, ruta `/adjudicacion` (menú «Auditoría y adjudicación») y decisión desde el expediente. **Md:** `CU05 - Auditar y adjudicar becas.md`.

Datos: la convocatoria `[PRUEBA]` principal tiene cupos 5, puntaje mínimo 70 y cantidad de finalistas 15 (valores de **ejemplo**); hay dos convocatorias de casos límite (un cupo libre con varios validados compitiendo, y otra **sin cupos ni mínimo**).

### Panel y selección de finalistas

- [x] Abre `/adjudicacion` y elige la convocatoria: panel con **cupos, adjudicados, no adjudicados, disponibles y puntaje mínimo** + lista priorizada. (CA01)
- [x] «Seleccionar finalistas» propone la **cantidad configurada**; ajústala y confirma → se registran en el orden de la lista con su **posición real** y sin repetir a los ya seleccionados. (CA02, CA22)
- [x] Cantidad **0, negativa o mayor** que las calificadas disponibles → `400`, no selecciona a nadie. (CA03)
- [x] Pedir más finalistas que cupos **se permite** (suplentes). (A3)
- [x] Convocatoria sin cupos/mínimo definidos (o no cerrada) → `409` `ERR_ADJ_05`: no deja seleccionar ni decidir. (CA14)
- [x] La cantidad configurada **no** aparece en ninguna respuesta a postulantes ni pública. (CA22)

### Decisión sobre un finalista (desde su expediente)

- [x] **Adjudicar** un finalista «Validado», con puntaje total ≥ mínimo y cupos libres: ventana de confirmación con nombre, puntaje total y cupos; confirma → postulación **«adjudicada»**; los cupos disponibles bajan. (BD: fila en `adjudicacion` con puntaje final, usuario, fecha; `postulacion_historial`; `bitacora`.) (CA04)
- [x] **No adjudicar** con motivo válido → **«no adjudicada»** y fila en `adjudicacion` con resultado `NO_ADJUDICADA`. Sin motivo / solo espacios / > 500 → `400`. (CA05, CA06)
- [x] Finalista **sin auditoría** → ambas decisiones dan `409` `ERR_ADJ_02`. (CA07)
- [x] Última auditoría **no «Validado»** → «Adjudicar» da `409` `ERR_ADJ_02`, «No adjudicar» sí funciona. (CA08)
- [x] Puntaje total **bajo el mínimo** (el finalista validado con puntaje bajo de la semilla) → «Adjudicar» `409` `ERR_ADJ_03`; «No adjudicar» sí. (CA09)
- [x] **Cupos agotados** → «Adjudicar» `409` `ERR_ADJ_04`; «No adjudicar» sí. (CA10)
- [x] En la convocatoria de **un solo cupo libre**, intenta adjudicar a dos finalistas (idealmente casi a la vez desde dos ventanas) → solo uno se adjudica, el otro `409`; nunca se superan los cupos. (CA11)
- [x] Doble clic en confirmar → una sola decisión y una sola anotación en bitácora; la segunda `409` `ERR_ADJ_01`. (CA13)
- [x] Postulación que ya no es «calificada» o resultados publicados → `409` `ERR_ADJ_01`. (CA12)
- [x] **Cancelar** la ventana → nada cambia. (CA18)
- [x] Una vez decidida, la postulación **no aparece** en la lista priorizada ni se puede auditar/decidir otra vez. (CA19)
- [x] Al decidir **no se avisa** al postulante (se hará al publicar, CU22). Verifica que no hay aviso nuevo. (CA20)
- [x] La bitácora anota la decisión **sin** nombres, CI ni motivo. (CA17)
- [x] Finalista/convocatoria inexistente → `404`; rol no admin → `403`; sin sesión → `401`. (CA21)

**UT:** `./mvnw test -Dtest='Adjudicacion*Test,CantidadFinalistasPrivadaTest,ListaPriorizadaAdjudicacionTest,SeleccionFinalistas*Test'` · `pnpm test -- src/features/adjudicacion src/features/expedientes`. (Pendiente anotado: imagen 091 del diseño lógico por actualizar en la documentación de grado.)

---

## CU22 — Publicar resultados de adjudicación

> Swagger y pantallas verificados el 2026-11-17 (administrador y postulante), con `[PRUEBA] Beca Comedor 2026-II` ya publicada.

**Plataforma:** web, rutas `/publicar-resultados` (Administrador, menú «Publicar resultados») y `/resultados` (Postulante). **Md:** `CU22 - Publicar resultados de adjudicación.md`.

Convocatorias de prueba: **`[PRUEBA] Beca Comedor 2026-II`** (todo decidido, sin publicar → lista para publicar), **`[PRUEBA] Con pendientes`** (un finalista «calificada» → no se puede publicar), **`[PRUEBA] Publicada`** (ya publicada, para la consulta pública).

### Administrador

- [x] Swagger: `GET /api/v1/convocatorias/{codigo}/resultados/resumen` y `/decisiones` (admin) responden con adjudicados, no adjudicados y puntaje final de cada uno; `puedePublicar` verdadero/falso según el caso.
- [x] `/publicar-resultados` → elige `[PRUEBA] Con pendientes`: muestra cuántos finalistas **faltan por decidir** y el botón «Publicar» no debe permitir publicar; por Swagger `POST .../publicar` da `409` `ERR_PUB_01` con cuántos faltan. (A1)
- [x] Elige `[PRUEBA] Beca Comedor 2026-II`: resumen con adjudicados/no adjudicados y puntaje final.
- [x] Pulsa «Publicar» → ventana que avisa que **no se puede deshacer** y que se avisará a los finalistas; **cancela** → nada se publica. (A4)
- [x] Confirma → publicada. BD: fila en `publicacion_resultado` (usuario y fecha), `convocatoria.estado='PUBLICADA'`, fila en `bitacora`. Se crea **un aviso por cada finalista con decisión** (bandeja, correo en Mailpit, push en log).
- [x] Publica **otra vez** la misma convocatoria (doble clic o desde Swagger) → `409` `ERR_PUB_02`, muestra quién y cuándo publicó y no registra otra. (A2)
- [x] Convocatoria que no está **cerrada** → `409` `ERR_PUB_03`; inexistente → `404` `ERR_PRI_01`. (A3)
- [x] Fallo de envío de un aviso (apaga Mailpit antes de publicar) → la publicación queda registrada igual. (A5)
- [x] Se publica **aunque no haya adjudicados** (si todos fueron «no adjudicada») — decisión tuya; prueba con una convocatoria así si puedes.
- [x] **No** se puede despublicar (no existe endpoint ni botón).
- [x] Rol no admin → `403`; sin sesión → `401`.

### Postulante

- [x] Como `postulante.finalista@prueba.local`, `/resultados` lista convocatorias con resultados publicados (`GET /api/v1/resultados/convocatorias`).
- [x] La lista pública de una convocatoria muestra **solo nombre, apellido, nota, carrera y facultad** de los adjudicados. La **nota es el puntaje final**.
- [x] «Mi resultado» (`.../resultados/mio`) muestra «adjudicada»/«no adjudicada» **sin el motivo** de la decisión final.
- [x] Una convocatoria sin publicar → `404` `ERR_PUB_04` para el postulante (no se filtran resultados antes).
- [x] Lo mismo en la app móvil si aplica.

**UT:** `./mvnw test -Dtest='Resultados*Test,PublicacionResultado*Test,MarcaPublicacionRepositoryTest,EventosResultadosPlantillaTest'` · `pnpm test -- src/features/resultados`.

---

## CU08 — Generar acta de adjudicación

**Plataforma:** web, ruta `/actas` (menú «Actas»). **Md:** `CU08 - Generar acta de adjudicación.md`. Requiere resultados **publicados** (CU22). Para probar usa `[PRUEBA] Publicada` (si ya le generaste acta, publica antes `[PRUEBA] Beca Comedor 2026-II` en CU22 y úsala).

- [ ] `/actas` muestra la tabla de actas generadas (número, convocatoria, fecha, quién) y el panel «Generar acta» con las convocatorias **publicadas sin acta**.
- [ ] Si ninguna cumple → mensaje «No hay convocatorias con resultados publicados pendientes de acta», la tabla se sigue viendo. (A1)
- [ ] Elige una convocatoria y pulsa **«Vista previa»**: muestra el documento con número «borrador»; **no** guarda nada ni toca la bitácora.
- [ ] Pulsa **«Generar PDF»** → ventana que avisa que el acta es **definitiva** (no se regenera ni se elimina). **Cancela** → nada se guarda. (A6)
- [ ] Confirma → se genera el PDF con número automático `NNN/AAAA`, adjudicados con nombre, apellido, CI, carrera y puntaje final; se guarda en **MinIO** (verifica el objeto en `:9001`); fila en `acta_adjudicacion` y en `bitacora`.
- [ ] «Descargar» → abre/descarga el PDF por una dirección **temporal firmada (5 min)**; la descarga queda en la bitácora. Verifica que el PDF abre bien y sus datos coinciden con los adjudicados de la convocatoria.
- [ ] Generar una **segunda acta** para la misma convocatoria (o vista previa de una que ya tiene) → `409` `ERR_ACT_02`; muestra la existente. (A4)
- [ ] Convocatoria con resultados **sin publicar** → `409` `ERR_ACT_01`. (A3)
- [ ] Descargar el acta de una convocatoria **sin acta** → `404` `ERR_ACT_03`. (A5)
- [ ] Borra el objeto del acta en MinIO y descarga → `404` `ERR_ACT_04`, no se anota descarga. (A9)
- [ ] Dos actas de convocatorias distintas generadas casi a la vez → números distintos, nunca repetidos. (A7)
- [ ] Rol no admin → `403`; sin sesión → `401`.

**UT:** `./mvnw test -Dtest='Acta*Test,ConsultaActasServiceTest,NumeroActaService*Test,ActasConsultaRepositoryTest'` · `pnpm test -- src/features/actas`.

---

## CU04 — Consultar estado de postulación

**Plataforma:** web (`/mis-postulaciones`, `/mis-postulaciones/{codigo}`) y móvil. **Rol:** Postulante. **Md:** `CU04 - Consultar estado de postulación.md`.

Entra con `postulante.finalista@prueba.local`. La semilla crea una convocatoria `[PRUEBA] Mis postulaciones - …` por estado (pendiente, devuelta, archivada, decisión final sin publicar, finalista, adjudicada publicada, rechazada) y un borrador, en **dos gestiones**.

- [ ] La lista muestra las postulaciones enviadas, de la más reciente a la más antigua, con gestión, fecha de envío y estado visible. **El borrador no aparece.** (CA01, CA12)
- [ ] Filtro por **gestión académica** funciona. (CA03)
- [ ] Detalle de una postulación: estado visible y **línea de tiempo** con fecha y hora de cada cambio, **sin mostrar quién lo hizo**, más los pasos pendientes. (CA04)
- [ ] **Devuelta:** muestra las observaciones del admin con fecha. El botón «Corregir y reenviar» sigue oculto hasta que exista el CU02 (CA05, ver CU04 A3). (CA05)
- [ ] **Archivada** y **rechazada (CU20):** muestran el **motivo**. (CA06)
- [ ] **Decisión final sin publicar:** se ve «calificada» y se **oculta** esa fila del historial. (CA07)
- [ ] **Resultados publicados:** se ve «adjudicada»/«no adjudicada», botón «Ver resultado» hacia `/resultados`, **sin el motivo** de la decisión final. (CA08)
- [ ] **Finalista:** bloque «Eres finalista» con plazo, documentos a presentar y si la documentación ya fue recibida, **solo mientras la postulación sigue «calificada»**; devuelta, archivada, rechazada o adjudicada no lo muestran. (CA09)
- [ ] Desde un aviso de la campana (o push en móvil) se abre el detalle de la postulación correspondiente. (CA10)
- [ ] Una cuenta sin postulaciones (la que creaste en CU01) ve mensaje informativo. (CA02)
- [ ] `GET /api/v1/postulaciones/{codigo}` de una postulación ajena o inexistente → `404` `ERR_POS_01`; con rol admin → `403`; sin sesión → `401`. (CA11)
- [ ] Consultar no modifica estados, avisos ni bitácora. (CA13)

**UT:** `./mvnw test -Dtest='MisPostulaciones*Test,EstadoVisibleTest,NotificacionPostulacionCodigo*Test'` · `pnpm test -- src/features/postulaciones` · `flutter test`.

---

## CU12 — Visualizar dashboard de indicadores BI

**Plataforma:** web, ruta `/dashboard`. **Rol:** Directivo. **Md:** `CU12 - Visualizar dashboard de indicadores BI.md`.

Entra con `directivo@dubss.com`. La semilla crea dos convocatorias `[PRUEBA] Dashboard …` (una con resultados publicados y otra sin publicar) con postulaciones en varios meses.

- [ ] Al iniciar sesión llegas a `/dashboard`; el menú del Directivo solo tiene «Dashboard».
- [ ] Se muestran **cuatro indicadores**, los gráficos de **postulaciones por mes** y **asistencia por mes**, y la fecha de última actualización de los indicadores.
- [ ] Cambia la **gestión académica** o la **convocatoria** y pulsa «Aplicar» → se actualizan los indicadores. Los valores coinciden con lo que ves en BD.
- [ ] Selección sin datos → mensaje informativo e indicadores en cero (no es error). (A1)
- [ ] «Descargar PDF» → PDF con lo que estás viendo. Sin datos → `400` `ERR_DAS_02`, no descarga.
- [ ] `GET /dashboard/indicadores` con una gestión/convocatoria inexistente o que no corresponde a la gestión → `404` `ERR_DAS_01`; el dashboard conserva la última selección válida.
- [ ] Si los indicadores nunca se calcularon, lo indica en lugar de inventar números.
- [ ] Admin/postulante llamando a `/api/v1/dashboard/*` → `403`; sin sesión → `401`.

**UT:** `./mvnw test -Dtest='Dashboard*Test,Indicadores*Test,FiltrosDashboard*Test,RefrescoIndicadores*Test,VistaIndicadoresMigracionTest'` · `pnpm test -- src/features/dashboard`.

---

## Cierre

- [ ] Todas las casillas de «Resultado» de la tabla 0 llenas.
- [ ] Lista de fallos encontrados (cuál CU, qué paso, qué pasó, CA o `ERR_` involucrado) para que los corrijamos: _____________________
- [ ] Verifica que `git status` quedó limpio tras las pruebas (los datos de prueba están en BD, no en archivos).
