package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCompanyServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;

    private UpdateCompanyService service;

    @BeforeEach
    void setUp() {
        service = new UpdateCompanyService(
                new CompanyProfileCompanyResolver(ruleEntityRepository),
                new CompanyProfileInputNormalizer(),
                new CompanyProfileCatalogValidator(ruleEntityRepository),
                ruleEntityRepository,
                companyProfileRepository
        );
    }

    @Test
    void updateChangesCompanyAndProfile() {
        RuleEntity company = ruleEntity();
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(company));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP", "Spain", true)));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.of(
                new CompanyProfile("Old legal", null, null, null, null, null, null, null)
        ));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var result = service.update(new UpdateCompanyCommand(
                "ESP",
                "ACME",
                "Acme Renamed",
                "Updated description",
                "Acme Spain SA",
                "A123",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "ESP",
                "4719"
        ));

        assertEquals("Acme Renamed", result.name());
        assertEquals("Acme Spain SA", result.legalName());
        // Y la actividad economica llega al perfil (backend#122). Este servicio pasaba null aqui
        // y editar una empresa le borraba el CNAE; mientras la columna no la leia nadie no se
        // notaba, y desde el #122 le quita a sus empleados la cuota de accidentes de trabajo y
        // hace fallar la corrida siguiente.
        assertEquals("4719", result.cnaeCode());
    }

    /**
     * Guardar la empresa sin decir el CNAE lo borra, y eso es lo que el cliente pide.
     *
     * <p>Esta aqui escrito porque es la mitad incomoda: el campo es opcional y un {@code null}
     * significa «sin actividad declarada», no «no lo cambies». Quien edite una empresa por la API
     * tiene que mandarlo, igual que manda el domicilio.
     */
    @Test
    void updateWithoutACnaeClearsIt() {
        RuleEntity company = ruleEntity();
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(company));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP", "Spain", true)));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.of(
                new CompanyProfile("Old legal", null, null, null, null, null, null, "4719")
        ));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var result = service.update(new UpdateCompanyCommand(
                "ESP", "ACME", "Acme", null, "Acme Spain SA",
                null, null, null, null, null, "ESP", null));

        assertNull(result.cnaeCode());
    }

    // backend#34: antes este caso creaba el perfil de un fallback fabricado al vuelo; ahora
    // una empresa sin perfil es una inconsistencia de datos y el update falla sin escribir.
    // Si esto vuelve a guardar algo, se ha reintroducido el fallback.
    @Test
    void updateFailsWhenProfileMissing() {
        RuleEntity company = ruleEntity();
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(company));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP", "Spain", true)));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.empty());

        assertThrows(RequiredExtensionMissingException.class, () -> service.update(new UpdateCompanyCommand(
                "ESP",
                "ACME",
                "Acme Renamed",
                "Updated description",
                "Acme Spain SA",
                "A123",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "ESP",
                "4719"
        )));

        verify(companyProfileRepository, never()).save(any(Long.class), any(CompanyProfile.class));
    }

    @Test
    void updateMapsNotApplicableAsConflictException() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "ACME", null, null))
                .thenReturn(java.util.List.of(ruleEntity()));

        assertThrows(CompanyNotApplicableException.class, () -> service.update(new UpdateCompanyCommand(
                "ESP",
                "ACME",
                "Acme",
                null,
                "Acme Spain SA",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )));
    }

    private RuleEntity ruleEntity() {
        return ruleEntity(10L, "ESP", "COMPANY", "ACME", "Acme", true);
    }

    private RuleEntity ruleEntity(
            Long id,
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            String name,
            boolean active
    ) {
        return new RuleEntity(
                id,
                ruleSystemCode,
                ruleEntityTypeCode,
                code,
                name,
                null,
                active,
                LocalDate.of(2020, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
