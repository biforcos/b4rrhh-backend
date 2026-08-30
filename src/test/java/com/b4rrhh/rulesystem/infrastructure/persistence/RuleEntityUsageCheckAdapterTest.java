package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEntityUsageCheckAdapterTest {

    @Test
    void sumsWhatEveryParticipantDeclaresAndDropsTheOnesWithNothing() {
        RuleEntityUsageCheckAdapter adapter = new RuleEntityUsageCheckAdapter(List.of(
                participant("work-centers", 3),
                participant("addresses", 0),
                participant("presences", 412)
        ));

        assertThat(adapter.findReferences("ESP", "COMPANY", "ES01")).containsExactly(
                new RuleEntityReference("presences", 412),
                new RuleEntityReference("work-centers", 3)
        );
    }

    @Test
    void isEmptyWithoutParticipantsOrWithoutReferences() {
        assertThat(new RuleEntityUsageCheckAdapter(List.of()).findReferences("ESP", "COMPANY", "ES01")).isEmpty();
        assertThat(new RuleEntityUsageCheckAdapter(List.of(participant("presences", 0)))
                .findReferences("ESP", "COMPANY", "ES01")).isEmpty();
    }

    private static RuleEntityUsageParticipant participant(String resource, long count) {
        return new RuleEntityUsageParticipant() {
            @Override
            public String resource() {
                return resource;
            }

            @Override
            public long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
                return count;
            }
        };
    }
}
