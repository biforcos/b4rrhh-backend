package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListLayersService implements ListLayersUseCase {

    private final LayerRepository layerRepository;

    public ListLayersService(LayerRepository layerRepository) {
        this.layerRepository = layerRepository;
    }

    @Override
    public List<Layer> list() {
        return layerRepository.findAll();
    }
}
