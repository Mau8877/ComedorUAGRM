import { useQuery } from '@tanstack/react-query'

import { obtenerPanelApi } from './adjudicacionApi'
import { adjudicacionKeys } from './keys'

/** El panel de adjudicación de la convocatoria; no consulta mientras no haya una elegida. */
export function usePanelAdjudicacion(convocatoriaCodigo: string | undefined) {
  return useQuery({
    queryKey: adjudicacionKeys.panel(convocatoriaCodigo as string),
    queryFn: () => obtenerPanelApi(convocatoriaCodigo as string),
    enabled: Boolean(convocatoriaCodigo),
  })
}
