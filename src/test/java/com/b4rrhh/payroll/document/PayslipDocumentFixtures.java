package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSubsection;

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

    /**
     * Las mismas cinco secciones, con los cuatro apartados del recuadro de bases
     * ({@code backend#121}, {@code V149}).
     */
    static List<PayslipSection> seccionesConLosApartadosDelRecuadro() {
        return List.of(
                new PayslipSection("DEVENGOS", "Devengos", 10),
                new PayslipSection("DEDUCCIONES", "Deducciones", 20),
                new PayslipSection("LIQUIDO", "Liquido total a percibir", 30),
                new PayslipSection("BASES", "Determinacion de las bases de cotizacion", 40,
                        List.of(
                                new PayslipSubsection("BASE_CC", "1. Contingencias comunes", 10),
                                new PayslipSubsection("BASE_CP",
                                        "2. Contingencias profesionales y recaudacion conjunta", 20),
                                new PayslipSubsection("BASE_HE", "3. Horas extraordinarias", 30),
                                new PayslipSubsection("BASE_IRPF",
                                        "4. Base sujeta a retencion del IRPF", 40))),
                new PayslipSection("APORTACION_EMPRESARIAL", "Aportacion empresarial", 50)
        );
    }

    /**
     * Un recibo con el recuadro de las tres bases, de los 245 con horas extra
     * ({@code backend#121}).
     *
     * <p>Las diez lineas del recuadro con su apartado, un devengo y el liquido: lo justo para ver
     * en el papel las dos cosas que este paso anade —que el recuadro sale en cuatro apartados y
     * que los demas bloques siguen sin ninguno—. Los importes son los del ejemplo del issue.
     */
    static Payroll elRecuadroDeLasTresBases() {
        return recibo("EMP001000", 1, PayrollStatus.CALCULATED, """
                101|101|SALARIO_BASE|Salario base|30|61.67|1850.10|EARNING|101|DEVENGOS|
                102|102|IMPORTE_HORAS_EXTRA|Horas extraordinarias|18|7.71|138.78|EARNING|102|DEVENGOS|
                103|970|TOTAL_DEVENGOS|Total devengado|||1988.88|TOTAL_EARNING|970|DEVENGOS|
                104|B03|REMUNERACION_MENSUAL|Remuneracion mensual|||1850.10|BASE|401|BASES|BASE_CC
                105|B04|PRORRATA_EN_LA_BASE|Prorrata de pagas extraordinarias|||616.70|BASE|402|BASES|BASE_CC
                106|B01|BASE_CONTINGENCIAS_COMUNES|Base de cotizacion|||2466.80|BASE|403|BASES|BASE_CC
                107|B_CC|BASE_COTIZACION_COTIZ|Base tras topes|||2466.80|BASE|404|BASES|BASE_CC
                108|B05|BASE_COMUNES_EN_PROFESIONALES|Base de contingencias comunes|||2466.80|BASE|411|BASES|BASE_CP
                109|B06|HORAS_EXTRA_EN_PROFESIONALES|Horas extraordinarias|||138.78|BASE|412|BASES|BASE_CP
                110|B07|BASE_CONTINGENCIAS_PROFESIONALES|Base de cotizacion|||2605.58|BASE|413|BASES|BASE_CP
                111|B_CP|BASE_PROFESIONALES_COTIZ|Base tras topes|||2605.58|BASE|414|BASES|BASE_CP
                112|B08|BASE_HORAS_EXTRAORDINARIAS|Base|||138.78|BASE|421|BASES|BASE_HE
                113|B09|BASE_SUJETA_A_RETENCION|Base|||1988.88|BASE|431|BASES|BASE_IRPF
                114|990|LIQUIDO_A_PAGAR|Liquido total a percibir|||1500.00|NET_PAY|990|LIQUIDO|
                """);
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

    /**
     * El recibo mas largo de la semilla de hoy: treinta y dos lineas ({@code backend#125}).
     *
     * <p>Copiado de {@code EMP000323}, presencia 2, {@code 202609}, con la semilla recalculada
     * hasta la {@code V155}. Tiene todo lo que alarga un recibo a la vez —mes partido en dos
     * tramos de salario, prorrata pagada en dos lineas, horas extraordinarias con sus dos cuotas,
     * las diez lineas del recuadro y las siete de la aportacion empresarial— y por eso es el que
     * decide si la maqueta cabe en una hoja.
     *
     * <p>Los dos recibos de arriba tienen 18 y 19 lineas y son de antes del {@code backend#121}.
     * Mientras el test de A4 solo los mirara a ellos, <b>no estaba vigilando nada</b>: cabian de
     * sobra y el que no cabia no estaba.
     */
    static Payroll elMasLargoDeLaSemilla() {
        return recibo("EMP000323", 2, PayrollStatus.CALCULATED, """
                1|101|SALARIO_BASE|Salario base|6|47.5|285.00|EARNING|101|DEVENGOS|
                2|101|SALARIO_BASE|Salario base|24|40|960.00|EARNING|101|DEVENGOS|
                3|102|IMPORTE_HORAS_EXTRA|Horas extraordinarias|12|5|60.00|EARNING|102|DEVENGOS|
                4|103|PRORRATA_PAGAS_EXTRAS|Prorrata de pagas extraordinarias|1|95|95.00|EARNING|103|DEVENGOS|
                5|103|PRORRATA_PAGAS_EXTRAS|Prorrata de pagas extraordinarias|1|320|320.00|EARNING|103|DEVENGOS|
                6|B03|REMUNERACION_MENSUAL|Remuneracion mensual|||1245.00|BASE|401|BASES|BASE_CC
                7|B04|PRORRATA_EN_LA_BASE|Prorrata de pagas extraordinarias|||415.00|BASE|402|BASES|BASE_CC
                8|B01|BASE_COTIZABLE|Base de cotizacion|||1660.00|BASE|403|BASES|BASE_CC
                9|B_CC|BASE_COTIZACION_COTIZ|Base tras topes|||1660.00|BASE|404|BASES|BASE_CC
                10|B05|BASE_COMUNES_EN_PROFESIONALES|Base de contingencias comunes antes de topes|||1660.00|BASE|411|BASES|BASE_CP
                11|B06|HORAS_EXTRA_EN_PROFESIONALES|Horas extraordinarias|||60.00|BASE|412|BASES|BASE_CP
                12|B07|BASE_CONTINGENCIAS_PROFESIONALES|Base de cotizacion|||1720.00|BASE|413|BASES|BASE_CP
                13|B_CP|BASE_PROFESIONALES_COTIZ|Base tras topes|||1720.00|BASE|414|BASES|BASE_CP
                14|B08|BASE_HORAS_EXTRAORDINARIAS|Base|||60.00|BASE|421|BASES|BASE_HE
                15|B09|BASE_SUJETA_A_RETENCION|Base|||1720.00|BASE|431|BASES|BASE_IRPF
                16|700|CC_TRABAJADOR|Contingencias comunes|1660|4.7|78.02|DEDUCTION|700|DEDUCCIONES|
                17|701|FP_TRABAJADOR|Formacion profesional|1720|0.1|1.72|DEDUCTION|701|DEDUCCIONES|
                18|702|MEI_TRABAJADOR|Mecanismo de equidad intergeneracional|1660|0.15|2.49|DEDUCTION|702|DEDUCCIONES|
                19|703|DESEMPLEO_TRABAJADOR|Desempleo|1720|1.6|27.52|DEDUCTION|703|DEDUCCIONES|
                20|704|HORAS_EXTRA_TRABAJADOR|Horas extraordinarias|60|4.7|2.82|DEDUCTION|704|DEDUCCIONES|
                21|720|SS_CC_EMPRESARIO|Contingencias comunes (aportacion empresarial)|1660|23.6|391.76|INFORMATIONAL|720|APORTACION_EMPRESARIAL|
                22|721|SS_DESEMPLEO_EMPRESARIO|Desempleo (aportacion empresarial)|1720|6.7|115.24|INFORMATIONAL|721|APORTACION_EMPRESARIAL|
                23|722|SS_FP_EMPRESARIO|Formacion profesional (aportacion empresarial)|1720|0.6|10.32|INFORMATIONAL|722|APORTACION_EMPRESARIAL|
                24|723|SS_FOGASA_EMPRESARIO|FOGASA (aportacion empresarial)|1720|0.2|3.44|INFORMATIONAL|723|APORTACION_EMPRESARIAL|
                25|724|SS_MEI_EMPRESARIO|Mecanismo de equidad intergeneracional (aportacion empresarial)|1660|0.75|12.45|INFORMATIONAL|724|APORTACION_EMPRESARIAL|
                26|726|HORAS_EXTRA_EMPRESARIO|Horas extraordinarias|60|23.6|14.16|INFORMATIONAL|726|APORTACION_EMPRESARIAL|
                27|727|AT_EP_EMPRESARIO|Accidentes de trabajo y enfermedades profesionales|1720|1.65|28.38|INFORMATIONAL|727|APORTACION_EMPRESARIAL|
                28|725|TOTAL_APORTACION_EMPRESARIAL|Total aportacion empresarial|||575.75|TOTAL_EMPLOYER_CONTRIBUTION|790|APORTACION_EMPRESARIAL|
                29|800|RETENCION_IRPF|Retencion IRPF|1720|15|258.00|DEDUCTION|800|DEDUCCIONES|
                30|970|TOTAL_DEVENGOS|Total devengado|||1720.00|TOTAL_EARNING|970|DEVENGOS|
                31|980|TOTAL_DEDUCCIONES|Total a deducir|||370.57|TOTAL_DEDUCTION|980|DEDUCCIONES|
                32|990|LIQUIDO_A_PAGAR|Liquido total a percibir|||1349.43|NET_PAY|990|LIQUIDO|
                """);
    }

    /**
     * El mismo recibo con {@code cuantas} lineas de salario de mas ({@code backend#125}).
     *
     * <p>Sirve para medir cuanto aire le queda a la maqueta. Se copian lineas del {@code 101}
     * porque es lo que de verdad se repite: un mes partido en mas tramos deja un {@code 101} por
     * tramo, y es asi como un recibo crece sin que nadie anada un concepto al catalogo.
     */
    static Payroll conLineasDeMas(Payroll payroll, int cuantas) {
        List<PayrollConcept> lineas = new ArrayList<>(payroll.getConcepts());
        PayrollConcept modelo = lineas.stream()
                .filter(c -> "101".equals(c.getConceptCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("el recibo no tiene ninguna linea 101"));
        for (int i = 0; i < cuantas; i++) {
            lineas.add(new PayrollConcept(
                    lineas.size() + 1, modelo.getConceptCode(), modelo.getConceptMnemonic(),
                    modelo.getConceptLabel(), modelo.getAmount(), modelo.getQuantity(),
                    modelo.getRate(), modelo.getConceptNatureCode(), modelo.getOriginPeriodCode(),
                    modelo.getDisplayOrder(), modelo.getMergedStepCount(),
                    modelo.getPayslipSectionCode(), modelo.getPayslipSubsectionCode()));
        }
        return Payroll.rehydrate(
                payroll.getId(), payroll.getRuleSystemCode(), payroll.getEmployeeTypeCode(),
                payroll.getEmployeeNumber(), payroll.getPayrollPeriodCode(),
                payroll.getPayrollTypeCode(), payroll.getPresenceNumber(), payroll.getStatus(),
                payroll.getStatusReasonCode(), payroll.getCalculatedAt(),
                payroll.getCalculationEngineCode(), payroll.getCalculationEngineVersion(),
                payroll.getRunId(), payroll.getWarnings(), lineas,
                payroll.getContextSnapshots(), payroll.getSegments(),
                payroll.getCreatedAt(), payroll.getUpdatedAt());
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
                    campo[9],
                    // El apartado del bloque, cuando la fila lo trae (backend#121). Casi ninguna:
                    // solo el recuadro de bases tiene apartados.
                    campo.length > 10 && !campo[10].isEmpty() ? campo[10] : null
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
