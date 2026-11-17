package com.comedoruagrm.backend.features.postulaciones.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.web.multipart.MultipartFile;

/**
 * Un archivo que llego en una subida (CU16), ya leido en memoria. `contentType` es lo que DECLARA el cliente:
 * no se confia en el, se comprueba contra el contenido real ({@link ReglasArchivoDocumento}).
 */
public record ArchivoSubido(String nombre, String contentType, byte[] contenido) {

    /** Lee el archivo de la peticion. Un fallo de lectura es un error de infraestructura (500), no de negocio. */
    public static ArchivoSubido de(MultipartFile archivo) {
        try {
            return new ArchivoSubido(archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido", e);
        }
    }
}
