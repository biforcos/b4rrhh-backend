package com.b4rrhh.rulesystem.domain.model;

import java.util.List;

/**
 * Una capa (ADR-077 §2): donde se definen las cosas. No es una reglamentación: una reglamentación
 * monta una capa por nivel, y {@code mountedBy} dice cuáles montan ésta, que son las que ven lo que
 * se cambie en ella (frontend#131).
 */
public record Layer(String code, String name, int level, boolean active, List<String> mountedBy) {

    public Layer {
        mountedBy = List.copyOf(mountedBy);
    }
}
