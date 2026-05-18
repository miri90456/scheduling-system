package com.scheduling.task;

import com.scheduling.model.TaskParameterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class HttpPingTask implements ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(HttpPingTask.class);

    @Override
    public String getId() {
        return "http-ping-task";
    }

    @Override
    public String getName() {
        return "HTTP Ping Task";
    }

    @Override
    public String getDescription() {
        return "Pings a URL and logs the HTTP status code";
    }

    @Override
    public List<TaskParameterSchema> getParameterSchema() {
        return List.of(
            new TaskParameterSchema("url", "string", true, "The URL to ping"),
            new TaskParameterSchema("timeout", "number", false, "Timeout in seconds (default: 10)")
        );
    }

    @Override
    public void execute(Map<String, Object> parameters) {
        String url = (String) parameters.get("url");
        int timeout = 10;
        if (parameters.containsKey("timeout") && parameters.get("timeout") != null) {
            timeout = ((Number) parameters.get("timeout")).intValue();
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(timeout))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[HttpPingTask] GET {} -> status {}", url, response.statusCode());
        } catch (Exception e) {
            log.error("[HttpPingTask] Failed to ping {}: {}", url, e.getMessage());
        }
    }
}
