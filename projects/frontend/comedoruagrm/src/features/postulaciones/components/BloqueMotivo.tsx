import type { EstadoPostulacion } from '../types'

/** El motivo del archivo o del rechazo de la postulación (CU20). Nunca el de la decisión final del CU05. */
export function BloqueMotivo({ motivo, estado }: { motivo: string; estado: EstadoPostulacion }) {
  const titulo = estado === 'archivada' ? 'Motivo del archivo' : 'Motivo del rechazo'
  return (
    <section aria-label="Motivo" className="rounded-2xl border border-border bg-card p-4">
      <h3 className="font-heading text-base">{titulo}</h3>
      <p className="mt-2 text-sm">{motivo}</p>
    </section>
  )
}
