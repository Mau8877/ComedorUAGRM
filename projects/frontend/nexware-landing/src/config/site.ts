// Datos del sitio que usan varias features. Se cambian acá, una sola vez.
export const SITE = {
  name: 'Nexware',
  // Correo y dominio de ejemplo: el TLD .example está reservado y no existe.
  email: 'ceo.desarrollo@nexware.example',
  // El demo de ComedorUAGRM es el panel, que se sirve en la raíz detrás del
  // proxy (ver infrastructure/nginx/nginx.conf).
  demoUrl: '/',
  address: { locality: 'Santa Cruz de la Sierra', country: 'BO' },
} as const
