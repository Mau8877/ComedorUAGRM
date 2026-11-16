import { CargandoContenido } from './CargandoContenido'

// * `pendingComponent` global del router: se dibuja en el lugar del <Outlet/> de la ruta que carga, o sea en
// * el contenedor central, así que header y sidebar no se tocan.
export function CargandoPagina() {
  return <CargandoContenido etiqueta="Cargando página…" forma="detalle" />
}
