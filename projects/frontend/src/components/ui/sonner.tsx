import type { CSSProperties } from "react"
import { Toaster as Sonner, type ToasterProps } from "sonner"
import { CircleCheckIcon, InfoIcon, TriangleAlertIcon, OctagonXIcon, Loader2Icon } from "lucide-react"

import { useUiStore } from "@/store"

// * Se usa el `theme` de nuestro useUiStore (no `next-themes`, que la CLI de
// * shadcn instala por default) -- el proyecto ya tiene su propio store de
// * tema persistido, duplicarlo con otra librería sería redundante.
const Toaster = ({ ...props }: ToasterProps) => {
  const theme = useUiStore((s) => s.theme)

  return (
    <Sonner
      theme={theme}
      richColors
      className="toaster group"
      icons={{
        success: (
          <CircleCheckIcon className="size-4" />
        ),
        info: (
          <InfoIcon className="size-4" />
        ),
        warning: (
          <TriangleAlertIcon className="size-4" />
        ),
        error: (
          <OctagonXIcon className="size-4" />
        ),
        loading: (
          <Loader2Icon className="size-4 animate-spin" />
        ),
      }}
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
          "--success-bg": "var(--success)",
          "--success-text": "var(--success-foreground)",
          "--success-border": "var(--success)",
          "--error-bg": "var(--destructive)",
          "--error-text": "var(--destructive-foreground)",
          "--error-border": "var(--destructive)",
          // * `header`, no `primary` -- info debe verse igual en claro y
          // * oscuro, y `primary` cambia de tono entre modos (ver la regla
          // * de diseño del header en TAILWIND_STYLES_FRONTEND.md).
          "--info-bg": "var(--header)",
          "--info-text": "var(--header-foreground)",
          "--info-border": "var(--header)",
          "--warning-bg": "var(--accent)",
          "--warning-text": "var(--accent-foreground)",
          "--warning-border": "var(--accent)",
        } as CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
