# Pendiente: revisar la documentación de grado por el CU15

Las UT del CU15 (recuperar contraseña y gestionar perfil) se redactaron **sin poder
abrir la documentación de grado** (`D:\UAGRM\Taller De Grado - Junior\doc-comedor-univesitario\`,
fuera de este repo; no estaba en la máquina). Este documento lista qué hay que
mirar o actualizar ahí, para que el código y la documentación no se contradigan
(regla de [`CASOS_DE_USO.md`](../../.claude/rules/CASOS_DE_USO.md#fuente-de-verdad-la-documentación-de-grado)).

Las UT están en [`docs/UnidadesTrabajo/CU15 - Recuperar contraseña y gestionar perfil/`](../UnidadesTrabajo/CU15%20-%20Recuperar%20contrase%C3%B1a%20y%20gestionar%20perfil/UT-BACKEND/README.md).

## 1. Cambio de base de datos que hay que **agregar** a la documentación

Dos columnas nuevas en la tabla `usuario` (ver `UT-CU15-BE-02`), para guardar el enlace de
recuperación vigente de cada cuenta. Ya se agregan a `docs/DiagramaConceptual.mermaid`; falta
reflejarlo en la documentación de grado:

| Columna | Tipo | Nota |
| --- | --- | --- |
| `restablecimiento_token_hash` | `varchar(64)`, nullable, único (parcial) | Hash SHA-256 del token del enlace; nunca el token. `null` si no hay enlace vigente |
| `restablecimiento_expira_en` | `timestamptz`, nullable | Cuándo vence el enlace (60 minutos desde que se pide) |

- [ ] Actualizar `images/091_Diseno_Logico_Base_de_Datos.jpg` (tabla `usuario`).
- [ ] Actualizar `diseno_base_de_datos.md` (columnas de `usuario` y el índice único parcial).
- [ ] Si hay un diagrama de clases / modelo de dominio con `Usuario` (`090`, `018`), agregar ahí también los dos atributos.

**Alternativa descartada:** una tabla aparte `restablecimiento_contrasena`. El CU pide un solo enlace
vigente por cuenta y ningún historial, así que no hace falta. Si la DUBSS pide auditar los enlaces
emitidos, se reabre esta decisión.

## 2. Diagramas del CU15 a comparar contra el CU

Abrir los de contexto, análisis, secuencia y el mockup 140 del CU15 y confirmar que coinciden con
[`CU15`](../CasosDeUso/CU15%20-%20Recuperar%20contraseña%20y%20gestionar%20perfil.md). Cosas puntuales a mirar:

- [ ] **Mockup 140:** muestra nombres, apellidos y carrera como editables. El CU permite editar **solo correo y
      teléfono** (el CI nunca; nombres del personal los corrige el Administrador en el CU14).
- [ ] **Vigencia del enlace:** 60 minutos sale de la maqueta; confirmar con la DUBSS.
- [ ] **RF-10, «¿Quién?»:** no menciona al Personal del Comedor; el CU sí lo incluye (todos los roles).
- [ ] **Cierre de sesiones:** el CU decide que restablecer cierra **todas** las sesiones y desbloquea la cuenta, y que
      cambiar la contraseña desde el perfil cierra las **demás** y mantiene la actual. La documentación de grado no
      lo precisa; si tiene otro criterio, avisar.
- [ ] **Cambiar el correo exige la contraseña actual** (A7, «por definir» en la documentación): decisión del equipo.
- [ ] **Bitácora (CU25):** confirmar que la lista de acciones auditables incluye restablecimiento de contraseña, cambio de
      contraseña y actualización de perfil, y que **no** incluye la solicitud del enlace.

## 3. Cosas que hay que definir con la DUBSS / la universidad

- [ ] Vigencia definitiva del enlace (hoy 60 minutos).
- [ ] Si el cambio de correo debe verificarse además con un enlace al correo nuevo, o avisarse al anterior.
- [ ] Si el personal debe estar obligado a cambiar la contraseña temporal del CU14 en su primer acceso
      (necesitaría otra columna en `usuario`).
- [ ] **Proveedor de correo real:** quién da el SMTP o la cuenta emisora (la universidad, la DUBSS u otro). En desarrollo se usa
      Mailpit; pasar a producción es solo cambiar variables de entorno (`MAIL_*`). Conviene que la documentación de despliegue
      (RNF / arquitectura de despliegue, imágenes `118`–`120`) mencione el servicio de correo como componente externo.
