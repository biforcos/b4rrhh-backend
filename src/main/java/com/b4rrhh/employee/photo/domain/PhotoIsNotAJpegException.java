package com.b4rrhh.employee.photo.domain;

public class PhotoIsNotAJpegException extends RuntimeException {

    public PhotoIsNotAJpegException() {
        super("La foto tiene que ser un JPEG");
    }
}
