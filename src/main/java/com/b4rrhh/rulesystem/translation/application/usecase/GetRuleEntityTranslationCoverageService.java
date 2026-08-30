package com.b4rrhh.rulesystem.translation.application.usecase;

import com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort;
import com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort.UntranslatedRuleEntity;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.MissingCode;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.TypeCoverage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class GetRuleEntityTranslationCoverageService implements GetRuleEntityTranslationCoverageUseCase {

    private final RuleEntityTranslationCoverageReadPort coverageReadPort;

    public GetRuleEntityTranslationCoverageService(RuleEntityTranslationCoverageReadPort coverageReadPort) {
        this.coverageReadPort = coverageReadPort;
    }

    @Override
    public RuleEntityTranslationCoverage getCoverage(String languageCode) {
        Map<String, Long> totalsByType = new TreeMap<>(coverageReadPort.countRuleEntitiesByType());
        Map<String, List<UntranslatedRuleEntity>> untranslatedByType = coverageReadPort
                .findUntranslated(languageCode)
                .stream()
                .collect(Collectors.groupingBy(UntranslatedRuleEntity::ruleEntityTypeCode));

        List<TypeCoverage> types = totalsByType.entrySet().stream()
                .map(entry -> typeCoverage(
                        entry.getKey(),
                        entry.getValue(),
                        untranslatedByType.getOrDefault(entry.getKey(), List.of())
                ))
                .toList();

        return new RuleEntityTranslationCoverage(languageCode, types);
    }

    private TypeCoverage typeCoverage(String typeCode, long total, List<UntranslatedRuleEntity> untranslated) {
        List<MissingCode> missingCodes = untranslated.stream()
                .map(row -> new MissingCode(row.ruleSystemCode(), row.code(), row.name()))
                .toList();

        return new TypeCoverage(typeCode, total, total - missingCodes.size(), missingCodes.size(), missingCodes);
    }
}
