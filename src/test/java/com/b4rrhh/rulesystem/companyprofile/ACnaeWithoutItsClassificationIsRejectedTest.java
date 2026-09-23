package com.b4rrhh.rulesystem.companyprofile;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un codigo de actividad sin decir en que clasificacion esta no entra, y las empresas que no son
 * espanolas no tienen ninguno ({@code backend#122}).
 *
 * <h2>Lo que paso</h2>
 *
 * <p>La V150 puso {@code 4719} —CNAE-2009, derogado— en las cuatro empresas de la semilla. La V152
 * tenia que dejar las espanolas en {@code 4712} y <b>quitarselo a las de Francia y Portugal</b>, y
 * solo hizo la primera mitad: su {@code update} unia con {@code rule_entity} pidiendo
 * {@code rule_system_code = 'ESP'}, o sea que filtraba por la propiedad que el propio cambio queria
 * distinguir, y las dos filas que tenian que cambiar fueron las dos que no alcanzo.
 *
 * <p>La restriccion que se anadio en esa misma migracion para impedir exactamente eso no dijo nada.
 * Con el codigo puesto y la clasificacion a nulo, su expresion vale <b>desconocido</b>, y un
 * {@code CHECK} solo rechaza la fila cuando vale falso. La comparacion de una columna que admite
 * nulos necesita su {@code is not null} al lado, o el candado se abre justo cuando el dato falta.
 *
 * <h2>Por que el dato y el candado, y no solo el dato</h2>
 *
 * <p>La primera comprobacion mira lo que hay sembrado y las otras dos el candado. Arreglar solo el
 * dato dejaria la puerta abierta para el siguiente que escriba un codigo a medias; arreglar solo el
 * candado dejaria la semilla como estaba. Las tres son rojas antes de la V154.
 */
@TestSobreEsquemaReal
class ACnaeWithoutItsClassificationIsRejectedTest {

    @Autowired
    private JdbcTemplate jdbc;

    /** Solo las empresas espanolas tienen CNAE, y el suyo esta escrito en CNAE-2025. */
    @Test
    void onlyTheSpanishCompaniesCarryACnae() {
        List<Map<String, Object>> empresas = jdbc.queryForList(
                "select re.rule_system_code, re.code, cp.cnae_code, cp.cnae_classification"
                        + "  from rulesystem.company_profile cp"
                        + "  join rulesystem.rule_entity re on re.id = cp.company_rule_entity_id"
                        + " where re.rule_entity_type_code = 'COMPANY'"
                        + " order by re.rule_system_code, re.code");

        assertTrue(!empresas.isEmpty(), "la semilla tiene empresas");

        for (Map<String, Object> empresa : empresas) {
            String sistema       = (String) empresa.get("rule_system_code");
            String codigo        = (String) empresa.get("cnae_code");
            String clasificacion = (String) empresa.get("cnae_classification");

            if ("ESP".equals(sistema)) {
                assertEquals("4712", codigo,
                        () -> "la empresa " + empresa.get("code") + " cotiza en Espana");
                assertEquals("CNAE-2025", clasificacion,
                        () -> "y su codigo esta escrito en CNAE-2025");
            } else {
                assertEquals(null, codigo,
                        () -> "la empresa " + empresa.get("code") + " no cotiza en Espana: la CNAE"
                                + " es la clasificacion espanola y un codigo espanol en una empresa"
                                + " francesa o portuguesa no significa nada");
                assertEquals(null, clasificacion,
                        () -> "y sin codigo no hay clasificacion que declarar");
            }
        }
    }

    /**
     * Y la base no deja guardar un codigo sin su clasificacion.
     *
     * <p>Rojo con el check de la V152: su expresion valia desconocido y la fila entraba.
     */
    @Test
    void theDatabaseRefusesACodeWithoutItsClassification() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update rulesystem.company_profile"
                        + "    set cnae_code = '4712', cnae_classification = null"
                        + "  where id = ?", unaEmpresaEspanola()),
                "un codigo sin clasificacion no dice nada, y la base tiene que rechazarlo");
    }

    /**
     * Y tampoco deja guardar una clasificacion que la tarifa no entiende.
     *
     * <p>Va en su propio test y no pegado al anterior porque el primer fallo aborta la transaccion:
     * la segunda sentencia saldria con «current transaction is aborted» y estaria comprobando eso.
     */
    @Test
    void theDatabaseRefusesAClassificationItCannotResolve() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update rulesystem.company_profile"
                        + "    set cnae_code = '4719', cnae_classification = 'CNAE-2009'"
                        + "  where id = ?", unaEmpresaEspanola()),
                "la unica clasificacion que la tarifa entiende hoy es CNAE-2025");
    }

    private Long unaEmpresaEspanola() {
        return jdbc.queryForObject(
                "select cp.id from rulesystem.company_profile cp"
                        + "  join rulesystem.rule_entity re on re.id = cp.company_rule_entity_id"
                        + " where re.rule_system_code = 'ESP' and re.rule_entity_type_code = 'COMPANY'"
                        + " order by re.code limit 1",
                Long.class);
    }
}
