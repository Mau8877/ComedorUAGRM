import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/store', async (original) => ({
  ...(await original<object>()),
  apiClient: (await import('./clienteSimulado')).cliente,
}))
vi.mock('@/features/auth', async () => {
  const sim = await import('./clienteSimulado')
  return { etiquetaRol: sim.etiquetaRolSimulada, useCerrarSesion: () => ({ mutate: sim.cerrarSesion }) }
})
vi.mock('@/features/perfil', () => ({ useAccionesDeCuenta: () => ({}) }))
vi.mock('@/layouts', () => ({
  EstudianteLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))
vi.mock('@tanstack/react-router', async (original) => {
  const sim = await import('./clienteSimulado')
  return { ...(await original<object>()), useNavigate: () => sim.navegar }
})
// Los enlaces que dependen del CU-02: cada test decide si esa pantalla «existe».
const enlaces = vi.hoisted(() => ({ corregir: null as string | null, postular: null as string | null }))
vi.mock('../components/enlacesCu02', () => ({
  get RUTA_CORREGIR() {
    return enlaces.corregir
  },
  get RUTA_POSTULAR() {
    return enlaces.postular
  },
}))

import { ApiError } from '@/store'

import { DetallePostulacionScreen } from '../screens/DetallePostulacionScreen'
import type { MiPostulacionDetalle } from '../types'

import { cliente, navegar } from './clienteSimulado'
import { detalle, yo } from './datos'

function responder(d: MiPostulacionDetalle) {
  cliente.get.mockResolvedValue({ data: d })
}

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <DetallePostulacionScreen
        titulo="Detalle de postulación"
        activeHref="/mis-postulaciones"
        usuarioActual={yo}
        codigo="post-1"
      />
    </QueryClientProvider>,
  )
  return { user: userEvent.setup() }
}

