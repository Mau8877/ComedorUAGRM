import { fireEvent, screen, waitFor, within } from '@testing-library/react'
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
  AdminLayout: ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

import { ApiError } from '@/store'

import { GestionesAcademicasScreen } from '../screens/GestionesAcademicasScreen'

import { cliente, GESTION_2026_I, GESTION_2026_II, montar, reiniciarCliente, simularBackend, ultimaConsulta, yo } from './montar'

function pantalla() {
  return montar(<GestionesAcademicasScreen titulo="Gestiones académicas" activeHref="/gestiones-academicas" usuarioActual={yo} />)
}

function escribir(etiqueta: string, valor: string) {
  fireEvent.change(screen.getByLabelText(etiqueta), { target: { value: valor } })
}

describe('GestionesAcademicasScreen', () => {
  beforeEach(() => {
    reiniciarCliente()
    simularBackend({ gestiones: [GESTION_2026_II, GESTION_2026_I] })
    cliente.post.mockResolvedValue({ data: GESTION_2026_II })
    cliente.put.mockResolvedValue({ data: GESTION_2026_II })
  })

  it('pide la lista y muestra la etiqueta y las fechas dd/mm/aaaa', async () => {
    pantalla()

    expect((await screen.findAllByText('2026-II')).length).toBeGreaterThan(0)
    expect(ultimaConsulta('/gestiones-academicas')).toEqual({ page: 1, pageSize: 20 })
    expect(screen.getAllByText('01/07/2026').length).toBeGreaterThan(0)
    expect(screen.getAllByText('20/12/2026').length).toBeGreaterThan(0)
  })

  it('la búsqueda manda search y vuelve a la página 1', async () => {
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    await user.type(screen.getByPlaceholderText('Buscar por etiqueta...'), '2026')

    await waitFor(() => expect(ultimaConsulta('/gestiones-academicas')).toMatchObject({ search: '2026', page: 1 }))
  })

  it('ordenar por fecha de inicio manda sort', async () => {
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    await user.click(screen.getByRole('button', { name: /Inicio/ }))

    await waitFor(() => expect(ultimaConsulta('/gestiones-academicas')).toMatchObject({ sort: 'fechaInicio' }))
  })

  it('sin gestiones muestra el mensaje de lista vacía', async () => {
    simularBackend({ gestiones: [] })
    pantalla()

    expect((await screen.findAllByText('Todavía no hay gestiones académicas')).length).toBeGreaterThan(0)
  })

  it('«Nueva gestión» crea con POST y los tres campos', async () => {
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    await user.click(screen.getByRole('button', { name: 'Nueva gestión' }))
    escribir('Etiqueta', '2027-I')
    escribir('Fecha de inicio', '2027-02-01')
    escribir('Fecha de fin', '2027-06-30')
    await user.click(screen.getByRole('button', { name: 'Crear gestión' }))

    await waitFor(() =>
      expect(cliente.post).toHaveBeenCalledWith('/gestiones-academicas', {
        etiqueta: '2027-I',
        fechaInicio: '2027-02-01',
        fechaFin: '2027-06-30',
      }),
    )
  })

  it('un fin igual al inicio se marca bajo la fecha de fin y no se envía', async () => {
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    await user.click(screen.getByRole('button', { name: 'Nueva gestión' }))
    escribir('Etiqueta', '2027-I')
    escribir('Fecha de inicio', '2027-02-01')
    escribir('Fecha de fin', '2027-02-01')
    await user.click(screen.getByRole('button', { name: 'Crear gestión' }))

    expect(await screen.findByText('La fecha de fin debe ser posterior a la de inicio')).toBeInTheDocument()
    expect(cliente.post).not.toHaveBeenCalled()
  })

  it('una etiqueta repetida (ERR_CV_12) se marca bajo el campo etiqueta y conserva lo escrito', async () => {
    cliente.post.mockRejectedValue(new ApiError('ERR_CV_12', 'Ya existe una gestión con esa etiqueta'))
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    await user.click(screen.getByRole('button', { name: 'Nueva gestión' }))
    escribir('Etiqueta', '2026-II')
    escribir('Fecha de inicio', '2027-02-01')
    escribir('Fecha de fin', '2027-06-30')
    await user.click(screen.getByRole('button', { name: 'Crear gestión' }))

    expect(await screen.findByText('Ya existe una gestión con esa etiqueta')).toBeInTheDocument()
    expect(screen.getByLabelText('Etiqueta')).toHaveValue('2026-II')
  })

  it('«Editar» precarga la gestión y guarda con PUT', async () => {
    const { user } = pantalla()
    await screen.findAllByText('2026-II')

    const fila = screen.getAllByText('2026-II')[0].closest('tr') as HTMLElement
    await user.click(within(fila).getByRole('button', { name: 'Editar' }))
    expect(screen.getByLabelText('Etiqueta')).toHaveValue('2026-II')
    expect(screen.getByLabelText('Fecha de fin')).toHaveValue('2026-12-20')
    escribir('Fecha de fin', '2026-12-30')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() =>
      expect(cliente.put).toHaveBeenCalledWith('/gestiones-academicas/g2', {
        etiqueta: '2026-II',
        fechaInicio: '2026-07-01',
        fechaFin: '2026-12-30',
      }),
    )
  })

  it('si la gestión ya no existe (ERR_CV_02) avisa, cierra el modal y refresca', async () => {
    cliente.put.mockRejectedValue(new ApiError('ERR_CV_02', 'La gestión académica indicada no existe'))
    const { user } = pantalla()
    await screen.findAllByText('2026-II')
    const fila = screen.getAllByText('2026-II')[0].closest('tr') as HTMLElement
    await user.click(within(fila).getByRole('button', { name: 'Editar' }))
    cliente.get.mockClear()

    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(cliente.get).toHaveBeenCalledWith('/gestiones-academicas', expect.anything()))
  })
})
