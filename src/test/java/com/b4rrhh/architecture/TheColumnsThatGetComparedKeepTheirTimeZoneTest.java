package com.b4rrhh.architecture;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las columnas que se comparan entre sí conservan su zona ({@code backend#116}).
 *
 * <h2>Qué protege</h2>
 *
 * <p>Ocho columnas —siete {@code updated_at} de la reglamentación y el {@code calculated_at} del
 * recibo— entran en la comparación que decide si un recibo puede haber dejado de reflejar las
 * reglas. Si alguna vuelve a ser {@code timestamp without time zone}, lo que se compara son dos
 * horas de pared de dos máquinas distintas, y eso <b>no falla: contesta mal</b>. Con la semilla
 * escrita en {@code Europe/Madrid} y la demo corriendo en {@code UTC}, contesta mal durante las
 * dos horas siguientes a cada resiembra.
 *
 * <p>El {@code ARuleEditedFromAnotherTimeZoneStillMarksTheReceiptTest} cubre el comportamiento;
 * esto cubre <b>la forma</b>, que es lo que se puede perder sin querer: una tabla nueva en el
 * conjunto, una migración que recree una columna, un {@code create table} copiado de otro sitio.
 * Ninguna de esas tres rompería aquel test si la tabla nueva no participa todavía en la consulta.
 *
 * <h2>Y la otra mitad: las 96 que siguen sin zona</h2>
 *
 * <p>Se cuentan también, y a propósito. No para obligar a que sean 96 para siempre, sino para que
 * <b>nadie las convierta en masa creyendo que arregla esto</b>: convertir 104 columnas y todas sus
 * entidades es otro tamaño de cambio, no arregla nada más, y el criterio para traer una aquí es
 * que alguien la compare, no que quede más ordenado.
 */
@TestSobreEsquemaReal
class TheColumnsThatGetComparedKeepTheirTimeZoneTest {

    /**
     * Las ocho, escritas. La lista sale de la consulta del
     * {@code RuleSystemLastChangeLookupAdapter}: si esa consulta suma una tabla, esta lista tiene
     * que crecer con ella, y que haya que tocar dos sitios es el aviso.
     */
    private static final List<String> LAS_OCHO = List.of(
            "payroll.payroll.calculated_at",
            "payroll.payroll_object_binding.updated_at",
            "payroll.payroll_table_row.updated_at",
            "payroll_engine.concept_assignment.updated_at",
            "payroll_engine.payroll_concept.updated_at",
            "payroll_engine.payroll_concept_feed_relation.updated_at",
            "payroll_engine.payroll_concept_operand.updated_at",
            "payroll_engine.payroll_object.updated_at");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theEightColumnsOfTheComparisonCarryATimeZone() {
        List<String> sinZona = LAS_OCHO.stream()
                .filter(columna -> !"timestamp with time zone".equals(tipoDe(columna)))
                .toList();

        assertEquals(List.of(), sinZona,
                """
                Hay columnas de la comparacion sin zona horaria.

                Estas ocho se comparan entre si: los siete updated_at de la reglamentacion contra \
                el calculated_at del recibo. Sin zona, cada maquina guarda su hora de pared y la \
                comparacion deja de significar nada -- no falla, contesta mal, que es peor.

                Si una columna nueva tiene que entrar en la comparacion, entra con zona: \
                'timestamptz using columna at time zone ...', como la V143.""");
    }

    /** Y la lista no se queda corta: las ocho existen con ese nombre exacto. */
    @Test
    void theEightColumnsExistWithThoseExactNames() {
        for (String columna : LAS_OCHO) {
            assertTrue(tipoDe(columna) != null,
                    "la columna " + columna + " no existe: o se renombro o esta lista se quedo vieja");
        }
    }

    /**
     * Las demás siguen sin zona, y eso es el alcance y no un olvido.
     *
     * <p>El número no se defiende como una cifra sagrada: se defiende como una señal. Que baje de
     * golpe quiere decir que alguien ha hecho la conversión en masa que este issue decidió no
     * hacer, y entonces hay que mirar por qué.
     */
    @Test
    void theOtherStampsAreDeliberatelyLeftWithoutOne() {
        Integer sinZona = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                 where data_type = 'timestamp without time zone'
                   and table_schema not in ('pg_catalog', 'information_schema')
                """, Integer.class);

        assertEquals(96, sinZona,
                """
                El numero de columnas timestamp sin zona ha cambiado.

                Eran 104 antes de la V143 y son 96 despues: las ocho de la comparacion se \
                llevaron su zona y las demas se quedaron como estaban, a proposito -- son sellos, \
                se escriben y se leen, y nadie pregunta cual es anterior a cual.

                Si este numero SUBE, hay una tabla nueva con sellos sin zona y probablemente \
                esta bien. Si BAJA de golpe, alguien esta convirtiendolas en masa: eso es otro \
                tamano de cambio y el criterio para traer una columna al grupo con zona es que \
                ALGUIEN LA COMPARE, no que quede mas ordenado.""");
    }

    private String tipoDe(String columnaCualificada) {
        String[] partes = columnaCualificada.split("\\.");
        List<Map<String, Object>> filas = jdbc.queryForList("""
                select data_type from information_schema.columns
                 where table_schema = ? and table_name = ? and column_name = ?
                """, partes[0], partes[1], partes[2]);
        return filas.isEmpty() ? null : (String) filas.get(0).get("data_type");
    }
}
