package com.scheduling.dto;

import com.scheduling.model.TaskParameterSchema;
import java.util.List;

public record TaskResponse(
    String id,
    String name,
    String description,
    List<TaskParameterSchema> parameterSchema
) {}
