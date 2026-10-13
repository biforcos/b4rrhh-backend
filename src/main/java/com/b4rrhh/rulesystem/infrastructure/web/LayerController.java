package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.ListLayersUseCase;
import com.b4rrhh.rulesystem.infrastructure.web.dto.LayerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las capas con su nivel (ADR-077, frontend#131). No son reglamentaciones: el Ámbito no las ofrece. */
@RestController
@RequestMapping("/layers")
public class LayerController {

    private final ListLayersUseCase listLayersUseCase;

    public LayerController(ListLayersUseCase listLayersUseCase) {
        this.listLayersUseCase = listLayersUseCase;
    }

    @GetMapping
    public ResponseEntity<List<LayerResponse>> list() {
        return ResponseEntity.ok(listLayersUseCase.list().stream()
                .map(layer -> new LayerResponse(
                        layer.code(), layer.name(), layer.level(), layer.active(), layer.mountedBy()))
                .toList());
    }
}
