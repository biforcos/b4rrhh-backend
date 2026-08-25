package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El cese cierra todo lo que deriva de la presencia del empleado en la empresa.
 *
 * Un vertical nuevo se engancha solo al cese —Spring inyecta la lista entera de
 * TerminationParticipant, no hay ningun registro central que mantener—, pero
 * nada obliga a escribir ese participante. Si a alguien se le olvida, la baja
 * dejara ese vertical abierto y no se enterara nadie: no falla, simplemente no
 * cierra. Este test es lo que convierte ese olvido en un fallo de la suite.
 *
 * La regla son dos condiciones a la vez: si un vertical SE PUEDE CERRAR y
 * ademas sus periodos DEPENDEN de la presencia, entonces tiene que cerrarse
 * cuando la presencia se cierra.
 *
 * Hacen falta las dos. Solo con la primera entraria address, que se cierra pero
 * no depende de la presencia: la direccion de una persona no caduca porque deje
 * de trabajar aqui. Solo con la segunda entraria tax_information, que consulta
 * la presencia para comprobar una fecha de inicio pero no tiene nada que cerrar.
 *
 * "Se puede cerrar" se detecta por la convencion Close*Service. Si algun dia un
 * vertical llama de otra forma a esa operacion, este test dejara de verlo: es la
 * limitacion conocida de mirar nombres en vez de bytecode.
 *
 * Se mira el codigo fuente y no el bytecode a proposito. Con bytecode haria
 * falta ArchUnit; leyendo los .java no hace falta ninguna dependencia y la
 * regla se puede comprobar de un vistazo, que en un test de arquitectura vale
 * mas que la elegancia.
 */
class TerminationCoversEveryPresenceVerticalTest {

    private static final Path VERTICALES = Path.of("src/main/java/com/b4rrhh/employee");
    private static final Path PARTICIPANTES =
            VERTICALES.resolve("lifecycle/application/participant");

    /** No son verticales de datos: presence es la raiz y lifecycle la orquesta. */
    private static final Set<String> NO_SON_VERTICALES = Set.of("presence", "lifecycle", "temporal", "journey");

    @Test
    void everyVerticalConstrainedByPresenceParticipatesInTermination() {
        assertTrue(
                Files.isDirectory(VERTICALES),
                "No encuentro " + VERTICALES.toAbsolutePath()
                        + ". Si la estructura del proyecto ha cambiado, este test hay que actualizarlo, "
                        + "no borrarlo: la regla que protege sigue siendo cierta."
        );

        Set<String> acotadosPorLaPresencia = verticales().stream()
                .filter(this::sePuedeCerrar)
                .filter(this::dependeDeLaPresencia)
                .map(this::normalizar)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        Set<String> conParticipante = participantes();

        Set<String> olvidados = new TreeSet<>(acotadosPorLaPresencia);
        olvidados.removeAll(conParticipante);

        assertTrue(olvidados.isEmpty(), """
                Estos verticales se pueden cerrar y dependen de la presencia, pero no
                cierran nada cuando el empleado causa baja: %s

                Cada uno necesita un TerminationParticipant en
                employee/lifecycle/application/participant/. Con implementarlo basta: Spring
                lo recoge solo. Y recuerda que va DETRAS de la presencia, que se cierra la
                primera (orden 5) porque es la raiz de la que todos derivan.

                Verticales que deberian participar: %s
                Verticales con participante:        %s
                """.formatted(olvidados, acotadosPorLaPresencia, conParticipante));
    }

    /** Cada carpeta directa bajo employee/ es un vertical, menos las de servicio. */
    private List<Path> verticales() {
        try (Stream<Path> hijos = Files.list(VERTICALES)) {
            return hijos
                    .filter(Files::isDirectory)
                    .filter(p -> !NO_SON_VERTICALES.contains(p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tiene una operacion de cierre, luego hay algo que cerrar en una baja. */
    private boolean sePuedeCerrar(Path vertical) {
        return hayFichero(vertical, f -> {
            String nombre = f.getFileName().toString();
            return nombre.startsWith("Close") && nombre.endsWith("Service.java");
        });
    }

    /**
     * Nombra a Presence en algun sitio: puertos de consistencia, validadores o
     * excepciones de tipo "fuera del periodo de presencia".
     */
    private boolean dependeDeLaPresencia(Path vertical) {
        return hayFichero(vertical, f -> f.toString().endsWith(".java") && leer(f).contains("Presence"));
    }

    private boolean hayFichero(Path vertical, java.util.function.Predicate<Path> criterio) {
        try (Stream<Path> ficheros = Files.walk(vertical)) {
            return ficheros.filter(Files::isRegularFile).anyMatch(criterio);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Set<String> participantes() {
        try (Stream<Path> ficheros = Files.list(PARTICIPANTES)) {
            return ficheros
                    .map(f -> f.getFileName().toString())
                    .filter(n -> n.endsWith("TerminationParticipant.java"))
                    .map(n -> n.replace("TerminationParticipant.java", ""))
                    .map(this::normalizar)
                    .filter(n -> !n.equals("presence"))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Las carpetas usan guion bajo (cost_center) y las clases mayusculas
     * (CostCenter). Quitando separadores y bajando a minusculas, las dos formas
     * se encuentran sin tener que mantener una tabla de equivalencias.
     */
    private String normalizar(Object nombre) {
        String texto = nombre instanceof Path ruta
                ? ruta.getFileName().toString()
                : nombre.toString();
        return texto.replace("_", "").toLowerCase();
    }

    private String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
