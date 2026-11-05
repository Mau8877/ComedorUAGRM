import { etiquetaRol } from '@/features/auth'

import type { Perfil } from '../types'
import { CampoSoloLectura } from './CampoSoloLectura'

/**
 * Datos del usuario que NO se editan desde el perfil (CU15, flujo C): nombres, apellidos, CI, rol y estado
 * y, si es Postulante, su registro universitario y carrera. El CI nunca es un campo editable (A4, RF-10);
 * los nombres del personal los corrige el Administrador DUBSS en la gestión de cuentas.
 */
export function MisDatos({ perfil }: { perfil: Perfil }) {
  return (
    <section aria-labelledby="mis-datos" className="flex flex-col gap-4">
      <h2 id="mis-datos" className="font-heading text-lg font-semibold">
        Mis datos
      </h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <CampoSoloLectura id="perfil-nombres" label="Nombres" valor={perfil.nombres} />
        <CampoSoloLectura id="perfil-apellidos" label="Apellidos" valor={perfil.apellidos} />
        <CampoSoloLectura id="perfil-ci" label="CI" valor={perfil.ci} nota="El CI no se puede modificar." />
        <CampoSoloLectura id="perfil-rol" label="Rol" valor={etiquetaRol(perfil.rol)} />
        <CampoSoloLectura id="perfil-estado" label="Estado de la cuenta" valor={perfil.activo ? 'Activa' : 'Desactivada'} />
        {perfil.postulante && (
          <>
            <CampoSoloLectura
              id="perfil-registro"
              label="Registro universitario"
              valor={perfil.postulante.registroUniversitario}
            />
            <CampoSoloLectura
              id="perfil-carrera"
              label="Carrera"
              valor={`${perfil.postulante.carrera.nombre} (${perfil.postulante.carrera.facultad})`}
            />
          </>
        )}
      </div>
    </section>
  )
}
