package com.scheduling.task;

import com.scheduling.dto.TaskResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TaskRegistry {

    private final Map<String, ScheduledTask> tasks = new LinkedHashMap<>();

    public TaskRegistry(List<ScheduledTask> taskList) {
        for (ScheduledTask task : taskList) {
            tasks.put(task.getId(), task);
        }
    }

    public Optional<ScheduledTask> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<TaskResponse> getAllTaskResponses() {
        return tasks.values().stream()
                .map(t -> new TaskResponse(t.getId(), t.getName(), t.getDescription(), t.getParameterSchema()))
                .toList();
    }

    public boolean exists(String taskId) {
        return tasks.containsKey(taskId);
    }
}
