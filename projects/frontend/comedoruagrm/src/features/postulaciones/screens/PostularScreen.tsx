import { useEffect, useRef, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'

import {
  AlertDialog,
  AlertDialogClose,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { etiquetaRol, useCerrarSesion } from '@/features/auth'
import type { Usuario } from '@/features/auth'
import { useAccionesDeCuenta } from '@/features/perfil'
import { EstudianteLayout } from '@/layouts'

import { mapearErrorEnvio, useEnviarPostulacion, useGuardarBorrador, useIniciarPostulacion } from '../api'
import type { ErrorEnvio } from '../api'
import { ComprobanteEnvio } from '../components/ComprobanteEnvio'
import { DocumentosPostulacionPanel } from '../components/DocumentosPostulacionPanel'
import { FormularioSocioeconomico } from '../components/FormularioSocioeconomico'
import { formatearFecha, formatearHora } from '../components/formatos'
import { valoresIniciales } from '../components/valoresIniciales'
import { aBorrador, aRequest } from '../schemas'
import type { RespuestasSocioeconomicasValues } from '../schemas'
import type { ComprobantePostulacion, FormularioPostulacion, GuardarBorradorRequest } from '../types'

export interface PostularScreenProps {
  titulo: string
  /** Ítem del sidebar que se ve activo. */
  activeHref: string
  /** El usuario conectado (el Postulante). */
  usuarioActual: Usuario
}

const PASOS = ['Datos del postulante', 'Datos socioeconómicos', 'Documentos y envío']

/**
 * «Nueva postulación» (CU-02): paso 1 datos del perfil, paso 2 formulario socioeconómico, paso 3 documentos (CU-16)
 * y envío con confirmación. Al abrirse crea (o recupera) la postulación en edición; una devuelta se abre con sus
 * respuestas para corregirla y reenviarla (A5). Muestra el comprobante al terminar.
 */
export function PostularScreen({ titulo, activeHref, usuarioActual }: PostularScreenProps) {
  const cerrarSesion = useCerrarSesion()
  const accionesDeCuenta = useAccionesDeCuenta()

  return (
    <EstudianteLayout
      title={titulo}
      activeHref={activeHref}
      user={{ nombre: `${usuarioActual.nombres} ${usuarioActual.apellidos}`, rol: etiquetaRol(usuarioActual.rol) }}
      onCerrarSesion={() => cerrarSesion.mutate()}
      {...accionesDeCuenta}
    >
      <PostularContenido />
    </EstudianteLayout>
  )
}

function PostularContenido() {
  const navigate = useNavigate()
  const iniciar = useIniciarPostulacion()
  const [formulario, setFormulario] = useState<FormularioPostulacion | null>(null)
  const [error, setError] = useState<ErrorEnvio | null>(null)
  // `mutate` en un efecto corre dos veces en StrictMode; la guarda evita el segundo POST (es idempotente igual).
  const pedido = useRef(false)

  const abrir = () => {
    setError(null)
    iniciar.mutate(undefined, {
      onSuccess: setFormulario,
      onError: (e) => setError(mapearErrorEnvio(e)),
    })
  }

  useEffect(() => {
    if (pedido.current) return
    pedido.current = true
    abrir()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo al montar
  }, [])

  if (error) {
    return (
      <div className="flex flex-col items-start gap-3">
        <div
          role="alert"
          className={`w-full rounded-md border p-3 text-sm ${
            error.sinConvocatoria
              ? 'border-info/30 bg-info/10 text-foreground'
              : 'border-destructive/30 bg-destructive/10 text-destructive'
          }`}
        >
          {error.mensaje}
        </div>
        <div className="flex gap-2">
          {error.red && <Button onClick={() => { pedido.current = true; abrir() }}>Reintentar</Button>}
          <Button variant="outline" onClick={() => void navigate({ to: '/mis-postulaciones' })}>
            Ir a mis postulaciones
          </Button>
        </div>
      </div>
    )
  }

  if (!formulario) {
    return <p className="py-8 text-center text-sm text-muted-foreground">Preparando tu postulación…</p>
  }

  return <AsistentePostulacion formulario={formulario} />
}

function AsistentePostulacion({ formulario }: { formulario: FormularioPostulacion }) {
  const navigate = useNavigate()
  const enviar = useEnviarPostulacion(formulario.postulacion)
  const guardarBorrador = useGuardarBorrador(formulario.postulacion)
  // Cuándo se guardó el borrador por última vez (CU-17): lo que ya había al abrir y, después, cada guardado.
  const [guardadoEn, setGuardadoEn] = useState<string | null>(formulario.respuesta?.guardadoEn ?? null)
  const [paso, setPaso] = useState(0)
  const [valores, setValores] = useState<RespuestasSocioeconomicasValues>(() => valoresIniciales(formulario.respuesta))
  const [confirmando, setConfirmando] = useState(false)
  const [error, setError] = useState<ErrorEnvio | null>(null)
  const [comprobante, setComprobante] = useState<ComprobantePostulacion | null>(null)
  // Evita el doble envío aunque el segundo clic llegue antes de que React pinte el estado «enviando» (A6).
  const enviando = useRef(false)

  if (comprobante) {
    return (
      <ComprobanteEnvio
        comprobante={comprobante}
        onVerPostulacion={() =>
          void navigate({ to: '/mis-postulaciones/$codigo', params: { codigo: comprobante.codigo } })
        }
      />
    )
  }

  const confirmarEnvio = () => {
    if (enviando.current) return
    const cuerpo = aRequest(valores)
    if (!cuerpo) {
      setConfirmando(false)
      setPaso(1)
      return
    }
    enviando.current = true
    setError(null)
    enviar.mutate(cuerpo, {
      onSuccess: setComprobante,
      onError: (e) => {
        enviando.current = false
        setConfirmando(false)
        setError(mapearErrorEnvio(e))
      },
    })
  }

  const guardar = (borrador: GuardarBorradorRequest, textos: RespuestasSocioeconomicasValues) => {
    setError(null)
    guardarBorrador.mutate(borrador, {
      onSuccess: (r) => {
        setValores(textos)
        setGuardadoEn(r.guardadoEn)
      },
      onError: (e) => setError(mapearErrorEnvio(e)),
    })
  }

  // En el paso 3 los valores ya pasaron la validación completa al «Continuar» del paso 2.
  const guardarDesdeDocumentos = () => {
    const resultado = aBorrador(valores)
    if (resultado.ok) guardar(resultado.borrador, valores)
  }

  const esReenvio = formulario.estado === 'devuelta'

  return (
    <div className="flex flex-col gap-4">
      <nav aria-label="Pasos" className="flex flex-col gap-2">
        <div className="flex gap-2">
          {PASOS.map((nombre, i) => (
            <div key={nombre} className={`h-1.5 flex-1 rounded-full ${i <= paso ? 'bg-primary' : 'bg-muted'}`} />
          ))}
        </div>
        <p className="text-sm text-muted-foreground">
          Paso {paso + 1} de {PASOS.length} · <strong className="text-foreground">{PASOS[paso]}</strong>
        </p>
      </nav>

      {esReenvio && (
        <p role="status" className="rounded-md border border-warning/30 bg-warning/10 p-3 text-sm">
          Esta postulación fue devuelta para corregirla. Revisa tus datos y documentos, y vuelve a enviarla.
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          {error.mensaje}
        </p>
      )}

      {paso === 0 && (
        <section aria-label="Datos del postulante" className="flex flex-col gap-4">
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 rounded-2xl border border-border bg-card p-4 text-sm">
            <dt className="text-muted-foreground">Nombre</dt>
            <dd>
              {formulario.perfil.nombres} {formulario.perfil.apellidos}
            </dd>
            <dt className="text-muted-foreground">CI</dt>
            <dd>{formulario.perfil.ci}</dd>
            <dt className="text-muted-foreground">Carrera</dt>
            <dd>{formulario.perfil.carrera}</dd>
            <dt className="text-muted-foreground">Convocatoria</dt>
            <dd>
              {formulario.convocatoria.nombre} · gestión {formulario.convocatoria.gestion}
            </dd>
            <dt className="text-muted-foreground">Cierra el</dt>
            <dd>{formatearFecha(formulario.convocatoria.fechaCierre)}</dd>
          </dl>
          <p className="text-xs text-muted-foreground">
            Estos datos salen de tu perfil. Si alguno está mal, corrígelo desde tu perfil antes de enviar.
          </p>
          <div className="flex justify-end">
            <Button onClick={() => setPaso(1)}>Continuar</Button>
          </div>
        </section>
      )}

      {paso === 1 && (
        <FormularioSocioeconomico
          valores={valores}
          opciones={formulario.opciones}
          onVolver={() => setPaso(0)}
          onGuardarBorrador={guardar}
          guardando={guardarBorrador.isPending}
          guardadoEn={guardadoEn}
          onContinuar={(v) => {
            setValores(v)
            setError(null)
            setPaso(2)
          }}
        />
      )}

      {paso === 2 && (
        <section aria-label="Documentos y envío" className="flex flex-col gap-4">
          {guardadoEn && (
            <p role="status" className="w-fit rounded-full border border-success/30 bg-success/10 px-3 py-1 text-sm text-success">
              ✓ Borrador guardado · {formatearHora(guardadoEn)}
            </p>
          )}
          <DocumentosPostulacionPanel codigo={formulario.postulacion} />
          <div className="flex flex-wrap justify-between gap-2">
            <Button variant="outline" onClick={() => setPaso(1)}>
              Atrás
            </Button>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" onClick={guardarDesdeDocumentos} disabled={guardarBorrador.isPending}>
                {guardarBorrador.isPending ? 'Guardando…' : 'Guardar borrador'}
              </Button>
              <Button onClick={() => setConfirmando(true)} disabled={enviar.isPending}>
                Enviar postulación
              </Button>
            </div>
          </div>
        </section>
      )}

      <AlertDialog open={confirmando} onOpenChange={(abierto) => !enviar.isPending && setConfirmando(abierto)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Enviar tu postulación?</AlertDialogTitle>
            <AlertDialogDescription>
              Una vez enviada no podrás editarla, salvo que la DUBSS la devuelva para corrección. Su estado será
              «pendiente».
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogClose disabled={enviar.isPending}>Cancelar</AlertDialogClose>
            <Button onClick={confirmarEnvio} disabled={enviar.isPending}>
              {enviar.isPending ? 'Enviando…' : 'Confirmar envío'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
