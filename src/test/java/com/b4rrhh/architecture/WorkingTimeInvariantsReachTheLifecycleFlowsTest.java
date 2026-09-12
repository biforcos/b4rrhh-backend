package com.b4rrhh.architecture;

import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeSeriesInvariantException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una excepcion de invariante de la jornada llega a los flujos de ciclo de vida
 * sin que nadie tenga que acordarse de nada (backend#59).
 *
 * El alta y la reincorporacion capturaban la familia listando sus miembros uno
 * a uno, y esa lista se quedo descolgada el dia que aparecio
 * WorkingTimeIsACorrectionException (backend#58): la excepcion existia, los
 * flujos no la nombraban, y si algun dia saliera contestarian un 500 en vez de
 * un error de validacion. Nadie aviso porque no habia nada que avisara.
 *
 * Este test es lo que avisa. Son dos condiciones, y hacen falta las dos:
 *
 * 1. Lo que requireAccepted lanza por un plan rechazado ES un invariante, asi
 *    que tiene que ser de la familia. Si alguien anade un motivo de rechazo y
 *    lo traduce a una excepcion nueva fuera del supertipo, aqui se ve.
 * 2. Los flujos capturan el supertipo y NO nombran a ningun miembro. Mientras
 *    eso se cumpla, un miembro nuevo entra solo.
 *
 * El cese queda fuera a proposito: cierra jornadas y solo le concierne la de
 * fuera de presencia, que captura junto a las suyas de no-encontrada y
 * ya-cerrada. Meterle el supertipo cambiaria lo que hace, y el backend#59 dice
 * expresamente que esto es solo que se captura, no que hacen los flujos.
 *
 * Se mira el codigo fuente y no el bytecode por lo mismo que en
 * TerminationCoversEveryPresenceVerticalTest: la regla se comprueba de un
 * vistazo y no hace falta ArchUnit.
 */
class WorkingTimeInvariantsReachTheLifecycleFlowsTest {

    private static final Path TIMELINE_SERVICE = Path.of(
            "src/main/java/com/b4rrhh/employee/working_time/application/service/WorkingTimeTimelineService.java");

    private static final Path ALTA = Path.of(
            "src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipant.java");

    private static final Path REINCORPORACION = Path.of(
            "src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeService.java");

    /** Lo que requireAccepted construye al rechazar un plan. */
    private static final Pattern LANZA = Pattern.compile("new (WorkingTime[A-Za-z]*Exception)");

    @Test
    void everyRejectionTheTimelineTranslatesIsAMemberOfTheFamily() {
        Set<String> familia = familia();
        Set<String> lanzadas = new TreeSet<>();

        Matcher m = LANZA.matcher(trozoDeRequireAccepted());
        while (m.find()) {
            lanzadas.add(m.group(1));
        }

        assertTrue(!lanzadas.isEmpty(), "No he encontrado ninguna excepcion en requireAccepted: "
                + "si el metodo ha cambiado de nombre o de forma, este test hay que actualizarlo, no borrarlo.");

        Set<String> huerfanas = new TreeSet<>(lanzadas);
        huerfanas.removeAll(familia);

        assertTrue(huerfanas.isEmpty(), """
                Estas excepciones traducen un plan rechazado, o sea un invariante de la serie
                violado, pero no son de la familia: %s

                Tienen que extender WorkingTimeSeriesInvariantException y entrar en su clausula
                permits. Si no, los flujos de alta y reincorporacion no las capturan y saldran
                como un 500.

                Lanzadas por requireAccepted: %s
                Familia (permits):            %s
                """.formatted(huerfanas, lanzadas, familia));
    }

    @Test
    void theLifecycleFlowsCatchTheFamilyAndNameNoMemberOfIt() {
        Set<String> familia = familia();

        for (Path flujo : java.util.List.of(ALTA, REINCORPORACION)) {
            String fuente = leer(flujo);

            assertTrue(
                    fuente.contains("WorkingTimeSeriesInvariantException"),
                    flujo + " no captura WorkingTimeSeriesInvariantException. Sin eso, la familia "
                            + "vuelve a ser una lista que hay que acordarse de actualizar (backend#59)."
            );

            Set<String> nombrados = new TreeSet<>();
            for (String miembro : familia) {
                if (fuente.contains(miembro)) {
                    nombrados.add(miembro);
                }
            }

            assertTrue(nombrados.isEmpty(), """
                    %s nombra miembros de la familia uno a uno: %s

                    Basta con capturar WorkingTimeSeriesInvariantException. Nombrar a uno vuelve
                    a hacer de esto una lista que se queda atras, que es justo lo que paso con
                    WorkingTimeIsACorrectionException (backend#58, backend#59).
                    """.formatted(flujo, nombrados));
        }
    }

    /** La familia, leida de la clausula permits: el supertipo es sealed. */
    private Set<String> familia() {
        Class<?>[] permitidas = WorkingTimeSeriesInvariantException.class.getPermittedSubclasses();

        assertTrue(
                permitidas != null && permitidas.length > 0,
                "WorkingTimeSeriesInvariantException ha dejado de ser sealed. Puede ser una decision "
                        + "legitima, pero entonces este test tiene que leer la familia de otra forma."
        );

        return Arrays.stream(permitidas)
                .map(Class::getSimpleName)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    /** Desde la firma de requireAccepted hasta el final del metodo. */
    private String trozoDeRequireAccepted() {
        String fuente = leer(TIMELINE_SERVICE);
        int inicio = fuente.indexOf("public void requireAccepted(");
        assertTrue(inicio >= 0, "No encuentro requireAccepted en " + TIMELINE_SERVICE);

        int fin = fuente.indexOf("\n    private ", inicio);
        return fin > inicio ? fuente.substring(inicio, fin) : fuente.substring(inicio);
    }

    private String leer(Path fichero) {
        assertTrue(Files.isRegularFile(fichero), "No encuentro " + fichero.toAbsolutePath()
                + ". Si el fichero se ha movido, este test hay que actualizarlo, no borrarlo.");
        try {
            return Files.readString(fichero);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
