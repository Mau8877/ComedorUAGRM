// Datos de ejemplo de los tests de «Mis postulaciones».
import type { MiPostulacionDetalle, MiPostulacionResumen } from '../types'

export const yo = {
  codigo: 'yo',
  nombres: 'Pia',
  apellidos: 'Perez',
  correo: 'pia@prueba.test',
  rol: 'POSTULANTE',
}

export function resumen(n: number, extra: Partial<MiPostulacionResumen> = {}): MiPostulacionResumen {
  return {
    codigo: `post-${n}`,
    convocatoria: { codigo: `conv-${n}`, nombre: `Beca ${n}` },
    gestionAcademica: { codigo: `gestion-${n}`, nombre: `2026-${n}` },
    fechaEnvio: '2026-11-13T15:30:00Z',
    estado: 'pendiente',
    numeroEnvios: 1,
    ...extra,
  }
}

export function detalle(extra: Partial<MiPostulacionDetalle> = {}): MiPostulacionDetalle {
  return {
    ...resumen(1),
    lineaDeTiempo: [{ estado: 'pendiente', fecha: '2026-11-13T15:30:00Z', situacion: 'COMPLETADO' }],
    observaciones: null,
    motivo: null,
    finalista: null,
    resultado: null,
    puedeCorregir: false,
    ...extra,
  }
}
