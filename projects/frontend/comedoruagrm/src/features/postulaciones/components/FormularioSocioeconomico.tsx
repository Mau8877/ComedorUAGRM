import { useState } from 'react'
import { useForm } from '@tanstack/react-form'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'

import { aBorrador, GRUPO_FAMILIAR_MAXIMO, respuestasSocioeconomicasSchema } from '../schemas'
import type { RespuestasSocioeconomicasValues } from '../schemas'
import type { GuardarBorradorRequest, OpcionesFormulario } from '../types'

import { formatearHora } from './formatos'
import { miembroVacio } from './valoresIniciales'

const ETIQUETAS: Record<keyof RespuestasSocioeconomicasValues, string> = {
  ingresosFamiliares: 'ingresos familiares',
  dependientes: 'dependientes',
  procedencia: 'procedencia',
  patrimonio: 'patrimonio',
  estadoVulnerabilidad: 'estado de vulnerabilidad',
  promedioAcademico: 'promedio académico',
  complementarios: 'datos del formulario oficial',
}

type CampoTexto =
  | 'ingresosFamiliares'
  | 'dependientes'
  | 'promedioAcademico'
  | 'complementarios.provincia'
  | 'complementarios.zonaAnillo'
  | 'complementarios.barrio'
  | 'complementarios.calle'
  | 'complementarios.dormitorios'
  | 'complementarios.banos'
  | 'complementarios.comedor'
  | 'complementarios.sala'
  | 'complementarios.patio'
  | 'complementarios.otroBeneficioDetalle'
  | 'complementarios.discapacidadDetalle'

type CampoLista =
  | 'procedencia'
  | 'patrimonio'
  | 'estadoVulnerabilidad'
  | 'complementarios.dependenciaEconomica'
  | 'complementarios.ocupacionSosten'
  | 'complementarios.rangoIngresos'
  | 'complementarios.cantidadHijos'
  | 'complementarios.tenenciaVivienda'

type CampoSiNo = 'complementarios.tieneHijos' | 'complementarios.otroBeneficio' | 'complementarios.discapacidad'

const SI_NO = [
  { valor: 'si', etiqueta: 'Sí' },
  { valor: 'no', etiqueta: 'No' },
]

export interface FormularioSocioeconomicoProps {
  valores: RespuestasSocioeconomicasValues
  opciones: OpcionesFormulario
  /** Se llama con los valores (texto) cuando todos los campos son válidos. */
  onContinuar: (valores: RespuestasSocioeconomicasValues) => void
  onVolver: () => void
  /**
   * «Guardar borrador» (CU-17): se llama con lo que hay escrito (los campos vacíos van en `null`) y con el texto
   * del formulario. Si no se pasa, el botón no se muestra.
   */
  onGuardarBorrador?: (borrador: GuardarBorradorRequest, valores: RespuestasSocioeconomicasValues) => void
  guardando?: boolean
  /** Instante del último guardado, para el aviso «Borrador guardado · 14:32». */
  guardadoEn?: string | null
}

function Error({ campo }: { campo: { state: { meta: { isTouched: boolean; errors: Array<{ message: string } | undefined> } } } }) {
  const mensaje = campo.state.meta.isTouched ? campo.state.meta.errors[0]?.message : undefined
  return mensaje ? <span className="text-xs text-destructive">{mensaje}</span> : null
}

function Seccion({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-4">
      <legend className="px-1 font-heading text-base text-foreground">{titulo}</legend>
      {children}
    </fieldset>
  )
}

/**
 * El paso «Datos socioeconómicos» del CU-02 (paso 3 del flujo): los datos del formulario oficial de la DUBSS
 * (FORM-DUBS-001): ingresos, dependencia económica, grupo familiar, residencia, vivienda y otros datos. Los marcados
 * con * son obligatorios; el error se ve inline cuando el campo ya se tocó, y «Continuar» marca todos los
 * pendientes de una vez (A3).
 */
