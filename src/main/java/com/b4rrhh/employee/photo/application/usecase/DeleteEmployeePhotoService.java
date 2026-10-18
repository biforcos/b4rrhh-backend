package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoUpdatePort;
import com.b4rrhh.employee.photo.domain.EmployeeNotFoundForPhotoException;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.springframework.stereotype.Component;

@Component
public class DeleteEmployeePhotoService implements DeleteEmployeePhotoUseCase {

    private final EmployeePhotoLookupPort lookupPort;
    private final EmployeePhotoStoragePort storagePort;
    private final EmployeePhotoUpdatePort updatePort;

    public DeleteEmployeePhotoService(
            EmployeePhotoLookupPort lookupPort,
            EmployeePhotoStoragePort storagePort,
            EmployeePhotoUpdatePort updatePort) {
        this.lookupPort = lookupPort;
        this.storagePort = storagePort;
        this.updatePort = updatePort;
    }

    @Override
    public void delete(DeleteEmployeePhotoCommand command) {
        EmployeePhotoContext context = lookupPort.findByBusinessKey(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber()
        ).orElseThrow(() -> new EmployeeNotFoundForPhotoException(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber()
        ));

        if (context.photoKey() == null) {
            return;
        }

        // La fila primero: si el almacén falla después, queda un objeto huérfano, no un enlace roto.
        updatePort.setPhotoKey(context.employeeId(), null);
        storagePort.delete(context.photoKey());
    }
}
