package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoUpdatePort;
import com.b4rrhh.employee.photo.domain.EmployeeNotFoundForPhotoException;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteEmployeePhotoServiceTest {

    private static final String CLAVE = "photos/ESP/EMP/00001/0b6d3c1e-6f0a-4c43-9d6a-2b1a3e5f7c90.jpg";

    @Mock private EmployeePhotoLookupPort lookupPort;
    @Mock private EmployeePhotoStoragePort storagePort;
    @Mock private EmployeePhotoUpdatePort updatePort;
    @InjectMocks private DeleteEmployeePhotoService service;

    @Test
    void clears_the_key_first_and_then_deletes_the_object() {
        when(lookupPort.findByBusinessKey("ESP", "EMP", "00001"))
                .thenReturn(Optional.of(new EmployeePhotoContext(1L, CLAVE)));

        service.delete(new DeleteEmployeePhotoCommand("ESP", "EMP", "00001"));

        InOrder orden = inOrder(updatePort, storagePort);
        orden.verify(updatePort).setPhotoKey(1L, null);
        orden.verify(storagePort).delete(CLAVE);
    }

    @Test
    void does_nothing_when_employee_has_no_photo() {
        when(lookupPort.findByBusinessKey("ESP", "EMP", "00001"))
                .thenReturn(Optional.of(new EmployeePhotoContext(1L, null)));

        service.delete(new DeleteEmployeePhotoCommand("ESP", "EMP", "00001"));

        verify(storagePort, never()).delete(anyString());
        verify(updatePort, never()).setPhotoKey(anyLong(), any());
    }

    @Test
    void throws_when_employee_not_found() {
        when(lookupPort.findByBusinessKey("ESP", "EMP", "99999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(
                new DeleteEmployeePhotoCommand("ESP", "EMP", "99999")))
                .isInstanceOf(EmployeeNotFoundForPhotoException.class);
    }
}
