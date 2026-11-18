import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@tanstack/react-router', () => ({
  Link: ({ to, children, ...props }: { to: string; children?: ReactNode }) => (
    <a href={to} {...props}>
      {children}
    </a>
  ),
}))
vi.mock('@/components/layout', () => ({
  ComedoruLogo: () => <svg />,
  PublicHeader: () => <header />,
}))

import { LandingScreen } from '../screens/LandingScreen'

describe('LandingScreen', () => {
  it('muestra el título y los accesos a iniciar sesión y registro', () => {
    render(<LandingScreen />)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Comedor universitario')
    const login = screen.getAllByText('Iniciar sesión')
    expect(login[0].closest('a')?.getAttribute('href')).toBe('/login')
    expect(screen.getByText('Crear cuenta').closest('a')?.getAttribute('href')).toBe('/registro')
  })

  it('explica los pasos para postular', () => {
    render(<LandingScreen />)
    expect(screen.getByRole('heading', { name: '¿Cómo postular?' })).toBeTruthy()
    expect(screen.getByText('Paso 4')).toBeTruthy()
  })
})
