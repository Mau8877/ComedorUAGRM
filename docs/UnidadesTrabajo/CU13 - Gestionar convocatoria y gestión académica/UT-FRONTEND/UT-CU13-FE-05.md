# UT-CU13-FE-05 — Crear y editar una convocatoria (formulario con documentos)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU13-FE-05` |
| **Nombre** | Crear y editar una convocatoria (formulario con documentos) |
| **Caso de uso** | CU13-FE |
| **Depende de** | `UT-CU13-FE-01`, `UT-CU13-FE-02`, `UT-CU13-FE-04` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (el error solo se muestra si el campo fue tocado; `onBlur={field.handleBlur}` en cada campo)
- [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)
- `F/features/usuarios/components/CrearUsuarioModal.tsx`, `EditarUsuarioModal.tsx` y `CampoUsuario.tsx` (patrón de modal con TanStack Form)

## Solicitud de negocio

El Administrador crea o corrige una convocatoria indicando su gestión, fechas, cupos, puntaje mínimo, cantidad de finalistas y los documentos que se exigen (flujo principal, pasos 4 a 8).

## Objetivo

Desde la lista se puede crear una convocatoria o editar una existente con un formulario validado, y ver el detalle en modo lectura.

## Alcance

- `F/features/convocatorias/components/ConvocatoriaFormModal.tsx`: un modal (`components/ui/modal-layout`) con TanStack Form y `convocatoriaSchema` como validador (`validators.onChange`). Tres modos según la convocatoria que recibe: **crear** (sin convocatoria), **editar** (`BORRADOR` o `ABIERTA`) y **ver** (`CERRADA` o `PUBLICADA`, todos los campos deshabilitados y sin botón de guardar). Al editar y ver, carga `useConvocatoria(codigo)` (el detalle trae `documentos` y `tienePostulaciones`) con un estado de carga.
- Campos: nombre, gestión (selector con `useGestiones`; en `ABIERTA` queda deshabilitado, porque el backend solo permite cambiarla en borrador), fecha de apertura y de cierre (`<input type="date">` con `yyyy-MM-dd`), cupos, puntaje mínimo y cantidad de finalistas (numéricos y opcionales, con el texto de ayuda «Se define con la DUBSS» donde corresponda) y la lista de documentos.
- `F/features/convocatorias/components/DocumentosRequeridosField.tsx`: lista editable de filas (selector de tipo desde `useTiposDocumento` con `pageSize` grande, que no repite los ya elegidos, más una casilla «Obligatorio» y un botón de quitar) y un botón «Agregar documento». Muestra inline el error de «al menos uno» y «al menos uno obligatorio».
- Si `tienePostulaciones`, la fecha de apertura y la lista de documentos van **deshabilitadas** con el aviso «Esta convocatoria ya tiene postulaciones: no se pueden cambiar la fecha de apertura ni los documentos» (A6). El estado de la convocatoria se muestra como etiqueta, no como campo editable.
- Envío: `useCrearConvocatoria` o `useEditarConvocatoria` (que manda la convocatoria completa, los opcionales vacíos como `null`). Al terminar: cierra el modal, avisa con un toast («Convocatoria creada» / «Convocatoria actualizada») y la lista se refresca por la invalidación del hook.
- Errores: `mapearErrorConvocatorias` decide dónde se ven. Los de campo (`ERR_CV_04` bajo la fecha de cierre) aparecen debajo de su campo; los de formulario (`ERR_SYS_01`, `ERR_CV_05`, `ERR_CV_06`, `ERR_CV_02`, `ERR_CV_03`) como aviso en el modal; los de estado o postulaciones (`ERR_CV_08`, `ERR_CV_09`) como aviso y cierran el modal refrescando la lista; error de red con reintentar. El modal conserva lo escrito si falla.
- Se conecta en `ConvocatoriasScreen`: «Nueva convocatoria» (modo crear), «Editar» y «Ver detalle».
- Se exporta `ConvocatoriaFormModal` desde el barrel de `components/`.
- Tests: crear con datos válidos llama al hook con el cuerpo correcto (opcionales vacíos como `null`); los errores de Zod aparecen inline solo en campos tocados; no se puede enviar sin documento obligatorio; un documento no se repite en el selector; con `tienePostulaciones` esos campos quedan deshabilitados; en `CERRADA` el modal es de solo lectura; cada código de error de la API se muestra donde corresponde; el modal no se cierra si falla.

Archivos esperados (crear/tocar): `ConvocatoriaFormModal.tsx`, `DocumentosRequeridosField.tsx`, `ConvocatoriasScreen.tsx`, barrels y tests.

## Fuera de alcance (no tocar)

- Abrir, cerrar y eliminar (`UT-CU13-FE-06`) y la pantalla de gestiones (`UT-CU13-FE-07`).
- Crear gestiones o tipos de documento desde este formulario (las gestiones se crean en su pantalla).
- El selector de estado del formulario del mockup (ver las decisiones del README).
- Editar ni validar nada que solo conoce el servidor (otra abierta en la gestión, postulaciones): lo informa el backend.

## Resultado esperado

El Administrador crea y edita convocatorias con sus documentos desde la lista, con errores claros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
4. Con el stack levantado: crear una convocatoria con dos documentos aparece en la lista como `BORRADOR`; editar su cupo la actualiza; una fecha de cierre igual a la de apertura muestra el error bajo la fecha de cierre.
