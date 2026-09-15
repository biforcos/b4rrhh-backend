package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.infrastructure.web.PayrollExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El recalculo envuelve como «el calculo fallo» todo lo que sale del motor, y <b>solo</b> eso.
 *
 * <p>Ese envoltorio es util y es peligroso a la vez: si manana el manejador de /payrolls aprende a
 * contestar una excepcion nueva y aqui nadie se acuerda de anadirla, esa excepcion dejara de salir
 * con su codigo de estado y saldra como un 422 «el calculo fallo». No romperia ningun test —el
 * cliente seguiria recibiendo algo con forma— y por eso hace falta cruzarlo: es un control que se
 * cumple solo mientras dos listas coincidan, y nada obliga a que coincidan.
 *
 * <p>Sale del #100, donde el envoltorio se anadio.
 */
class RecalculateOnlyDisguisesWhatNobodyAnswersTest {

    @Test
    void thePassThroughListIsExactlyWhatTheHandlerAnswers() {
        Set<Class<?>> loQueElManejadorContesta = new LinkedHashSet<>();
        for (Method metodo : PayrollExceptionHandler.class.getDeclaredMethods()) {
            ExceptionHandler anotacion = metodo.getAnnotation(ExceptionHandler.class);
            if (anotacion != null) {
                loQueElManejadorContesta.addAll(Arrays.asList(anotacion.value()));
            }
        }

        assertThat(loQueElManejadorContesta)
                .withFailMessage("No he encontrado ningun @ExceptionHandler en PayrollExceptionHandler."
                        + " Si han cambiado de forma, este test hay que actualizarlo, no borrarlo.")
                .isNotEmpty();

        // El envoltorio la produce; no puede pasarla de largo.
        Set<Class<?>> esperadas = new LinkedHashSet<>(loQueElManejadorContesta);
        esperadas.remove(PayrollCalculationFailedException.class);

        assertThat(nombres(RecalculatePayrollService.YA_TIENE_RESPUESTA))
                .withFailMessage("""
                        La lista de lo que el recalculo NO disfraza ya no es la del manejador.

                        El manejador contesta: %s
                        El recalculo deja pasar: %s

                        Lo que sobre aqui saldra con un 422 «el calculo fallo» en vez de con su
                        codigo de estado y su cuerpo (#100).
                        """.formatted(nombres(esperadas), nombres(RecalculatePayrollService.YA_TIENE_RESPUESTA)))
                .isEqualTo(nombres(esperadas));
    }

    private Set<String> nombres(Iterable<? extends Class<?>> tipos) {
        Set<String> nombres = new TreeSet<>();
        tipos.forEach(tipo -> nombres.add(tipo.getSimpleName()));
        return nombres;
    }
}
