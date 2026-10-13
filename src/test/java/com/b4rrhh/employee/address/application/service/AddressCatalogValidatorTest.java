package com.b4rrhh.employee.address.application.service;

import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressCatalogValidatorTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private AddressCatalogValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AddressCatalogValidator(ruleEntityRepository);
    }

    @Test
    void normalizesRequiredCode() {
        assertEquals("HOME", validator.normalizeRequiredCode("addressTypeCode", " home "));
    }

    @Test
    void validatesInclusiveEndDateSemantics() {
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME"))
                .thenReturn(Optional.of(activeRuleEntityWithDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))));

        // Inclusive end date: referenceDate == endDate is valid.
        assertDoesNotThrow(() -> validator.validateAddressTypeCode("ESP", "HOME", LocalDate.of(2026, 1, 31)));
    }

    @Test
    void rejectsReferenceDateAfterEndDate() {
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME"))
                .thenReturn(Optional.of(activeRuleEntityWithDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))));

        assertThrows(
                AddressCatalogValueInvalidException.class,
                () -> validator.validateAddressTypeCode("ESP", "HOME", LocalDate.of(2026, 2, 1))
        );
    }

    private RuleEntity activeRuleEntityWithDates(LocalDate startDate, LocalDate endDate) {
        return new RuleEntity(
                1L,
                "ESP",
                "EMPLOYEE_ADDRESS_TYPE",
                "HOME",
                "Home",
                null,
                true,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private RuleEntity activeCountryRuleEntityWithDates(String code, LocalDate startDate, LocalDate endDate) {
        return new RuleEntity(
                2L,
                "ESP",
                "COUNTRY",
                code,
                "Country",
                null,
                true,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
