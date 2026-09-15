package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.CatalogColumnIntegrity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * El resultado de comprobar los códigos de catálogo contra los datos: columna a columna, con sus
 * recuentos (backend#44).
 *
 * <p>Se lee igual desde un test y desde el despliegue, y dice siempre las dos cifras: cuánto se
 * miró y cuánto está roto. Un «todo bien» sin denominador es exactamente lo que el issue viene a
 * evitar: sobre una base recién migrada la comprobación no puede fallar, y eso hay que poder
 * verlo en el resultado en vez de confundirlo con salud.
 */
public record CatalogCodeIntegrityReport(List<CatalogColumnIntegrity> columns) {

    public CatalogCodeIntegrityReport {
        columns = List.copyOf(columns);
    }

    public List<CatalogColumnIntegrity> orphanColumns() {
        return columns.stream()
                .filter(CatalogColumnIntegrity::hasOrphans)
                .sorted(Comparator.comparingLong(CatalogColumnIntegrity::orphanRows).reversed()
                        .thenComparing(CatalogColumnIntegrity::qualifiedColumn))
                .toList();
    }

    /** Las columnas que no tienen ni una fila: no prueban nada, y conviene saber cuántas son. */
    public List<CatalogColumnIntegrity> emptyColumns() {
        return columns.stream()
                .filter(CatalogColumnIntegrity::isEmpty)
                .sorted(Comparator.comparing(CatalogColumnIntegrity::qualifiedColumn))
                .toList();
    }

    public long totalRows() {
        return columns.stream().mapToLong(CatalogColumnIntegrity::rows).sum();
    }

    public long totalOrphanRows() {
        return columns.stream().mapToLong(CatalogColumnIntegrity::orphanRows).sum();
    }

    public boolean isClean() {
        return totalOrphanRows() == 0;
    }

    /** El titular: columnas declaradas, cuántas tenían dato y cuántas filas se miraron. */
    public String summary() {
        return "%d columnas declaradas, %d con datos y %d vacias, %d filas miradas, %d huerfanas"
                .formatted(columns.size(), columns.size() - emptyColumns().size(),
                        emptyColumns().size(), totalRows(), totalOrphanRows());
    }

    /** Las columnas rotas, con su tipo, sus filas y los códigos concretos. */
    public String describeOrphans() {
        return orphanColumns().stream()
                .map(column -> "  " + column.qualifiedColumn()
                        + " (" + column.ruleEntityTypeCode() + "): "
                        + column.orphanRows() + " de " + column.rows() + " fila(s): "
                        + column.orphanCodes().entrySet().stream()
                                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed()
                                        .thenComparing(java.util.Map.Entry.comparingByKey()))
                                .map(code -> code.getKey() + " (" + code.getValue() + ")")
                                .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    /** Todas las columnas, rotas o no, ordenadas por filas: es el informe con denominador. */
    public String describeAll() {
        return columns.stream()
                .sorted(Comparator.comparingLong(CatalogColumnIntegrity::rows).reversed()
                        .thenComparing(CatalogColumnIntegrity::qualifiedColumn))
                .map(column -> "  %-48s %-40s %8d filas %8d huerfanas"
                        .formatted(column.qualifiedColumn(), column.ruleEntityTypeCode(),
                                column.rows(), column.orphanRows()))
                .collect(Collectors.joining("\n"));
    }
}
