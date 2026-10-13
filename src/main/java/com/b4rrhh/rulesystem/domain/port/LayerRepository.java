package com.b4rrhh.rulesystem.domain.port;

import com.b4rrhh.rulesystem.domain.model.Layer;

import java.util.List;
import java.util.Optional;

public interface LayerRepository {

    /** Todas, por nivel y código. */
    List<Layer> findAll();

    Optional<Layer> findByCode(String code);
}
