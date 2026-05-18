package com.scheduling.task;

import com.scheduling.model.TaskParameterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DummyEmailTask implements ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DummyEmailTask.class);

    @Override
    public String getId() {
        return "dummy-email-task";
    }

    @Override
    public String getName() {
        return "Dummy Email Sender";
    }

    @Override
    public String getDescription() {
        return "Simulates sending an email and logs the details (no actual email is sent)";
    }

    @Override
    public List<TaskParameterSchema> getParameterSchema() {
        return List.of(
            new TaskParameterSchema("to", "string", true, "Recipient email address"),
            new TaskParameterSchema("subject", "string", true, "Email subject line"),
            new TaskParameterSchema("body", "string", false, "Email body content (default: empty)")
        );
    }

    @Override
    public void execute(Map<String, Object> parameters) {
        String to = (String) parameters.get("to");
        String subject = (String) parameters.get("subject");
        String body = (String) parameters.getOrDefault("body", "");

        log.info("[DummyEmailTask] Sending email to: {}, subject: '{}', body length: {} chars",
                to, subject, body.length());
        log.info("[DummyEmailTask] Email sent successfully (simulated)");
    }
}
