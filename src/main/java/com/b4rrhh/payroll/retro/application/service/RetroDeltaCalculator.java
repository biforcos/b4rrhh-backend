package com.b4rrhh.payroll.retro.application.service;

import com.b4rrhh.payroll.retro.application.port.PaidForPeriod;
import com.b4rrhh.payroll.retro.application.port.PaidForPeriodLookupPort;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculationConcept;
import com.b4rrhh.payroll.retro.domain.model.RetroBucketing;
import com.b4rrhh.payroll.retro.domain.model.RetroDelta;
import com.b4rrhh.payroll.retro.domain.model.RetroDeltaLine;
import com.b4rrhh.payroll.retro.domain.port.CurrentCalculationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <b>Lo que agosto vale hoy menos lo que por agosto se ha pagado</b> ({@code backend#133}).
 *
 * <h2>El caso que decide el diseno</h2>
 *
 * <table>
 *   <tr><th></th><th>que pasa</th><th>que se cobra</th></tr>
 *   <tr><td>ago-26</td><td>cerrado con 0 horas</td><td>recibo A, congelado, entregado</td></tr>
 *   <tr><td>sep-26</td><td>«agosto tenia 10»</td><td>recibo B + atraso origen=202608 por <b>10</b></td></tr>
 *   <tr><td>oct-26</td><td>«no, eran 20»</td><td>recibo C + atraso origen=202608 por <b>+10</b>, no por 20</td></tr>
 * </table>
 *
 * <p>Octubre sabe que son diez y no veinte porque el atraso <b>no se calcula contra el recibo cerrado de
 * agosto, sino contra lo que ya se ha pagado por agosto</b>: su recibo mas todos sus atrasos posteriores.
 * Calcularlo contra el recibo daria veinte, y el empleado cobraria treinta por unas horas que valen
 * veinte.
 *
 * <p>Esa es la diferencia entre los dos disenos posibles, y no se nota en el primer atraso: los dos dan
 * diez. Se nota en el segundo, que es el que casi nunca se prueba.
 *
 * <h2>El IRPF no viaja</h2>
 *
 * <p>La retencion es sobre lo que se paga cuando se paga (ADR-070 §4), asi que <b>no hay linea
 * {@code 800} con origen</b>: los devengos atrasados entran en el {@code 970} de este mes y el
 * {@code 800} de este mes los absorbe. La invariante del {@code #133} deja el {@code 800} fuera a
 * proposito y por esto mismo.
 */
@Service
public class RetroDeltaCalculator {

    private static final Logger log = LoggerFactory.getLogger(RetroDeltaCalculator.class);
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * Los conceptos que <b>no</b> se atribuyen a su mes de origen, con su motivo.
     *
     * <p>{@code 800} es la retencion de IRPF: se calcula sobre lo que se paga cuando se paga, asi que lo
     * suyo de agosto ya esta pagado y lo de los atrasos lo absorbe el {@code 800} de este mes.
     *
     * <p>Los tres {@code A_*} son los conceptos tecnicos que meten los atrasos en los totales de este
     * mes ({@code backend#133}, V162). No pueden viajar: <b>un atraso de un atraso no existe</b>, y si
     * viajaran, el vigente de agosto llevaria el total de los atrasos que agosto genero y la invariante
     * se perseguiria la cola.
     *
     * <p>Los totales tampoco: {@code 970}, {@code 980}, {@code 990} y {@code 725} son sumas de este mes,
     * y sus lineas de atraso serian el mismo dinero contado dos veces en el mismo folio.
     */
    private static final Set<String> NO_VIAJAN = new LinkedHashSet<>(List.of(
            "800", "A_DEV", "A_DED", "A_EMP", "970", "980", "990", "725"));

    private final CurrentCalculationRepository currentCalculations;
    private final PaidForPeriodLookupPort paidForPeriod;

    public RetroDeltaCalculator(
            CurrentCalculationRepository currentCalculations,
            PaidForPeriodLookupPort paidForPeriod
    ) {
        this.currentCalculations = currentCalculations;
        this.paidForPeriod = paidForPeriod;
    }

    /**
     * Los deltas del tramo, listos para bajar al recibo del mes abierto.
     *
     * @param fromPeriodCode el mes mas antiguo del tramo de esta corrida, o {@code null} si no hay tramo
     * @param toPeriodCode   {@code P-1}, o {@code null} si no hay tramo
     */
    public RetroDelta deltasOf(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            Integer presenceNumber,
            String fromPeriodCode,
            String toPeriodCode,
            RetroBucketing bucketing
    ) {
        if (fromPeriodCode == null || toPeriodCode == null) {
            return RetroDelta.none();
        }

        List<RetroDeltaLine> lineas = new ArrayList<>();
        YearMonth desde = YearMonth.parse(fromPeriodCode, PERIODO);
        YearMonth hasta = YearMonth.parse(toPeriodCode, PERIODO);

        for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
            String periodo = mes.format(PERIODO);

            Optional<CurrentCalculation> vigente = currentCalculations.findByBusinessKey(
                    ruleSystemCode, employeeTypeCode, employeeNumber,
                    periodo, payrollTypeCode, presenceNumber);
            if (vigente.isEmpty()) {
                // Ese mes no se recalculo -no se pudo, o la presencia no existia entonces-, asi que no
                // hay con que comparar. No es un delta de cero: es que no hay delta.
                continue;
            }

            PaidForPeriod pagado = paidForPeriod.findByEmployeeAndPeriod(
                    ruleSystemCode, employeeTypeCode, employeeNumber, payrollTypeCode, periodo);

            lineas.addAll(deltasDelMes(periodo, vigente.get(), pagado));
        }

        RetroDelta delta = RetroDelta.of(lineas, bucketing);
        if (!delta.isEmpty()) {
            log.info("[RETRO] Deltas de {} a {} | {} lineas | devengos={} deducciones={} empresa={}",
                    fromPeriodCode, toPeriodCode, delta.lines().size(),
                    delta.totalEarnings(), delta.totalEmployeeDeductions(),
                    delta.totalEmployerContributions());
        }
        return delta;
    }

    /**
     * El delta de un mes, concepto a concepto.
     *
     * <p>Se recorren <b>los dos lados</b> y no solo el vigente: un concepto que se pago y que hoy ya no
     * esta —unas horas que se borraron— tiene delta <b>negativo</b>, y mirar solo el vigente lo dejaria
     * fuera. Eso es dinero que se cobro de mas y que hay que devolver, y es exactamente el caso que se
     * pierde si se piensa en «lo que hay que anadir» en vez de en «la diferencia».
     */
    private List<RetroDeltaLine> deltasDelMes(
            String periodo,
            CurrentCalculation vigente,
            PaidForPeriod pagado
    ) {
        Map<String, CurrentCalculationConcept> porConcepto = new LinkedHashMap<>();
        for (CurrentCalculationConcept c : vigente.getConcepts()) {
            // Un concepto puede venir en varias lineas del folio -los de ambito de tramo-, y lo que se
            // compara es el total del mes por concepto: es lo que la invariante enuncia.
            porConcepto.merge(c.conceptCode(), c, RetroDeltaCalculator::sumadas);
        }

        Set<String> todos = new LinkedHashSet<>(porConcepto.keySet());
        todos.addAll(pagado.amountsByConcept().keySet());

        List<RetroDeltaLine> lineas = new ArrayList<>();
        for (String conceptCode : todos) {
            if (NO_VIAJAN.contains(conceptCode)) {
                continue;
            }
            CurrentCalculationConcept enElVigente = porConcepto.get(conceptCode);
            BigDecimal valeHoy = enElVigente == null ? BigDecimal.ZERO : enElVigente.amount();
            BigDecimal seHaPagado = pagado.of(conceptCode);
            BigDecimal diferencia = valeHoy.subtract(seHaPagado);

            if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            lineas.add(new RetroDeltaLine(
                    periodo,
                    conceptCode,
                    enElVigente == null ? null : enElVigente.conceptMnemonic(),
                    literalDelAtraso(
                            enElVigente == null ? conceptCode : enElVigente.conceptLabel(), periodo),
                    diferencia,
                    // Cantidad y tarifa NO se restan: una diferencia de importe no tiene cantidad ni
                    // precio propios. Se copian las del vigente cuando la linea existe alli, que es lo
                    // que le permite al recibo decir «diez horas a 12,50» y nada mas.
                    enElVigente == null ? null : enElVigente.quantity(),
                    enElVigente == null ? null : enElVigente.rate(),
                    // La naturaleza y el bloque son los del concepto: un atraso de salario base se
                    // imprime donde se imprime el salario base. Cuando el concepto ya no esta en el
                    // vigente no hay de donde sacarlos, y entonces la linea sale sin bloque, que es una
                    // ausencia que se ve.
                    enElVigente == null ? null : enElVigente.conceptNatureCode(),
                    enElVigente == null ? null : enElVigente.displayOrder(),
                    enElVigente == null ? null : enElVigente.payslipSectionCode(),
                    enElVigente == null ? null : enElVigente.payslipSubsectionCode()));
        }
        return lineas;
    }

    /**
     * El literal de una linea de atraso: el nombre del concepto <b>y su origen a la vista</b>
     * ({@code backend#134}).
     *
     * <blockquote>{@code Salario base (atraso 08/2026)}</blockquote>
     *
     * <p>Un sufijo sobre el nombre del concepto, y <b>no un concepto nuevo por mes</b>. Un catalogo con
     * «Salario base de agosto» y «Salario base de septiembre» como conceptos distintos se llenaria de
     * conceptos que no son conceptos, y el grafo -que es lo que explica un recibo- pasaria a tener una
     * rama por mes de origen.
     *
     * <p>Se construye <b>aqui y se congela en la linea</b>, como el resto del literal: en cuanto existe el
     * PDF el recibo tiene un gemelo fisico fuera del sistema, y si la pantalla compusiera el sufijo al
     * pintar podria decir algo distinto de lo que el empleado tiene impreso (ADR-059, ADR-062).
     *
     * <p>El mes se escribe {@code MM/AAAA} y no {@code AAAAMM}: lo lee una persona en un papel. El codigo
     * de periodo sigue estando en la columna {@code origin_period_code} para quien lo necesite en
     * maquina.
     */
    private static String literalDelAtraso(String literalDelConcepto, String periodo) {
        String mes = periodo.substring(4) + "/" + periodo.substring(0, 4);
        String sufijo = " (atraso " + mes + ")";
        // El literal de la linea tiene 200 caracteres en la base (V53). Si el nombre del concepto no
        // deja sitio para el sufijo, se recorta EL NOMBRE y no el sufijo: un «Salario base» sin el mes
        // seria una linea de atraso que no se distingue de una del propio mes, y eso es peor que un
        // nombre a medias.
        int sitio = 200 - sufijo.length();
        String nombre = literalDelConcepto.length() > sitio
                ? literalDelConcepto.substring(0, sitio)
                : literalDelConcepto;
        return nombre + sufijo;
    }

    /** Dos lineas del mismo concepto en el vigente: el importe se suma y lo demas es de la primera. */
    private static CurrentCalculationConcept sumadas(
            CurrentCalculationConcept a, CurrentCalculationConcept b) {
        return new CurrentCalculationConcept(
                a.lineNumber(),
                a.conceptCode(),
                a.conceptMnemonic(),
                a.conceptLabel(),
                a.amount().add(b.amount()),
                sumaONulo(a.quantity(), b.quantity()),
                // La tarifa no se suma: dos tramos al mismo precio tienen ese precio, y a precios
                // distintos no hay un precio. Se queda la del primero y el importe es la verdad.
                a.rate(),
                a.conceptNatureCode(),
                a.displayOrder(),
                a.payslipSectionCode(),
                a.payslipSubsectionCode());
    }

    private static BigDecimal sumaONulo(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
    }
}
