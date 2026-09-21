package com.b4rrhh.payroll.domain.model;

import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El criterio 5 del {@code backend#102}: no aparece ninguna forma de reabrir.
 *
 * <p>Es un criterio negativo, y un criterio negativo no lo sujeta mirar la pantalla de hoy: lo
 * sujeta que el dominio no tenga por donde. Cerrar dice «estas nominas ya no se tocan», y de ahi
 * salen las salidas de nomina; lo que venga despues —retros, complementarias— es <b>otro recibo</b>.
 * La irreversibilidad no es una carencia que tapar, es la regla.
 *
 * <p>Los metodos no se enumeran a mano. Se recorren <b>todos</b> los que devuelven un
 * {@code Payroll} sin recibir argumentos —que es la forma de una transicion en este agregado— y
 * cada uno tiene que rebotar sobre un recibo cerrado. Escribir la lista a mano tendria el mismo
 * defecto que el criterio quiere evitar: un {@code reopen()} nuevo no estaria en ella, y el test
 * seguiria verde diciendo que no hay forma de reabrir.
 *
 * <p>Lo que este test no ve, y conviene saberlo: un metodo de reapertura que <b>pida</b> argumentos
 * —un motivo, por ejemplo— no tiene esta forma. Por eso va con el segundo, que afirma que las
 * transiciones de este agregado son exactamente las tres conocidas.
 */
class NothingLeavesADefinitivePayrollTest {

    /** Lo unico que un recibo cerrado admite recibir: las tres que ya existen, y las tres rebotan. */
    private static final Set<String> TRANSICIONES_CONOCIDAS =
            new TreeSet<>(Set.of("invalidate", "validateExplicitly", "finalizePayroll"));

    @Test
    void everyTransitionOfTheAggregateBouncesOffADefinitivePayroll() {
        Payroll cerrada = payroll(PayrollStatus.DEFINITIVE);
        Set<String> probadas = new TreeSet<>();

        for (Method metodo : transicionesSinArgumentos()) {
            probadas.add(metodo.getName());
            try {
                metodo.invoke(cerrada);
                assertFalse(true, "Payroll." + metodo.getName() + "() saca un recibo de DEFINITIVE."
                        + " El backend#102 dice que no hay reapertura: un recibo cerrado no se edita,"
                        + " y lo que venga despues es otro recibo.");
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            } catch (InvocationTargetException ex) {
                assertThat(ex.getCause())
                        .withFailMessage("Payroll.%s() sobre un recibo cerrado tiene que rebotar con"
                                + " PayrollInvalidStateTransitionException, y reboto con %s",
                                metodo.getName(), ex.getCause())
                        .isInstanceOf(PayrollInvalidStateTransitionException.class);
            }
        }

        assertTrue(!probadas.isEmpty(),
                "No he encontrado ninguna transicion en Payroll. Si han cambiado de forma, este test"
                        + " hay que actualizarlo, no borrarlo.");
    }

    /**
     * Y el otro lado, que es el que caza una reapertura con argumentos: las transiciones de este
     * agregado son las tres conocidas. Una cuarta hay que mirarla a mano antes de darla por buena,
     * y este rojo es lo que obliga a mirarla.
     */
    @Test
    void theAggregateHasExactlyTheThreeKnownTransitions() {
        Set<String> encontradas = new TreeSet<>();
        for (Method metodo : Payroll.class.getDeclaredMethods()) {
            if (metodo.getReturnType() == Payroll.class
                    && java.lang.reflect.Modifier.isPublic(metodo.getModifiers())
                    && !java.lang.reflect.Modifier.isStatic(metodo.getModifiers())) {
                encontradas.add(metodo.getName());
            }
        }

        assertThat(encontradas)
                .withFailMessage("""
                        Las transiciones de Payroll ya no son las tres conocidas.

                        Conocidas:  %s
                        Encontradas: %s

                        Si la nueva saca un recibo de DEFINITIVE, el backend#102 dice que no: no hay
                        reapertura, y lo que venga despues de un cierre es otro recibo. Si no lo
                        saca, anadela aqui y comprueba que rebota en el test de arriba.
                        """.formatted(TRANSICIONES_CONOCIDAS, encontradas))
                .isEqualTo(TRANSICIONES_CONOCIDAS);
    }

    /** Las transiciones que se pueden invocar sin inventarse argumentos. */
    private List<Method> transicionesSinArgumentos() {
        return List.of(Payroll.class.getDeclaredMethods()).stream()
                .filter(m -> m.getReturnType() == Payroll.class)
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .filter(m -> !java.lang.reflect.Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 0)
                .toList();
    }

    private Payroll payroll(PayrollStatus status) {
        return Payroll.rehydrate(
                1L, "ESP", "INTERNAL", "EMP000001", "202501", "NORMAL", 1,
                status, null, Instant.now(), "ENGINE", "1.0",
                List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
