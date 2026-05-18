package com.scheduling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.dto.ScheduleRequest;
import com.scheduling.dto.ScheduleResponse;
import com.scheduling.model.Schedule;
import com.scheduling.model.ScheduleType;
import com.scheduling.model.TaskParameterSchema;
import com.scheduling.repository.ScheduleRepository;
import com.scheduling.task.ScheduledTask;
import com.scheduling.task.TaskRegistry;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ScheduleRepository scheduleRepository;
    private final QuartzService quartzService;
    private final TaskRegistry taskRegistry;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           QuartzService quartzService,
                           TaskRegistry taskRegistry) {
        this.scheduleRepository = scheduleRepository;
        this.quartzService = quartzService;
        this.taskRegistry = taskRegistry;
    }

    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ScheduleResponse getSchedule(UUID id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        validate(request);

        Schedule schedule = new Schedule();
        applyRequest(schedule, request);
        schedule = scheduleRepository.save(schedule);

        if (schedule.isEnabled()) {
            try {
                quartzService.scheduleJob(schedule);
            } catch (SchedulerException e) {
                log.error("Failed to schedule Quartz job", e);
                throw new RuntimeException("Failed to schedule job", e);
            }
        }

        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(UUID id, ScheduleRequest request) {
        validate(request);

        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));

        try {
            quartzService.unscheduleJob(id);
        } catch (SchedulerException e) {
            log.warn("Failed to unschedule old job", e);
        }

        applyRequest(schedule, request);
        schedule = scheduleRepository.save(schedule);

        if (schedule.isEnabled()) {
            try {
                quartzService.scheduleJob(schedule);
            } catch (SchedulerException e) {
                log.error("Failed to reschedule Quartz job", e);
                throw new RuntimeException("Failed to reschedule job", e);
            }
        }

        return toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        if (!scheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("Schedule not found: " + id);
        }

        try {
            quartzService.unscheduleJob(id);
        } catch (SchedulerException e) {
            log.warn("Failed to unschedule job during delete", e);
        }

        scheduleRepository.deleteById(id);
    }

    private void validate(ScheduleRequest request) {
        if (!taskRegistry.exists(request.getTaskId())) {
            throw new IllegalArgumentException("Unknown task: " + request.getTaskId());
        }

        switch (request.getScheduleType()) {
            case RECURRING -> {
                if (request.getIntervalSeconds() == null || request.getIntervalSeconds() <= 0) {
                    throw new IllegalArgumentException("Interval is required for RECURRING schedules");
                }
            }
            case WEEKLY -> {
                if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isBlank()) {
                    throw new IllegalArgumentException("Days of week are required for WEEKLY schedules");
                }
            }
            case CRON -> {
                if (request.getCronExpression() == null || request.getCronExpression().isBlank()) {
                    throw new IllegalArgumentException("Cron expression is required for CRON schedules");
                }
            }
            default -> {}
        }

        validateTaskParameters(request.getTaskId(), request.getTaskParameters());
    }

    private void validateTaskParameters(String taskId, Map<String, Object> parameters) {
        ScheduledTask task = taskRegistry.getTask(taskId).orElseThrow();
        for (TaskParameterSchema param : task.getParameterSchema()) {
            if (param.required()) {
                if (parameters == null || !parameters.containsKey(param.name())
                        || parameters.get(param.name()) == null
                        || parameters.get(param.name()).toString().isBlank()) {
                    throw new IllegalArgumentException(
                            "Required parameter '" + param.name() + "' is missing for task '" + taskId + "'");
                }
            }
        }
    }

    private void applyRequest(Schedule schedule, ScheduleRequest request) {
        schedule.setTaskId(request.getTaskId());
        schedule.setScheduleType(request.getScheduleType());
        schedule.setCronExpression(request.getCronExpression());
        schedule.setIntervalSeconds(request.getIntervalSeconds());
        schedule.setDaysOfWeek(request.getDaysOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setEnabled(request.isEnabled());

        if (request.getTaskParameters() != null) {
            try {
                schedule.setTaskParametersJson(objectMapper.writeValueAsString(request.getTaskParameters()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize task parameters", e);
            }
        } else {
            schedule.setTaskParametersJson(null);
        }
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setTaskId(schedule.getTaskId());
        response.setScheduleType(schedule.getScheduleType());
        response.setCronExpression(schedule.getCronExpression());
        response.setIntervalSeconds(schedule.getIntervalSeconds());
        response.setDaysOfWeek(schedule.getDaysOfWeek());
        response.setStartTime(schedule.getStartTime());
        response.setEndTime(schedule.getEndTime());
        response.setEnabled(schedule.isEnabled());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        taskRegistry.getTask(schedule.getTaskId())
                .ifPresent(t -> response.setTaskName(t.getName()));

        if (schedule.getTaskParametersJson() != null) {
            try {
                Map<String, Object> params = objectMapper.readValue(
                        schedule.getTaskParametersJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
                response.setTaskParameters(params);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize task parameters for schedule {}", schedule.getId());
            }
        }

        return response;
    }
}
