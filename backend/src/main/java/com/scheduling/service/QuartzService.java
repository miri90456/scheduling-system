package com.scheduling.service;

import com.scheduling.job.ScheduleJobExecutor;
import com.scheduling.model.Schedule;
import com.scheduling.model.ScheduleType;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

@Service
public class QuartzService {

    private static final Logger log = LoggerFactory.getLogger(QuartzService.class);
    private final Scheduler scheduler;

    public QuartzService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void scheduleJob(Schedule schedule) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(schedule);
        Trigger trigger = buildTrigger(schedule);

        if (scheduler.checkExists(jobDetail.getKey())) {
            scheduler.deleteJob(jobDetail.getKey());
        }

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job: {}", jobDetail.getKey());
    }

    public void unscheduleJob(UUID scheduleId) throws SchedulerException {
        JobKey jobKey = buildJobKey(scheduleId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.info("Unscheduled job: {}", jobKey);
        }
    }

    public void pauseJob(UUID scheduleId) throws SchedulerException {
        JobKey jobKey = buildJobKey(scheduleId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
        }
    }

    public void resumeJob(UUID scheduleId) throws SchedulerException {
        JobKey jobKey = buildJobKey(scheduleId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
        }
    }

    private JobKey buildJobKey(UUID scheduleId) {
        return new JobKey("schedule-" + scheduleId, "scheduling-system");
    }

    private TriggerKey buildTriggerKey(UUID scheduleId) {
        return new TriggerKey("trigger-" + scheduleId, "scheduling-system");
    }

    JobDetail buildJobDetail(Schedule schedule) {
        return JobBuilder.newJob(ScheduleJobExecutor.class)
                .withIdentity(buildJobKey(schedule.getId()))
                .usingJobData("taskId", schedule.getTaskId())
                .usingJobData("taskParametersJson",
                        schedule.getTaskParametersJson() != null ? schedule.getTaskParametersJson() : "")
                .storeDurably(false)
                .build();
    }

    Trigger buildTrigger(Schedule schedule) {
        return switch (schedule.getScheduleType()) {
            case ONE_TIME -> buildOneTimeTrigger(schedule);
            case RECURRING -> buildRecurringTrigger(schedule);
            case WEEKLY -> buildWeeklyTrigger(schedule);
            case CRON -> buildCronTrigger(schedule);
        };
    }

    private Trigger buildOneTimeTrigger(Schedule schedule) {
        Date startDate = Date.from(schedule.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
        return TriggerBuilder.newTrigger()
                .withIdentity(buildTriggerKey(schedule.getId()))
                .startAt(startDate)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();
    }

    private Trigger buildRecurringTrigger(Schedule schedule) {
        Date startDate = Date.from(schedule.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
        long intervalMs = schedule.getIntervalSeconds() * 1000;

        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(intervalMs)
                .repeatForever()
                .withMisfireHandlingInstructionNextWithRemainingCount();

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(buildTriggerKey(schedule.getId()))
                .startAt(startDate)
                .withSchedule(scheduleBuilder);

        if (schedule.getEndTime() != null) {
            Date endDate = Date.from(schedule.getEndTime().atZone(ZoneId.systemDefault()).toInstant());
            triggerBuilder.endAt(endDate);
        }

        return triggerBuilder.build();
    }

    private Trigger buildWeeklyTrigger(Schedule schedule) {
        String cronExpression = buildWeeklyCron(schedule.getDaysOfWeek(), schedule.getStartTime());

        return TriggerBuilder.newTrigger()
                .withIdentity(buildTriggerKey(schedule.getId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .inTimeZone(TimeZone.getDefault())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    private Trigger buildCronTrigger(Schedule schedule) {
        return TriggerBuilder.newTrigger()
                .withIdentity(buildTriggerKey(schedule.getId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpression())
                        .inTimeZone(TimeZone.getDefault())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    String buildWeeklyCron(String daysOfWeek, java.time.LocalDateTime startTime) {
        int second = startTime.getSecond();
        int minute = startTime.getMinute();
        int hour = startTime.getHour();
        return String.format("%d %d %d ? * %s", second, minute, hour, daysOfWeek);
    }
}
