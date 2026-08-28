---
globs: projects/frontend/**/*
---

# TanStack Query — Frontend

## No es RTK Query

TanStack Query **no usa `tags`** para invalidar cache como RTK Query. La
invalidación es manual y se dispara por **`queryKey`** — arrays que
identifican qué se cacheó. Si el `queryKey` de una query no coincide
exactamente (o por prefijo) con lo que se pasa a `invalidateQueries`, esa
query no se invalida. Por eso la convención de `queryKey` de acá abajo es
**estricta y obligatoria**, no una sugerencia — un `queryKey` inventado
ad-hoc en un componente rompe la invalidación de otras partes de la app sin
que se note hasta que el dato queda desactualizado en pantalla.

## Convención de `queryKey`

Por feature/módulo, siempre arrays, siempre el nombre del módulo primero:

```ts
// Listado (con filtros/paginación como parte de la key)
['usuarios', 'list', { page, pageSize, search, filter }]

// Recurso individual
['usuarios', 'detail', id]
```

- El primer elemento (`'usuarios'`) es el nombre del módulo — mismo nombre
  que la carpeta de la feature en `src/features/{modulo}/`.
- El segundo elemento distingue `'list'` de `'detail'` — nunca se mezcla un
  listado y un detalle bajo la misma key.
- Para `'list'`, el objeto de filtros/paginación va completo como parte de
  la key (no se serializa a string a mano) — TanStack Query lo serializa
  internamente de forma determinística.

### Invalidación

```ts
// Invalida TODOS los listados de usuarios (cualquier filtro/página)
queryClient.invalidateQueries({ queryKey: ['usuarios', 'list'] })

// Invalida solo el detalle de un usuario puntual
queryClient.invalidateQueries({ queryKey: ['usuarios', 'detail', id] })

// Invalida usuarios completo (listados + detalles)
queryClient.invalidateQueries({ queryKey: ['usuarios'] })
```

TanStack Query invalida por **prefijo** de array — por eso el orden
`[modulo, tipo, ...params]` importa: siempre se puede invalidar "todo lo de
este módulo" o "todo lo de este tipo dentro del módulo" sin tener que
enumerar cada combinación de filtros a mano. Después de una mutación
(crear/editar/eliminar un usuario), como mínimo se invalida
`['usuarios', 'list']`; si la mutación afecta un recurso puntual que ya
estaba cacheado, se invalida también su `['usuarios', 'detail', id]`.

## Dónde viven las queries/mutations

Dentro de la carpeta `api/` de cada feature (arquitectura feature-based, ver
[ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md)):

```
src/features/usuarios/
├── api/
│   ├── useUsuarios.ts       # useQuery de listado
│   ├── useUsuario.ts        # useQuery de detalle
│   ├── useCrearUsuario.ts   # useMutation
│   └── keys.ts              # factory de queryKey del módulo (ver abajo)
├── components/
└── ...
```

Un componente de UI **nunca** arma un `queryKey` a mano ni llama a
`useQuery`/`axios` directo — importa el hook ya armado desde `api/`. Esto es
lo que hace posible mantener la convención de keys sin que cada componente
la reinvente.

### Endpoints usados por más de una feature

Si un endpoint se consume desde más de un módulo, su hook va en
`src/shared/api/` (no duplicado copiando el hook en cada feature que lo
necesita, ni movido arbitrariamente "adentro" de la feature que lo usó
primero como si fuera su dueña). El `queryKey` sigue el mismo esquema, con
`'shared'` como primer elemento en vez del nombre de una feature puntual:

```ts
// src/shared/api/keys.ts
export const sharedKeys = {
  all: ['shared'] as const,
  catalogos: (tipo: string) => [...sharedKeys.all, 'catalogos', tipo] as const,
}
```

Regla para decidir si algo es `shared/` o de una feature: si day 1 solo lo
usa una feature, vive ahí adentro; se **mueve** a `shared/api/` recién
cuando una segunda feature necesita el mismo endpoint — no se anticipa
moviéndolo "por si acaso" antes de que exista ese segundo consumidor.

### Factory de keys por módulo

Cada feature define sus keys en un solo lugar (`api/keys.ts`) para que no
haya arrays sueltos escritos a mano en distintos archivos:

```ts
// src/features/usuarios/api/keys.ts
export const usuariosKeys = {
  all: ['usuarios'] as const,
  lists: () => [...usuariosKeys.all, 'list'] as const,
  list: (params: UsuariosListParams) => [...usuariosKeys.lists(), params] as const,
  details: () => [...usuariosKeys.all, 'detail'] as const,
  detail: (id: string) => [...usuariosKeys.details(), id] as const,
}
```

## Cliente HTTP centralizado

Hoy `axios` está instalado pero **no existe ningún cliente configurado
todavía** — es un prerequisito antes de escribir la primera query real.

- Se crea `src/lib/api-client.ts` con una instancia única de axios
  (`axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL })`).
- Un interceptor de response desempaqueta el sobre estándar del backend
  (`{ status, data, message, error, timestamp, meta? }`, ver
  [RESPONSES_BACKEND.md](../backend/RESPONSES_BACKEND.md)):
  - Si `status === "success"`, el interceptor devuelve `data` (y `meta`
    cuando existe) directamente — las queries no desestructuran el sobre a
    mano en cada hook.
  - Si `status === "failed"`, el interceptor lanza un `ApiError` tipado
    (`{ code: error, message }`) en vez de dejar pasar la respuesta 2xx con
    `status: "failed"` como si fuera éxito, o un error HTTP crudo de axios
    sin el código de negocio.
- Todo hook de `api/` usa esta instancia — no se crea una instancia de axios
  nueva por feature.
- Falta agregar `QueryClientProvider` en `main.tsx` (todavía no está
  wireado) — también prerequisito antes de la primera query.
