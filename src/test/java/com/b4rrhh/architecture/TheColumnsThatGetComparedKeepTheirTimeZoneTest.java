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
 * <h2>Y la otra mitad: que no aparezca una novena</h2>
 *
 * <p>Se comprueba también <b>el conjunto entero de columnas con zona</b>, por nombre. Es para
 * que nadie las convierta en masa creyendo que arregla esto: convertirlas todas es otro tamaño
 * de cambio, no arregla nada más, y el criterio para traer una al grupo es que <b>alguien la
 * compare</b>, no que quede más ordenado.
 *
 * <p>Aquí había un recuento de las que <i>no</i> llevan zona —96— y estaba mal pensado. Su
 * propio mensaje admitía que subir era «probablemente está bien»: cualquier tabla nueva con su
 * {@code created_at}, que es la forma más común de tabla de este árbol, lo ponía rojo sin que
 * pasara nada malo. Un candado que muerde en el caso bueno enseña a ajustar el número hasta que
 * salga verde, y entonces deja de proteger.
 *
 * <p>Por el conjunto con zona se reparte al revés, que es como tiene que estar: una conversión
 * en masa lo pone rojo; una tabla nueva con sellos sin zona no lo toca; y una columna con zona
 * nueva y deliberada lo pone rojo <b>justo en el momento en que hay que decidir si entra en la
 * lista</b>. El coste de equivocarse es añadir un nombre una vez al año, no revisar cada tabla
 * nueva que se cree.
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
     * Que no aparezca una novena con zona sin que nadie lo decida.
     *
     * <p>Se afirma el conjunto entero <b>por nombre</b>, y no cuántas quedan sin zona. La
     * diferencia es en qué caso muerde: una tabla nueva con su {@code created_at} —lo más común
     * que se hace en este árbol— no lo toca, y una columna que gane zona lo pone rojo justo
     * cuando hay que decidir si entra.
     *
     * <p>Un apunte que sale al medirlo: en un volcado restaurado hay una novena,
     * {@code deploy.semilla.capturada_en}, que escribe {@code crear-semilla.sh} con la
     * procedencia de la semilla. No la crea ninguna migración, así que sobre el esquema de estos
     * tests no existe y no está en la lista.
     */
    @Test
    void noNinthColumnHasQuietlyJoinedTheOnesWithATimeZone() {
        List<String> conZona = jdbc.queryForList("""
                select table_schema || '.' || table_name || '.' || column_name
                  from information_schema.columns
                 where data_type = 'timestamp with time zone'
                   and table_schema not in ('pg_catalog', 'information_schema')
                 order by 1
                """, String.class);

        assertEquals(LAS_OCHO.stream().sorted().toList(), conZona,
                """
                El conjunto de columnas CON zona ha cambiado.

                Si SOBRA alguna: se le ha puesto zona a una columna que no esta en la lista. \
                Este rojo es el sitio donde hay que decidir si entra, y entra solo si ALGUIEN \
                LA COMPARA con otra -- ese es el criterio, no que quede mas ordenado. Si entra, \
                se anade su nombre a LAS_OCHO y la migracion dice con quien se compara.

                Si FALTA alguna: una migracion ha deshecho la V143 para esa columna, y la \
                comparacion del RuleSystemLastChangeLookupAdapter vuelve a mezclar horas de \
                pared de dos maquinas. Eso no falla: contesta mal.

                Lo que este test NO vigila, a proposito: las demas columnas timestamp del \
                esquema, que siguen sin zona y estan bien asi. Son sellos -- se escriben, se \
                leen, y nadie pregunta cual es anterior a cual. Una tabla nueva con su \
                created_at sin zona no tiene que pasar por aqui.""");
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
