package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CloseRuleEntityService implements CloseRuleEntityUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final NationalLayerGuard nationalLayerGuard;

    public CloseRuleEntityService(RuleEntityRepository ruleEntityRepository, NationalLayerGuard nationalLayerGuard) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.nationalLayerGuard = nationalLayerGuard;
    }

    @Override
    @Transactional
    public RuleEntity close(CloseRuleEntityCommand command) {
        String ruleSystemCode = normalizeRequiredCode("ruleSystemCode", command.ruleSystemCode());
        String ruleEntityTypeCode = normalizeRequiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = normalizeRequiredCode("code", command.code());
        LocalDate startDate = normalizeRequiredStartDate(command.startDate());
        LocalDate endDate = normalizeRequiredEndDate(command.endDate());

        RuleEntity existing = ruleEntityRepository.findByBusinessKeyAndStartDate(ruleSystemCode, ruleEntityTypeCode, code, startDate)
                .orElseThrow(() -> new RuleEntityNotFoundException(ruleSystemCode, ruleEntityTypeCode, code, startDate));
        nationalLayerGuard.requireDefinedInNationalLayer(ruleSystemCode, existing);

        existing.close(endDate);

        boolean overlap = ruleEntityRepository.existsOverlapExcludingStartDate(
                ruleSystemCode,
                ruleEntityTypeCode,
                code,
                existing.getStartDate(),
                existing.getEndDate(),
                existing.getStartDate()
        );
        if (overlap) {
            throw new RuleEntityOverlapException(ruleSystemCode, ruleEntityTypeCode, code);
        }

        return ruleEntityRepository.save(existing);
    }

    private String normalizeRequiredCode(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim().toUpperCase();
    }

    private LocalDate normalizeRequiredStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return startDate;
    }

    private LocalDate normalizeRequiredEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }

        return endDate;
    }
}
