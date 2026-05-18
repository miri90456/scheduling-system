package com.scheduling.model;

public record TaskParameterSchema(
    String name,
    String type,
    boolean required,
    String description
) {}
