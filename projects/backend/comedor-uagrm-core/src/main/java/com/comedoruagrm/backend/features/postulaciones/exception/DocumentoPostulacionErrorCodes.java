package com.comedoruagrm.backend.features.postulaciones.exception;

/**
 * Codigos de error de los documentos de una postulacion (CU16), prefijo DOC. Catalogo vivo en
 * docs/errors/postulaciones/ERRORES_POSTULACIONES.md. Una subida que supera el limite de Spring cae en
 * ERR_SYS_06 y un request multipart mal formado en ERR_SYS_05.
 */
public final class DocumentoPostulacionErrorCodes {

    /** 400 - No llego ningun archivo, o esta vacio. */
    public static final String ERR_DOC_01 = "ERR_DOC_01";

    /** 400 - El archivo supera el tamaño maximo permitido. */
    public static final String ERR_DOC_02 = "ERR_DOC_02";

    /** 400 - El formato no esta permitido, o el contenido no corresponde al formato declarado. */
    public static final String ERR_DOC_03 = "ERR_DOC_03";

    /** 404 - La postulacion no existe, es de otra persona o todavia no se creo. */
    public static final String ERR_DOC_04 = "ERR_DOC_04";

    /** 404 - El tipo de documento no existe o no figura en la lista de la convocatoria. */
    public static final String ERR_DOC_05 = "ERR_DOC_05";

    /** 409 - La postulacion ya fue enviada (no esta en borrador ni devuelta): sus documentos no se modifican. */
    public static final String ERR_DOC_06 = "ERR_DOC_06";

    /** 409 - La convocatoria ya no esta abierta. */
    public static final String ERR_DOC_07 = "ERR_DOC_07";

    /** 409 - Ya hay el maximo de archivos de ese tipo de documento. */
    public static final String ERR_DOC_08 = "ERR_DOC_08";

    /** 404 - El documento adjunto no existe, esta eliminado o no es de esa postulacion. */
    public static final String ERR_DOC_09 = "ERR_DOC_09";

    private DocumentoPostulacionErrorCodes() {
    }
}
