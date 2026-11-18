import { create } from 'zustand'

interface ConvocatoriaElegidaState {
  /** Código de la convocatoria que el Administrador eligió en la última pantalla que la pide; `undefined` = ninguna. */
  codigo: string | undefined
  setCodigo: (codigo: string | undefined) => void
}

export const useConvocatoriaElegidaStore = create<ConvocatoriaElegidaState>()((set) => ({
  codigo: undefined,
  setCodigo: (codigo) => set({ codigo }),
}))

/**
 * La convocatoria elegida, compartida por las pantallas del Administrador que trabajan sobre una (lista
 * priorizada, adjudicación, finalistas, publicar resultados): se elige una vez y las demás ya la traen
 * seleccionada, en vez de volver a la más reciente cada vez que se cambia de pantalla.
 *
 * * Es estado de UI (qué se está mirando), no datos del servidor: cada pantalla sigue validando que el código
 * * exista en su lista de convocatorias y, si no, cae en la más reciente.
 * * Misma firma que `useState`: `const [elegida, setElegida] = useConvocatoriaElegida()`.
 */
export function useConvocatoriaElegida(): [string | undefined, (codigo: string | undefined) => void] {
  const codigo = useConvocatoriaElegidaStore((s) => s.codigo)
  const setCodigo = useConvocatoriaElegidaStore((s) => s.setCodigo)
  return [codigo, setCodigo]
}
