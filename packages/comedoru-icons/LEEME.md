# Íconos ComedorU (diseño Q)

## Colocar en el proyecto (carpeta public/)
- favicon/favicon.ico  -> /favicon.ico
- favicon/favicon.svg  -> /favicon.svg   (cambia solo entre claro/oscuro según el sistema)
- app/apple-touch-icon.png, app/icon-192.png, app/icon-512.png, app/icon-maskable-192.png, app/icon-maskable-512.png, app/site.webmanifest -> raíz de public/

## <head>
<link rel="icon" href="/favicon.ico" sizes="48x48">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/site.webmanifest">
<meta name="theme-color" content="#2A3964" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#141C26" media="(prefers-color-scheme: dark)">

## Cuál usar
- svg/comedoru-mark-light|dark.svg   logo solo, fondo transparente (sobre #EAEEF3 / #141C26)
- svg/comedoru-mark-auto.svg         logo solo que cambia de color con el modo del sistema (úsalo con <img>)
- svg/comedoru-mark-currentColor.svg  una sola tinta (hereda color del texto)
- svg/comedoru-mark-white.svg        monocromo blanco para el header azul #2A3964
- svg/comedoru-lockup-*.svg          logo + "ComedorU" (texto convertido a curvas, no necesita fuente)
- png/                                mismos logos en 16 a 1024 px
- Favicon: versión simplificada (plato + U, sin cubiertos) porque a 16 px los cubiertos se pierden.
- Con tema de la app (no del sistema): usa <picture> o clases dark: para elegir light/dark.
