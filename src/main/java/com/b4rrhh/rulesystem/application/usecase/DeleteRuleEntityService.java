package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeleteRuleEntityService implements DeleteRuleEntityUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final RuleEntityUsageCheckPort ruleEntityUsageCheckPort;

    public DeleteRuleEntityService(
            RuleEntityRepository ruleEntityRepository,
            RuleEntityUsageCheckPort ruleEntityUsageCheckPort
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleEntityUsageCheckPort = ruleEntityUsageCheckPort;
    }

    @Override
    @Transactional
    public void delete(DeleteRuleEntityCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedRuleEntityTypeCode = normalizeRuleEntityTypeCode(command.ruleEntityTypeCode());
        String normalizedCode = normalizeCode(command.code());
        LocalDate startDate = normalizeStartDate(command.startDate());

        ruleEntityRepository.findByBusinessKeyAndStartDate(
                        normalizedRuleSystemCode,
                        normalizedRuleEntityTypeCode,
                        normalizedCode,
                        startDate
                )
                .orElseThrow(() -> new RuleEntityNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedRuleEntityTypeCode,
                        normalizedCode,
                        startDate
                ));

        List<RuleEntityReference> references = ruleEntityUsageCheckPort.findReferences(
                normalizedRuleSystemCode,
                normalizedRuleEntityTypeCode,
                normalizedCode
        );
        if (!references.isEmpty()) {
            throw new RuleEntityInUseException(
                    normalizedRuleSystemCode,
                    normalizedRuleEntityTypeCode,
                    normalizedCode,
                    references
            );
        }

        ruleEntityRepository.deleteByBusinessKeyAndStartDate(
                normalizedRuleSystemCode,
                normalizedRuleEntityTypeCode,
                normalizedCode,
                startDate
        );
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeRuleEntityTypeCode(String ruleEntityTypeCode) {
        if (ruleEntityTypeCode == null || ruleEntityTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleEntityTypeCode is required");
        }

        return ruleEntityTypeCode.trim().toUpperCase();
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code is required");
        }

        return code.trim().toUpperCase();
    }

    private LocalDate normalizeStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return startDate;
    }
}
