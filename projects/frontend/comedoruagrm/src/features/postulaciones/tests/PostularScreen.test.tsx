import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
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
// El panel de documentos (CU16) tiene sus propios tests: acá solo importa que el paso 3 lo muestre.
vi.mock('@/features/postulaciones/components/DocumentosPostulacionPanel', () => {
  return {
    DocumentosPostulacionPanel: ({ codigo }: { codigo: string }) => <div>Panel de documentos de {codigo}</div>,
  }
})

import { ApiError } from '@/store'

import { PostularScreen } from '../screens/PostularScreen'
import type { FormularioPostulacion } from '../types'

import { cliente, navegar } from './clienteSimulado'
import { complementariosMinimos, opcionesFormulario, yo } from './datos'

const formulario = (extra: Partial<FormularioPostulacion> = {}): FormularioPostulacion => ({
  postulacion: 'post-1',
  estado: 'borrador',
  numeroEnvios: 0,
  convocatoria: { codigo: 'conv-1', nombre: 'Convocatoria 2026-II', gestion: '2026-II', fechaCierre: '2026-12-20T20:00:00Z' },
  perfil: { nombres: 'Pia', apellidos: 'Perez', ci: '1234567', carrera: 'Ing. Informática' },
  opciones: opcionesFormulario,
  respuesta: null,
  ...extra,
})

const respuestaCompleta = {
  ingresosFamiliares: 1800,
  dependientes: 4,
  procedencia: 'Provincia',
  patrimonio: 'Sin vivienda propia',
  estadoVulnerabilidad: 'Hogar monoparental',
  promedioAcademico: 72.5,
  complementarios: complementariosMinimos,
}

function montar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <PostularScreen titulo="Nueva postulación" activeHref="/mis-postulaciones" usuarioActual={yo} />
    </QueryClientProvider>,
  )
  return { user: userEvent.setup() }
}

