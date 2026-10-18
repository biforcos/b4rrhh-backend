package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La foto se sirve sin token sólo con la versión vigente: es la llave (backend#177). Cualquier otra
 * pregunta da lo mismo que un empleado sin foto.
 */
@ExtendWith(MockitoExtension.class)
class GetEmployeePhotoServiceTest {

    private static final String VERSION = "0b6d3c1e-6f0a-4c43-9d6a-2b1a3e5f7c90";
    private static final String CLAVE = "photos/ESP/INTERNAL/EMP000025/" + VERSION + ".jpg";
    private static final byte[] BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    @Mock private EmployeePhotoLookupPort lookupPort;
    @Mock private EmployeePhotoStoragePort storagePort;
    @InjectMocks private GetEmployeePhotoService service;

    private void conFoto() {
        lenient().when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, CLAVE)));
        lenient().when(storagePort.retrieve(CLAVE)).thenReturn(Optional.of(BYTES));
    }

    @Test
    void theCurrentVersionGetsThePhoto() {
        conFoto();

        Optional<EmployeePhotoContent> foto = service.get(
                new GetEmployeePhotoQuery("ESP", "INTERNAL", "EMP000025", VERSION));

        assertThat(foto).hasValueSatisfying(c -> {
            assertThat(c.objectKey()).isEqualTo(CLAVE);
            assertThat(c.jpeg()).isEqualTo(BYTES);
        });
    }

    @Test
    void anOldVersionGetsNothing() {
        conFoto();

        assertThat(service.get(new GetEmployeePhotoQuery("ESP", "INTERNAL", "EMP000025",
                "11111111-2222-3333-4444-555555555555"))).isEmpty();
        verify(storagePort, never()).retrieve(anyString());
    }

    @Test
    void withoutAVersionThereIsNothingEvenIfThePhotoExists() {
        conFoto();

        assertThat(service.get(new GetEmployeePhotoQuery("ESP", "INTERNAL", "EMP000025", null))).isEmpty();
        verify(lookupPort, never()).findByBusinessKey(anyString(), anyString(), anyString());
    }

    @Test
    void anEmployeeWithoutAPhotoGetsNothing() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, null)));

        assertThat(service.get(new GetEmployeePhotoQuery("ESP", "INTERNAL", "EMP000025", VERSION))).isEmpty();
    }
}
