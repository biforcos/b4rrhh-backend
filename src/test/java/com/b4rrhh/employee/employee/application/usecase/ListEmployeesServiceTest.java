package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryItem;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;
import com.b4rrhh.employee.employee.domain.port.EmployeeDirectoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeesServiceTest {

    @Mock
    private EmployeeDirectoryRepository employeeDirectoryRepository;

    private ListEmployeesService service;

    @BeforeEach
    void setUp() {
        service = new ListEmployeesService(employeeDirectoryRepository);
    }

    @Test
    void normalizesFiltersAndAppliesDefaultPagination() {
        List<EmployeeDirectoryItem> expectedItems = List.of(
                new EmployeeDirectoryItem(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "Juan Perez",
                        "ACTIVE",
                        "MADRID"
                )
        );
        // El total viaja tal cual desde el repositorio: el servicio no lo recalcula (backend#18).
        EmployeeDirectoryPage expected = new EmployeeDirectoryPage(expectedItems, 0, 50, 250);

        when(employeeDirectoryRepository.findDirectoryByFilters("JUAN", "ESP", "INTERNAL", "ACTIVE", 0, 50))
                .thenReturn(expected);

        EmployeeDirectoryPage result = service.list(
                new ListEmployeesQuery(" juan ", " esp ", " internal ", " active ", null, null)
        );

        assertEquals(1, result.items().size());
        assertEquals("ESP", result.items().get(0).getRuleSystemCode());
        assertEquals(250, result.total());
        verify(employeeDirectoryRepository).findDirectoryByFilters("JUAN", "ESP", "INTERNAL", "ACTIVE", 0, 50);
    }

    @Test
    void throwsWhenPageIsNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.list(new ListEmployeesQuery(null, null, null, null, -1, 25))
        );

        assertEquals("page must be greater than or equal to 0", exception.getMessage());
    }

    @Test
    void throwsWhenSizeIsOutOfRange() {
        IllegalArgumentException low = assertThrows(
                IllegalArgumentException.class,
                () -> service.list(new ListEmployeesQuery(null, null, null, null, 0, 0))
        );

        IllegalArgumentException high = assertThrows(
                IllegalArgumentException.class,
                () -> service.list(new ListEmployeesQuery(null, null, null, null, 0, 201))
        );

        assertEquals("size must be between 1 and 200", low.getMessage());
        assertEquals("size must be between 1 and 200", high.getMessage());
    }
}