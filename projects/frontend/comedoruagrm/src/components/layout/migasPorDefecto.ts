import type { MigaPagina } from './EncabezadoPagina'
import type { NavItem } from './types'

interface Entrada {
  title: string
  navItems: NavItem[]
  /** Ítem del menú al que pertenece la página. */
  activeHref?: string
  /** Ruta actual (`location.pathname`). */
  pathname: string
}

/**
 * Las migas de una página, derivadas de su ítem del menú y de la ruta: ninguna pantalla las declara.
 *
 * * La página principal de un ítem (`/finalistas`) lleva solo su título.
 * * Una página que cuelga de un ítem (`/finalistas/{codigo}`, `/postular` dentro de «Mis postulaciones»)
 * * lleva antes el nombre del ítem, enlazado: «Finalistas > Auditar expediente».
 * * Una pantalla solo declara `migas` cuando su camino tiene un tramo que el menú no conoce.
 */
export function migasPorDefecto({ title, navItems, activeHref, pathname }: Entrada): MigaPagina[] {
  const seccion = navItems.find((item) => item.href === activeHref)
  const cuelgaDelItem = seccion !== undefined && pathname !== '' && pathname !== seccion.href
  return cuelgaDelItem ? [{ label: seccion.label, href: seccion.href }, { label: title }] : [{ label: title }]
}
