package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.application.port.RuleSystemLastChangeLookupPort;
import com.b4rrhh.payroll.domain.model.Payroll;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PayrollRuleFreshnessService implements PayrollRuleFreshness {

    private final RuleSystemLastChangeLookupPort lastChangeLookup;

    public PayrollRuleFreshnessService(RuleSystemLastChangeLookupPort lastChangeLookup) {
        this.lastChangeLookup = lastChangeLookup;
    }

    @Override
    public boolean rulesChangedSinceCalculation(Payroll payroll) {
        LocalDateTime calculatedAt = payroll.getCalculatedAt();
        if (calculatedAt == null) {
            // Un recibo sin fecha de calculo no tiene contra que comparar. Decir que si seria
            // inventarse un cambio, y decir que no seria afirmar que esta al dia: no se marca.
            return false;
        }
        return lastChangeLookup.lastChangedAt(payroll.getRuleSystemCode())
                .map(calculatedAt::isBefore)
                .orElse(false);
    }
}
