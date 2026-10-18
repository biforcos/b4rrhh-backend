package com.b4rrhh.employee.photo.application.usecase;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoUpdatePort;
import com.b4rrhh.employee.photo.domain.EmployeeNotFoundForPhotoException;
import com.b4rrhh.employee.photo.domain.PhotoIsNotAJpegException;
import com.b4rrhh.employee.photo.domain.PhotoTooLargeException;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La subida en un viaje (backend#177): la nueva primero, luego la fila, luego se borra la vieja. */
@ExtendWith(MockitoExtension.class)
class UploadEmployeePhotoServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    private static final String VIEJA = "photos/ESP/INTERNAL/EMP000025/0b6d3c1e-6f0a-4c43-9d6a-2b1a3e5f7c90.jpg";

    @Mock private EmployeePhotoLookupPort lookupPort;
    @Mock private EmployeePhotoStoragePort storagePort;
    @Mock private EmployeePhotoUpdatePort updatePort;
    @InjectMocks private UploadEmployeePhotoService service;

    @Test
    void storesTheNewPhotoUnderANewKeyThenPointsTheRowAtItThenDeletesTheOldOne() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, VIEJA)));

        service.upload(new UploadEmployeePhotoCommand("ESP", "INTERNAL", "EMP000025", JPEG));

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        InOrder orden = inOrder(storagePort, updatePort);
        orden.verify(storagePort).store(clave.capture(), eq(JPEG));
        orden.verify(updatePort).setPhotoKey(7L, clave.getValue());
        orden.verify(storagePort).delete(VIEJA);
        assertThat(clave.getValue())
                .matches("photos/ESP/INTERNAL/EMP000025/[0-9a-f-]{36}\\.jpg")
                .isNotEqualTo(VIEJA);
    }

    @Test
    void withoutAPreviousPhotoNothingIsDeleted() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, null)));

        service.upload(new UploadEmployeePhotoCommand("ESP", "INTERNAL", "EMP000025", JPEG));

        verify(storagePort, never()).delete(anyString());
    }

    @Test
    void whatIsNotAJpegIsRefusedWhateverItsContentTypeSaid() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, null)));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2};

        assertThatThrownBy(() -> service.upload(new UploadEmployeePhotoCommand("ESP", "INTERNAL", "EMP000025", png)))
                .isInstanceOf(PhotoIsNotAJpegException.class);
        verify(storagePort, never()).store(anyString(), any());
    }

    @Test
    void moreThanTwoMegabytesIsRefused() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP000025"))
                .thenReturn(Optional.of(new EmployeePhotoContext(7L, null)));
        byte[] grande = new byte[UploadEmployeePhotoService.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

        assertThatThrownBy(() -> service.upload(new UploadEmployeePhotoCommand("ESP", "INTERNAL", "EMP000025", grande)))
                .isInstanceOf(PhotoTooLargeException.class);
        verify(updatePort, never()).setPhotoKey(anyLong(), any());
    }

    @Test
    void anUnknownEmployeeIsNotFound() {
        when(lookupPort.findByBusinessKey("ESP", "INTERNAL", "EMP999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(new UploadEmployeePhotoCommand("ESP", "INTERNAL", "EMP999999", JPEG)))
                .isInstanceOf(EmployeeNotFoundForPhotoException.class);
    }
}
