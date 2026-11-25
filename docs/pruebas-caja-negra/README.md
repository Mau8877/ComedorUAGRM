# Pruebas de caja negra: Persona A

Casos de prueba de los 13 casos de uso de la persona A (cuentas, auditoría y adjudicación, BI y estado de la postulación), con la estructura de la sección 9.2.1 del documento de taller. Cada caso se ejecutó sobre el sistema real levantado con `docker compose up -d` (web en `http://localhost` (proxy nginx) y API en `http://localhost:8080`, perfil `dev`, datos sembrados `[PRUEBA]`). Las capturas se tomaron con Playwright (`tests/e2e/caja-negra/`, `tests/e2e/playwright.cn.config.ts`).

Fecha de ejecución: 24/11/2026. El resultado obtenido y el veredicto reflejan lo que respondió el sistema en esa ejecución.

| CU | Casos | Resultado de cada caso |
| --- | --- | --- |
| [CU01](CU01%20-%20Registrarse%20y%20autenticarse.md) | 4 | Correcto, Correcto, Correcto, Correcto |
| [CU15](CU15%20-%20Recuperar%20contraseña%20y%20gestionar%20perfil.md) | 3 | Correcto, Correcto, Correcto |
| [CU14](CU14%20-%20Gestionar%20usuarios%20y%20roles.md) | 3 | Correcto, Correcto, Correcto |
| [CU06](CU06%20-%20Visualizar%20lista%20priorizada%20de%20postulantes.md) | 3 | Correcto, Correcto, Correcto |
| [CU21](CU21%20-%20Notificar%20finalistas%20y%20publicar%20comunicados.md) | 4 | Correcto, Correcto, Correcto, Correcto |
| [CU07](CU07%20-%20Auditar%20expediente%20finalista.md) | 3 | Correcto, Correcto, Correcto |
| [CU19](CU19%20-%20Devolver%20postulación%20para%20corrección.md) | 2 | Correcto, Correcto |
| [CU20](CU20%20-%20Archivar%20o%20rechazar%20postulación.md) | 2 | Correcto, Correcto |
| [CU05](CU05%20-%20Auditar%20y%20adjudicar%20becas.md) | 3 | Correcto, Correcto, Correcto |
| [CU22](CU22%20-%20Publicar%20resultados%20de%20adjudicación.md) | 3 | Correcto, Correcto, Correcto |
| [CU08](CU08%20-%20Generar%20acta%20de%20adjudicación.md) | 3 | Correcto (tras corregir el entorno de MinIO), Correcto, Correcto |
| [CU04](CU04%20-%20Consultar%20estado%20de%20postulación.md) | 2 | Correcto, Correcto |
| [CU12](CU12%20-%20Visualizar%20dashboard%20de%20indicadores%20BI.md) | 4 | Correcto, Correcto, Correcto, Correcto |

## Hallazgos de la ejecución

- **CU08, primer intento con error:** la generación del acta falló con «Ocurrió un error en el servidor» porque el backend no podía subir el PDF a MinIO. La causa es el entorno: el `.env` raíz de esta máquina fija `MINIO_ROOT_USER=admin` / `MINIO_ROOT_PASSWORD=admin12345` (distinto del `.env.example`, que usa `minio_admin` / `minio_local_pass_123`) y `docker-compose.yml` no pasa esas variables al contenedor `core`, que usa las predeterminadas de `application.properties`. Se creó en MinIO el usuario `minio_admin` con la contraseña predeterminada y se repitió el caso, que pasó. No se modificó ningún archivo del repositorio.
- **CU12:** la gestión que el dashboard selecciona por defecto («[PRUEBA] Escenarios») no tiene indicadores calculados porque se sembró después de la última actualización de los indicadores (15/11/2026); se ve el aviso «No hay datos para la selección elegida todavía.». Con la gestión «[PRUEBA] 2026-II» sí hay datos.
- **CU08, PDF:** el nombre largo de la convocatoria de prueba se corta en el encabezado del acta (es un dato de siembra).
- Ningún caso de esta persona quedó «Incorrecto».

## Datos que cambiaron durante las pruebas

Las pruebas modifican la base de desarrollo: E03 (Ana Zambrana auditada, Elena notificada), E04 (Elena adjudicada, Ana Zambrana devuelta, Ana Salvatierra archivada), E05 (publicada), E06 (acta 001/2026 generada) y varias cuentas nuevas. Para repetirlas hay que volver a sembrar la base (`docker compose down -v`).
