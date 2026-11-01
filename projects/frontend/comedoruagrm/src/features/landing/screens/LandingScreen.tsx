import { Link } from '@tanstack/react-router'
import { ComedoruLogo, PublicHeader } from '@/components/layout'
import { Button } from '@/components/ui/button'

export function LandingScreen() {
  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <PublicHeader appName="ComedorU" />

      <main className="flex flex-1 items-center justify-center px-4 py-16">
        <section className="mx-auto flex max-w-2xl flex-col items-center gap-6 text-center">
          <ComedoruLogo className="size-24 text-primary" />
          <h1 className="font-heading text-4xl font-bold text-primary md:text-5xl">
            Comedor universitario UAGRM
          </h1>
          <p className="text-lg text-muted-foreground">
            Consultá el menú, gestioná tus reservas y seguí tus pedidos del comedor de la UAGRM en
            un solo lugar.
          </p>
          <Button size="lg" nativeButton={false} render={<Link to="/login" />}>
            Iniciar sesión
          </Button>
        </section>
      </main>

      <footer className="border-t border-border px-4 py-6 text-center text-sm text-muted-foreground">
        © {new Date().getFullYear()} ComedorU · Universidad Autónoma Gabriel René Moreno
      </footer>
    </div>
  )
}
