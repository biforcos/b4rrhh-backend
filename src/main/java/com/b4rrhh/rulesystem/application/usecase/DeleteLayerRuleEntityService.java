package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.domain.exception.LayerNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Borra la fila de una capa si nada la usa <b>desde ninguna</b> de las reglamentaciones que la
 * montan (frontend#131).
 *
 * <p>Las referencias se cuentan por reglamentación ({@link RuleEntityUsageCheckPort}), así que una
 * capa compartida se pregunta a cada una y se suma: un país de INT que sólo usa un empleado de FRA
 * está en uso, aunque ESP no lo use.</p>
 */
@Service
public class DeleteLayerRuleEntityService implements DeleteLayerRuleEntityUseCase {

    private final LayerRepository layerRepository;
    private final RuleEntityRepository ruleEntityRepository;
    private final RuleEntityUsageCheckPort ruleEntityUsageCheckPort;

    public DeleteLayerRuleEntityService(
            LayerRepository layerRepository,
            RuleEntityRepository ruleEntityRepository,
            RuleEntityUsageCheckPort ruleEntityUsageCheckPort
    ) {
        this.layerRepository = layerRepository;
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleEntityUsageCheckPort = ruleEntityUsageCheckPort;
    }

    @Override
    @Transactional
    public void delete(DeleteLayerRuleEntityCommand command) {
        String layerCode = LayerRuleEntityInput.requiredCode("layerCode", command.layerCode());
        String ruleEntityTypeCode = LayerRuleEntityInput.requiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = LayerRuleEntityInput.requiredCode("code", command.code());
        LocalDate startDate = LayerRuleEntityInput.requiredDate("startDate", command.startDate());

        Layer layer = layerRepository.findByCode(layerCode).orElseThrow(() -> new LayerNotFoundException(layerCode));
        ruleEntityRepository.findInLayerByBusinessKeyAndStartDate(layerCode, ruleEntityTypeCode, code, startDate)
                .orElseThrow(() -> new RuleEntityNotFoundException(layerCode, ruleEntityTypeCode, code, startDate));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String ruleSystemCode : layer.mountedBy()) {
            ruleEntityUsageCheckPort.findReferences(ruleSystemCode, ruleEntityTypeCode, code)
                    .forEach(reference -> counts.merge(reference.resource(), reference.count(), Long::sum));
        }
        if (!counts.isEmpty()) {
            List<RuleEntityReference> references = counts.entrySet().stream()
                    .map(entry -> new RuleEntityReference(entry.getKey(), entry.getValue()))
                    .toList();
            throw new RuleEntityInUseException(layerCode, ruleEntityTypeCode, code, references);
        }

        ruleEntityRepository.deleteInLayerByBusinessKeyAndStartDate(layerCode, ruleEntityTypeCode, code, startDate);
    }
}
