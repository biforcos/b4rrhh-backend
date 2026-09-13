package com.b4rrhh.payroll_engine.metamodel.infrastructure.web;

/**
 * Cuerpo del 400 cuando una vigencia de la reglamentacion no cubre periodos enteros. El
 * mensaje dice que campo, que valor traia y cual se esperaba.
 */
public record MetamodelValidityErrorResponse(String message) {
}
