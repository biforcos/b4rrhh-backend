package com.b4rrhh.employee.photo.infrastructure.web;

import com.b4rrhh.employee.photo.domain.EmployeeNotFoundForPhotoException;
import com.b4rrhh.employee.photo.domain.EmployeePhotoStorageUnavailableException;
import com.b4rrhh.employee.photo.domain.PhotoIsNotAJpegException;
import com.b4rrhh.employee.photo.domain.PhotoTooLargeException;
import com.b4rrhh.employee.photo.infrastructure.web.dto.EmployeePhotoErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeePhotoController.class)
public class EmployeePhotoExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundForPhotoException.class)
    public ResponseEntity<EmployeePhotoErrorResponse> handleEmployeeNotFound(
            EmployeeNotFoundForPhotoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EmployeePhotoErrorResponse("EMPLOYEE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PhotoIsNotAJpegException.class)
    public ResponseEntity<EmployeePhotoErrorResponse> handleNotAJpeg(PhotoIsNotAJpegException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new EmployeePhotoErrorResponse("PHOTO_NOT_JPEG", ex.getMessage()));
    }

    @ExceptionHandler(PhotoTooLargeException.class)
    public ResponseEntity<EmployeePhotoErrorResponse> handleTooLarge(PhotoTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new EmployeePhotoErrorResponse("PHOTO_TOO_LARGE", ex.getMessage()));
    }

    @ExceptionHandler(EmployeePhotoStorageUnavailableException.class)
    public ResponseEntity<EmployeePhotoErrorResponse> handleStorageUnavailable(
            EmployeePhotoStorageUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new EmployeePhotoErrorResponse("PHOTO_STORAGE_UNAVAILABLE", ex.getMessage()));
    }
}
