import type { Auditoria, DocumentoExpediente, Expediente } from '../types'

export function auditoria(extra: Partial<Auditoria> = {}): Auditoria {
  return {
    codigo: 'a-1',
    resultado: 'VALIDADO',
    observaciones: null,
    puntajeAcademico: 25,
    fecha: '2026-11-07T15:30:00Z',
    auditor: 'Ana Auditora',
    ...extra,
  }
}

export function documento(extra: Partial<DocumentoExpediente> = {}): DocumentoExpediente {
  return {
    codigo: 'd-1',
    tipo: 'Carnet de identidad',
    obligatorio: true,
    estado: 'RECIBIDO',
    nombreArchivo: 'carnet.pdf',
    mimeType: 'application/pdf',
    subidoEn: '2026-11-01T10:00:00Z',
    ...extra,
  }
}

export function expediente(extra: Partial<Expediente> = {}): Expediente {
  return {
    finalista: { codigo: 'f-1', posicion: 1, plazoEntrega: '2026-11-20', entrega: { estado: 'RECIBIDA', fecha: '2026-11-05' } },
    postulante: { nombres: 'Luis', apellidos: 'Pérez', ci: '9001', carrera: 'Ingeniería Informática' },
    convocatoria: { codigo: 'c-1', nombre: 'Beca Comedor 2026' },
    respuestaSocioeconomica: {
      ingresosFamiliares: 1500,
      dependientes: 3,
      patrimonio: 'Vivienda alquilada',
      procedencia: 'Montero',
      promedioAcademico: 71.5,
      estadoVulnerabilidad: 'Alta',
    },
    documentos: [documento(), documento({ codigo: null, tipo: 'Certificado de notas', estado: 'FALTA', nombreArchivo: null, mimeType: null, subidoEn: null })],
    estadoPostulacion: 'calificada',
    puntajeIa: 55.5,
    desglose: null,
    puntajeAcademico: 25,
    puntajeTotal: 80.5,
    auditorias: [auditoria()],
    ...extra,
  }
}
