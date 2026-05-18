package com.scheduling.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.task.ScheduledTask;
import com.scheduling.task.TaskRegistry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleJobExecutor implements Job {

    private static final Logger log = LoggerFactory.getLogger(ScheduleJobExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TaskRegistry taskRegistry;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String taskId = dataMap.getString("taskId");
        String parametersJson = dataMap.getString("taskParametersJson");

        log.debug("Executing job for taskId={}, scheduleKey={}", taskId, context.getJobDetail().getKey());

        ScheduledTask task = taskRegistry.getTask(taskId)
                .orElseThrow(() -> new JobExecutionException("Task not found: " + taskId));

        Map<String, Object> parameters = new HashMap<>();
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                parameters = objectMapper.readValue(parametersJson, new TypeReference<>() {});
            } catch (Exception e) {
                throw new JobExecutionException("Failed to parse task parameters", e);
            }
        }

        try {
            task.execute(parameters);
            log.info("Successfully executed task '{}' for schedule '{}'", taskId, context.getJobDetail().getKey());
        } catch (Exception e) {
            log.error("Task '{}' execution failed: {}", taskId, e.getMessage(), e);
            throw new JobExecutionException("Task execution failed", e);
        }
    }
}
