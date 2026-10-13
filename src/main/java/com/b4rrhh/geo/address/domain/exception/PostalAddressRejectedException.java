package com.b4rrhh.geo.address.domain.exception;

/**
 * Lo poco que no se guarda ni sin normalizar (ADR-078 §5): una dirección sin país, con un país que
 * no está en el catálogo, con un CP que contradice a la provincia, o con un campo más largo de lo que
 * cabe. El mensaje es el mismo en el empleado, la empresa y el centro: lo escribe la validación
 * única, no cada vertical (backend#155).
 */
public class PostalAddressRejectedException extends RuntimeException {

    public PostalAddressRejectedException(String message) {
        super(message);
    }
}
