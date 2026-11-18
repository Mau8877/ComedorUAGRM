import '@testing-library/jest-dom'
import { beforeEach } from 'vitest'

import { useConvocatoriaElegidaStore } from '@/store/useConvocatoriaElegidaStore'

// * La convocatoria elegida es estado global: cada test arranca sin ninguna, como una sesión nueva.
beforeEach(() => {
  useConvocatoriaElegidaStore.setState({ codigo: undefined })
})
