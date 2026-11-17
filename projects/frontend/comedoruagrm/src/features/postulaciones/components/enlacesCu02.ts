// * Los dos enlaces del CU-02 (postular y corregir una postulación devuelta): los dos abren la misma pantalla,
// * `/postular`, y el backend decide si crea un borrador nuevo o devuelve la postulación devuelta con sus respuestas.
// ? Los botones usan `href` (no `Link` tipado): recargan la app, que es inocuo porque la sesión vive en cookies.

/** Pantalla para postular a la convocatoria abierta (CU-02). */
export const RUTA_POSTULAR: string | null = '/postular'

/** Pantalla para corregir y reenviar una postulación devuelta (CU-02). */
export const RUTA_CORREGIR: string | null = '/postular'
