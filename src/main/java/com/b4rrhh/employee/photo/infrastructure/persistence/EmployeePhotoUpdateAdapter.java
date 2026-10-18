package com.b4rrhh.employee.photo.infrastructure.persistence;

import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeeEntity;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoUpdatePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EmployeePhotoUpdateAdapter implements EmployeePhotoUpdatePort {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Sobre la entidad gestionada y no con un {@code UPDATE} masivo, que es lo que había. El masivo
     * se salta el contexto de persistencia: en la misma petición el empleado ya está cargado (lo
     * cargó la búsqueda), y quien lo leía después —el controlador, para devolverlo— recibía la clave
     * vieja. La respuesta de la subida enseñaba la foto de antes (backend#177).
     *
     * <p>Y con {@code flush}: los servicios cuentan con que la fila está escrita antes de tocar el
     * almacén (la nueva se apunta antes de borrar la vieja; al borrar, la fila se limpia antes que el
     * objeto). Si esto se une a una transacción de fuera, sin el {@code flush} el cambio esperaría a
     * su commit y ese orden sería sólo aparente.
     */
    @Override
    @Transactional
    public void setPhotoKey(Long employeeId, String photoKey) {
        EmployeeEntity employee = entityManager.find(EmployeeEntity.class, employeeId);
        employee.setPhotoKey(photoKey);
        entityManager.flush();
    }
}
