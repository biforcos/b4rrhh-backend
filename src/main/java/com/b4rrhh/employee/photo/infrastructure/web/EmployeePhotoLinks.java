package com.b4rrhh.employee.photo.infrastructure.web;

import com.b4rrhh.employee.photo.domain.EmployeePhotoKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El enlace de la foto de un empleado, tal como lo pinta un {@code <img>} (backend#177):
 * {@code /api/employees/ESP/INTERNAL/EMP000025/photo?v=<uuid>}.
 *
 * <p><b>Relativo, y de la API.</b> Antes era la URL del almacén vista desde el backend que la recibió
 * ({@code http://localhost:9000/...} en local, {@code http://minio:9000/...} en la demo), que no
 * servía a ningún navegador fuera de esa máquina. Un enlace relativo de la API vale en todos los
 * entornos sin configurar nada: lo resuelve el mismo proxy que ya lleva {@code /api} al backend.
 *
 * <p><b>La versión</b> es el UUID de la clave: cambia con cada foto, así que el navegador puede cachear
 * un año sin servir nunca una foto vieja, y es además la llave con la que el {@code GET} sin token
 * acepta servirla.
 */
@Component
public class EmployeePhotoLinks {

    private final String contextPath;

    public EmployeePhotoLinks(@Value("${server.servlet.context-path:}") String contextPath) {
        this.contextPath = contextPath;
    }

    /** {@code null} si el empleado no tiene foto: nulo es nulo, no un enlace a nada. */
    public String linkFor(String ruleSystemCode, String employeeTypeCode, String employeeNumber, String photoKey) {
        if (photoKey == null) {
            return null;
        }
        return contextPath + "/employees/" + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber
                + "/photo?v=" + new EmployeePhotoKey(photoKey).version();
    }
}
