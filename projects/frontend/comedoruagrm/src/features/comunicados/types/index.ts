// Contrato del backend de CU21 -- ver UT-BACKEND/README.md (ComunicadoResponse, PublicarComunicadoRequest).

/** `ADJUDICADOS` existe en el diseño lógico pero todavía no se publica (CU-05 / CU-22). */
export type DestinatariosComunicado = 'TODOS' | 'FINALISTAS'

export interface Comunicado {
  codigo: string
  convocatoriaId: string | null
  titulo: string
  contenido: string
  destinatarios: DestinatariosComunicado | 'ADJUDICADOS'
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
