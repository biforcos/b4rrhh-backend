package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptLabelCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptLabelUseCase;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabel;
import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabelLanguage;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.port.ConceptLabelRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia el nombre de un concepto en el catalogo ({@code backend#109}).
 *
 * <p><b>No toca ningun recibo ya calculado</b>, y eso no es un olvido: el literal de una linea se
 * congelo cuando se calculo. Un recibo dice lo que el motor calculo, y en cuanto exista el PDF el
 * empleado tiene un gemelo fisico de ese documento fuera del sistema. Lo que cambia aqui es lo que
 * dira el <b>proximo</b> calculo.
 */
@Service
public class UpdateConceptLabelService implements UpdateConceptLabelUseCase {

    private final ConceptLabelRepository conceptLabelRepository;
    private final PayrollConceptRepository conceptRepository;

    public UpdateConceptLabelService(
            ConceptLabelRepository conceptLabelRepository,
            PayrollConceptRepository conceptRepository
    ) {
        this.conceptLabelRepository = conceptLabelRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public PayrollConcept update(UpdateConceptLabelCommand command) {
        PayrollConcept concept = conceptRepository
                .findByBusinessKey(command.ruleSystemCode(), command.conceptCode())
                .orElseThrow(() -> new PayrollConceptNotFoundException(
                        command.ruleSystemCode(), command.conceptCode()));

        conceptLabelRepository.save(
                command.ruleSystemCode(),
                new ConceptLabel(
                        command.conceptCode(),
                        ConceptLabelLanguage.DEFAULT,
                        command.label()));
        return concept;
    }
}
