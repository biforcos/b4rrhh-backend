package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.domain.EmployeePhotoKey;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * La foto, servida por la API (backend#177).
 *
 * <p>Se pide con la versión de la clave y sólo se sirve si es la vigente. Es lo que deja que la pida un
 * {@code <img>}, que no manda token: el enlace con el UUID hace de llave, como la hacía la URL del cubo
 * público, y en cuanto la foto se cambia o se borra el enlace viejo deja de servir. Sin versión, o con
 * otra, la respuesta es la misma que sin foto: no se dice si el empleado existe.
 */
@Component
public class GetEmployeePhotoService implements GetEmployeePhotoUseCase {

    private final EmployeePhotoLookupPort lookupPort;
    private final EmployeePhotoStoragePort storagePort;

    public GetEmployeePhotoService(EmployeePhotoLookupPort lookupPort, EmployeePhotoStoragePort storagePort) {
        this.lookupPort = lookupPort;
        this.storagePort = storagePort;
    }

    @Override
    public Optional<EmployeePhotoContent> get(GetEmployeePhotoQuery query) {
        if (query.version() == null || query.version().isBlank()) {
            return Optional.empty();
        }
        return lookupPort.findByBusinessKey(query.ruleSystemCode(), query.employeeTypeCode(), query.employeeNumber())
                .map(EmployeePhotoContext::photoKey)
                .filter(clave -> new EmployeePhotoKey(clave).version().equals(query.version()))
                .flatMap(clave -> storagePort.retrieve(clave).map(bytes -> new EmployeePhotoContent(clave, bytes)));
    }
}
