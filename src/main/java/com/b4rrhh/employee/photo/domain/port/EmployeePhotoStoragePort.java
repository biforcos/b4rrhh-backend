package com.b4rrhh.employee.photo.domain.port;

import java.util.Optional;

/**
 * El almacén de las fotos (backend#177). Sólo bytes por clave: ninguna URL sale de aquí, porque desde
 * fuera del backend el almacén no se ve. La foto la sirve la API.
 */
public interface EmployeePhotoStoragePort {

    void store(String objectKey, byte[] jpeg);

    /** Vacío si el objeto no está; una avería del almacén no es vacío, es una excepción. */
    Optional<byte[]> retrieve(String objectKey);

    void delete(String objectKey);
}