describe('PostularScreen (CU-02)', () => {
  beforeEach(() => {
    cliente.get.mockReset()
    cliente.post.mockReset()
    cliente.put.mockReset()
    navegar.mockReset()
  })

  it('al abrir crea la postulación y muestra el perfil precargado y la convocatoria', async () => {
    cliente.post.mockResolvedValue({ data: formulario() })
    montar()

    expect(await screen.findByText('Ing. Informática')).toBeInTheDocument()
    expect(screen.getByText('1234567')).toBeInTheDocument()
    expect(screen.getByText(/Convocatoria 2026-II · gestión 2026-II/)).toBeInTheDocument()
    expect(cliente.post).toHaveBeenCalledTimes(1)
    expect(cliente.post).toHaveBeenCalledWith('/postulaciones')
    expect(screen.getByText(/Paso 1 de 3/)).toBeInTheDocument()
  })

  it('sin convocatoria abierta muestra el aviso del backend y no el formulario', async () => {
    cliente.post.mockRejectedValue(
      new ApiError('ERR_ENV_01', 'No hay una convocatoria abierta para postular. La próxima abre el 10/01/2027.'),
    )
    montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('La próxima abre el 10/01/2027')
    expect(screen.queryByText(/Paso 1 de 3/)).not.toBeInTheDocument()
  })

  it('si ya tiene una postulación vigente muestra el motivo y ofrece ir a Mis postulaciones', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_VAL_01', 'Ya tienes una postulación vigente en la gestión 2026-II'))
    const { user } = montar()

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya tienes una postulación vigente')
    await user.click(screen.getByRole('button', { name: 'Ir a mis postulaciones' }))
    expect(navegar).toHaveBeenCalledWith({ to: '/mis-postulaciones' })
  })

  it('«Continuar» con el formulario vacío marca los campos obligatorios y no avanza', async () => {
    cliente.post.mockResolvedValue({ data: formulario() })
    const { user } = montar()

    await user.click(await screen.findByRole('button', { name: 'Continuar' }))
    await user.click(await screen.findByRole('button', { name: 'Continuar' }))

    expect(await screen.findByText(/Revisa los campos marcados/)).toBeInTheDocument()
    expect(screen.getAllByText('Este campo es obligatorio.').length).toBeGreaterThanOrEqual(3)
    expect(screen.getByText(/Paso 2 de 3/)).toBeInTheDocument()
  })

  it('una postulación devuelta se abre con sus respuestas y se reenvía hasta ver el comprobante', async () => {
    cliente.post
      .mockResolvedValueOnce({ data: formulario({ estado: 'devuelta', numeroEnvios: 1, respuesta: { ...respuestaCompleta, guardadoEn: null } }) })
      .mockResolvedValueOnce({
        data: {
          codigo: 'post-1',
          estado: 'pendiente',
          fechaEnvio: '2026-11-16T15:00:00Z',
          numeroEnvios: 2,
          reenvio: true,
          convocatoria: 'Convocatoria 2026-II',
          gestion: '2026-II',
        },
      })
    const { user } = montar()

    expect(await screen.findByText(/fue devuelta para corregirla/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(await screen.findByLabelText(/Promedio académico/)).toHaveValue('72.5')
    await user.click(screen.getByRole('button', { name: 'Continuar' }))

    expect(await screen.findByText('Panel de documentos de post-1')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Enviar postulación' }))
    expect(await screen.findByText('¿Enviar tu postulación?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Confirmar envío' }))

    expect(await screen.findByRole('heading', { name: 'Postulación reenviada' })).toBeInTheDocument()
    expect(cliente.post).toHaveBeenLastCalledWith('/postulaciones/post-1/enviar', respuestaCompleta)
    await user.click(screen.getByRole('button', { name: 'Ver mi postulación' }))
    expect(navegar).toHaveBeenCalledWith({ to: '/mis-postulaciones/$codigo', params: { codigo: 'post-1' } })
  })

  it('si el backend dice que faltan documentos muestra el motivo y deja corregir sin perder lo escrito', async () => {
    cliente.post
      .mockResolvedValueOnce({ data: formulario({ respuesta: { ...respuestaCompleta, guardadoEn: null } }) })
      .mockRejectedValueOnce(new ApiError('ERR_ENV_04', 'Falta completar documentos: Carnet de identidad'))
    const { user } = montar()

    await user.click(await screen.findByRole('button', { name: 'Continuar' }))
    await user.click(await screen.findByRole('button', { name: 'Continuar' }))
    await user.click(await screen.findByRole('button', { name: 'Enviar postulación' }))
    await user.click(await screen.findByRole('button', { name: 'Confirmar envío' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Falta completar documentos: Carnet de identidad')
    await waitFor(() => expect(screen.queryByText('¿Enviar tu postulación?')).not.toBeInTheDocument())
    expect(screen.getByText('Panel de documentos de post-1')).toBeInTheDocument()
  })

  describe('formulario oficial de la DUBSS', () => {
    it('muestra las secciones del formulario en papel con las opciones del backend', async () => {
      cliente.post.mockResolvedValue({ data: formulario() })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))

      for (const titulo of ['Ingresos y datos académicos', 'Dependencia económica', 'Grupo familiar', 'Residencia y vivienda', 'Otros datos']) {
        expect(await screen.findByText(titulo)).toBeInTheDocument()
      }
      expect(screen.getByText(/¿De quién depende usted\?/)).toBeInTheDocument()
      expect(screen.getByText(/Tenencia de la vivienda/)).toBeInTheDocument()
      expect(screen.getByLabelText(/Aclaración o comentario personal/)).toBeInTheDocument()
    })

    it('«¿Cuántos?» solo aparece si dijo que tiene hijos', async () => {
      cliente.post.mockResolvedValue({
        data: formulario({ respuesta: { ...respuestaCompleta, guardadoEn: null, complementarios: { ...complementariosMinimos, tieneHijos: true } } }),
      })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))

      expect(await screen.findByText(/¿Cuántos\?/)).toBeInTheDocument()
    })

    it('sin hijos no pregunta cuántos', async () => {
      cliente.post.mockResolvedValue({ data: formulario({ respuesta: { ...respuestaCompleta, guardadoEn: null } }) })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))

      await screen.findByText('Grupo familiar')
      expect(screen.queryByText(/¿Cuántos\?/)).not.toBeInTheDocument()
    })

    it('agrega y quita integrantes del grupo familiar, hasta 10', async () => {
      cliente.post.mockResolvedValue({ data: formulario() })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))
      const agregar = await screen.findByRole('button', { name: 'Agregar familiar' })
      expect(screen.queryByRole('group', { name: 'Familiar 1' })).not.toBeInTheDocument()

      await user.click(agregar)
      expect(await screen.findByRole('group', { name: 'Familiar 1' })).toBeInTheDocument()
      expect(screen.getByText('1 de 10')).toBeInTheDocument()

      for (let i = 0; i < 9; i++) await user.click(agregar)
      expect(screen.getByText('10 de 10')).toBeInTheDocument()
      expect(agregar).toBeDisabled()

      await user.click(screen.getByRole('button', { name: 'Quitar familiar 10' }))
      expect(screen.getByText('9 de 10')).toBeInTheDocument()
      expect(agregar).toBeEnabled()
    })

    it('envía el grupo familiar y los datos del formulario oficial con la forma del backend', async () => {
      cliente.post
        .mockResolvedValueOnce({ data: formulario({ respuesta: { ...respuestaCompleta, guardadoEn: null } }) })
        .mockResolvedValueOnce({
          data: { codigo: 'post-1', estado: 'pendiente', fechaEnvio: '2026-11-16T15:00:00Z', numeroEnvios: 1, reenvio: false, convocatoria: 'C', gestion: '2026-II' },
        })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))
      await user.click(await screen.findByRole('button', { name: 'Agregar familiar' }))
      await user.type(await screen.findByLabelText('Nombre completo'), 'María')
      await user.type(screen.getByLabelText('Edad'), '45')
      await user.click(screen.getByRole('button', { name: 'Continuar' }))
      await user.click(await screen.findByRole('button', { name: 'Enviar postulación' }))
      await user.click(await screen.findByRole('button', { name: 'Confirmar envío' }))

      await screen.findByRole('heading', { name: /Postulación enviada/ })
      expect(cliente.post).toHaveBeenLastCalledWith('/postulaciones/post-1/enviar', {
        ...respuestaCompleta,
        complementarios: {
          ...complementariosMinimos,
          grupoFamiliar: [{ nombre: 'María', parentesco: null, edad: 45, ocupacion: null, observacion: null }],
        },
      })
    })
  })

  describe('guardar borrador (CU-17)', () => {
    it('guarda lo que hay escrito con los campos vacíos en null y muestra la hora del guardado', async () => {
      cliente.post.mockResolvedValue({ data: formulario() })
      cliente.put.mockResolvedValue({
        data: { postulacion: 'post-1', guardadoEn: '2026-07-12T18:32:00Z', camposPendientes: 5 },
      })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))
      await user.type(await screen.findByLabelText(/Ingresos familiares/), '1800')
      await user.click(screen.getByRole('button', { name: 'Guardar borrador' }))

      expect(await screen.findByText(/Borrador guardado · 14:32/)).toBeInTheDocument()
      expect(cliente.put).toHaveBeenCalledWith('/postulaciones/post-1/borrador', {
        ingresosFamiliares: 1800,
        dependientes: null,
        procedencia: null,
        patrimonio: null,
        estadoVulnerabilidad: null,
        promedioAcademico: null,
        // * Sin tocar el bloque del formulario oficial: todo en null (un borrador no exige nada).
        complementarios: expect.objectContaining({
          dependenciaEconomica: null,
          tieneHijos: null,
          grupoFamiliar: null,
          tenenciaVivienda: null,
        }),
      })
      // Sigue en el paso 2: guardar no envía ni avanza.
      expect(screen.getByText(/Paso 2 de 3/)).toBeInTheDocument()
    })

    it('un valor mal escrito no se guarda y se señala; los campos vacíos no son error', async () => {
      cliente.post.mockResolvedValue({ data: formulario() })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))
      await user.type(await screen.findByLabelText(/Promedio académico/), 'abc')
      await user.click(screen.getByRole('button', { name: 'Guardar borrador' }))

      expect(await screen.findByRole('alert')).toHaveTextContent(/No se guardó el borrador.*promedio académico/s)
      expect(cliente.put).not.toHaveBeenCalled()
    })

    it('al retomar un borrador ya guardado muestra la hora del último guardado y sus datos', async () => {
      cliente.post.mockResolvedValue({
        data: formulario({
          respuesta: {
            ingresosFamiliares: 1800,
            dependientes: null,
            procedencia: null,
            patrimonio: null,
            estadoVulnerabilidad: null,
            promedioAcademico: null,
            complementarios: null,
            guardadoEn: '2026-07-12T18:32:00Z',
          },
        }),
      })
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))

      expect(await screen.findByLabelText(/Ingresos familiares/)).toHaveValue('1800')
      expect(screen.getByText(/Borrador guardado · 14:32/)).toBeInTheDocument()
    })

    it('si el backend rechaza el guardado (convocatoria cerrada) muestra el motivo', async () => {
      cliente.post.mockResolvedValue({ data: formulario() })
      cliente.put.mockRejectedValue(new ApiError('ERR_VAL_03', 'El plazo finalizó'))
      const { user } = montar()

      await user.click(await screen.findByRole('button', { name: 'Continuar' }))
      await user.click(await screen.findByRole('button', { name: 'Guardar borrador' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('el plazo finalizó')
    })
  })
})
