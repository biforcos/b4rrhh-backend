package com.b4rrhh.employee.temporal.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimelineCoverageTest {

    @Test
    void onlyMandatoryCoverageRequiresTheGapInvariant() {
        assertTrue(TimelineCoverage.MANDATORY.requiresNoGaps());
        assertFalse(TimelineCoverage.OPTIONAL.requiresNoGaps());
    }
}
