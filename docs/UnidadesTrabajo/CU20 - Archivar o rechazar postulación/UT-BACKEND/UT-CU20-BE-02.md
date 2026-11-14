# UT-CU20-BE-02 — Códigos de error, bitácora, DTOs y eventos de aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-02` |
| **Nombre** | Códigos de error, bitácora, DTOs y eventos de aviso |
| **Caso de uso** | CU20-BE |
| **Depende de** | — |

## Antes de implementar

- [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [`GUIA_ERRORES_BACKEND.md`](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md).
- Como referencia: `DevolucionErrorCodes`, `DevolverPostulacionRequest`, `DevolucionResponse` y el evento `POSTULACION_DEVUELTA` (CU-19).

## Solicitud de negocio

El cierre necesita su error catalogado, sus acciones en la bitácora, su contrato y sus avisos al postulante.

## Objetivo

Quedan listos el código de error, las acciones de bitácora, los DTOs y los eventos de aviso del archivo y del rechazo.

## Alcance

- `B/features/postulaciones/exception/CierrePostulacionErrorCodes.java` (prefijo `ARC`, *propuesto, por revisar*): `ERR_ARC_01` (`409`, la postulación ya no está «calificada» o los resultados de la convocatoria ya fueron publicados). Finalista inexistente reutiliza `ERR_FIN_01`; motivo inválido cae en `ERR_SYS_01`.
- **Documentar el código** en `docs/errors/postulaciones/ERRORES_POSTULACIONES.md` (HTTP, excepción, significado, dónde se lanza) y agregar «Archivo y rechazo (`ARC`)» con su link en `docs/errors/README.md`, como exige `GUIA_ERRORES_BACKEND.md`.
- `BitacoraAcciones.ARCHIVO_POSTULACION` y `BitacoraAcciones.RECHAZO_POSTULACION` (el actor es el administrador; el detalle lleva códigos y estados, nunca el motivo, nombres, CI, rutas ni tokens).
- `B/features/postulaciones/dto/CierrePostulacionRequest.java` (`record`): `motivo` con `@NotBlank` («El motivo es obligatorio») y `@Size(max = 500)` («El motivo no puede pasar de 500 caracteres»). Se usa para los dos endpoints.
- `B/features/postulaciones/dto/CierrePostulacionResponse.java` (`record`): `postulacion`, `estado` (`archivada` o `no_adjudicada`), `motivo`, `fecha` y `decididaPor` (nombre del administrador).
- `EventoNotificacion.POSTULACION_ARCHIVADA` (tipo `INFO`) y `EventoNotificacion.POSTULACION_RECHAZADA` (tipo `ERROR`), ambos **bandeja, push y correo** (`enviaCorreo = true`), con `{nombres}` y `{motivo}`. Textos *(por revisar con la DUBSS, pregunta 6)*:
  - Archivada: título «Tu postulación fue archivada - Beca Comedor»; mensaje «Hola {nombres}, la DUBSS archivó tu postulación. Motivo: {motivo}».
  - Rechazada: título «Tu postulación no fue adjudicada - Beca Comedor»; mensaje «Hola {nombres}, la DUBSS no pudo adjudicarte la beca con esta postulación. Motivo: {motivo}».
- Tests: validación del request (vacío, solo espacios, 501 caracteres, 500 exactos válidos) y que cada plantilla se arma con sus dos datos y no queda incompleta.

Archivos esperados (crear): `CierrePostulacionErrorCodes`, `CierrePostulacionRequest`, `CierrePostulacionResponse` y sus tests. (Tocar): `BitacoraAcciones`, `EventoNotificacion`, `docs/errors/README.md` y `docs/errors/postulaciones/ERRORES_POSTULACIONES.md`.

## Fuera de alcance (no tocar)

- La lógica que lanza el error (UT-04).
- Idempotencia por clave: no se usa.
- Otros eventos de aviso ni las plantillas del CU-07, CU-19 o CU-21.
- Cambiar `ERR_DEV_01` / `ERR_DEV_02`.

## Resultado esperado

El módulo tiene el código de error documentado, sus acciones de bitácora, su contrato y sus dos avisos.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
