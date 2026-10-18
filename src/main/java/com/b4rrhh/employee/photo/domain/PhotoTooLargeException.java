package com.b4rrhh.employee.photo.domain;

public class PhotoTooLargeException extends RuntimeException {

    public PhotoTooLargeException(int bytes, int maxBytes) {
        super("La foto ocupa " + bytes + " bytes y el máximo es " + maxBytes);
    }
}
