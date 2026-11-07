# UT-CU07-FE-03 — Componentes del expediente: datos, documentos, puntajes e historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-03` |
| **Nombre** | Componentes del expediente: datos, documentos, puntajes e historial |
| **Caso de uso** | CU07-FE |
| **Depende de** | `UT-CU07-FE-01` |

## Antes de implementar

- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y [`TESTING_FRONTEND.md`](../../../../.claude/rules/frontend/TESTING_FRONTEND.md).
- Maqueta `107` de la documentación de grado (panel «Documentos presentados»).

## Solicitud de negocio

Para decidir con respaldo, el administrador necesita ver todo lo del postulante y abrir sus documentos.

## Objetivo

Existen los componentes de lectura del expediente.

## Alcance

- `F/features/expedientes/components/ExpedienteResumen.tsx`: nombre, CI, carrera, posición, convocatoria y respuestas socioeconómicas (con el promedio académico declarado, como referencia para el puntaje académico).
- `DocumentosDigitales.tsx`: tabla con documento, estado («Recibido» / «Falta») y botón «Abrir»; al abrir pide la URL temporal (`useAccesoDocumento`) y muestra el archivo en una vista previa o en una pestaña nueva. Si falla, muestra un mensaje claro con «Reintentar» y el resto del expediente sigue (A4).
- `PuntajePanel.tsx`: puntaje de la IA (de 0 a 70) con su desglose, puntaje académico (de la última auditoría) y total, **solo lectura**; «—» donde falte.
- `HistorialAuditorias.tsx`: auditorías anteriores (resultado, fecha, auditor, puntaje académico y observaciones), la más reciente primero; texto «Sin auditorías todavía» si no hay.
- Todo con tokens del tema (sin valores arbitrarios). Tests de cada componente: estados, abrir un documento, error con reintento.

Archivos esperados (crear): los cuatro componentes y sus tests.

## Fuera de alcance (no tocar)

- El formulario de auditoría (UT-04).
- Editar el puntaje de la IA o los datos del postulante.
- Descargar todos los documentos.

## Resultado esperado

El expediente se puede leer completo y abrir cada documento.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
