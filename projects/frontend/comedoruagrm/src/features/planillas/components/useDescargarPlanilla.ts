import { useState } from 'react'

import { mapearErrorPlanillas, useAccesoPlanilla } from '../api'

/**
 * Descargar el PDF de una planilla (CU09): pide la dirección temporal y firmada y la abre en una pestaña nueva.
 *
 * * La dirección vence en minutos y anota la descarga en la bitácora: se pide en cada clic y no se guarda.
 * * `descargando` es el código de la planilla en curso (bloquea su botón); un error deja su mensaje en `error` y
 * * no abre nada.
 */
export function useDescargarPlanilla() {
  const acceso = useAccesoPlanilla()
  const [error, setError] = useState<string | null>(null)

  const descargar = (planilla: string) => {
    setError(null)
    acceso.mutate(planilla, {
      onSuccess: ({ url }) => {
        window.open(url, '_blank', 'noopener,noreferrer')
      },
      onError: (e) => setError(mapearErrorPlanillas(e).mensaje),
    })
  }

  return {
    descargar,
    descargando: acceso.isPending ? acceso.variables : undefined,
    error,
    limpiarError: () => setError(null),
  }
}
