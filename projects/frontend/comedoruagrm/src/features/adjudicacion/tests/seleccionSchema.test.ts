import { describe, expect, it } from 'vitest'

import { aSeleccionRequest, leerCantidad, seleccionSchema, seleccionSchemaRequerido } from '../schemas'

const MENSAJE = 'La cantidad debe ser un número entero positivo'

describe('seleccionSchema', () => {
  it('admite el campo vacío (sin cantidad: el backend usa la configurada)', () => {
    expect(seleccionSchema.safeParse({ cantidad: '' }).success).toBe(true)
    expect(seleccionSchema.safeParse({ cantidad: '   ' }).success).toBe(true)
  })

  it.each(['1', '15', ' 20 '])('acepta el entero positivo «%s»', (cantidad) => {
    expect(seleccionSchema.safeParse({ cantidad }).success).toBe(true)
  })

  it.each(['0', '-3', '2.5', '2,5', 'diez', '1e3'])('rechaza «%s» con el mensaje del backend', (cantidad) => {
    const resultado = seleccionSchema.safeParse({ cantidad })

    expect(resultado.success).toBe(false)
    expect(resultado.error?.issues[0].message).toBe(MENSAJE)
  })
})

describe('seleccionSchemaRequerido', () => {
  it('exige escribir la cantidad cuando la convocatoria no tiene una configurada', () => {
    const resultado = seleccionSchemaRequerido.safeParse({ cantidad: '' })

    expect(resultado.success).toBe(false)
    expect(resultado.error?.issues[0].message).toBe('Indica cuántos finalistas seleccionar')
  })

  it('con una cantidad válida pasa y con una inválida muestra el mensaje de entero positivo', () => {
    expect(seleccionSchemaRequerido.safeParse({ cantidad: '4' }).success).toBe(true)
    expect(seleccionSchemaRequerido.safeParse({ cantidad: '0' }).error?.issues[0].message).toBe(MENSAJE)
  })
})

describe('aSeleccionRequest y leerCantidad', () => {
  it('el campo vacío se envía sin cantidad y uno con valor, como número', () => {
    expect(aSeleccionRequest({ cantidad: '' })).toEqual({})
    expect(aSeleccionRequest({ cantidad: ' 12 ' })).toEqual({ cantidad: 12 })
  })

  it('leerCantidad devuelve NaN si no es un entero positivo', () => {
    expect(leerCantidad('7')).toBe(7)
    expect(Number.isNaN(leerCantidad('0'))).toBe(true)
    expect(Number.isNaN(leerCantidad('abc'))).toBe(true)
  })
})
