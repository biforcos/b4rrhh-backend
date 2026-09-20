package com.b4rrhh.payroll_engine.concept.application.usecase;

/** Poner o cambiar el nombre de un concepto del motor ({@code backend#109}). */
public record UpdateConceptLabelCommand(
        String ruleSystemCode,
        String conceptCode,
        String label
) {}
