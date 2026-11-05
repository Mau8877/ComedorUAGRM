// Contrato del backend de CU14 -- ver UT-BACKEND/README.md (UsuarioPersonalResponse y UsuarioCreadoResponse).

export interface UsuarioPersonal {
  codigo: string
  nombres: string
  apellidos: string
  ci: string
  correo: string
  /** Nombre del rol tal cual lo manda el backend. */
  rol: string
  activo: boolean
}

/** Respuesta de crear una cuenta: lo mismo más la contraseña temporal, que el backend entrega una única vez. */
export interface UsuarioCreado extends UsuarioPersonal {
  contrasenaTemporal: string
}

/** Parámetros de `GET /usuarios` (el rol y el estado viajan como `filter[rol]` y `filter[activo]`). */
export interface UsuariosListParams {
  page: number
  pageSize: number
  search?: string
  rol?: string
  activo?: boolean
  sort?: string
}
