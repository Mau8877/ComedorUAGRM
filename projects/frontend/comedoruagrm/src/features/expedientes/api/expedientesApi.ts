import { apiClient } from '@/store'

import type {
  AccesoDocumento,
  CerrarPostulacionRequest,
  CierrePostulacionResultado,
  DecisionAdjudicacionResultado,
  DevolucionResultado,
  DevolverPostulacionRequest,
  Expediente,
  NoAdjudicarRequest,
  RegistrarAuditoriaRequest,
  ResultadoRegistroAuditoria,
} from '../types'

// * Funciones sin hook: las usan los hooks de este mismo directorio. Todas van por el apiClient
// * compartido, que ya desempaqueta el sobre del backend. El expediente cuelga del finalista:
// * `/finalistas/{codigo}/...`.

export async function obtenerExpedienteApi(finalista: string): Promise<Expediente> {
  const respuesta = await apiClient.get<Expediente>(`/finalistas/${finalista}/expediente`)
  return respuesta.data
}

/**
 * `claveIdempotencia` va en el header `Idempotency-Key`: repetir el envío con la misma clave (doble clic,
 * respuesta perdida) devuelve la auditoría ya guardada en vez de crear otra.
 */
export async function registrarAuditoriaApi(
  finalista: string,
  pedido: RegistrarAuditoriaRequest,
  claveIdempotencia?: string,
): Promise<ResultadoRegistroAuditoria> {
  const respuesta = await apiClient.post<ResultadoRegistroAuditoria>(
    `/finalistas/${finalista}/auditorias`,
    pedido,
    claveIdempotencia ? { headers: { 'Idempotency-Key': claveIdempotencia } } : undefined,
  )
  return respuesta.data
}

/** Pide la URL temporal para abrir un documento; la ruta guardada nunca llega al cliente. */
export async function accesoDocumentoApi(finalista: string, documento: string): Promise<AccesoDocumento> {
  const respuesta = await apiClient.get<AccesoDocumento>(`/finalistas/${finalista}/documentos/${documento}/acceso`)
  return respuesta.data
}

/** Devuelve la postulación del finalista para que la corrija (CU19); avisa al postulante el backend. */
export async function devolverPostulacionApi(
  finalista: string,
  pedido: DevolverPostulacionRequest,
): Promise<DevolucionResultado> {
  const respuesta = await apiClient.post<DevolucionResultado>(`/finalistas/${finalista}/devolver`, pedido)
  return respuesta.data
}

/** Archiva la postulación calificada del finalista (CU20); avisa al postulante el backend. Sin idempotencia. */
export async function archivarPostulacionApi(
  finalista: string,
  pedido: CerrarPostulacionRequest,
): Promise<CierrePostulacionResultado> {
  const respuesta = await apiClient.post<CierrePostulacionResultado>(`/finalistas/${finalista}/archivar`, pedido)
  return respuesta.data
}

/** Rechaza la postulación calificada del finalista (CU20): queda «no adjudicada». Sin idempotencia. */
export async function rechazarPostulacionApi(
  finalista: string,
  pedido: CerrarPostulacionRequest,
): Promise<CierrePostulacionResultado> {
  const respuesta = await apiClient.post<CierrePostulacionResultado>(`/finalistas/${finalista}/rechazar`, pedido)
  return respuesta.data
}

/** Adjudica la beca al finalista auditado (CU05). Sin cuerpo y sin idempotencia: el backend rechaza la repetición. */
export async function adjudicarFinalistaApi(finalista: string): Promise<DecisionAdjudicacionResultado> {
  const respuesta = await apiClient.post<DecisionAdjudicacionResultado>(`/finalistas/${finalista}/adjudicar`)
  return respuesta.data
}

/** Decide no adjudicar la beca al finalista (CU05): queda «no adjudicada», con motivo. Sin idempotencia. */
export async function noAdjudicarFinalistaApi(
  finalista: string,
  pedido: NoAdjudicarRequest,
): Promise<DecisionAdjudicacionResultado> {
  const respuesta = await apiClient.post<DecisionAdjudicacionResultado>(
    `/finalistas/${finalista}/no-adjudicar`,
    pedido,
  )
  return respuesta.data
}
