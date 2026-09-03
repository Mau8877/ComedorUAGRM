import { toast } from "sonner"

// * Envuelve `toast.success`/`toast.error` de sonner con nuestros dos únicos
// * tipos de notificación -- el color (verde/rojo) ya sale de los tokens
// * --success-*/--error-* configurados en components/ui/sonner.tsx, así que
// * quien llama a estas funciones no vuelve a decidir estilos, solo el texto.
export function toastSuccess(message: string, description?: string) {
  toast.success(message, { description })
}

export function toastError(message: string, description?: string) {
  toast.error(message, { description })
}
