package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Carga el maestro geo desde sus ficheros de datos (backend#154, ADR-078).
 *
 * <p>Es Java y no SQL porque el issue lo pide así: los ocho mil municipios van en un fichero
 * de datos que la migración lee, no en ocho mil {@code insert} escritos en un {@code .sql}.
 * Flyway no lee ficheros desde SQL, y el fichero tiene que viajar dentro del jar.</p>
 *
 * <p>Los ficheros, su fuente, su licencia y cómo se regeneran están en
 * {@code src/main/resources/db/geo/LEEME.md}. <b>Quedan congelados</b>: el checksum de esta
 * migración es el de su contenido, así que tocarlos después de aplicarla hace que el backend
 * no arranque. Lo de cada año siguiente es una migración nueva (el procedimiento anual del
 * LEEME), no una edición de éstos.</p>
 */
public class V176__The_geo_master_is_loaded_from_its_data_files extends BaseJavaMigration {

    static final String MUNICIPALITIES = "db/geo/municipios-ine-2021-2025.tsv";
    static final String POSTAL_CODES = "db/geo/codigos-postales-geonames.tsv";

    private static final int EXPECTED_MUNICIPALITIES = 8132;
    private static final int EXPECTED_POSTAL_CODES = 11150;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        int municipalities = 0;
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into geo.municipality (country_code, code, name, province_code, start_date)
                values ('ESP', ?, ?, ?, ?)
                """)) {
            for (String[] row : rows(MUNICIPALITIES, 4)) {
                insert.setString(1, row[0]);
                insert.setString(2, row[1]);
                insert.setString(3, row[2]);
                insert.setDate(4, Date.valueOf(LocalDate.parse(row[3])));
                insert.addBatch();
                municipalities++;
            }
            insert.executeBatch();
        }

        int postalCodes = 0;
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into geo.postal_code (country_code, code, suggested_municipality_code)
                values ('ESP', ?, ?)
                """)) {
            for (String[] row : rows(POSTAL_CODES, 2)) {
                insert.setString(1, row[0]);
                insert.setString(2, row[1].isEmpty() ? null : row[1]);
                insert.addBatch();
                postalCodes++;
            }
            insert.executeBatch();
        }

        requireCount(connection, "geo.municipality", municipalities, EXPECTED_MUNICIPALITIES);
        requireCount(connection, "geo.postal_code", postalCodes, EXPECTED_POSTAL_CODES);
    }

    /**
     * El de los dos ficheros, byte a byte. Sin esto Flyway no guardaría ninguno para una
     * migración Java, y un fichero editado después de aplicarla pasaría sin que nadie lo viera.
     */
    @Override
    public Integer getChecksum() {
        CRC32 crc = new CRC32();
        for (String resource : List.of(MUNICIPALITIES, POSTAL_CODES)) {
            try (InputStream in = open(resource)) {
                crc.update(in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return (int) crc.getValue();
    }

    private static List<String[]> rows(String resource, int columns) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(open(resource), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.lines()
                    .skip(1)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.split("\t", -1))
                    .toList();
            for (String[] row : rows) {
                if (row.length != columns) {
                    throw new IllegalStateException(resource + ": una fila con " + row.length
                            + " columnas y no " + columns + ": " + String.join("|", row));
                }
            }
            return rows;
        }
    }

    private static InputStream open(String resource) {
        InputStream in = V176__The_geo_master_is_loaded_from_its_data_files.class.getClassLoader()
                .getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("falta el fichero de datos " + resource);
        }
        return in;
    }

    private static void requireCount(Connection connection, String table, int read, int expected)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from " + table)) {
            result.next();
            long stored = result.getLong(1);
            if (read != expected || stored != expected) {
                throw new IllegalStateException("backend#154: " + table + " tiene " + stored
                        + " filas, leídas " + read + "; se esperaban " + expected);
            }
        }
    }
}
