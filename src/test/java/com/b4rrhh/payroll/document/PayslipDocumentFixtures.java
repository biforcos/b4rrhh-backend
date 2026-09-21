package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dos recibos de la semilla de la demo, copiados tal cual ({@code backend#112}).
 *
 * <p><b>Son datos medidos, no inventados.</b> Las lineas, los literales, las naturalezas y las
 * secciones salen de {@code b4rrhh_semilla} recalculada con la {@code V141}; las fotos de contexto, de
 * {@code payroll.payroll_context_snapshot} del mismo recibo. Un recibo de laboratorio con tres
 * lineas redondas no habria enseñado ninguna de las dos cosas que esto tiene que enseñar: que el
 * bloque de bases sale, y que el literal mas largo del catalogo —«Mecanismo de equidad
 * intergeneracional (aportacion empresarial)», sesenta y dos caracteres— cabe en su columna.
 *
 * <p>Una nota sobre el recuento: el issue del {@code backend#112} dice que {@code EMP000001}
 * tiene quince lineas. Tenia quince <b>antes</b> de la {@code V139}; con el recuadro de bases
 * pasaron a diecisiete, y con el total de la aportacion empresarial de la {@code V141}
 * ({@code backend#114}) son <b>dieciocho</b>. El mas largo de la semilla
 * ({@code EMP000005}) tiene diecinueve.
 */
final class PayslipDocumentFixtures {

    /** Las cinco secciones del modelo oficial espanol, como las declara la {@code V138}. */
    static List<PayslipSection> seccionesDelModeloOficial() {
        return List.of(
                new PayslipSection("DEVENGOS", "Devengos", 10),
                new PayslipSection("DEDUCCIONES", "Deducciones", 20),
                new PayslipSection("LIQUIDO", "Liquido total a percibir", 30),
                new PayslipSection("BASES", "Determinacion de las bases de cotizacion", 40),
                new PayslipSection("APORTACION_EMPRESARIAL", "Aportacion empresarial", 50)
        );
    }

    /** Dieciocho lineas, cinco bloques, 822,97 € de liquido. Presencia 2: es un readmitido. */
    static Payroll emp000001() {
        return recibo("EMP000001", 2, PayrollStatus.CALCULATED, """
                101|101|SALARIO_BASE|Salario base|15|47.5|712.5|EARNING|101|DEVENGOS
                102|101|SALARIO_BASE|Salario base|15|23.75|356.25|EARNING|101|DEVENGOS
                103|B_CC|BASE_COTIZACION_COTIZ|Base de cotizacion por contingencias comunes|||1323|BASE|410|BASES
                104|B01|BASE_COTIZABLE|Base cotizable|||1068.75|BASE|430|BASES
                105|700|CC_TRABAJADOR|Contingencias comunes|1323|4.7|62.18|DEDUCTION|700|DEDUCCIONES
                106|701|FP_TRABAJADOR|Formacion profesional|1323|0.1|1.32|DEDUCTION|701|DEDUCCIONES
                107|702|MEI_TRABAJADOR|Mecanismo de equidad intergeneracional|1323|0.11|1.46|DEDUCTION|702|DEDUCCIONES
                108|703|DESEMPLEO_TRABAJADOR|Desempleo|1323|1.55|20.51|DEDUCTION|703|DEDUCCIONES
                109|720|SS_CC_EMPRESARIO|Contingencias comunes (aportacion empresarial)|1323|23.6|312.23|INFORMATIONAL|720|APORTACION_EMPRESARIAL
                110|721|SS_DESEMPLEO_EMPRESARIO|Desempleo (aportacion empresarial)|1323|7.05|93.27|INFORMATIONAL|721|APORTACION_EMPRESARIAL
                111|722|SS_FP_EMPRESARIO|Formacion profesional (aportacion empresarial)|1323|0.6|7.94|INFORMATIONAL|722|APORTACION_EMPRESARIAL
                112|723|SS_FOGASA_EMPRESARIO|FOGASA (aportacion empresarial)|1323|0.2|2.65|INFORMATIONAL|723|APORTACION_EMPRESARIAL
                113|724|SS_MEI_EMPRESARIO|Mecanismo de equidad intergeneracional (aportacion empresarial)|1323|0.58|7.67|INFORMATIONAL|724|APORTACION_EMPRESARIAL
                114|725|TOTAL_APORTACION_EMPRESARIAL|Total aportacion empresarial|||423.76|TOTAL_EMPLOYER_CONTRIBUTION|725|APORTACION_EMPRESARIAL
                115|800|RETENCION_IRPF|Retencion IRPF|1068.75|15|160.31|DEDUCTION|800|DEDUCCIONES
                116|970|TOTAL_DEVENGOS|Total devengado|||1068.75|TOTAL_EARNING|970|DEVENGOS
                117|980|TOTAL_DEDUCCIONES|Total a deducir|||245.78|TOTAL_DEDUCTION|980|DEDUCCIONES
                118|990|LIQUIDO_A_PAGAR|Liquido total a percibir|||822.97|NET_PAY|990|LIQUIDO
                """);
    }

    /**
     * El mismo recibo con otro nombre en la linea del 101.
     *
     * <p>Es lo que un cambio de literal en el catalogo produce <b>despues de recalcular</b>: el
     * nombre viaja congelado en la linea desde el {@code backend#109}, asi que un recibo que no se
     * recalcula no lo ve cambiar. Sirve para mirar los dos lados del criterio 2.
     */
    static Payroll emp000001ConLiteral(String literalDelSalarioBase) {
        Payroll original = emp000001();
        List<PayrollConcept> conNombreNuevo = new ArrayList<>();
        for (PayrollConcept concept : original.getConcepts()) {
            conNombreNuevo.add("101".equals(concept.getConceptCode())
                    ? new PayrollConcept(
                            concept.getLineNumber(), concept.getConceptCode(),
                            concept.getConceptMnemonic(), literalDelSalarioBase,
                            concept.getAmount(), concept.getQuantity(), concept.getRate(),
                            concept.getConceptNatureCode(), concept.getOriginPeriodCode(),
                            concept.getDisplayOrder(), concept.getMergedStepCount(),
                            concept.getPayslipSectionCode())
                    : concept);
        }
        return Payroll.rehydrate(
                original.getId(), original.getRuleSystemCode(), original.getEmployeeTypeCode(),
                original.getEmployeeNumber(), original.getPayrollPeriodCode(),
                original.getPayrollTypeCode(), original.getPresenceNumber(), original.getStatus(),
                original.getStatusReasonCode(), original.getCalculatedAt(),
                original.getCalculationEngineCode(), original.getCalculationEngineVersion(),
                original.getRunId(), original.getWarnings(), conNombreNuevo,
                original.getContextSnapshots(), original.getSegments(),
                original.getCreatedAt(), original.getUpdatedAt());
    }

    /** El mas largo de la semilla: diecinueve lineas, con horas extraordinarias. */
    static Payroll emp000005() {
        return recibo("EMP000005", 1, PayrollStatus.CALCULATED, """
                101|101|SALARIO_BASE|Salario base|15|47.5|712.5|EARNING|101|DEVENGOS
                102|101|SALARIO_BASE|Salario base|15|23.75|356.25|EARNING|101|DEVENGOS
                103|102|IMPORTE_HORAS_EXTRA|Horas extraordinarias|15|5.94|89.1|EARNING|102|DEVENGOS
                104|B_CC|BASE_COTIZACION_COTIZ|Base de cotizacion por contingencias comunes|||1323|BASE|410|BASES
                105|B01|BASE_COTIZABLE|Base cotizable|||1157.85|BASE|430|BASES
                106|700|CC_TRABAJADOR|Contingencias comunes|1323|4.7|62.18|DEDUCTION|700|DEDUCCIONES
                107|701|FP_TRABAJADOR|Formacion profesional|1323|0.1|1.32|DEDUCTION|701|DEDUCCIONES
                108|702|MEI_TRABAJADOR|Mecanismo de equidad intergeneracional|1323|0.11|1.46|DEDUCTION|702|DEDUCCIONES
                109|703|DESEMPLEO_TRABAJADOR|Desempleo|1323|1.55|20.51|DEDUCTION|703|DEDUCCIONES
                110|720|SS_CC_EMPRESARIO|Contingencias comunes (aportacion empresarial)|1323|23.6|312.23|INFORMATIONAL|720|APORTACION_EMPRESARIAL
                111|721|SS_DESEMPLEO_EMPRESARIO|Desempleo (aportacion empresarial)|1323|7.05|93.27|INFORMATIONAL|721|APORTACION_EMPRESARIAL
                112|722|SS_FP_EMPRESARIO|Formacion profesional (aportacion empresarial)|1323|0.6|7.94|INFORMATIONAL|722|APORTACION_EMPRESARIAL
                113|723|SS_FOGASA_EMPRESARIO|FOGASA (aportacion empresarial)|1323|0.2|2.65|INFORMATIONAL|723|APORTACION_EMPRESARIAL
                114|724|SS_MEI_EMPRESARIO|Mecanismo de equidad intergeneracional (aportacion empresarial)|1323|0.58|7.67|INFORMATIONAL|724|APORTACION_EMPRESARIAL
                115|725|TOTAL_APORTACION_EMPRESARIAL|Total aportacion empresarial|||423.76|TOTAL_EMPLOYER_CONTRIBUTION|725|APORTACION_EMPRESARIAL
                116|800|RETENCION_IRPF|Retencion IRPF|1157.85|15|173.68|DEDUCTION|800|DEDUCCIONES
                117|970|TOTAL_DEVENGOS|Total devengado|||1157.85|TOTAL_EARNING|970|DEVENGOS
                118|980|TOTAL_DEDUCCIONES|Total a deducir|||259.15|TOTAL_DEDUCTION|980|DEDUCCIONES
                119|990|LIQUIDO_A_PAGAR|Liquido total a percibir|||987.8|NET_PAY|990|LIQUIDO
                """);
    }

    /** El mismo recibo ya cerrado: lo que cambia es que deja de ser un borrador. */
    static Payroll cerrado(Payroll payroll) {
        return payroll.finalizePayroll();
    }

    private static Payroll recibo(String employeeNumber, int presenceNumber,
                                  PayrollStatus status, String lineas) {
        List<PayrollConcept> concepts = new ArrayList<>();
        int lineNumber = 1;
        for (String fila : lineas.strip().split("\n")) {
            String[] campo = fila.strip().split("\\|", -1);
            concepts.add(new PayrollConcept(
                    lineNumber++,
                    campo[1],
                    campo[2],
                    campo[3],
                    new BigDecimal(campo[6]),
                    campo[4].isEmpty() ? null : new BigDecimal(campo[4]),
                    campo[5].isEmpty() ? null : new BigDecimal(campo[5]),
                    campo[7],
                    "202609",
                    Integer.parseInt(campo[8]),
                    1,
                    campo[9]
            ));
        }
        return Payroll.rehydrate(
                1L, "ESP", "INTERNAL", employeeNumber, "202609", "NORMAL", presenceNumber,
                // El instante, no la hora de pared (backend#116). En la semilla este recibo
                // dice «18:04:37» y lo escribio el portatil, que va en Europe/Madrid: en
                // septiembre eso son dos horas por delante de UTC. Que haya que decidirlo
                // aqui es la mitad buena del cambio -- antes el numero no decia de que
                // reloj era, y el PDF lo reimprimia tal cual sin saberlo.
                status, null, Instant.parse("2026-09-20T16:04:37Z"),
                "GRAPH", "1.0", 1L,
                List.of(), concepts, snapshots(), List.of(),
                LocalDateTime.of(2026, 9, 20, 18, 4, 37),
                LocalDateTime.of(2026, 9, 20, 18, 4, 37)
        );
    }

    /** Las fotos del contexto del recibo de {@code EMP000001}, copiadas de la semilla. */
    private static List<PayrollContextSnapshot> snapshots() {
        return List.of(
                new PayrollContextSnapshot("COMPANY_DATA", "RULESYSTEM", "{}", """
                        {"city": "Madrid", "street": "Calle Alcala 100",
                         "legalName": "B4RRHH Spain Company 01, S.L.",
                         "postalCode": "28009", "taxIdentifier": "ESB4R001"}"""),
                new PayrollContextSnapshot("EMPLOYEE_DATA", "EMPLOYEE", "{}", """
                        {"nif": "00000001R", "city": "Porto", "street": "Avenida da Liberdade, 103",
                         "fullName": "Antonio Cano Ramos", "postalCode": "4000-593"}"""),
                new PayrollContextSnapshot("AGREEMENT_DATA", "RULESYSTEM", "{}", """
                        {"shortName": "DISCAPACIDAD", "annualHours": "1736.00",
                         "displayName": "Convenio colectivo del sector de grandes almacenes 99002405",
                         "agreementCategoryCode": "99002405-G2",
                         "officialAgreementNumber": "99002405011982"}"""),
                new PayrollContextSnapshot("EMPLOYEE_PAYROLL_CONTEXT", "PAYROLL_LAUNCH", "{}", """
                        {"agreementCode": "99002405011982", "executionMode": "ELIGIBLE_REAL",
                         "seniorityDate": "2023-11-25", "presenceEndDate": null,
                         "presenceStartDate": "2024-03-08",
                         "agreementCategoryCode": "99002405-G2"}"""),
                new PayrollContextSnapshot("WORK_CENTER_DATA", "RULESYSTEM", "{}", """
                        {"workCenterCode": "MAIN_OFFICE", "workCenterName": "Main Office"}""")
        );
    }

    private PayslipDocumentFixtures() {
    }
}
