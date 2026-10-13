package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CorrectRuleEntityService implements CorrectRuleEntityUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final NationalLayerGuard nationalLayerGuard;

    public CorrectRuleEntityService(RuleEntityRepository ruleEntityRepository, NationalLayerGuard nationalLayerGuard) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.nationalLayerGuard = nationalLayerGuard;
    }

    @Override
    @Transactional
    public RuleEntity correct(CorrectRuleEntityCommand command) {
        String ruleSystemCode = normalizeRequiredCode("ruleSystemCode", command.ruleSystemCode());
        String ruleEntityTypeCode = normalizeRequiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = normalizeRequiredCode("code", command.code());
        LocalDate startDate = normalizeRequiredStartDate(command.startDate());
        String name = normalizeRequiredName(command.name());
        String description = normalizeDescription(command.description());
        LocalDate endDate = command.endDate();

        RuleEntity existing = ruleEntityRepository.findByBusinessKeyAndStartDate(ruleSystemCode, ruleEntityTypeCode, code, startDate)
                .orElseThrow(() -> new RuleEntityNotFoundException(ruleSystemCode, ruleEntityTypeCode, code, startDate));
        nationalLayerGuard.requireDefinedInNationalLayer(ruleSystemCode, existing);

        existing.correct(name, description, endDate);

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

    private String normalizeRequiredName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }

        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("name exceeds max length 100");
        }

        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > 500) {
            throw new IllegalArgumentException("description exceeds max length 500");
        }

        return normalized;
    }
}
