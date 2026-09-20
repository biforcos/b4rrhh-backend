package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.payroll_engine.concept.application.usecase.ListPayslipSectionsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Las agrupaciones del recibo, servidas ({@code backend#109}).
 *
 * <p>Existen para que el PDF del paso 2 y la pantalla no tengan que deducir los bloques de un
 * rango de codigos. Una linea ya trae su {@code payslipSectionCode} congelado; esto es lo que da
 * a ese codigo un nombre y un orden.
 */
@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/payslip-sections")
public class PayslipSectionController {

    private final ListPayslipSectionsUseCase listPayslipSectionsUseCase;

    public PayslipSectionController(ListPayslipSectionsUseCase listPayslipSectionsUseCase) {
        this.listPayslipSectionsUseCase = listPayslipSectionsUseCase;
    }

    @GetMapping
    public List<PayslipSectionResponse> list(@PathVariable String ruleSystemCode) {
        return listPayslipSectionsUseCase.listByRuleSystemCode(ruleSystemCode).stream()
                .map(section -> new PayslipSectionResponse(
                        section.sectionCode(), section.label(), section.displayOrder()))
                .toList();
    }
}
