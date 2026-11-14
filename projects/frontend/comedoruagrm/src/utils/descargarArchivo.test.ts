import { afterEach, describe, expect, it, vi } from 'vitest'

import { descargarArchivo } from './descargarArchivo'

describe('descargarArchivo', () => {
  afterEach(() => vi.restoreAllMocks())

  it('crea un enlace temporal con el nombre dado, lo cliquea y lo revoca', () => {
    const crear = vi.fn().mockReturnValue('blob:prueba')
    const revocar = vi.fn()
    Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar })
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    descargarArchivo(new Blob(['%PDF']), 'lista.pdf')

    expect(crear).toHaveBeenCalledTimes(1)
    expect(clic).toHaveBeenCalledTimes(1)
    expect(revocar).toHaveBeenCalledWith('blob:prueba')
    expect(document.querySelector('a[download]')).toBeNull()
  })
})
