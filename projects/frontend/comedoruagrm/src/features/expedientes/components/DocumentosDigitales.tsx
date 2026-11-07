import { ExternalLinkIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

import { mapearErrorExpedientes, useAccesoDocumento } from '../api'
import type { DocumentoExpediente } from '../types'

interface DocumentosDigitalesProps {
  /** Código del finalista dueño del expediente. */
  finalista: string
  documentos: DocumentoExpediente[]
}

/**
 * Documentos digitales del expediente (CU07, paso 3): cada tipo requerido figura como «Recibido» (con botón
 * «Abrir», que pide una URL temporal al backend) o como «Falta». La ruta guardada nunca llega acá.
 */
export function DocumentosDigitales({ finalista, documentos }: DocumentosDigitalesProps) {
  const acceso = useAccesoDocumento()
  const abriendo = acceso.isPending ? acceso.variables?.documento : undefined

  const abrir = (documento: string) => {
    acceso.mutate(
      { finalista, documento },
      {
        onSuccess: ({ url }) => {
          window.open(url, '_blank', 'noopener,noreferrer')
        },
      },
    )
  }

  const ultimo = acceso.isError ? acceso.variables?.documento : undefined

  return (
    <Card>
      <CardHeader>
        <CardTitle>Documentos digitales</CardTitle>
      </CardHeader>
      <CardContent>
        {documentos.length === 0 ? (
          <p className="text-sm text-muted-foreground">Esta convocatoria no exige documentos.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-border">
            {documentos.map((doc, indice) => (
              <li key={doc.codigo ?? `${doc.tipo}-${indice}`} className="flex flex-col gap-1 py-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-col">
                    <span className="text-sm">
                      {doc.tipo}
                      {doc.obligatorio ? '' : ' (opcional)'}
                    </span>
                    {doc.nombreArchivo && <span className="text-xs text-muted-foreground">{doc.nombreArchivo}</span>}
                  </div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${doc.estado === 'RECIBIDO' ? 'bg-success/15 text-success' : 'bg-destructive/15 text-destructive'}`}
                    >
                      {doc.estado === 'RECIBIDO' ? 'Recibido' : 'Falta'}
                    </span>
                    {doc.estado === 'RECIBIDO' && doc.codigo && (
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={abriendo === doc.codigo}
                        aria-label={`Abrir ${doc.tipo}`}
                        onClick={() => abrir(doc.codigo as string)}
                      >
                        <ExternalLinkIcon data-icon="inline-start" />
                        {abriendo === doc.codigo ? 'Abriendo…' : 'Abrir'}
                      </Button>
                    )}
                  </div>
                </div>
                {ultimo === doc.codigo && acceso.isError && (
                  <div role="alert" className="flex flex-wrap items-center gap-2 text-xs text-destructive">
                    <span>{mapearErrorExpedientes(acceso.error).mensaje}</span>
                    <Button variant="outline" size="xs" onClick={() => abrir(doc.codigo as string)}>
                      Reintentar
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
