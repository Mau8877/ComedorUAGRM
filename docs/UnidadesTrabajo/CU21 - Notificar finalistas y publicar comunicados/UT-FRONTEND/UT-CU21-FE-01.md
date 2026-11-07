# UT-CU21-FE-01 — API: hooks, `keys.ts` y schemas de finalistas y comunicados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-FE-01` |
| **Nombre** | API: hooks, `keys.ts` y schemas de finalistas y comunicados |
| **Caso de uso** | CU21-FE |
| **Depende de** | `UT-CU21-BE-10` |

## Antes de implementar

- [`TANSTACK_QUERY_FRONTEND.md`](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md) (`queryKey` por feature, `keys.ts`, `apiClient`) y [`FORMULARIOS_ZOD_TANSTACK_FRONTEND.md`](../../../../.claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md) (un schema, dos usos).
- Como referencia: `F/features/priorizacion/api/` (`listaPriorizadaApi.ts`, `keys.ts`, `useListaPriorizada.ts`, `mapearErrorPriorizacion.ts`).

## Solicitud de negocio

La pantalla necesita hablar con los endpoints del CU sin que cada componente arme sus pedidos.

## Objetivo

Existen los hooks, las keys, los schemas y los tipos de las features `finalistas` y `comunicados`.

## Alcance

- `F/features/finalistas/api/`: `finalistasApi.ts` (funciones sobre `apiClient`), `keys.ts` (`['finalistas','list',{...}]`), `useFinalistas`, `useNotificarFinalistas` y `useRegistrarEntrega`; cada mutación invalida `['finalistas','list']` (y la de notificar vuelve a consultar unos segundos después, porque el despacho del aviso es asíncrono).
- `F/features/comunicados/api/`: `comunicadosApi.ts`, `keys.ts`, `usePublicarComunicado` y `useComunicados`.
- `F/features/finalistas/schemas/` (`notificarFinalistasSchema`: plazo posterior a hoy, lugar y horario, documentos; `registrarEntregaSchema`: fecha no futura y observaciones) y `F/features/comunicados/schemas/` (`comunicadoSchema`: título ≤ 150, contenido, destinatarios `TODOS | FINALISTAS`). Mensajes en español; los tipos se infieren con `z.infer`.
- `types/` con `Finalista`, `EstadoNotificacion`, `Comunicado`, y un mapeo de los códigos `ERR_FIN_*`/`ERR_COM_*` a mensajes claros.
- Tests: cada función de la API manda el método, la ruta y los parámetros correctos (con `filter[convocatoriaId]`, sin `sort`), los schemas rechazan los casos de borde (plazo de hoy, fecha futura, título largo) y el mapeo de errores.

Archivos esperados (crear): los de `api/`, `schemas/`, `types/` y `tests/` de las dos features, y sus `index.ts`.

## Fuera de alcance (no tocar)

- Componentes y pantalla (UT-02 a 06).
- La bandeja de avisos (UT-07).

## Resultado esperado

Los hooks y schemas están listos y probados contra un cliente simulado.

## Validación

1. `pnpm test` pasa.
2. `pnpm lint` sin errores.
3. `pnpm build` compila.
