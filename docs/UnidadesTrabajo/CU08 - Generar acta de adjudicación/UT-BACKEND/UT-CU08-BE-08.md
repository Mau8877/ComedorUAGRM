# UT-CU08-BE-08 — Listar, consultar y descargar actas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-08` |
| **Nombre** | Listar, consultar y descargar actas |
| **Caso de uso** | CU08-BE |
| **Depende de** | `UT-CU08-BE-02`, `UT-CU08-BE-03`, `UT-CU08-BE-04` |

## Antes de implementar

- [`AUDITORIA_BITACORA_BACKEND.md`](../../../../.claude/rules/backend/AUDITORIA_BITACORA_BACKEND.md) (`@Auditar` sobre un método de otro bean; el resultado debe ser `Auditable`), [`LOGGING_BACKEND.md`](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [`EXCEPCIONES_BACKEND.md`](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).
- Como referencia: `ExpedienteService` (acceso a un documento con `AlmacenamientoService.urlTemporal`).

## Solicitud de negocio

El Administrador necesita ver las actas ya generadas, saber qué convocatorias están pendientes de acta y descargar cada PDF, dejando constancia de quién lo hizo.

## Objetivo

El Administrador lista, consulta y descarga las actas.

## Alcance

- `B/features/actas/service/ConsultaActasService.java` (`@RequiredArgsConstructor`, `@PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")`):
  - `listar(String search, Pageable)` → página de `ActaResponse` (sin bitácora).
  - `pendientes(Pageable)` → página de `ConvocatoriaPendienteActaResponse`.
  - `obtener(UUID convocatoriaCodigo)` → `ActaResponse`; `ERR_PRI_01` si la convocatoria no existe y `ERR_ACT_03` (`404`) si no tiene acta. Sin bitácora.
  - `descargar(UUID actorCodigo, UUID convocatoriaCodigo)` con `@Transactional` y `@Auditar(accion = BitacoraAcciones.DESCARGA_ACTA, entidad = "acta_adjudicacion")`, devolviendo `ResultadoAuditado<AccesoActaResponse>`: busca el acta (`ERR_PRI_01` o `ERR_ACT_03`), comprueba con `AlmacenamientoService.existe` que el archivo esté (`ERR_ACT_04`, `404`, **sin** anotar la descarga) y devuelve la dirección temporal firmada (`urlTemporal`, vigencia `AlmacenamientoService.VIGENCIA_ACCESO`) con su `expiraEn`. La bitácora lleva solo los códigos de acta y convocatoria.
- Ninguna respuesta ni la bitácora incluye la ruta del archivo ni la dirección firmada. No hay logging de la dirección.
- Tests con los colaboradores simulados: el listado pagina, busca y ordena; `sort` inválido (`ERR_SYS_05`); `pendientes` excluye las que ya tienen acta; `obtener` y `descargar` con acta, sin acta (`ERR_ACT_03`), con convocatoria inexistente y con archivo ausente (`ERR_ACT_04`, sin bitácora); `descargar` anota una vez; la respuesta no contiene la ruta; rol distinto de Administrador recibe `403`.

Archivos esperados (crear): `ConsultaActasService` y su test.

## Fuera de alcance (no tocar)

- Los endpoints (UT-09).
- Generar el acta o la vista previa (UT-07).
- Descargar el binario a través del backend: solo se entrega la dirección temporal.

## Resultado esperado

El Administrador ve las actas, sabe qué falta y descarga el PDF con constancia en la bitácora.

## Validación

1. `./mvnw test` pasa.
2. `./mvnw checkstyle:check` sin errores.