export function FormularioSocioeconomico({
  valores,
  opciones,
  onContinuar,
  onVolver,
  onGuardarBorrador,
  guardando = false,
  guardadoEn,
}: FormularioSocioeconomicoProps) {
  const [erroresBorrador, setErroresBorrador] = useState<string[]>([])
  const form = useForm({
    defaultValues: valores,
    validators: { onChange: respuestasSocioeconomicasSchema, onBlur: respuestasSocioeconomicasSchema },
    onSubmit: ({ value }) => onContinuar(value),
  })

  // A diferencia de «Continuar», guardar un borrador no exige los campos vacíos: solo rechaza un valor mal escrito.
  const guardarBorrador = () => {
    if (!onGuardarBorrador) return
    const valoresActuales = form.state.values
    const resultado = aBorrador(valoresActuales)
    if (!resultado.ok) {
      setErroresBorrador(
        Object.entries(resultado.errores).map(
          ([campo, mensaje]) => `${ETIQUETAS[campo as keyof typeof ETIQUETAS]}: ${mensaje}`,
        ),
      )
      return
    }
    setErroresBorrador([])
    onGuardarBorrador(resultado.borrador, valoresActuales)
  }

  const texto = (
    campo: CampoTexto,
    etiqueta: string,
    { requerido = false, inputMode, placeholder }: { requerido?: boolean; inputMode?: 'decimal' | 'numeric'; placeholder?: string } = {},
  ) => (
    <form.Field name={campo}>
      {(field) => (
        <div className="flex flex-col gap-1">
          <label htmlFor={field.name} className="text-sm font-medium text-foreground">
            {etiqueta}
            {requerido ? ' *' : ''}
          </label>
          <Input
            id={field.name}
            name={field.name}
            inputMode={inputMode}
            placeholder={placeholder}
            value={field.state.value}
            aria-invalid={field.state.meta.isTouched && field.state.meta.errors.length > 0 ? true : undefined}
            onBlur={field.handleBlur}
            onChange={(e) => field.handleChange(e.target.value)}
          />
          <Error campo={field} />
        </div>
      )}
    </form.Field>
  )

  const lista = (campo: CampoLista, etiqueta: string, items: string[], requerido = true) => (
    <form.Field name={campo}>
      {(field) => (
        <div className="flex flex-col gap-1">
          <span id={`postular-${campo}-label`} className="text-sm font-medium text-foreground">
            {etiqueta}
            {requerido ? ' *' : ''}
          </span>
          <Select value={field.state.value} onValueChange={(v) => field.handleChange(v ?? '')}>
            <SelectTrigger className="w-full" aria-labelledby={`postular-${campo}-label`}>
              <SelectValue>{(v: string | null) => v || 'Selecciona una opción'}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {items.map((item) => (
                <SelectItem key={item} value={item}>
                  {item}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Error campo={field} />
        </div>
      )}
    </form.Field>
  )

  const siNo = (campo: CampoSiNo, etiqueta: string) => (
    <form.Field name={campo}>
      {(field) => (
        <div className="flex flex-col gap-1">
          <span id={`postular-${campo}-label`} className="text-sm font-medium text-foreground">
            {etiqueta} *
          </span>
          <Select value={field.state.value} onValueChange={(v) => field.handleChange(v ?? '')}>
            <SelectTrigger className="w-full" aria-labelledby={`postular-${campo}-label`}>
              <SelectValue>
                {(v: string | null) => SI_NO.find((o) => o.valor === v)?.etiqueta ?? 'Selecciona una opción'}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {SI_NO.map((o) => (
                <SelectItem key={o.valor} value={o.valor}>
                  {o.etiqueta}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Error campo={field} />
        </div>
      )}
    </form.Field>
  )

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        void form.handleSubmit()
      }}
    >
      {guardadoEn && (
        <p role="status" className="w-fit rounded-full border border-success/30 bg-success/10 px-3 py-1 text-sm text-success">
          ✓ Borrador guardado · {formatearHora(guardadoEn)}
        </p>
      )}

      {erroresBorrador.length > 0 && (
        <div role="alert" className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
          <p>No se guardó el borrador. Corrige estos datos:</p>
          <ul className="list-disc pl-5">
            {erroresBorrador.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </div>
      )}

      <form.Subscribe selector={(s) => s.submissionAttempts > 0 && !s.isValid}>
        {(hayErrores) =>
          hayErrores && (
            <p role="alert" className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
              Revisa los campos marcados: faltan datos obligatorios o alguno no es válido.
            </p>
          )
        }
      </form.Subscribe>

      <Seccion titulo="Ingresos y datos académicos">
        {texto('ingresosFamiliares', 'Ingresos familiares mensuales (Bs)', { requerido: true, inputMode: 'decimal' })}
        {lista('complementarios.rangoIngresos', 'Rango de ingresos', opciones.rangosIngresos)}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {texto('dependientes', 'N.º de dependientes', { requerido: true, inputMode: 'numeric' })}
          {lista('procedencia', 'Procedencia', opciones.procedencias)}
        </div>
        {texto('promedioAcademico', 'Promedio académico', { requerido: true, inputMode: 'decimal', placeholder: 'Ej. 72' })}
      </Seccion>

      <Seccion titulo="Dependencia económica">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {lista('complementarios.dependenciaEconomica', '¿De quién depende usted?', opciones.dependencias)}
          {lista('complementarios.ocupacionSosten', 'Ocupación de quien lo sostiene', opciones.ocupacionesSosten)}
        </div>
      </Seccion>

      <Seccion titulo="Grupo familiar">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {siNo('complementarios.tieneHijos', '¿Usted tiene hijos?')}
          <form.Subscribe selector={(s) => s.values.complementarios.tieneHijos === 'si'}>
            {(tieneHijos) => tieneHijos && lista('complementarios.cantidadHijos', '¿Cuántos?', opciones.cantidadesHijos)}
          </form.Subscribe>
        </div>

        <form.Field name="complementarios.grupoFamiliar" mode="array">
          {(grupo) => (
            <div className="flex flex-col gap-3">
              <p className="text-sm font-medium text-foreground">Integrantes del grupo familiar (opcional)</p>
              {grupo.state.value.map((_, i) => (
                <div
                  key={i}
                  role="group"
                  aria-label={`Familiar ${i + 1}`}
                  className="grid grid-cols-1 gap-3 rounded-xl border border-border p-3 sm:grid-cols-2"
                >
                  <form.Field name={`complementarios.grupoFamiliar[${i}].nombre`}>
                    {(f) => (
                      <div className="flex flex-col gap-1">
                        <label htmlFor={f.name} className="text-sm text-foreground">Nombre completo</label>
                        <Input id={f.name} value={f.state.value} onBlur={f.handleBlur} onChange={(e) => f.handleChange(e.target.value)} />
                        <Error campo={f} />
                      </div>
                    )}
                  </form.Field>
                  <form.Field name={`complementarios.grupoFamiliar[${i}].parentesco`}>
                    {(f) => (
                      <div className="flex flex-col gap-1">
                        <label htmlFor={f.name} className="text-sm text-foreground">Parentesco</label>
                        <Input id={f.name} value={f.state.value} onBlur={f.handleBlur} onChange={(e) => f.handleChange(e.target.value)} />
                        <Error campo={f} />
                      </div>
                    )}
                  </form.Field>
                  <form.Field name={`complementarios.grupoFamiliar[${i}].edad`}>
                    {(f) => (
                      <div className="flex flex-col gap-1">
                        <label htmlFor={f.name} className="text-sm text-foreground">Edad</label>
                        <Input id={f.name} inputMode="numeric" value={f.state.value} onBlur={f.handleBlur} onChange={(e) => f.handleChange(e.target.value)} />
                        <Error campo={f} />
                      </div>
                    )}
                  </form.Field>
                  <form.Field name={`complementarios.grupoFamiliar[${i}].ocupacion`}>
                    {(f) => (
                      <div className="flex flex-col gap-1">
                        <label htmlFor={f.name} className="text-sm text-foreground">Ocupación</label>
                        <Input id={f.name} value={f.state.value} onBlur={f.handleBlur} onChange={(e) => f.handleChange(e.target.value)} />
                        <Error campo={f} />
                      </div>
                    )}
                  </form.Field>
                  <form.Field name={`complementarios.grupoFamiliar[${i}].observacion`}>
                    {(f) => (
                      <div className="flex flex-col gap-1 sm:col-span-2">
                        <label htmlFor={f.name} className="text-sm text-foreground">Observación</label>
                        <Input id={f.name} value={f.state.value} onBlur={f.handleBlur} onChange={(e) => f.handleChange(e.target.value)} />
                        <Error campo={f} />
                      </div>
                    )}
                  </form.Field>
                  <div className="sm:col-span-2">
                    <Button type="button" variant="outline" size="sm" onClick={() => void grupo.removeValue(i)}>
                      Quitar familiar {i + 1}
                    </Button>
                  </div>
                </div>
              ))}
              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={grupo.state.value.length >= GRUPO_FAMILIAR_MAXIMO}
                  onClick={() => void grupo.pushValue(miembroVacio())}
                >
                  Agregar familiar
                </Button>
                <span className="text-xs text-muted-foreground">
                  {grupo.state.value.length} de {GRUPO_FAMILIAR_MAXIMO}
                </span>
              </div>
              <Error campo={grupo} />
            </div>
          )}
        </form.Field>
      </Seccion>

      <Seccion titulo="Residencia y vivienda">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {texto('complementarios.provincia', 'Provincia')}
          {texto('complementarios.zonaAnillo', 'Zona o anillo')}
          {texto('complementarios.barrio', 'Barrio')}
          {texto('complementarios.calle', 'Calle')}
        </div>
        {lista('patrimonio', 'Patrimonio', opciones.patrimonios)}
        {lista('complementarios.tenenciaVivienda', 'Tenencia de la vivienda', opciones.tenenciasVivienda)}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
          {texto('complementarios.dormitorios', 'Dormitorios', { inputMode: 'numeric' })}
          {texto('complementarios.banos', 'Baños', { inputMode: 'numeric' })}
          {texto('complementarios.comedor', 'Comedor', { inputMode: 'numeric' })}
          {texto('complementarios.sala', 'Sala', { inputMode: 'numeric' })}
          {texto('complementarios.patio', 'Patio', { inputMode: 'numeric' })}
        </div>
      </Seccion>

      <Seccion titulo="Otros datos">
        {lista('estadoVulnerabilidad', 'Estado de vulnerabilidad', opciones.estadosVulnerabilidad)}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {siNo('complementarios.otroBeneficio', '¿Tiene otro beneficio dentro de la universidad?')}
          <form.Subscribe selector={(s) => s.values.complementarios.otroBeneficio === 'si'}>
            {(si) => si && texto('complementarios.otroBeneficioDetalle', '¿Cuál?')}
          </form.Subscribe>
          {siNo('complementarios.discapacidad', '¿Tiene algún tipo de discapacidad?')}
          <form.Subscribe selector={(s) => s.values.complementarios.discapacidad === 'si'}>
            {(si) => si && texto('complementarios.discapacidadDetalle', 'Tipo y grado')}
          </form.Subscribe>
        </div>
        <form.Field name="complementarios.comentario">
          {(field) => (
            <div className="flex flex-col gap-1">
              <label htmlFor={field.name} className="text-sm font-medium text-foreground">
                Aclaración o comentario personal
              </label>
              <Textarea
                id={field.name}
                name={field.name}
                rows={3}
                value={field.state.value}
                aria-invalid={field.state.meta.isTouched && field.state.meta.errors.length > 0 ? true : undefined}
                onBlur={field.handleBlur}
                onChange={(e) => field.handleChange(e.target.value)}
              />
              <Error campo={field} />
            </div>
          )}
        </form.Field>
      </Seccion>

      <div className="flex flex-wrap justify-between gap-2">
        <Button type="button" variant="outline" onClick={onVolver}>
          Atrás
        </Button>
        <div className="flex flex-wrap gap-2">
          {onGuardarBorrador && (
            <Button type="button" variant="outline" onClick={guardarBorrador} disabled={guardando}>
              {guardando ? 'Guardando…' : 'Guardar borrador'}
            </Button>
          )}
          <Button type="submit">Continuar</Button>
        </div>
      </div>
      {onGuardarBorrador && (
        <p className="text-xs text-muted-foreground">
          Puedes salir y continuar después; los campos pendientes no bloquean el borrador.
        </p>
      )}
    </form>
  )
}
