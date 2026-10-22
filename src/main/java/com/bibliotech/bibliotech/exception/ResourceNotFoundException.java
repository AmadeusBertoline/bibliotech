package com.bibliotech.bibliotech.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s não encontrado(a) com id %s".formatted(resource, id));
    }
}