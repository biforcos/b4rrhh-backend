package com.b4rrhh.payroll.document.application.service;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.port.PayslipDocumentContent;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * De un recibo guardado al contenido de su documento ({@code backend#112}).
 *
 * <p>Esto es el gemelo del getter {@code bloques} del folio, y lo es <b>a proposito</b>: mismas
 * reglas, mismo orden, mismos nombres. Lo que coloca una linea en un bloque es su
 * {@code payslipSectionCode}, que viene congelado con ella; el orden y el nombre de cada bloque
 * los da el catalogo. Aqui no hay ninguna lista de bloques escrita a mano, igual que alli.
 *
 * <p>Un bloque sin lineas no sale. Una linea cuya seccion el catalogo no sabe nombrar <b>sale
 * igual</b>, al final y con el codigo de su bloque por nombre: callarsela en el papel seria peor
 * que en la pantalla, porque del papel no se puede recargar.
 */
@Component
public class PayslipDocumentContentFactory {

    /**
     * El momento del calculo, escrito en la hora de Madrid.
     *
     * <p>Desde el {@code backend#116} el {@code calculated_at} es un {@code Instant}, y un
     * instante no se puede escribir sin decir en que reloj. Se elige {@code Europe/Madrid} y no
     * el huso de la maquina que genera el PDF: este es un recibo de salarios espanol y quien lo
     * lee esta en Espana. Si el servidor corre en UTC -la CT de la demo lo hace-, el papel
     * seguiria diciendo la hora de aqui.
     */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Madrid"));
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] MESES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private static final String SIN_DATO = "—";

    private final PayrollSnapshotProfiles profiles;

    public PayslipDocumentContentFactory(PayrollSnapshotProfiles profiles) {
        this.profiles = profiles;
    }

    public PayslipDocumentContent contentOf(Payroll payroll, List<PayslipSection> sections) {
        List<PayrollContextSnapshot> snapshots = payroll.getContextSnapshots();
        PayrollSnapshotProfiles.Company company = profiles.company(snapshots);
        PayrollSnapshotProfiles.Employee employee = profiles.employee(snapshots);
        PayrollSnapshotProfiles.Agreement agreement = profiles.agreement(snapshots);

        return new PayslipDocumentContent(
                payroll.getStatus() != PayrollStatus.DEFINITIVE,
                new PayslipDocumentContent.Party(
                        company == null ? SIN_DATO : orDash(company.legalName()),
                        detalles(
                                etiqueta("CIF", company == null ? null : company.taxIdentifier()),
                                company == null ? null : company.street(),
                                company == null ? null : cityLine(company.postalCode(), company.city())
                        )
                ),
                new PayslipDocumentContent.Party(
                        employee == null ? SIN_DATO : orDash(employee.fullName()),
                        detalles(
                                etiqueta("NIF", employee == null ? null : employee.nif()),
                                // La matricula va en la caja del trabajador y no en los datos
                                // laborales: en el modelo oficial es un dato de quien cobra, al
                                // lado de su NIF, y ahi es donde se busca.
                                etiqueta("Matrícula", payroll.getEmployeeNumber()),
                                employee == null ? null : employee.street(),
                                employee == null ? null : cityLine(employee.postalCode(), employee.city())
                        )
                ),
                new PayslipDocumentContent.LaborData(
                        agreement == null ? SIN_DATO : orDash(agreement.displayName()),
                        agreement == null ? SIN_DATO : orDash(agreement.agreementCategoryCode()),
                        periodLabel(payroll, snapshots),
                        workCenterLabel(snapshots),
                        seniorityLabel(snapshots)
                ),
                blocks(payroll.getConcepts(), sections),
                provenance(payroll)
        );
    }

    // ---------------------------------------------------------------------
    // Los bloques, con las mismas reglas que el folio
    // ---------------------------------------------------------------------

    private List<PayslipDocumentContent.Block> blocks(
            List<PayrollConcept> concepts,
            List<PayslipSection> sections
    ) {
        Map<String, List<PayrollConcept>> porSeccion = new LinkedHashMap<>();
        for (PayrollConcept concept : concepts) {
            porSeccion.computeIfAbsent(concept.getPayslipSectionCode(), key -> new ArrayList<>())
                    .add(concept);
        }

        List<PayslipDocumentContent.Block> blocks = new ArrayList<>();
        List<PayslipSection> declaradas = new ArrayList<>(sections);
        declaradas.sort(Comparator.comparingInt(PayslipSection::displayOrder));

        for (PayslipSection section : declaradas) {
            List<PayrollConcept> lineas = porSeccion.remove(section.sectionCode());
            if (lineas != null && !lineas.isEmpty()) {
                blocks.add(block(section.label(), lineas));
            }
        }

        // Lo que queda son lineas que el catalogo no ha colocado: o su naturaleza no tiene seccion
        // declarada, o el catalogo no contesto. Van al final y se pintan igual.
        for (Map.Entry<String, List<PayrollConcept>> resto : porSeccion.entrySet()) {
            blocks.add(block(
                    resto.getKey() == null ? "Sin bloque declarado" : resto.getKey(),
                    resto.getValue()
            ));
        }
        return List.copyOf(blocks);
    }

    private PayslipDocumentContent.Block block(String label, List<PayrollConcept> lineas) {
        List<PayrollConcept> ordenadas = new ArrayList<>(lineas);
        ordenadas.sort(Comparator.comparingInt(PayrollConcept::getDisplayOrder));

        return new PayslipDocumentContent.Block(
                label,
                ordenadas.stream().map(PayslipDocumentContentFactory::line).toList(),
                // Aqui ya no se suma nada, y hasta el backend#114 se sumaba una cosa: el recuadro
                // de aportacion empresarial, que era el unico bloque con total en el modelo
                // oficial al que el motor no le daba uno. Ahora se lo da —el 725 de la V141— y
                // llega como una linea mas, igual que el 970, el 980 y el 990.
                //
                // El recuadro de bases sigue sin total y sigue sin necesitarlo: la base de
                // contingencias comunes y la base sujeta a retencion son dos magnitudes distintas
                // del mismo mes, y sumarlas da un numero que no significa nada. Eso lo decidio el
                // frontend#79 para la pantalla y esto lo sostiene para el papel.
                //
                // Dice «el liquido» y no «un bloque con una sola linea de total»: con esa segunda
                // regla, un recibo cuyas deducciones se hubieran quedado todas a cero pintaria
                // «Total a deducir» como linea de cierre (frontend#76).
                ordenadas.size() == 1 && "NET_PAY".equals(ordenadas.get(0).getConceptNatureCode())
        );
    }

    private static PayslipDocumentContent.Line line(PayrollConcept concept) {
        return new PayslipDocumentContent.Line(
                concept.getOriginPeriodCode() == null ? SIN_DATO : concept.getOriginPeriodCode(),
                concept.getConceptCode(),
                concept.getConceptLabel(),
                PayslipNumbers.valor(concept.getQuantity()),
                PayslipNumbers.valor(concept.getRate()),
                PayslipNumbers.valor(concept.getAmount())
        );
    }

    // ---------------------------------------------------------------------
    // Cabecera y procedencia
    // ---------------------------------------------------------------------

    /**
     * De donde sale este papel, impreso en el.
     *
     * <p>La clave de negocio es lo que permite que quien tenga el folio en la mano encuentre el
     * recibo en el sistema y vea de donde sale cada linea. <b>El puente a la explicacion es la
     * clave, no el contenido</b>: por eso aqui hay una direccion y ni un solo paso de calculo.
     */
    private static PayslipDocumentContent.Provenance provenance(Payroll payroll) {
        return new PayslipDocumentContent.Provenance(
                String.join("/",
                        payroll.getRuleSystemCode(),
                        payroll.getEmployeeTypeCode(),
                        payroll.getEmployeeNumber(),
                        payroll.getPayrollPeriodCode(),
                        payroll.getPayrollTypeCode(),
                        String.valueOf(payroll.getPresenceNumber())),
                String.valueOf(payroll.getPresenceNumber()),
                payroll.getRunId() == null ? SIN_DATO : String.valueOf(payroll.getRunId()),
                payroll.getCalculatedAt() == null ? SIN_DATO : MOMENTO.format(payroll.getCalculatedAt()),
                payroll.getCalculationEngineCode() + " " + payroll.getCalculationEngineVersion()
        );
    }

    /** «Del 1 al 30 de septiembre de 2026», acotado por la presencia, igual que el folio. */
    private String periodLabel(Payroll payroll, List<PayrollContextSnapshot> snapshots) {
        String code = payroll.getPayrollPeriodCode();
        if (code == null || code.length() < 6) {
            return code == null ? SIN_DATO : code;
        }
        int year;
        int month;
        try {
            year = Integer.parseInt(code.substring(0, 4));
            month = Integer.parseInt(code.substring(4, 6));
        } catch (NumberFormatException e) {
            return code;
        }
        if (month < 1 || month > 12) {
            return code;
        }
        int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
        int startDay = 1;
        int endDay = lastDay;

        LocalDate start = parseDate(profiles.presenceStartDate(snapshots));
        if (start != null && start.getYear() == year && start.getMonthValue() == month
                && start.getDayOfMonth() > 1) {
            startDay = start.getDayOfMonth();
        }
        LocalDate end = parseDate(profiles.presenceEndDate(snapshots));
        if (end != null && end.getYear() == year && end.getMonthValue() == month) {
            endDay = end.getDayOfMonth();
        }
        return "Del " + startDay + " al " + endDay + " de " + MESES[month - 1] + " de " + year;
    }

    private String workCenterLabel(List<PayrollContextSnapshot> snapshots) {
        String name = profiles.workCenterName(snapshots);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return orDash(profiles.workCenterCode(snapshots));
    }

    /**
     * La antiguedad sale como <b>fecha</b>, que es lo que es: un punto de partida. La ficha enseña
     * la duracion porque cuenta hasta hoy; un recibo de septiembre no puede contar hasta hoy sin
     * envejecer solo cada mes que pasa ({@code backend#91}).
     */
    private String seniorityLabel(List<PayrollContextSnapshot> snapshots) {
        LocalDate seniority = parseDate(profiles.seniorityDate(snapshots));
        return seniority == null ? SIN_DATO : DIA.format(seniority);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String cityLine(String postalCode, String city) {
        String line = (postalCode == null ? "" : postalCode) + " " + (city == null ? "" : city);
        return line.isBlank() ? null : line.trim();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? SIN_DATO : value;
    }

    /** Las lineas de una caja de cabecera, sin los huecos: lo que no se sabe no deja un renglon. */
    private static List<String> detalles(String... lineas) {
        List<String> puestas = new ArrayList<>();
        for (String linea : lineas) {
            if (linea != null && !linea.isBlank()) {
                puestas.add(linea);
            }
        }
        return List.copyOf(puestas);
    }

    private static String etiqueta(String rotulo, String valor) {
        return valor == null || valor.isBlank() ? null : rotulo + ": " + valor;
    }
}
