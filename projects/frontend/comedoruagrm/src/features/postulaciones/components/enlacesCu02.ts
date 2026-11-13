// * Los dos enlaces que dependen del CU-02 (enviar y corregir una postulación), que desarrolla otra persona del
// * equipo. Mientras esa pantalla no exista valen `null` y los botones «Postular» y «Corregir y reenviar» se ocultan
// * (no se muestran deshabilitados sin explicación). Cuando exista, se completa la ruta acá y aparecen solos.
// ? Al engancharlas, `Link` necesita rutas tipadas: se cambia a `{ to: '/…' }` junto con la ruta nueva.

/** Pantalla para postular a la convocatoria abierta (CU-02), o `null` si todavía no existe. */
export const RUTA_POSTULAR: string | null = null

/** Pantalla para corregir y reenviar una postulación devuelta (CU-02), o `null` si todavía no existe. */
export const RUTA_CORREGIR: string | null = null
