import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('../../finalistas/tests/clienteSimulado')).cliente,
}))

import { ApiError } from '@/store'

import { cliente } from '../../finalistas/tests/clienteSimulado'
import { DocumentosDigitales, ExpedienteResumen, HistorialAuditorias, PuntajePanel } from '../components'
import { auditoria, documento, expediente } from './fixtures'

function conCliente(ui: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

describe('ExpedienteResumen', () => {
  it('muestra los datos del finalista, la entrega y las respuestas socioeconómicas', () => {
    render(<ExpedienteResumen expediente={expediente()} />)

    expect(screen.getByText('Luis Pérez')).toBeInTheDocument()
    expect(screen.getByText('9001')).toBeInTheDocument()
    expect(screen.getByText('Ingeniería Informática')).toBeInTheDocument()
    expect(screen.getByText('Beca Comedor 2026')).toBeInTheDocument()
    expect(screen.getByText('Recibida el 05/11/2026')).toBeInTheDocument()
    expect(screen.getByText('Montero')).toBeInTheDocument()
  })

  it('sin entrega ni respuestas lo dice', () => {
    const e = expediente({
      respuestaSocioeconomica: null,
      finalista: { codigo: 'f-1', posicion: 2, plazoEntrega: null, entrega: { estado: 'PENDIENTE', fecha: null } },
    })
    render(<ExpedienteResumen expediente={e} />)

    expect(screen.getByText('Pendiente')).toBeInTheDocument()
    expect(screen.getByText('El postulante no registró respuestas socioeconómicas.')).toBeInTheDocument()
  })
})

describe('PuntajePanel', () => {
  it('muestra IA, académico y total', () => {
    render(<PuntajePanel puntajeIa={55.5} puntajeAcademico={25} puntajeTotal={80.5} />)

    expect(screen.getByText('55.50')).toBeInTheDocument()
    expect(screen.getByText('25.00')).toBeInTheDocument()
    expect(screen.getByText('80.50')).toBeInTheDocument()
  })

  it('sin total explica cuándo aparece', () => {
    render(<PuntajePanel puntajeIa={55.5} puntajeAcademico={null} puntajeTotal={null} />)

    expect(screen.getByText(/El total aparece cuando/)).toBeInTheDocument()
  })
})

describe('HistorialAuditorias', () => {
  it('sin auditorías muestra el texto vacío', () => {
    render(<HistorialAuditorias auditorias={[]} />)

    expect(screen.getByText('Sin auditorías todavía.')).toBeInTheDocument()
  })

  it('lista cada auditoría con resultado, auditor y observaciones', () => {
    render(
      <HistorialAuditorias
        auditorias={[
          auditoria({ codigo: 'a-2', resultado: 'VALIDADO' }),
          auditoria({ codigo: 'a-1', resultado: 'OBSERVADO', observaciones: 'Falta el certificado', puntajeAcademico: null }),
        ]}
      />,
    )

    expect(screen.getByText('Validado')).toBeInTheDocument()
    expect(screen.getByText('Observado')).toBeInTheDocument()
    expect(screen.getByText('Falta el certificado')).toBeInTheDocument()
    expect(screen.getAllByText(/Ana Auditora/)).toHaveLength(2)
  })
})

describe('DocumentosDigitales', () => {
  beforeEach(() => cliente.get.mockReset())
  afterEach(() => vi.restoreAllMocks())

  const documentos = [documento(), documento({ codigo: null, tipo: 'Certificado de notas', estado: 'FALTA', nombreArchivo: null })]

  it('marca como «Falta» el documento sin adjunto y solo ofrece «Abrir» en el recibido', () => {
    conCliente(<DocumentosDigitales finalista="f-1" documentos={documentos} />)

    expect(screen.getByText('Recibido')).toBeInTheDocument()
    expect(screen.getByText('Falta')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /Abrir/ })).toHaveLength(1)
  })

  it('«Abrir» pide la URL temporal y la abre en otra pestaña sin opener', async () => {
    cliente.get.mockResolvedValue({ data: { url: 'http://minio/firmada', expiraEn: 't' } })
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    conCliente(<DocumentosDigitales finalista="f-1" documentos={documentos} />)

    await userEvent.click(screen.getByRole('button', { name: 'Abrir Carnet de identidad' }))

    await waitFor(() => expect(abrir).toHaveBeenCalledWith('http://minio/firmada', '_blank', 'noopener,noreferrer'))
    expect(cliente.get).toHaveBeenCalledWith('/finalistas/f-1/documentos/d-1/acceso')
  })

  it('si falla muestra el error con «Reintentar»', async () => {
    cliente.get.mockRejectedValueOnce(new ApiError('ERR_EXP_05', 'x'))
    cliente.get.mockResolvedValueOnce({ data: { url: 'http://minio/firmada', expiraEn: 't' } })
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null)
    conCliente(<DocumentosDigitales finalista="f-1" documentos={documentos} />)

    await userEvent.click(screen.getByRole('button', { name: 'Abrir Carnet de identidad' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El documento ya no está disponible')

    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(abrir).toHaveBeenCalledTimes(1))
  })
})
