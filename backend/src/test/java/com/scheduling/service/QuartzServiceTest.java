package com.scheduling.service;

import com.scheduling.model.Schedule;
import com.scheduling.model.ScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QuartzServiceTest {

    @Mock
    private Scheduler scheduler;

    private QuartzService quartzService;

    @BeforeEach
    void setUp() {
        quartzService = new QuartzService(scheduler);
    }

    @Test
    void buildTrigger_WhenOneTime_ShouldCreateSimpleTrigger() {
        // Arrange
        Schedule schedule = createSchedule(ScheduleType.ONE_TIME);
        schedule.setStartTime(LocalDateTime.now().plusHours(1));

        // Act
        Trigger trigger = quartzService.buildTrigger(schedule);

        // Assert
        assertInstanceOf(SimpleTrigger.class, trigger);
    }

    @Test
    void buildTrigger_WhenRecurring_ShouldCreateSimpleTriggerWithInterval() {
        // Arrange
        Schedule schedule = createSchedule(ScheduleType.RECURRING);
        schedule.setStartTime(LocalDateTime.now());
        schedule.setIntervalSeconds(300L);

        // Act
        Trigger trigger = quartzService.buildTrigger(schedule);

        // Assert
        assertInstanceOf(SimpleTrigger.class, trigger);
        SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
        assertEquals(300_000L, simpleTrigger.getRepeatInterval());
    }

    @Test
    void buildTrigger_WhenWeekly_ShouldCreateCronTrigger() {
        // Arrange
        Schedule schedule = createSchedule(ScheduleType.WEEKLY);
        schedule.setStartTime(LocalDateTime.of(2026, 1, 1, 9, 30, 0));
        schedule.setDaysOfWeek("MON,WED,FRI");

        // Act
        Trigger trigger = quartzService.buildTrigger(schedule);

        // Assert
        assertInstanceOf(CronTrigger.class, trigger);
        CronTrigger cronTrigger = (CronTrigger) trigger;
        assertTrue(cronTrigger.getCronExpression().contains("MON,WED,FRI"));
    }

    @Test
    void buildTrigger_WhenCron_ShouldCreateCronTriggerWithExpression() {
        // Arrange
        Schedule schedule = createSchedule(ScheduleType.CRON);
        schedule.setCronExpression("0 0/15 * * * ?");

        // Act
        Trigger trigger = quartzService.buildTrigger(schedule);

        // Assert
        assertInstanceOf(CronTrigger.class, trigger);
        CronTrigger cronTrigger = (CronTrigger) trigger;
        assertEquals("0 0/15 * * * ?", cronTrigger.getCronExpression());
    }

    @Test
    void buildWeeklyCron_ShouldFormatCorrectly() {
        // Act
        String cron = quartzService.buildWeeklyCron("MON,FRI", LocalDateTime.of(2026, 1, 1, 14, 30, 0));

        // Assert
        assertEquals("0 30 14 ? * MON,FRI", cron);
    }

    @Test
    void buildJobDetail_ShouldContainTaskIdInJobData() {
        // Arrange
        Schedule schedule = createSchedule(ScheduleType.ONE_TIME);
        schedule.setStartTime(LocalDateTime.now().plusHours(1));
        schedule.setTaskParametersJson("{\"message\":\"hello\"}");

        // Act
        JobDetail jobDetail = quartzService.buildJobDetail(schedule);

        // Assert
        assertEquals("log-task", jobDetail.getJobDataMap().getString("taskId"));
        assertEquals("{\"message\":\"hello\"}", jobDetail.getJobDataMap().getString("taskParametersJson"));
    }

    private Schedule createSchedule(ScheduleType type) {
        Schedule schedule = new Schedule();
        schedule.setId(UUID.randomUUID());
        schedule.setTaskId("log-task");
        schedule.setScheduleType(type);
        schedule.setStartTime(LocalDateTime.now().plusMinutes(5));
        schedule.setEnabled(true);
        return schedule;
    }
}
