import { Link } from '@tanstack/react-router'
import {
  BadgeCheckIcon,
  BellRingIcon,
  CalendarCheckIcon,
  ClipboardListIcon,
  FileUpIcon,
  MailIcon,
  ScaleIcon,
  ShieldCheckIcon,
  SparklesIcon,
  UserPlusIcon,
  UtensilsCrossedIcon,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

import { ComedoruLogo, PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { ChipIcono, estiloTarjeta } from '@/features/dashboard'
import type { Tono } from '@/features/dashboard'

interface Elemento {
  icono: LucideIcon
  tono: Tono
  titulo: string
  texto: string
}

// * Contenido descriptivo del proceso, sin cifras ni fechas: lo concreto (cupos, requisitos,
// * fechas) lo publica la DUBSS en cada convocatoria.
const PASOS: Elemento[] = [
  {
    icono: UserPlusIcon,
    tono: 'primary',
    titulo: 'Crea tu cuenta',
    texto: 'Regístrate con tus datos de estudiante de la UAGRM.',
  },
  {
    icono: ClipboardListIcon,
    tono: 'info',
    titulo: 'Postula en la convocatoria',
    texto: 'Cuando haya una convocatoria abierta, completa el formulario socioeconómico.',
  },
  {
    icono: FileUpIcon,
    tono: 'warning',
    titulo: 'Adjunta tus documentos',
    texto: 'Sube los documentos que pide la convocatoria para respaldar tu postulación.',
  },
  {
    icono: BadgeCheckIcon,
    tono: 'success',
    titulo: 'Conoce tu resultado',
    texto: 'Consulta el estado de tu postulación y el resultado publicado desde tu cuenta.',
  },
]

const BENEFICIOS: Elemento[] = [
  {
    icono: SparklesIcon,
    tono: 'accent',
    titulo: 'Preselección con apoyo de IA',
    texto: 'Un puntaje de vulnerabilidad explicable ayuda a ordenar las postulaciones.',
  },
  {
    icono: ShieldCheckIcon,
    tono: 'primary',
    titulo: 'Revisión por la DUBSS',
    texto: 'El personal de la DUBSS audita cada expediente antes de adjudicar.',
  },
  {
    icono: ScaleIcon,
    tono: 'info',
    titulo: 'Proceso transparente',
    texto: 'Cada etapa deja registro, y los resultados se publican oficialmente.',
  },
  {
    icono: UtensilsCrossedIcon,
    tono: 'success',
    titulo: 'Asistencia al comedor',
    texto: 'Si resultas becado, tu asistencia se registra de forma digital.',
  },
]

const ETAPAS = [
  'Convocatoria abierta',
  'Postulación y documentos',
  'Preselección y auditoría',
  'Adjudicación y resultados',
]

function TarjetaElemento({ icono, tono, titulo, texto }: Elemento) {
  return (
    <div
      className={`flex h-full flex-col gap-3 rounded-xl border border-border p-5 ${estiloTarjeta(tono)}`}
    >
      <ChipIcono icono={icono} tono={tono} />
      <h3 className="font-heading text-lg">{titulo}</h3>
      <p className="text-sm text-muted-foreground">{texto}</p>
    </div>
  )
}

export function LandingScreen() {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <PublicHeader appName="ComedorU" />

      <main className="flex-1">
        <section
          aria-labelledby="titulo-landing"
          className="bg-linear-to-br from-primary/15 via-background to-accent/10"
        >
          <div className="mx-auto flex max-w-5xl flex-col items-center gap-6 px-4 py-16 text-center md:py-24">
            <ComedoruLogo className="size-20 text-primary md:size-24" />
            <p className="rounded-full bg-accent/15 px-4 py-1 text-sm font-medium text-accent">
              Beca Comedor · DUBSS · UAGRM
            </p>
            <h1
              id="titulo-landing"
              className="font-heading text-4xl font-bold text-primary md:text-6xl"
            >
              Comedor universitario UAGRM
            </h1>
            <p className="max-w-2xl text-lg text-muted-foreground">
              Postula a la Beca Comedor, sigue el estado de tu postulación y consulta los resultados
              desde un solo lugar.
            </p>
            <div className="flex flex-wrap justify-center gap-3">
              <Button size="lg" nativeButton={false} render={<Link to="/login" />}>
                Iniciar sesión
              </Button>
              <Button
                size="lg"
                variant="outline"
                nativeButton={false}
                render={<Link to="/registro" />}
              >
                Crear cuenta
              </Button>
            </div>
          </div>
        </section>

        <section aria-labelledby="titulo-que-es" className="mx-auto max-w-5xl px-4 py-14">
          <h2
            id="titulo-que-es"
            className="border-l-4 border-l-accent pl-3 font-heading text-2xl md:text-3xl"
          >
            ¿Qué es la Beca Comedor?
          </h2>
          <p className="mt-4 max-w-3xl text-muted-foreground">
            Es el apoyo alimentario que la Dirección Universitaria de Bienestar Social y Salud
            (DUBSS) otorga a estudiantes de la UAGRM que lo necesitan. Esta plataforma digitaliza
            todo el proceso: la postulación, la preselección, la auditoría de expedientes, la
            adjudicación y el control de asistencia.
          </p>
          <ul className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {BENEFICIOS.map((b) => (
              <li key={b.titulo}>
                <TarjetaElemento {...b} />
              </li>
            ))}
          </ul>
        </section>

        <section aria-labelledby="titulo-como" className="bg-muted/40">
          <div className="mx-auto max-w-5xl px-4 py-14">
            <h2
              id="titulo-como"
              className="border-l-4 border-l-primary pl-3 font-heading text-2xl md:text-3xl"
            >
              ¿Cómo postular?
            </h2>
            <ol className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {PASOS.map((p, i) => (
                <li key={p.titulo} className="flex flex-col gap-2">
                  <span className="font-heading text-sm font-semibold text-muted-foreground">
                    Paso {i + 1}
                  </span>
                  <TarjetaElemento {...p} />
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section aria-labelledby="titulo-convocatoria" className="mx-auto max-w-5xl px-4 py-14">
          <div className="flex flex-col gap-6 rounded-2xl border border-border bg-linear-to-br from-info/10 to-card p-6 shadow-md md:p-8">
            <div className="flex items-center gap-3">
              <ChipIcono icono={CalendarCheckIcon} tono="info" />
              <h2 id="titulo-convocatoria" className="font-heading text-2xl">
                Convocatorias
              </h2>
            </div>
            <p className="text-muted-foreground">
              La DUBSS abre una convocatoria por gestión académica. Los requisitos, los documentos y
              las fechas se publican en cada una: inicia sesión para ver la que está vigente.
            </p>
            <ol className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {ETAPAS.map((etapa, i) => (
                <li
                  key={etapa}
                  className="flex items-center gap-3 rounded-lg bg-background/70 px-4 py-3 text-sm"
                >
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
                    {i + 1}
                  </span>
                  {etapa}
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section aria-labelledby="titulo-cta" className="bg-header text-header-foreground">
          <div className="mx-auto flex max-w-5xl flex-col items-center gap-4 px-4 py-12 text-center">
            <BellRingIcon className="size-8" aria-hidden />
            <h2 id="titulo-cta" className="font-heading text-2xl md:text-3xl">
              Empieza tu postulación hoy
            </h2>
            <p className="max-w-xl text-header-foreground/80">
              Ingresa con tu cuenta o crea una nueva para ver la convocatoria vigente.
            </p>
            <Button
              size="lg"
              variant="secondary"
              nativeButton={false}
              render={<Link to="/login" />}
            >
              Iniciar sesión
            </Button>
          </div>
        </section>
      </main>

      <footer className="border-t border-border px-4 py-8">
        <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-3 text-center text-sm text-muted-foreground md:flex-row md:text-left">
          <p>© {new Date().getFullYear()} ComedorU · Universidad Autónoma Gabriel René Moreno</p>
          <p className="flex items-center gap-2">
            <MailIcon className="size-4" aria-hidden />
            Para consultas, comunícate con la DUBSS de la UAGRM.
          </p>
        </div>
      </footer>
    </div>
  )
}
