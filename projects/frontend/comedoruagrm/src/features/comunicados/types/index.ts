// Contrato del backend de CU21 -- ver UT-BACKEND/README.md (ComunicadoResponse, PublicarComunicadoRequest).

/** `ADJUDICADOS` (finalistas con adjudicación «adjudicada» de la convocatoria) se habilitó con el CU22. */
export type DestinatariosComunicado = 'TODOS' | 'FINALISTAS' | 'ADJUDICADOS'

export interface Comunicado {
  codigo: string
  convocatoriaId: string | null
  titulo: string
  contenido: string
  destinatarios: DestinatariosComunicado
  publicadoEn: string
  /** Avisos que dejó el comunicado en las bandejas. */
  totalDestinatarios: number
}

export interface PublicarComunicadoRequest {
  titulo: string
  contenido: string
  destinatarios: DestinatariosComunicado
  convocatoriaId?: string
}
