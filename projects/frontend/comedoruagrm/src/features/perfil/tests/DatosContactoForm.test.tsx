import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/store'

const toastSuccess = vi.hoisted(() => vi.fn())
vi.mock('@/components/ui/toast', () => ({ toastSuccess }))
vi.mock('../api/useActualizarPerfil', () => ({ useActualizarPerfil: vi.fn() }))

import { useActualizarPerfil } from '../api/useActualizarPerfil'
import { DatosContactoForm } from '../components/DatosContactoForm'
import type { Perfil } from '../types'

const postulante: Perfil = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Perez',
  ci: '1234567',
  correo: 'ana@uagrm.edu.bo',
  rol: 'POSTULANTE',
  activo: true,
  postulante: {
    registroUniversitario: '219012345',
    carrera: { codigo: 'c1', nombre: 'Ingeniería', facultad: 'Tecnología' },
    telefono: '70000001',
  },
}
const administrador: Perfil = { ...postulante, rol: 'ADMINISTRADOR_DUBSS', postulante: null }

type Mutate = (r: unknown, o: { onSuccess: (p: Perfil) => void; onError: (e: unknown) => void }) => void

function montar(perfil: Perfil, opciones: { mutate?: Mutate; isPending?: boolean } = {}) {
  const mutate = vi.fn(opciones.mutate ?? (() => {}))
  vi.mocked(useActualizarPerfil).mockReturnValue({ mutate, isPending: opciones.isPending ?? false } as never)
  const { rerender } = render(<DatosContactoForm perfil={perfil} />)
  return { mutate, rerender, user: userEvent.setup() }
}

const boton = () => screen.getByRole('button', { name: 'Guardar cambios' })

describe('DatosContactoForm', () => {
  beforeEach(() => {
    toastSuccess.mockReset()
    vi.mocked(useActualizarPerfil).mockReset()
  })

  it('el Postulante ve correo y teléfono con sus valores; el Administrador no ve teléfono', () => {
    montar(postulante)
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('ana@uagrm.edu.bo')
    expect(screen.getByLabelText('Teléfono')).toHaveValue('70000001')
  })

  it('para un Administrador no se muestra el teléfono', () => {
    montar(administrador)
    expect(screen.queryByLabelText('Teléfono')).not.toBeInTheDocument()
  })

  it('sin cambios el botón está deshabilitado', () => {
    montar(postulante)
    expect(boton()).toBeDisabled()
  })

  it('cambiar solo el teléfono no pide contraseña y manda solo el teléfono', async () => {
    const { mutate, user } = montar(postulante)

    await user.clear(screen.getByLabelText('Teléfono'))
    await user.type(screen.getByLabelText('Teléfono'), '71111111')
    expect(screen.queryByLabelText('Contraseña actual')).not.toBeInTheDocument()
    await user.click(boton())

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    expect(mutate.mock.calls[0][0]).toEqual({ telefono: '71111111' })
  })

  it('cambiar el correo muestra la contraseña actual, la exige y la manda con el correo (A7)', async () => {
    const { mutate, user } = montar(postulante)

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'nuevo@uagrm.edu.bo')
    expect(await screen.findByLabelText('Contraseña actual')).toBeInTheDocument()

    await user.click(boton())
    expect(await screen.findByText('Ingresa tu contraseña actual para cambiar el correo')).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Contraseña actual'), 'secreta1')
    await user.click(boton())

    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
    expect(mutate.mock.calls[0][0]).toEqual({ correo: 'nuevo@uagrm.edu.bo', contrasenaActual: 'secreta1' })
  })

  it('si el correo vuelve al original la contraseña actual desaparece', async () => {
    const { user } = montar(postulante)

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'otro@uagrm.edu.bo')
    expect(await screen.findByLabelText('Contraseña actual')).toBeInTheDocument()
    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'ana@uagrm.edu.bo')

    await waitFor(() => expect(screen.queryByLabelText('Contraseña actual')).not.toBeInTheDocument())
  })

  it('un correo vacío, mal formado o un teléfono vacío muestran el error inline y no envían', async () => {
    const { mutate, user } = montar(postulante)

    await user.clear(screen.getByLabelText('Teléfono'))
    await user.tab()
    expect(await screen.findByText('El teléfono es obligatorio')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'no-es-correo')
    await user.tab()
    expect(await screen.findByText('El correo electrónico no es válido')).toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it('el éxito avisa «Datos actualizados» y deja el formulario con lo guardado, sin contraseña', async () => {
    const guardado = { ...postulante, correo: 'nuevo@uagrm.edu.bo' }
    const { user, rerender } = montar(postulante, { mutate: (_r, o) => o.onSuccess(guardado) })

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'nuevo@uagrm.edu.bo')
    await user.type(await screen.findByLabelText('Contraseña actual'), 'secreta1')
    await user.click(boton())

    await waitFor(() => expect(toastSuccess).toHaveBeenCalledWith('Datos actualizados'))
    // En la app, `setQueryData` entrega el perfil nuevo como prop; acá se simula.
    rerender(<DatosContactoForm perfil={guardado} />)
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('nuevo@uagrm.edu.bo')
    await waitFor(() => expect(screen.queryByLabelText('Contraseña actual')).not.toBeInTheDocument())
  })

  it('ERR_US_02 marca el correo (A5) y conserva lo escrito', async () => {
    const { user } = montar(postulante, {
      mutate: (_r, o) => o.onError(new ApiError('ERR_US_02', 'Ya existe una cuenta con ese correo electrónico')),
    })

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'otro@uagrm.edu.bo')
    await user.type(await screen.findByLabelText('Contraseña actual'), 'secreta1')
    await user.click(boton())

    expect(await screen.findByText('Ya existe una cuenta con ese correo electrónico')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('otro@uagrm.edu.bo')
    expect(toastSuccess).not.toHaveBeenCalled()
  })

  it('ERR_AUTH_09 marca la contraseña actual (A6)', async () => {
    const { user } = montar(postulante, { mutate: (_r, o) => o.onError(new ApiError('ERR_AUTH_09', 'x')) })

    await user.clear(screen.getByLabelText('Correo electrónico'))
    await user.type(screen.getByLabelText('Correo electrónico'), 'otro@uagrm.edu.bo')
    await user.type(await screen.findByLabelText('Contraseña actual'), 'mala')
    await user.click(boton())

    expect(await screen.findByText('La contraseña actual es incorrecta.')).toBeInTheDocument()
  })

  it('un error de red muestra el aviso de conexión con «Reintentar» (A10)', async () => {
    const { mutate, user } = montar(postulante, {
      mutate: (_r, o) => o.onError(new AxiosError('Network Error', 'ERR_NETWORK')),
    })

    await user.clear(screen.getByLabelText('Teléfono'))
    await user.type(screen.getByLabelText('Teléfono'), '71111111')
    await user.click(boton())

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(mutate).toHaveBeenCalledTimes(2))
  })
})
