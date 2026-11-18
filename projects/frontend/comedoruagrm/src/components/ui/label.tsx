"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

// * Asterisco rojo de campo obligatorio. Va como pseudo-elemento (no como texto) para que no entre en el
// * nombre accesible de la etiqueta ni en `getByLabelText`.
export const MARCA_OBLIGATORIO = "after:ml-0.5 after:text-destructive after:content-['*']"

function Label({
  className,
  required,
  ...props
}: React.ComponentProps<"label"> & {
  /** Muestra el asterisco rojo de campo obligatorio después del texto. */
  required?: boolean
}) {
  return (
    <label
      data-slot="label"
      className={cn(
        "flex items-center gap-2 text-sm leading-none font-medium select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
        required && "gap-0 " + MARCA_OBLIGATORIO,
        className
      )}
      {...props}
    />
  )
}

export { Label }
