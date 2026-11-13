import { useState } from 'react'

import { mapearErrorActas, useAccesoActa } from '../api'

/**
 * Descargar el PDF de un acta (CU08): pide la dirección temporal y firmada y la abre en una pestaña nueva.
 *
 * * La dirección vence en minutos y anota la descarga en la bitácora: se pide en cada clic y no se guarda.
 * * `descargando` es el código de la convocatoria en curso (bloquea su botón); un error deja su mensaje en
 * * `error` y no abre nada.
 */
export function useDescargarActa() {
  const acceso = useAccesoActa()
  const [error, setError] = useState<string | null>(null)

  const descargar = (convocatoria: string) => {
    setError(null)
    acceso.mutate(convocatoria, {
      onSuccess: ({ url }) => {
        window.open(url, '_blank', 'noopener,noreferrer')
      },
      onError: (e) => setError(mapearErrorActas(e).mensaje),
    })
  }

  return {
    descargar,
    descargando: acceso.isPending ? acceso.variables : undefined,
    error,
    limpiarError: () => setError(null),
  }
}
