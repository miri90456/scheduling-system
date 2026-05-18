package com.scheduling.controller;

import com.scheduling.dto.TaskResponse;
import com.scheduling.task.TaskRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskRegistry taskRegistry;

    public TaskController(TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskRegistry.getAllTaskResponses());
    }
}
