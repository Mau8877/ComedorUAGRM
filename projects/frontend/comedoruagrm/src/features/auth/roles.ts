const ETIQUETAS: Record<string, string> = {
  POSTULANTE: 'Postulante',
  ADMINISTRADOR_DUBSS: 'Administrador DUBSS',
  DIRECTIVO: 'Directivo',
  PERSONAL_COMEDOR: 'Personal del comedor',
}

/** Nombre legible del rol para mostrar en el header (un rol desconocido se muestra tal cual). */
export function etiquetaRol(rol: string): string {
  return ETIQUETAS[rol] ?? rol
}
