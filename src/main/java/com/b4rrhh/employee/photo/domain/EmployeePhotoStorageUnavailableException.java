package com.b4rrhh.employee.photo.domain;

/** El almacén de fotos no contesta. No es «no hay foto»: eso es un vacío, esto una avería. */
public class EmployeePhotoStorageUnavailableException extends RuntimeException {

    public EmployeePhotoStorageUnavailableException(String objectKey, Throwable cause) {
        super("El almacén de fotos no está disponible (" + objectKey + ")", cause);
    }
}
