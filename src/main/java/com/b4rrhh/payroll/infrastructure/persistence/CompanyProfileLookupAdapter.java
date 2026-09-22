package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.CompanyProfileContext;
import com.b4rrhh.payroll.application.port.CompanyProfileLookupPort;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence.SpringDataCompanyProfileRepository;
import com.b4rrhh.rulesystem.infrastructure.persistence.SpringDataRuleEntityRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompanyProfileLookupAdapter implements CompanyProfileLookupPort {

    private final SpringDataRuleEntityRepository ruleEntityRepository;
    private final SpringDataCompanyProfileRepository companyProfileRepository;

    public CompanyProfileLookupAdapter(
            SpringDataRuleEntityRepository ruleEntityRepository,
            SpringDataCompanyProfileRepository companyProfileRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Override
    public Optional<CompanyProfileContext> findByRuleSystemAndCode(String ruleSystemCode, String companyCode) {
        return ruleEntityRepository
                .findByRuleSystemCodeAndRuleEntityTypeCodeAndCode(ruleSystemCode, "COMPANY", companyCode)
                .flatMap(entity -> companyProfileRepository.findByCompanyRuleEntityId(entity.getId()))
                .map(cp -> new CompanyProfileContext(
                        cp.getLegalName(),
                        cp.getTaxIdentifier(),
                        cp.getStreet(),
                        cp.getCity(),
                        cp.getPostalCode(),
                        cp.getCnaeCode()
                ));
    }
}
