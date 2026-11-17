// Datos de ejemplo de los tests de «Mis postulaciones».
import type { DatosComplementarios, MiPostulacionDetalle, MiPostulacionResumen, OpcionesFormulario } from '../types'

/** Las opciones del formulario oficial que ofrece el backend (las mismas del papel de la DUBSS). */
export const opcionesFormulario: OpcionesFormulario = {
  procedencias: ['Capital', 'Provincia'],
  patrimonios: ['Sin vivienda propia', 'Vivienda propia'],
  estadosVulnerabilidad: ['Hogar monoparental', 'Sin situación especial'],
  dependencias: ['Independiente', 'Solo de padre o madre', 'Otro familiar', 'De la pareja', 'Ambos padres'],
  ocupacionesSosten: ['Asalariado formal', 'Asalariado informal', 'Comerciante minorista', 'Rentista'],
  rangosIngresos: ['Menor de Bs. 2500', 'De Bs. 2501 a Bs. 4000', 'De Bs. 4001 a Bs. 6000', 'Más de Bs. 6000'],
  cantidadesHijos: ['1', 'Más de 1'],
  tenenciasVivienda: ['Herencia', 'De los padres', 'Cedida', 'Anticrético', 'Alquiler'],
}

/** Solo lo obligatorio del formulario oficial contestado; lo opcional queda vacío (así lo arma el formulario). */
export const complementariosMinimos: DatosComplementarios = {
  dependenciaEconomica: 'Solo de padre o madre',
  ocupacionSosten: 'Comerciante minorista',
  rangoIngresos: 'De Bs. 2501 a Bs. 4000',
  tieneHijos: false,
  cantidadHijos: null,
  grupoFamiliar: null,
  residencia: { provincia: null, zonaAnillo: null, barrio: null, calle: null },
  tenenciaVivienda: 'Alquiler',
  infraestructura: { dormitorios: null, banos: null, comedor: null, sala: null, patio: null },
  otroBeneficio: false,
  otroBeneficioDetalle: null,
  discapacidad: false,
  discapacidadDetalle: null,
  comentario: null,
}

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
