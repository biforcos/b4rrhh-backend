package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.usecase.PayrollLaunchInputMissingException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollUnitAlreadyClaimedException;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Todo codigo de mensaje que alguien emite esta sembrado en el catalogo
 * (backend#81).
 *
 * La pantalla de una ejecucion (frontend#61) pinta el literal del codigo, y si
 * no lo hay pinta el codigo desnudo. Eso es feo pero honesto, y era lo correcto
 * mientras no hubiera catalogo. Con catalogo, un codigo que nadie sembro ya no
 * es honestidad: es un despiste que solo se ve ejecutando una nomina y mirando
 * la fila que sale rara.
 *
 * Es la regla que el ADR-059 §7 deja escrita para el motivo de invalidez —cada
 * comprobacion nueva se implementa en el codigo y se da de alta en el catalogo,
 * en el mismo commit—, y este test es lo que la hace cumplir aqui.
 *
 * Los codigos se leen del fuente y no de una lista: un codigo de mensaje va
 * siempre seguido de su severidad, tanto en saveRunMessage como en el
 * constructor de CalculationRunMessage, y esa pareja es la firma que se busca.
 * Si algun dia se emiten de otra forma, este test dejara de verlos: es la
 * limitacion conocida de mirar el fuente en vez del bytecode, la misma que
 * asume TerminationCoversEveryPresenceVerticalTest.
 */
@TestSobreEsquemaReal
class EveryRunMessageCodeIsInTheCatalogTest {

    private static final List<Path> EMISORES = List.of(
            Path.of("src/main/java/com/b4rrhh/payroll/application/usecase/LaunchPayrollCalculationService.java"),
            Path.of("src/main/java/com/b4rrhh/payroll/application/usecase/"
                    + "RecoverAbandonedPayrollCalculationRunsService.java")
    );

    /** Un codigo de mensaje, y detras su severidad. */
    private static final Pattern CODIGO_CON_SEVERIDAD =
            Pattern.compile("\"([A-Z][A-Z0-9_]{3,})\",\\s*\"(INFO|WARNING|ERROR)\"");

    private static final String TIPO = "PAYROLL_RUN_MESSAGE";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyEmittedCodeHasALiteralInTheCatalog() {
        Set<String> emitidos = codigosEmitidos();
        Set<String> sembrados = codigosSembrados();

        Set<String> sinLiteral = new TreeSet<>(emitidos);
        sinLiteral.removeAll(sembrados);

        assertThat(sinLiteral)
                .withFailMessage("""
                        Estos codigos de mensaje se emiten y no estan en el catalogo %s: %s

                        La pantalla de la ejecucion los pintara desnudos. Se siembran con una
                        migracion, en el mismo commit que los emite (ADR-059 §7), con el literal
                        base en ingles y su traduccion en rule_entity_translation (ADR-052).

                        Emitidos:  %s
                        Sembrados: %s
                        """.formatted(TIPO, sinLiteral, emitidos, sembrados))
                .isEmpty();
    }

    /**
     * Y al reves: un codigo sembrado que ya no emite nadie es un literal que
     * nunca se vera. No rompe nada, pero es exactamente lo que el issue pedia no
     * hacer —sembrar «por si acaso»—, y cuando sobra es porque alguien retiro el
     * emisor y se dejo la fila.
     */
    @Test
    void noCodeIsSeededThatNobodyEmits() {
        Set<String> emitidos = codigosEmitidos();
        Set<String> sobrantes = new TreeSet<>(codigosSembrados());
        sobrantes.removeAll(emitidos);

        assertThat(sobrantes)
                .withFailMessage("""
                        Estos codigos estan en el catalogo %s y no los emite nadie: %s

                        O se retiro el emisor y hay que dar de baja la fila, o se sembraron por si
                        acaso, que es lo que el backend#81 dice que no se haga.
                        """.formatted(TIPO, sobrantes))
                .isEmpty();
    }

    /**
     * Y los que no se emiten guardando un mensaje, sino contestando a un cliente.
     *
     * <p>Desde el #100 un recalculo que falla contesta 422 con el codigo del suceso, y desde el
     * backend#101 uno que no consigue la reserva contesta 409 con el suyo, para que el mismo
     * suceso se llame igual por las dos puertas. Eso pone el literal en dos sitios —la
     * excepcion y el emisor del lanzamiento—, y el patron de arriba solo ve el del emisor porque
     * alli va seguido de su severidad. Este test mira el otro: si alguien retoca uno de los dos,
     * o retira la fila del catalogo, aqui se ve.
     */
    @Test
    void theCodesThatTheApiAnswersAreAlsoInTheCatalog() {
        Set<String> contestados = new TreeSet<>(Set.of(
                PayrollCalculationFailedException.MESSAGE_CODE,
                PayrollLaunchInputMissingException.MESSAGE_CODE,
                PayrollUnitAlreadyClaimedException.MESSAGE_CODE
        ));

        Set<String> sinLiteral = new TreeSet<>(contestados);
        sinLiteral.removeAll(codigosSembrados());

        assertThat(sinLiteral)
                .withFailMessage("""
                        Estos codigos los contesta /payrolls en un 4xx y no estan en el catalogo %s: %s

                        Contestados: %s
                        Sembrados:   %s

                        O el literal de la excepcion ya no es el que emite el lanzamiento, o se dio
                        de baja la fila. En los dos casos la pantalla pintara un codigo desnudo justo
                        cuando alguien esta mirando por que no le sale la nomina (#100).
                        """.formatted(TIPO, sinLiteral, contestados, codigosSembrados()))
                .isEmpty();
    }

    private Set<String> codigosEmitidos() {
        Set<String> codigos = new TreeSet<>();
        for (Path emisor : EMISORES) {
            assertTrue(Files.isRegularFile(emisor), "No encuentro " + emisor.toAbsolutePath()
                    + ". Si el fichero se ha movido, este test hay que actualizarlo, no borrarlo.");
            Matcher m = CODIGO_CON_SEVERIDAD.matcher(leer(emisor));
            while (m.find()) {
                codigos.add(m.group(1));
            }
        }

        assertTrue(!codigos.isEmpty(),
                "No he encontrado ningun codigo de mensaje en " + EMISORES
                        + ". Si han cambiado de forma, este test hay que actualizarlo, no borrarlo.");
        return codigos;
    }

    private Set<String> codigosSembrados() {
        return new TreeSet<>(jdbcTemplate.queryForList("""
                select code
                  from rulesystem.rule_entity
                 where rule_entity_type_code = ?
                   and active
                """, String.class, TIPO));
    }

    private String leer(Path fichero) {
        try {
            return Files.readString(fichero);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
