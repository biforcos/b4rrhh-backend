package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.CompanyProfileContext;
import com.b4rrhh.payroll.application.port.CompanyProfileLookupPort;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence.SpringDataCompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompanyProfileLookupAdapter implements CompanyProfileLookupPort {

    private final RuleEntityRepository ruleEntityRepository;
    private final SpringDataCompanyProfileRepository companyProfileRepository;

    public CompanyProfileLookupAdapter(
            RuleEntityRepository ruleEntityRepository,
            SpringDataCompanyProfileRepository companyProfileRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Override
    public Optional<CompanyProfileContext> findByRuleSystemAndCode(String ruleSystemCode, String companyCode) {
        return ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, "COMPANY", companyCode)
                .map(RuleEntity::getId)
                .flatMap(companyProfileRepository::findByCompanyRuleEntityId)
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
