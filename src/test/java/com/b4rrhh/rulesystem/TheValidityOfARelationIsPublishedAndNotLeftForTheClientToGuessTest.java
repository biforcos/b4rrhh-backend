package com.b4rrhh.rulesystem;

import com.b4rrhh.employee.contract.application.port.ContractSubtypeCatalogLookupPort;
import com.b4rrhh.employee.labor_classification.application.model.AgreementCategoryCatalogItem;
import com.b4rrhh.employee.labor_classification.application.port.AgreementCategoryCatalogLookupPort;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las relaciones con vigencia publican su fecha ({@code backend#115}).
 *
 * <h2>De dónde sale esto</h2>
 *
 * <p>Del {@code workforce-loader#1}. El loader necesitaba saber desde cuándo hay convenio,
 * categoría, tipo de contrato y subtipo vigentes a la vez en {@code ESP}, y <b>tuvo que
 * averiguarlo bisecando</b>: proponer una fecha, pedir el catálogo con ella, y mirar si
 * contestaba algo. Quince sondeos para una fecha que el servidor tenía delante.
 *
 * <h2>Los dos defectos que lo causaban</h2>
 *
 * <ol>
 *   <li>El conversor era {@code row[i] instanceof LocalDate ? ... : null} y el driver devuelve
 *       {@code java.sql.Date} para una columna {@code date}. <b>Nunca casaba</b>, así que la
 *       fecha salía nula siempre, sin un error, sin un aviso y sin que nadie lo notara.
 *   <li>Y la fecha que intentaba publicar era la de la categoría, no la de la <b>relación</b>
 *       convenio-categoría, que es la que decide y la única que un cliente no puede deducir.
 * </ol>
 *
 * <h2>Qué se publica ahora</h2>
 *
 * <p>La <b>intersección</b> de las tres vigencias: la del convenio, la de la categoría y la de
 * la relación. Es la única que contesta la pregunta que se hace —desde cuándo se puede usar
 * esta opción— y es exactamente lo que el filtro temporal de esa misma consulta exige, escrito
 * como dato en vez de como condición.
 */
@TestSobreEsquemaReal
class TheValidityOfARelationIsPublishedAndNotLeftForTheClientToGuessTest {

    private static final String RULE_SYSTEM = "ESP";
    private static final String AGREEMENT   = "99002405011982";
    private static final String CONTRACT    = "100";

    /**
     * El día que el loader tardó quince sondeos en encontrar.
     *
     * <p>El convenio arranca el 1980-01-01 y las categorías el 2023-01-01; la relación entre
     * ellos, también el 2023-01-01. La intersección es el 2023-01-01, y es la fecha desde la
     * que el backend acepta un alta con esa categoría.
     */
    private static final LocalDate RELACION_CONVENIO_CATEGORIA = LocalDate.of(2023, 1, 1);

    /** Los tipos de contrato de la reforma laboral (RDL 32/2021). */
    private static final LocalDate RELACION_TIPO_SUBTIPO = LocalDate.of(2022, 3, 30);

    @Autowired
    private AgreementCategoryCatalogLookupPort categorias;

    @Autowired
    private ContractSubtypeCatalogLookupPort subtipos;

    @Test
    void anAgreementCategoryCarriesTheDateFromWhichItCanBeUsed() {
        List<AgreementCategoryCatalogItem> resultado =
                categorias.listActiveCategoriesByAgreement(RULE_SYSTEM, AGREEMENT);

        assertTrue(!resultado.isEmpty(), "el escenario necesita categorias de ESP en el catalogo");
        for (AgreementCategoryCatalogItem categoria : resultado) {
            assertEquals(RELACION_CONVENIO_CATEGORIA, categoria.startDate(),
                    """
                    La categoria %s viene sin la fecha desde la que se puede usar, o con otra.

                    Es la mitad que un cliente no puede deducir: la vigencia de la RELACION \
                    convenio-categoria no se ve por ningun otro sitio del API, y el \
                    workforce-loader#1 tuvo que bisecarla a base de sondeos porque aqui llegaba \
                    nula.""".formatted(categoria.code()));
        }
    }

    /** Y el hermano, que tenía el mismo defecto por la misma línea copiada. */
    @Test
    void aContractSubtypeCarriesItToo() {
        var resultado = subtipos.listActiveSubtypesByContractType(RULE_SYSTEM, CONTRACT);

        assertTrue(!resultado.isEmpty(), "el escenario necesita subtipos del contrato " + CONTRACT);
        assertTrue(resultado.stream().allMatch(s -> RELACION_TIPO_SUBTIPO.equals(s.startDate())),
                "los subtipos tienen que traer la vigencia de su relacion con el tipo: "
                        + resultado);
    }

    /**
     * Y lo que <b>no</b> se publica: un fin que no existe.
     *
     * <p>Ninguna de las tres vigencias de {@code ESP} caduca, así que {@code endDate} tiene que
     * venir nulo. El {@code least} de la consulta trabaja con un {@code 9999-12-31} como
     * centinela, y si se escapara a la respuesta un cliente lo leería como una caducidad de
     * verdad — que es el mismo error, en el otro extremo de la fecha.
     */
    @Test
    void anOptionThatNeverExpiresSaysSoWithANullAndNotWithTheYear9999() {
        List<AgreementCategoryCatalogItem> resultado =
                categorias.listActiveCategoriesByAgreement(RULE_SYSTEM, AGREEMENT);

        assertTrue(resultado.stream().allMatch(categoria -> categoria.endDate() == null),
                "ninguna vigencia de ESP caduca, asi que el fin es nulo y no 9999-12-31: "
                        + resultado);
    }
}
