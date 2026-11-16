import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const estado = vi.hoisted(() => ({ status: 'idle' }))

vi.mock('@tanstack/react-router', () => ({
  useRouterState: ({ select }: { select: (s: { status: string }) => unknown }) => select({ status: estado.status }),
}))

import { NavigationProgress } from './NavigationProgress'

describe('NavigationProgress', () => {
  beforeEach(() => {
    estado.status = 'idle'
  })

  it('no dibuja nada si el router está en reposo', () => {
    render(<NavigationProgress />)
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
  })

  it('muestra la barra mientras la ruta destino está cargando', () => {
    estado.status = 'pending'
    render(<NavigationProgress />)
    expect(screen.getByRole('progressbar', { name: 'Cargando página' })).toBeInTheDocument()
  })
})
