package com.b4rrhh.rulesystem.domain.exception;

public class LayerNotFoundException extends RuntimeException {

    public LayerNotFoundException(String layerCode) {
        super("Layer not found: " + layerCode);
    }
}
