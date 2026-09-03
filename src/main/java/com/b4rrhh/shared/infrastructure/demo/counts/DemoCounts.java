package com.b4rrhh.shared.infrastructure.demo.counts;

/**
 * El tamano de la demo, en tres numeros y nada mas.
 *
 * Es lo que la portada ensena antes de que exista token, asi que sale por un
 * endpoint publico. Por eso el contrato es cerrado a proposito: recuentos
 * agregados, nunca filas ni identificadores. Un total no dice nada de nadie.
 * Si la portada quiere una cuarta cifra, se anade aqui y se discute entonces
 * (backend#45).
 *
 * Son las mismas tres cuentas que reset-demo.sh escribe en su registro al
 * terminar, con la misma definicion: si la portada y el registro del reseteo
 * dijeran cosas distintas, uno de los dos estaria mintiendo.
 */
public record DemoCounts(
        long employees,
        long calculatedPayrolls,
        long payrollConcepts
) {
}
