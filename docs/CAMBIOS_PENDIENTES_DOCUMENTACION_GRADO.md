# Cambios pendientes en la documentación de grado

La autenticación del sistema dejó de ser **Firebase Auth** y pasó a ser un **JWT propio del backend** (token de acceso + refresh token por dispositivo, contraseñas con BCrypt, bloqueo por intentos fallidos implementado por el sistema). Firebase queda **solo** para notificaciones push (FCM) de mobile.

El repositorio ya está al día (código, reglas de `.claude/rules/`, CU01, unidades de trabajo, `docs/errors/` y `docs/DiagramaConceptual.mermaid`). Lo que falta es actualizar la documentación de grado, que vive **fuera** de este repositorio en `D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`, para que todo siga siendo coherente entre sí. Hasta entonces, **ganan el CU01 y las UT de este repositorio** cuando haya contradicción.

## 1. Diagrama de diseño lógico de la base de datos (`images/091_Diseno_Logico_Base_de_Datos.jpg`) y entidad-relación

Es la fuente de verdad del modelo de datos; `docs/DiagramaConceptual.mermaid` ya es su transcripción actualizada.

**Tabla `usuario`**
- Quitar `firebase_uid varchar(128) UK`.
- Agregar `password_hash varchar(100)` (hash BCrypt, `NOT NULL`).
- Agregar `intentos_fallidos int` (`NOT NULL`, default 0).
- Agregar `bloqueos_consecutivos int` (`NOT NULL`, default 0).
- Agregar `bloqueado_hasta timestamptz` (`NULL`).

**Tabla nueva `sesion`** (una fila por dispositivo / refresh token)
- `id bigint PK`, `codigo_sesion uuid UK`.
- `usuario_id bigint FK` → `usuario`.
- `token_hash varchar(64) UK` (SHA-256 del refresh token; nunca el token).
- `expira_en timestamptz`, `revocada_en timestamptz NULL`.
- Columnas comunes: `creado_en`, `actualizado_en`, `eliminado_en`.

**Relación nueva:** `USUARIO ||--o{ SESION : abre`.

**Documentos de apoyo a corregir con lo mismo**
- `diseno_base_de_datos.md`: criterios de la tabla `usuario` (ya no hay `firebase_uid`), sección de índices (quitar el único parcial de `firebase_uid`, agregar el de `sesion.token_hash` y el índice de `sesion.usuario_id`), y cualquier mención de "contraseñas gestionadas por Firebase".
- `images/090_Diagrama_de_Clases.jpg` y `images/018_Modelo_Dominio.png`: en la clase/entidad `Usuario`, reemplazar el atributo de identificador de Firebase por la contraseña (hash) y el estado de bloqueo; agregar la clase `Sesion` asociada a `Usuario`.

## 2. Diagrama de análisis de CU01 (`images/051_Analisis_CU01_Registrarse_Autenticarse.jpg`)

- Quitar el servicio externo **Firebase Auth** como actor/clase de frontera del registro y el inicio de sesión.
- La verificación de credenciales, la emisión de tokens y el bloqueo por intentos son del **propio sistema**: la clase de control de autenticación del backend usa las entidades `Usuario` y `Sesion`.
- Agregar las operaciones que hoy existen: registrar (crea cuenta y abre sesión), iniciar sesión, renovar sesión (refresh), cerrar sesión (revoca la sesión del dispositivo) y consultar el usuario actual.

## 3. Diagrama de secuencia de CU01 (`images/121_Secuencia_CU01_Registrarse_Autenticarse.jpg`)

Reemplazar los mensajes a/desde Firebase por el siguiente flujo, todo contra el backend:

- **Registro:** formulario → `POST /auth/registro` (datos + contraseña + confirmación) → el sistema valida, verifica unicidad (CI, correo, registro universitario), crea `usuario` + `postulante` con rol Postulante y hash de la contraseña, abre la sesión y anota `REGISTRO_USUARIO` e `INICIO_SESION` en la bitácora → responde `201` con token de acceso, refresh token y usuario.
- **Inicio de sesión:** formulario → `POST /auth/login` → el sistema comprueba si la cuenta está bloqueada, verifica la contraseña (mismo `401` si falla, suma un intento; al 5.º bloquea), verifica que esté activa, abre la sesión, anota `INICIO_SESION` → responde `200` con tokens y usuario → el cliente lleva al módulo del rol.
- **Renovación:** con el token de acceso vencido, el cliente llama `POST /auth/refresh` con el refresh token → el sistema lo rota (revoca el usado, emite uno nuevo).
- **Cierre de sesión:** `POST /auth/logout` con el refresh token → el sistema lo revoca; solo cierra este dispositivo.
- Quitar el paso de "crear la cuenta en Firebase" y el de "`POST /auth/sesion` tras el login".

## 4. Diagrama general de casos de uso y lista de CU (`019_Diagrama_General_CU.jpg`, `casos_de_uso_filtrados.md`, `actores_por_cu.md`)

- Quitar a **Firebase Auth** como actor secundario/sistema externo de CU01 (y de CU15, que extiende a CU01, si figura).
- Si el diagrama de contexto de CU01 (`038_CU01_Registrarse_Autenticarse.png`) lo muestra, también.
- Firebase puede seguir apareciendo como sistema externo solo en los CU de notificaciones push (FCM).

## 5. Mockup de CU01 (`images/134_Mockup_CU01_Registrarse_autenticarse.png`)

Ya estaba desactualizado antes de este cambio (contraseña de mínimo 8 con letras y números, sin carrera, registro universitario ni teléfono, con casilla de términos y login/registro en pestañas). Rehacerlo con: formulario de registro con nombres, apellidos, CI, registro universitario, carrera, teléfono, correo, contraseña (mínimo 6 caracteres) y confirmación; login con correo y contraseña; aviso de cuenta bloqueada con los minutos restantes.

## 6. Arquitectura (`images/118`–`120`) y texto del stack

- Diagramas de despliegue, de lógica general y de lógica del backend: el backend **emite y verifica** los JWT; Firebase aparece solo como proveedor de **FCM (push)** y, si se mantiene, **Storage**.
- `CLAUDE.md` de la documentación de grado y el capítulo del stack (`secciones/…`): reemplazar "Spring Boot (REST, JWT, HTTPS) + Firebase: Auth, FCM, Storage" por "Spring Boot (REST, JWT propio, HTTPS) + Firebase: FCM (y Storage)".

## 7. Requisitos (`secciones/05_capitulo4_ieee830/03_requisitos_especificos.tex`)

- **RF-01:** redactar que el sistema verifica las credenciales y gestiona la sesión por sí mismo, y que bloquea la cuenta tras 5 intentos fallidos consecutivos (5 min la primera vez, más tiempo después; escala **por definir con la DUBSS**).
- Requisitos no funcionales de seguridad: contraseñas almacenadas solo como hash; tokens de acceso de vida corta; sesión revocable por dispositivo.

## 8. Pendientes de definición con la DUBSS

- Escala exacta del bloqueo por intentos fallidos (hoy: 5 min, 15 min, 1 h, 24 h).
- Formato del CI.
- Si los archivos se quedan en Firebase Storage o pasan a MinIO (hoy el stack tiene ambos; no es parte de este cambio).
