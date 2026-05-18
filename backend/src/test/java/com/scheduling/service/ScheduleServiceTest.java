package com.scheduling.service;

import com.scheduling.dto.ScheduleRequest;
import com.scheduling.dto.ScheduleResponse;
import com.scheduling.model.Schedule;
import com.scheduling.model.ScheduleType;
import com.scheduling.model.TaskParameterSchema;
import com.scheduling.repository.ScheduleRepository;
import com.scheduling.task.LogTask;
import com.scheduling.task.TaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private QuartzService quartzService;

    private TaskRegistry taskRegistry;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        taskRegistry = new TaskRegistry(List.of(new LogTask()));
        scheduleService = new ScheduleService(scheduleRepository, quartzService, taskRegistry);
    }

    @Test
    void getAllSchedules_ShouldReturnMappedResponses() {
        // Arrange
        Schedule schedule = createScheduleEntity();
        when(scheduleRepository.findAll()).thenReturn(List.of(schedule));

        // Act
        List<ScheduleResponse> result = scheduleService.getAllSchedules();

        // Assert
        assertEquals(1, result.size());
        assertEquals("log-task", result.get(0).getTaskId());
        assertEquals("Log Task", result.get(0).getTaskName());
    }

    @Test
    void createSchedule_WhenValid_ShouldSaveAndSchedule() throws Exception {
        // Arrange
        ScheduleRequest request = createValidRequest();
        Schedule savedSchedule = createScheduleEntity();
        when(scheduleRepository.save(any())).thenReturn(savedSchedule);

        // Act
        ScheduleResponse result = scheduleService.createSchedule(request);

        // Assert
        assertNotNull(result);
        assertEquals("log-task", result.getTaskId());
        verify(scheduleRepository).save(any());
        verify(quartzService).scheduleJob(any());
    }

    @Test
    void createSchedule_WhenUnknownTask_ShouldThrow() {
        // Arrange
        ScheduleRequest request = createValidRequest();
        request.setTaskId("nonexistent-task");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createSchedule(request));
    }

    @Test
    void createSchedule_WhenMissingRequiredParam_ShouldThrow() {
        // Arrange
        ScheduleRequest request = createValidRequest();
        request.setTaskParameters(Map.of());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scheduleService.createSchedule(request));
        assertTrue(ex.getMessage().contains("message"));
    }

    @Test
    void deleteSchedule_WhenExists_ShouldDeleteAndUnschedule() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        when(scheduleRepository.existsById(id)).thenReturn(true);

        // Act
        scheduleService.deleteSchedule(id);

        // Assert
        verify(quartzService).unscheduleJob(id);
        verify(scheduleRepository).deleteById(id);
    }

    @Test
    void deleteSchedule_WhenNotExists_ShouldThrow() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(scheduleRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> scheduleService.deleteSchedule(id));
    }

    @Test
    void validate_WhenRecurringWithoutInterval_ShouldThrow() {
        // Arrange
        ScheduleRequest request = createValidRequest();
        request.setScheduleType(ScheduleType.RECURRING);
        request.setIntervalSeconds(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createSchedule(request));
    }

    @Test
    void validate_WhenCronWithoutExpression_ShouldThrow() {
        // Arrange
        ScheduleRequest request = createValidRequest();
        request.setScheduleType(ScheduleType.CRON);
        request.setCronExpression(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createSchedule(request));
    }

    private ScheduleRequest createValidRequest() {
        ScheduleRequest request = new ScheduleRequest();
        request.setTaskId("log-task");
        request.setScheduleType(ScheduleType.ONE_TIME);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setTaskParameters(Map.of("message", "test message"));
        request.setEnabled(true);
        return request;
    }

    private Schedule createScheduleEntity() {
        Schedule schedule = new Schedule();
        schedule.setId(UUID.randomUUID());
        schedule.setTaskId("log-task");
        schedule.setScheduleType(ScheduleType.ONE_TIME);
        schedule.setStartTime(LocalDateTime.now().plusHours(1));
        schedule.setTaskParametersJson("{\"message\":\"test\"}");
        schedule.setEnabled(true);
        return schedule;
    }
}
