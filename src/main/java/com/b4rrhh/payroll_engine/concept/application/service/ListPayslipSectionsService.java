package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.ListPayslipSectionsUseCase;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPayslipSectionsService implements ListPayslipSectionsUseCase {

    private final PayslipSectionRepository payslipSectionRepository;

    public ListPayslipSectionsService(PayslipSectionRepository payslipSectionRepository) {
        this.payslipSectionRepository = payslipSectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipSection> listByRuleSystemCode(String ruleSystemCode) {
        return payslipSectionRepository.findByRuleSystemCode(ruleSystemCode);
    }
}
