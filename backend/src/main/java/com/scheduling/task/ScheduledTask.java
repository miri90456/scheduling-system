package com.scheduling.task;

import com.scheduling.model.TaskParameterSchema;
import java.util.List;
import java.util.Map;

public interface ScheduledTask {

    String getId();

    String getName();

    String getDescription();

    List<TaskParameterSchema> getParameterSchema();

    void execute(Map<String, Object> parameters);
}
