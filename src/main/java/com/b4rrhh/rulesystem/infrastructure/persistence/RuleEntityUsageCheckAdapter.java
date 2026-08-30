package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Suma lo que declara cada {@link RuleEntityUsageParticipant}. No sabe qué tablas existen:
 * eso lo dice cada vertical, y Spring trae la lista entera.
 */
@Component
public class RuleEntityUsageCheckAdapter implements RuleEntityUsageCheckPort {

    private final List<RuleEntityUsageParticipant> participants;

    public RuleEntityUsageCheckAdapter(List<RuleEntityUsageParticipant> participants) {
        this.participants = List.copyOf(participants);
    }

    @Override
    public List<RuleEntityReference> findReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        return participants.stream()
                .map(participant -> new RuleEntityReference(
                        participant.resource(),
                        participant.countReferences(ruleSystemCode, ruleEntityTypeCode, code)
                ))
                .filter(reference -> reference.count() > 0)
                .sorted(Comparator.comparing(RuleEntityReference::resource))
                .toList();
    }
}
