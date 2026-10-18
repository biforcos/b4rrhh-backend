package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoUpdatePort;
import com.b4rrhh.employee.photo.domain.EmployeeNotFoundForPhotoException;
import com.b4rrhh.employee.photo.domain.EmployeePhotoKey;
import com.b4rrhh.employee.photo.domain.PhotoIsNotAJpegException;
import com.b4rrhh.employee.photo.domain.PhotoTooLargeException;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.springframework.stereotype.Component;

/**
 * La foto entra por la API, en un solo viaje (backend#177). Antes eran tres —URL prefirmada, subida
 * directa al almacén y confirmación—, y la URL prefirmada lleva firmado el host del almacén tal como lo
 * ve el backend, que desde el navegador de la demo no es ninguno.
 *
 * <p>El orden importa: primero la foto nueva al almacén, luego la clave a la fila y sólo entonces se
 * borra la vieja. Si algo falla a medias, el empleado se queda con la foto que tenía, no sin ninguna;
 * lo peor que queda es un objeto huérfano en el almacén.
 */
@Component
public class UploadEmployeePhotoService implements UploadEmployeePhotoUseCase {

    /** El diálogo recorta a 800×800 en JPEG, que son unos cientos de KB: dos megas sobran. */
    static final int MAX_BYTES = 2 * 1024 * 1024;

    private final EmployeePhotoLookupPort lookupPort;
    private final EmployeePhotoStoragePort storagePort;
    private final EmployeePhotoUpdatePort updatePort;

    public UploadEmployeePhotoService(
            EmployeePhotoLookupPort lookupPort,
            EmployeePhotoStoragePort storagePort,
            EmployeePhotoUpdatePort updatePort) {
        this.lookupPort = lookupPort;
        this.storagePort = storagePort;
        this.updatePort = updatePort;
    }

    @Override
    public void upload(UploadEmployeePhotoCommand command) {
        EmployeePhotoContext context = lookupPort.findByBusinessKey(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber()
        ).orElseThrow(() -> new EmployeeNotFoundForPhotoException(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber()
        ));

        byte[] jpeg = command.jpeg();
        if (jpeg != null && jpeg.length > MAX_BYTES) {
            throw new PhotoTooLargeException(jpeg.length, MAX_BYTES);
        }
        if (!isJpeg(jpeg)) {
            throw new PhotoIsNotAJpegException();
        }

        EmployeePhotoKey key = EmployeePhotoKey.newFor(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber());
        storagePort.store(key.value(), jpeg);
        updatePort.setPhotoKey(context.employeeId(), key.value());

        if (context.photoKey() != null) {
            storagePort.delete(context.photoKey());
        }
    }

    /** Un JPEG empieza por FF D8 FF. Lo que diga la cabecera Content-Type no basta. */
    private static boolean isJpeg(byte[] bytes) {
        return bytes != null && bytes.length > 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }
}
