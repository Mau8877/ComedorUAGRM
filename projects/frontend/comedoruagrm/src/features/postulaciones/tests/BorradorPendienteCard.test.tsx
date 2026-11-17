import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { BorradorPendienteCard } from '../components/BorradorPendienteCard'
import type { BorradorResumen } from '../types'

const borrador = (extra: Partial<BorradorResumen> = {}): BorradorResumen => ({
  postulacion: 'post-1',
  convocatoria: 'Convocatoria 2026-II',
  gestion: '2026-II',
  fechaCierre: '2026-07-31T20:00:00Z',
  guardadoEn: '2026-07-12T18:32:00Z',
  camposPendientes: 3,
  documentosPendientes: 1,
  ...extra,
})

describe('BorradorPendienteCard (CU-17)', () => {
  it('muestra la convocatoria, la última modificación, lo que falta y el aviso del cierre', () => {
    render(<BorradorPendienteCard borrador={borrador()} />)

    expect(screen.getByText('Convocatoria 2026-II')).toBeInTheDocument()
    expect(screen.getByText(/Última modificación: 12\/07\/2026/)).toBeInTheDocument()
    expect(screen.getByText('Faltan 3 campos y 1 documento.')).toBeInTheDocument()
    expect(screen.getByText(/se conserva hasta el cierre de la convocatoria \(31\/07\/2026\)/)).toBeInTheDocument()
    expect(screen.getByText('Continuar').closest('a')).toHaveAttribute('href', '/postular')
  })

  it('sin ningún guardado lo dice, y con todo completo avisa que ya puede enviarse', () => {
    render(
      <BorradorPendienteCard borrador={borrador({ guardadoEn: null, camposPendientes: 0, documentosPendientes: 0 })} />,
    )

    expect(screen.getByText('Todavía no guardaste ningún dato.')).toBeInTheDocument()
    expect(screen.getByText('No falta nada: ya puedes enviarlo.')).toBeInTheDocument()
  })
})
