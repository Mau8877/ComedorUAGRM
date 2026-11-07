# UT-CU07-FE-04 — Formulario de auditoría con confirmación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-04` |
| **Nombre** | Formulario de auditoría con confirmación |
| **Caso de uso** | CU07-FE |
| **Depende de** | `UT-CU07-FE-01`, `UT-CU07-FE-03` |

## Antes de implementar

- [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (error solo en campos tocados) y [`TAILWIND_STYLES_FRONTEND.md`](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).
- Maqueta `107` (panel «Resultado de la auditoría»).

## Solicitud de negocio

El administrador deja constancia del resultado del cotejo, de sus observaciones y del puntaje académico.

## Objetivo

El administrador registra una auditoría con confirmación.

## Alcance

- `F/features/expedientes/components/AuditoriaForm.tsx` con TanStack Form + el schema de UT-01: resultado (Validado / Observado / No válido; con el `RadioGroup` de shadcn de `components/ui/radio-group.tsx`, nunca `input type="radio"` nativo), **puntaje académico** (0 a 30, visible y obligatorio solo si es «Validado») y observaciones con **contador de 500 caracteres** (obligatorias si no es «Validado»). Un solo campo de observaciones (la maqueta dibuja dos).
- Muestra el puntaje de la IA y el total resultante como referencia mientras se escribe el puntaje académico (solo lectura).
- El botón «Guardar auditoría» abre un `AlertDialog` de confirmación; al confirmar llama a `useRegistrarAuditoria`, muestra el éxito y refresca el expediente. Deshabilitado con el motivo visible si la entrega física está pendiente (`ERR_EXP_01`) o la postulación ya no está «calificada» (`ERR_EXP_02`); los errores del servidor se muestran con `mapearErrorExpedientes`.
- Evita el doble envío (botón en curso) y manda un `Idempotency-Key` (UUID) por intento: se reutiliza al reintentar exactamente lo mismo y se renueva tras un éxito o si cambia el contenido; así la idempotencia del backend (UT-CU07-BE-07) cubre también la respuesta perdida. Si el backend responde `ERR_FIN_01` (el finalista desapareció) avisa a la pantalla en vez de mostrar el error.
- Tests: reglas del schema en pantalla, puntaje solo con «Validado», contador, confirmación, éxito, y los dos estados deshabilitados.

Archivos esperados (crear): `AuditoriaForm.tsx` y su test. (Crear): `components/ui/radio-group.tsx` (equivalente al del CLI de shadcn, estilo base-rhea).

## Fuera de alcance (no tocar)

- El botón «Devolver para corrección» de la maqueta (es el CU-19).
- Cambiar el estado de la postulación.
- Editar o borrar una auditoría ya registrada (son append-only).

## Resultado esperado

Se registra una auditoría válida y se rechazan las inválidas con mensajes claros.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
