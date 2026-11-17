import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { useConvocatoriaElegida } from './useConvocatoriaElegidaStore'

describe('useConvocatoriaElegida', () => {
  it('arranca sin convocatoria elegida', () => {
    const { result } = renderHook(() => useConvocatoriaElegida())
    expect(result.current[0]).toBeUndefined()
  })

  it('lo que se elige en una pantalla ya viene elegido en la siguiente', () => {
    const pantallaA = renderHook(() => useConvocatoriaElegida())
    act(() => pantallaA.result.current[1]('conv-9'))
    pantallaA.unmount()

    const pantallaB = renderHook(() => useConvocatoriaElegida())
    expect(pantallaB.result.current[0]).toBe('conv-9')
  })

  it('se puede borrar para volver a la más reciente', () => {
    const { result } = renderHook(() => useConvocatoriaElegida())
    act(() => result.current[1]('conv-1'))
    act(() => result.current[1](undefined))
    expect(result.current[0]).toBeUndefined()
  })

  it('cada test arranca limpio (setupTests reinicia el estado)', () => {
    const { result } = renderHook(() => useConvocatoriaElegida())
    expect(result.current[0]).toBeUndefined()
  })
})
