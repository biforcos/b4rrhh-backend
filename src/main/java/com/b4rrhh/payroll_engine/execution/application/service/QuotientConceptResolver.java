package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingPlannedOperandException;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Un concepto que es <b>otro repartido entre un divisor</b> ({@code backend#119}).
 *
 * <h2>Por que un tipo de calculo y no un porcentaje</h2>
 *
 * <p>La prorrata de pagas extras es un importe anual repartido entre doce meses. Escribirla como
 * un {@code PERCENTAGE} al 8,33 % seria redondear dos veces —una al escribir el tipo y otra al
 * aplicarlo— y el {@code backend#61} dejo dicho que nada se redondea dos veces. Dividir entre doce
 * es exacto: hay un solo redondeo, el que el motor aplica al salir con los decimales del concepto.
 *
 * <h2>El intermedio, y por que 34 digitos</h2>
 *
 * <p>Un tercio no tiene escritura decimal exacta, asi que «el valor sin redondear» no existe y
 * hay que elegir una precision intermedia. Se usa {@link MathContext#DECIMAL128}: 34 cifras
 * significativas, veinte ordenes de magnitud por debajo del ultimo decimo de centimo que el
 * redondeo del concepto pueda mover. Es la misma idea que la escala 8 de {@code J01}, con mas
 * holgura porque aqui el resultado es dinero y no un coeficiente.
 *
 * <h2>Dividir entre cero</h2>
 *
 * <p>Revienta la corrida, y eso es lo correcto: un divisor a cero es un catalogo mal
 * parametrizado, no un caso de negocio. Calcular una nomina «como se pueda» sobre una regla rota
 * es peor que no calcularla.
 */
@Component
public class QuotientConceptResolver {

    public BigDecimal resolve(ConceptExecutionPlanEntry entry, SegmentExecutionState state) {
        ConceptNodeIdentity baseId    = getPlannedOperand(entry, OperandRole.BASE);
        ConceptNodeIdentity divisorId = getPlannedOperand(entry, OperandRole.DIVISOR);

        BigDecimal base    = state.getRequiredAmount(baseId);
        BigDecimal divisor = state.getRequiredAmount(divisorId);

        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException(
                    "El divisor de " + entry.identity().getConceptCode() + " vale cero: lo pone "
                            + divisorId.getConceptCode() + ", y un divisor a cero es un catalogo mal"
                            + " parametrizado y no un caso de nomina.");
        }

        return base.divide(divisor, MathContext.DECIMAL128);
    }

    private ConceptNodeIdentity getPlannedOperand(ConceptExecutionPlanEntry entry, OperandRole role) {
        ConceptNodeIdentity source = entry.operands().get(role);
        if (source == null) {
            throw new MissingPlannedOperandException(entry.identity(), role);
        }
        return source;
    }
}
