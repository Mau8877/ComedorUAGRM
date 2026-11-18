package com.comedoruagrm.backend.common.report;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Graficos sencillos para los PDF (Java2D, sin dependencias extra), con la paleta UAGRM/DUBSS.
 *
 * * Devuelven una imagen que la plantilla coloca en un `image` con `scaleImage="RetainShape"`. Se dibuja al doble
 * * de resolucion (`ESCALA`) para que se vea nitida al imprimir.
 */
public final class GraficosPdf {

    public static final Color AZUL = new Color(0x2A3964);
    public static final Color ROJO = new Color(0xCC100C);
    private static final Color FONDO_BARRA = new Color(0xEAEEF3);
    private static final Color TEXTO = new Color(0x141C26);
    private static final Color TEXTO_SUAVE = new Color(0x5B6A78);
    private static final int ESCALA = 3;

    /** Una barra: etiqueta, valor numerico y texto que se escribe a su derecha. */
    public record Barra(String etiqueta, double valor, String texto) {
    }

    private GraficosPdf() {
    }

    /**
     * Barras horizontales, una por fila de {@code filaAlto} puntos, en un lienzo de {@code ancho} x {@code alto}
     * puntos (el alto cubre {@code filasMaximas}, aunque haya menos barras). La primera barra va en azul y las
     * demas en un azul mas claro; {@code resaltarUltima} pinta la ultima en rojo de acento.
     */
    public static BufferedImage barrasHorizontales(List<Barra> barras, int ancho, int filaAlto, int filasMaximas,
            int anchoEtiqueta, int anchoTexto, boolean resaltarUltima) {
        BufferedImage imagen = new BufferedImage(ancho * ESCALA, filaAlto * filasMaximas * ESCALA,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.scale(ESCALA, ESCALA);
            Font fuente = new Font(Font.SANS_SERIF, Font.PLAIN, 7).deriveFont(8.2f);
            Font negrita = fuente.deriveFont(Font.BOLD);
            FontMetrics m = g.getFontMetrics(fuente);

            int xBarra = anchoEtiqueta + 6;
            int anchoMaximo = ancho - xBarra - anchoTexto;
            double maximo = barras.stream().mapToDouble(Barra::valor).max().orElse(1);
            if (maximo <= 0) {
                maximo = 1;
            }
            int n = Math.min(barras.size(), filasMaximas);
            for (int i = 0; i < n; i++) {
                Barra b = barras.get(i);
                int y = i * filaAlto;
                int centro = y + filaAlto / 2;
                g.setFont(fuente);
                g.setColor(TEXTO);
                g.drawString(recortar(b.etiqueta(), m, anchoEtiqueta), 0, centro + m.getAscent() / 2 - 1);

                int altoBarra = Math.max(6, filaAlto - 12);
                g.setColor(FONDO_BARRA);
                g.fillRoundRect(xBarra, y + (filaAlto - altoBarra) / 2, anchoMaximo, altoBarra, 6, 6);
                int largo = (int) Math.round(anchoMaximo * b.valor() / maximo);
                g.setColor(color(i, n, resaltarUltima));
                if (largo > 0) {
                    g.fillRoundRect(xBarra, y + (filaAlto - altoBarra) / 2, Math.max(largo, 6), altoBarra, 6, 6);
                }
                g.setFont(negrita);
                g.setColor(TEXTO_SUAVE);
                g.drawString(b.texto(), xBarra + anchoMaximo + 6, centro + m.getAscent() / 2 - 1);
            }
        } finally {
            g.dispose();
        }
        return imagen;
    }

    private static Color color(int indice, int total, boolean resaltarUltima) {
        if (resaltarUltima && indice == total - 1 && total > 1) {
            return ROJO;
        }
        float t = total <= 1 ? 0f : (float) indice / (total - 1);
        // Del azul marino de marca a un azul medio, para que se note el orden sin salirse de la paleta.
        int r = (int) (0x2A + (0x5F - 0x2A) * t);
        int gr = (int) (0x39 + (0x84 - 0x39) * t);
        int b = (int) (0x64 + (0xA6 - 0x64) * t);
        return new Color(r, gr, b);
    }

    private static String recortar(String texto, FontMetrics m, int ancho) {
        if (m.stringWidth(texto) <= ancho) {
            return texto;
        }
        String corto = texto;
        while (corto.length() > 1 && m.stringWidth(corto + "…") > ancho) {
            corto = corto.substring(0, corto.length() - 1);
        }
        return corto + "…";
    }
}