describe('DetallePostulacionScreen', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    navegar.mockReset()
    enlaces.corregir = null
    enlaces.postular = null
  })

  it('pide la postulación del código de la ruta y muestra convocatoria, gestión, fecha y estado', async () => {
    responder(detalle({ estado: 'calificada' }))
    montar()

    expect(await screen.findByRole('heading', { name: 'Beca 1' })).toBeInTheDocument()
    expect(cliente.get).toHaveBeenCalledWith('/postulaciones/post-1')
    expect(screen.getByText(/Gestión 2026-1/)).toHaveTextContent('Enviada el 13/11/2026')
    expect(screen.getByText('Calificada')).toBeInTheDocument()
  })

  it('«Volver a mis postulaciones» lleva a la lista', async () => {
    responder(detalle())
    const { user } = montar()
    await screen.findByRole('heading', { name: 'Beca 1' })

    await user.click(screen.getByRole('button', { name: 'Volver a mis postulaciones' }))

    expect(navegar).toHaveBeenCalledWith({ to: '/mis-postulaciones' })
  })

  it('la línea de tiempo muestra los pasos con su fecha y hora, y los pendientes atenuados', async () => {
    responder(
      detalle({
        estado: 'calificada',
        lineaDeTiempo: [
          { estado: 'pendiente', fecha: '2026-11-13T15:30:00Z', situacion: 'COMPLETADO' },
          { estado: 'calificada', fecha: '2026-11-14T12:00:00Z', situacion: 'COMPLETADO' },
          { estado: 'resultado', fecha: null, situacion: 'PENDIENTE' },
        ],
      }),
    )
    montar()

    const linea = await screen.findByRole('list', { name: 'Línea de tiempo' })
    const pasos = within(linea).getAllByRole('listitem')
    expect(pasos).toHaveLength(3)
    expect(pasos[0]).toHaveTextContent('Postulación enviada')
    expect(pasos[0]).toHaveTextContent('13/11/2026, 11:30')
    expect(pasos[0]).toHaveAttribute('data-situacion', 'COMPLETADO')
    expect(pasos[1]).toHaveTextContent('Calificada')
    expect(pasos[1]).toHaveTextContent('14/11/2026, 08:00')
    expect(pasos[2]).toHaveTextContent('Resultado de la adjudicación')
    expect(pasos[2]).toHaveTextContent('Pendiente')
    expect(pasos[2]).toHaveAttribute('data-situacion', 'PENDIENTE')
    expect(pasos[2]).toHaveClass('opacity-60')
  })

  it('el reenvío pendiente de una devuelta se llama distinto al envío ya completado', async () => {
    responder(
      detalle({
        estado: 'devuelta',
        lineaDeTiempo: [
          { estado: 'pendiente', fecha: '2026-11-13T15:30:00Z', situacion: 'COMPLETADO' },
          { estado: 'pendiente', fecha: null, situacion: 'PENDIENTE' },
        ],
      }),
    )
    montar()

    const pasos = within(await screen.findByRole('list', { name: 'Línea de tiempo' })).getAllByRole('listitem')
    expect(pasos[0]).toHaveTextContent('Postulación enviada')
    expect(pasos[1]).toHaveTextContent('Reenvío de la postulación corregida')
  })

  it('nunca muestra quién hizo un cambio', async () => {
    responder(detalle())
    montar()
    await screen.findByRole('heading', { name: 'Beca 1' })

    expect(document.body.textContent).not.toMatch(/Administrador|DUBSS hizo|por Admin/)
  })

  describe('bloques según el estado', () => {
    it('devuelta: observaciones con su fecha; sin el CU-02 no hay «Corregir y reenviar»', async () => {
      responder(
        detalle({
          estado: 'devuelta',
          observaciones: { texto: 'Falta el certificado de notas', fecha: '2026-11-15T14:00:00Z' },
          puedeCorregir: true,
        }),
      )
      montar()

      const bloque = await screen.findByRole('region', { name: 'Observaciones' })
      expect(bloque).toHaveTextContent('Falta el certificado de notas')
      expect(bloque).toHaveTextContent('15/11/2026, 10:00')
      expect(screen.queryByRole('link', { name: 'Corregir y reenviar' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Corregir y reenviar' })).not.toBeInTheDocument()
    })

    it('devuelta con el CU-02 y puedeCorregir: ofrece «Corregir y reenviar»', async () => {
      enlaces.corregir = '/postular'
      responder(
        detalle({
          estado: 'devuelta',
          observaciones: { texto: 'Falta algo', fecha: '2026-11-15T14:00:00Z' },
          puedeCorregir: true,
        }),
      )
      montar()

      expect(await screen.findByRole('link', { name: 'Corregir y reenviar' })).toHaveAttribute('href', '/postular')
    })

    it('devuelta con el CU-02 pero sin puedeCorregir: no ofrece «Corregir y reenviar»', async () => {
      enlaces.corregir = '/postular'
      responder(
        detalle({
          estado: 'devuelta',
          observaciones: { texto: 'Falta algo', fecha: '2026-11-15T14:00:00Z' },
          puedeCorregir: false,
        }),
      )
      montar()
      await screen.findByRole('region', { name: 'Observaciones' })

      expect(screen.queryByRole('link', { name: 'Corregir y reenviar' })).not.toBeInTheDocument()
    })

    it('archivada: muestra el motivo del archivo', async () => {
      responder(detalle({ estado: 'archivada', motivo: 'No entregó su documentación' }))
      montar()

      const bloque = await screen.findByRole('region', { name: 'Motivo' })
      expect(bloque).toHaveTextContent('Motivo del archivo')
      expect(bloque).toHaveTextContent('No entregó su documentación')
      expect(screen.queryByRole('region', { name: 'Observaciones' })).not.toBeInTheDocument()
    })

    it('no adjudicada por rechazo: muestra el motivo del rechazo', async () => {
      responder(detalle({ estado: 'no_adjudicada', motivo: 'Documentos que no coinciden' }))
      montar()

      expect(await screen.findByRole('region', { name: 'Motivo' })).toHaveTextContent('Motivo del rechazo')
    })

    it('finalista con documentación pendiente: plazo, documentos y «Pendiente de entrega»', async () => {
      responder(
        detalle({
          estado: 'calificada',
          finalista: { plazoEntrega: '2026-11-20', documentacionRecibida: false, documentosFisicos: 'CI y certificado' },
        }),
      )
      montar()

      const bloque = await screen.findByRole('region', { name: 'Eres finalista' })
      expect(bloque).toHaveTextContent('20/11/2026')
      expect(bloque).toHaveTextContent('CI y certificado')
      expect(bloque).toHaveTextContent('Pendiente de entrega')
    })

    it('finalista con documentación recibida: lo dice, y sin aviso todavía lo informa sin inventar datos', async () => {
      responder(
        detalle({
          estado: 'calificada',
          finalista: { plazoEntrega: null, documentacionRecibida: true, documentosFisicos: null },
        }),
      )
      montar()

      const bloque = await screen.findByRole('region', { name: 'Eres finalista' })
      expect(bloque).toHaveTextContent('Recibida')
      expect(bloque).toHaveTextContent('Todavía sin plazo')
      expect(bloque).toHaveTextContent('La DUBSS aún no los indicó')
    })

    it('adjudicada: muestra el resultado y «Ver resultados de la convocatoria» lleva a /resultados', async () => {
      responder(detalle({ estado: 'adjudicada', resultado: 'ADJUDICADA' }))
      const { user } = montar()

      const bloque = await screen.findByRole('region', { name: 'Resultado' })
      expect(bloque).toHaveTextContent('Adjudicada')
      expect(bloque).toHaveTextContent('Obtuviste la Beca Comedor')
      await user.click(within(bloque).getByRole('button', { name: 'Ver resultados de la convocatoria' }))
      expect(navegar).toHaveBeenCalledWith({ to: '/resultados' })
    })

    it('no adjudicada por decisión final: el resultado sin motivo', async () => {
      responder(detalle({ estado: 'no_adjudicada', resultado: 'NO_ADJUDICADA', motivo: null }))
      montar()

      const bloque = await screen.findByRole('region', { name: 'Resultado' })
      expect(bloque).toHaveTextContent('No adjudicada')
      expect(screen.queryByRole('region', { name: 'Motivo' })).not.toBeInTheDocument()
    })

    it('una pendiente muestra solo el recorrido: ningún bloque de información asociada', async () => {
      responder(detalle({ estado: 'pendiente' }))
      montar()
      await screen.findByRole('list', { name: 'Línea de tiempo' })

      for (const nombre of ['Observaciones', 'Motivo', 'Eres finalista', 'Resultado']) {
        expect(screen.queryByRole('region', { name: nombre })).not.toBeInTheDocument()
      }
    })
  })

  describe('errores', () => {
    it('ERR_POS_01 muestra «Postulación no encontrada» con el enlace a la lista', async () => {
      cliente.get.mockRejectedValue(new ApiError('ERR_POS_01', 'x'))
      const { user } = montar()

      expect(await screen.findByText('Postulación no encontrada')).toBeInTheDocument()
      expect(screen.getByText('La postulación no existe o no es tuya')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Ir a mis postulaciones' }))
      expect(navegar).toHaveBeenCalledWith({ to: '/mis-postulaciones' })
    })

    it('un error de conexión muestra el mensaje con «Reintentar», que vuelve a pedir', async () => {
      responder(detalle())
      // Solo la primera consulta falla; el reintento ya recibe la postulación.
      cliente.get.mockRejectedValueOnce(new ApiError('ERR_SYS_00', 'x'))
      const { user } = montar()

      const alerta = await screen.findByRole('alert')
      expect(within(alerta).getByText(/error en el servidor/)).toBeInTheDocument()
      await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

      expect(await screen.findByRole('heading', { name: 'Beca 1' })).toBeInTheDocument()
    })
  })
})
