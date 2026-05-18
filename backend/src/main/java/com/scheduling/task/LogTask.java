package com.scheduling.task;

import com.scheduling.model.TaskParameterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LogTask implements ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(LogTask.class);

    @Override
    public String getId() {
        return "log-task";
    }

    @Override
    public String getName() {
        return "Log Task";
    }

    @Override
    public String getDescription() {
        return "Writes a custom message to the application log";
    }

    @Override
    public List<TaskParameterSchema> getParameterSchema() {
        return List.of(
            new TaskParameterSchema("message", "string", true, "The message to write to the log")
        );
    }

    @Override
    public void execute(Map<String, Object> parameters) {
        String message = (String) parameters.getOrDefault("message", "No message provided");
        log.info("[LogTask] {}", message);
    }
}
