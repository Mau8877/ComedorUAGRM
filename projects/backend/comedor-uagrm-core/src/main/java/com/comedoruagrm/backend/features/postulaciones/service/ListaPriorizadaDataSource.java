package com.comedoruagrm.backend.features.postulaciones.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongFunction;

import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * Entrega a JasperReports las filas de la lista priorizada por tramos: pide un tramo, lo recorre y
 * pide el siguiente, de modo que nunca estan las 20 000 filas en memoria a la vez.
 *
 * * `cargador` recibe el desplazamiento (cantidad de filas ya leidas) y devuelve el tramo
 * * siguiente; un tramo vacio termina el recorrido. Los valores salen como texto ya formateado
 * * (la plantilla solo los coloca).
 */
class ListaPriorizadaDataSource implements JRDataSource {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LongFunction<List<PostulacionPriorizadaResponse>> cargador;
    private final ZoneId zona;

    private Iterator<PostulacionPriorizadaResponse> tramo = List.<PostulacionPriorizadaResponse>of().iterator();
    private PostulacionPriorizadaResponse actual;
    private long leidas;
    private boolean terminado;

    ListaPriorizadaDataSource(LongFunction<List<PostulacionPriorizadaResponse>> cargador, ZoneId zona) {
        this.cargador = cargador;
        this.zona = zona;
    }

    @Override
    public boolean next() {
        if (!tramo.hasNext() && !terminado) {
            List<PostulacionPriorizadaResponse> siguiente = cargador.apply(leidas);
            leidas += siguiente.size();
            terminado = siguiente.isEmpty();
            tramo = siguiente.iterator();
        }
        if (!tramo.hasNext()) {
            return false;
        }
        actual = tramo.next();
        return true;
    }

    @Override
    public Object getFieldValue(JRField campo) throws JRException {
        return switch (campo.getName()) {
            case "posicion" -> String.valueOf(actual.posicion());
            case "postulante" -> actual.nombres() + " " + actual.apellidos();
            case "ci" -> actual.ci();
            case "carrera" -> actual.carrera().nombre();
            case "puntaje" -> actual.puntaje().toPlainString();
            case "fechaCalculo" -> actual.fechaCalculo() == null
                    ? "" : FECHA.format(actual.fechaCalculo().atZone(zona));
            default -> throw new JRException("Campo desconocido en la lista priorizada: " + campo.getName());
        };
    }

    /** Cuantas filas se leyeron hasta ahora (para las pruebas). */
    long leidas() {
        return leidas;
    }
}
