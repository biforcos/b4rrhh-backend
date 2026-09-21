package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.ReplaceConceptFeedsCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.ReplaceConceptFeedsUseCase;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.MetamodelValidityWindow;
import com.b4rrhh.payroll_engine.object.domain.exception.PayrollObjectNotFoundException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.domain.port.PayrollObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replaces the feed-relation list pointing into a target concept atomically.
 *
 * <p>Validation rules enforced by this service:
 * <ul>
 *   <li>The target concept must exist (404 otherwise).</li>
 *   <li>Every {@code sourceObjectCode} must resolve to an existing payroll object of type
 *       CONCEPT, TABLE or CONSTANT. The type is inferred by searching across the allowed
 *       types in order; the first match wins.</li>
 *   <li>{@code effectiveTo} (when present) must not be before {@code effectiveFrom}
 *       (enforced by the domain model).</li>
 * </ul>
 *
 * <p>Only {@code FEED_BY_SOURCE} mode is supported in this iteration; the
 * {@code feedValue} is left null because the contribution comes from the source object
 * at execution time.
 */
@Service
public class ReplaceConceptFeedsService implements ReplaceConceptFeedsUseCase {

    private static final List<PayrollObjectTypeCode> ALLOWED_SOURCE_TYPES = List.of(
            PayrollObjectTypeCode.CONCEPT,
            PayrollObjectTypeCode.TABLE,
            PayrollObjectTypeCode.CONSTANT
    );

    private final PayrollConceptFeedRelationRepository feedRelationRepository;
    private final PayrollObjectRepository objectRepository;

    public ReplaceConceptFeedsService(
            PayrollConceptFeedRelationRepository feedRelationRepository,
            PayrollObjectRepository objectRepository
    ) {
        this.feedRelationRepository = feedRelationRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    @Transactional
    public List<PayrollConceptFeedRelation> replace(ReplaceConceptFeedsCommand command) {
        String ruleSystemCode = command.ruleSystemCode();
        String conceptCode = command.conceptCode();

        PayrollObject targetObject = objectRepository
                .findByBusinessKey(ruleSystemCode, PayrollObjectTypeCode.CONCEPT, conceptCode)
                .orElseThrow(() -> new PayrollConceptNotFoundException(ruleSystemCode, conceptCode));

        // Se comprueban todas antes de borrar nada: este endpoint reemplaza el juego entero,
        // y una vigencia mal puesta en la tercera no puede dejar al concepto sin las otras dos.
        if (command.items() != null) {
            for (ReplaceConceptFeedsCommand.Item item : command.items()) {
                MetamodelValidityWindow.requireWholePeriods(
                        item.effectiveFrom(),
                        item.effectiveTo(),
                        "la alimentacion de " + conceptCode + " desde " + item.sourceObjectCode(),
                        "effectiveFrom",
                        "effectiveTo"
                );
            }
        }

        feedRelationRepository.deleteAllByRuleSystemCodeAndTargetConceptCode(ruleSystemCode, conceptCode);

        List<PayrollConceptFeedRelation> persisted = new ArrayList<>();
        if (command.items() == null || command.items().isEmpty()) {
            return persisted;
        }

        LocalDateTime now = LocalDateTime.now();
        // createdAt sigue sin zona y updatedAt la lleva desde el backend#116:
        // solo la segunda entra en la comparacion que decide si un recibo sigue vigente.
        Instant ahora = Instant.now();
        for (ReplaceConceptFeedsCommand.Item item : command.items()) {
            PayrollObject sourceObject = resolveSourceObject(ruleSystemCode, item.sourceObjectCode());

            PayrollConceptFeedRelation feed = new PayrollConceptFeedRelation(
                    null,
                    sourceObject,
                    targetObject,
                    FeedMode.FEED_BY_SOURCE,
                    null,
                    item.invertSign(),
                    item.effectiveFrom(),
                    item.effectiveTo(),
                    now,
                    ahora
            );
            persisted.add(feedRelationRepository.save(feed));
        }
        return persisted;
    }

    private PayrollObject resolveSourceObject(String ruleSystemCode, String sourceObjectCode) {
        for (PayrollObjectTypeCode type : ALLOWED_SOURCE_TYPES) {
            Optional<PayrollObject> found = objectRepository
                    .findByBusinessKey(ruleSystemCode, type, sourceObjectCode);
            if (found.isPresent()) {
                return found.get();
            }
        }
        throw new PayrollObjectNotFoundException(
                ruleSystemCode,
                "CONCEPT|TABLE|CONSTANT",
                sourceObjectCode
        );
    }
}
