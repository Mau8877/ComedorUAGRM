import { apiClient } from '@/store'

import type {
  AccesoDocumento,
  ArchivoAdjunto,
  DocumentosPostulacion,
  MiPostulacionDetalle,
  MiPostulacionResumen,
  MisPostulacionesParams,
  Pagina,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient compartido, que ya
// * desempaqueta el sobre del backend (con `meta` devuelve { data, meta }). Las de «Mis postulaciones» son solo lecturas;
// * las de documentos (CU16) también escriben.

/** Los parámetros con valor: lo vacío o `undefined` no viaja en la URL. */
function consulta(params: Record<string, string | number | undefined>): Record<string, string | number> {
  const limpios: Record<string, string | number> = {}
  for (const [clave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== '') limpios[clave] = valor
  }
  return limpios
}

export async function listarMisPostulacionesApi(params: MisPostulacionesParams): Promise<Pagina<MiPostulacionResumen>> {
  const respuesta = await apiClient.get<Pagina<MiPostulacionResumen>>('/postulaciones/mias', {
    params: consulta({
      page: params.page,
      pageSize: params.pageSize,
      'filter[gestionAcademica]': params.gestionAcademica,
      sort: params.sort,
    }),
  })
  return respuesta.data
}

export async function obtenerPostulacionApi(codigo: string): Promise<MiPostulacionDetalle> {
  const respuesta = await apiClient.get<MiPostulacionDetalle>(`/postulaciones/${codigo}`)
  return respuesta.data
}

// ---- Documentos de la postulación (CU16) ----

/** Los documentos que exige la convocatoria de la postulación, con su estado y sus archivos. */
export async function listarDocumentosApi(codigo: string): Promise<DocumentosPostulacion> {
  const respuesta = await apiClient.get<DocumentosPostulacion>(`/postulaciones/${codigo}/documentos`)
  return respuesta.data
}

/** El archivo viaja como `multipart/form-data`: el navegador arma el encabezado con su separador. */
function formularioConArchivo(archivo: File, tipoDocumento?: string): FormData {
  const datos = new FormData()
  if (tipoDocumento) datos.append('tipoDocumento', tipoDocumento)
  datos.append('archivo', archivo)
  return datos
}

export async function adjuntarDocumentoApi(
  codigo: string,
  tipoDocumento: string,
  archivo: File,
): Promise<ArchivoAdjunto> {
  const respuesta = await apiClient.post<ArchivoAdjunto>(
    `/postulaciones/${codigo}/documentos`,
    formularioConArchivo(archivo, tipoDocumento),
  )
  return respuesta.data
}

export async function reemplazarDocumentoApi(
  codigo: string,
  documento: string,
  archivo: File,
): Promise<ArchivoAdjunto> {
  const respuesta = await apiClient.put<ArchivoAdjunto>(
    `/postulaciones/${codigo}/documentos/${documento}`,
    formularioConArchivo(archivo),
  )
  return respuesta.data
}

export async function eliminarDocumentoApi(codigo: string, documento: string): Promise<void> {
  await apiClient.delete(`/postulaciones/${codigo}/documentos/${documento}`)
}

/** Una dirección temporal (5 minutos) para abrir el archivo; el cliente la abre en otra pestaña. */
export async function accesoDocumentoApi(codigo: string, documento: string): Promise<AccesoDocumento> {
  const respuesta = await apiClient.get<AccesoDocumento>(`/postulaciones/${codigo}/documentos/${documento}/acceso`)
  return respuesta.data
}
