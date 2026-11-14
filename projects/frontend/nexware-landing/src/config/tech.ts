import { siFastapi, siFlutter, siReact, siSpringboot, siTypescript } from 'simple-icons'

// Stack fijo del equipo de Nexware. Los logos son los oficiales de cada
// marca (paquete simple-icons: path SVG y color de marca); son nombres
// propios, así que no se traducen y viven acá y no en el i18n.
export const TECH = {
  springboot: { label: 'Spring Boot', icon: siSpringboot },
  react: { label: 'React', icon: siReact },
  typescript: { label: 'TypeScript', icon: siTypescript },
  flutter: { label: 'Flutter', icon: siFlutter },
  fastapi: { label: 'FastAPI', icon: siFastapi },
} as const

export type TechId = keyof typeof TECH
