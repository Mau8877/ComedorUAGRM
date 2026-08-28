---
globs: projects/backend/**/*
---

# Convenciones de Código Java — Backend

## Estructura de paquetes

Todo bajo `com.comedoruagrm.backend`, siguiendo el scaffold feature-based ya
creado:

```
com.comedoruagrm.backend
├── common/
│   ├── constant/       # Constantes compartidas entre módulos
│   ├── exception/       # BusinessException y jerarquía (ver EXCEPCIONES_BACKEND.md)
│   ├── response/        # ApiResponse, PageMeta (ver RESPONSES_BACKEND.md)
│   └── util/            # Utilidades genéricas sin estado
├── config/               # Configuración de Spring (SecurityConfig, etc.)
├── security/             # JWT, filtros, @CurrentUserId, UserDetailsServiceImpl
└── features/
    └── {modulo}/         # ej. usuarios/, auth/, pedidos/
        ├── controller/
        ├── service/
        ├── repository/
        ├── dto/          # XxxRequest, XxxResponse
        ├── entity/
        └── mapper/       # (si el mapeo Entity↔DTO no es trivial)
```

Una clase que solo la usa un módulo vive **dentro de ese módulo**, no en
`common/` — `common/` es exclusivamente para lo que de verdad comparten dos o
más features. No se mueve algo a `common/` "por las dudas".

## Nombres de clases

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Controller | `{Recurso}Controller` | `UsuarioController` |
| Service | `{Recurso}Service` (clase concreta, **sin** interfaz `IXxx`/`XxxImpl` salvo que exista más de una implementación real) | `UsuarioService` |
| Repository | `{Recurso}Repository extends JpaRepository<...>` | `UsuarioRepository` |
| Entity | Nombre de dominio en singular, sin sufijo | `Usuario` |
| DTO de entrada | `{Recurso}Request` (o `Crear{Recurso}Request` / `Actualizar{Recurso}Request` si hace falta distinguir create de update) | `CrearUsuarioRequest` |
| DTO de salida | `{Recurso}Response` | `UsuarioResponse` |
| Mapper | `{Recurso}Mapper` | `UsuarioMapper` |
| Excepción de negocio | Ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md) | `NotFoundException` |
| Códigos de error de un módulo | `{Recurso}ErrorCodes` (interfaz de constantes o `enum`) | `UsuarioErrorCodes` |

No se usa la clase `Service` como interfaz con un único `ServiceImpl` — eso
es sobre-ingeniería para este proyecto salvo que un módulo concreto
justifique tener más de una implementación intercambiable (ej. para tests
con un doble real, algo que ya cubre Mockito sin necesidad de la interfaz).

## Anotaciones/decorators custom

Las anotaciones custom del proyecto (ej. `@CurrentUserId`, ver
[SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md)) se definen **una
sola vez**, en `security/` si están ligadas a autenticación, o en
`common/` si son de propósito general y no específicas de seguridad. No se
redefine la misma anotación en dos módulos — si dos features necesitan
"lo mismo", es la misma anotación importada, no una copia.

## Estilo general

- 4 espacios de indentación (ya fijado en `.editorconfig` para `*.java`).
- Constructor injection siempre (`private final` + constructor, o
  `@RequiredArgsConstructor` de Lombok si se agrega Lombok al proyecto —
  hoy **no está** en `pom.xml`; mientras no esté, constructores explícitos).
- Los `record` de Java se prefieren para DTOs y para `ApiResponse`/`PageMeta`
  (inmutables, sin boilerplate) por sobre clases con Lombok/getters manuales.
