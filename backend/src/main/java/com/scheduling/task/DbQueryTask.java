package com.scheduling.task;

import com.scheduling.model.TaskParameterSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DbQueryTask implements ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(DbQueryTask.class);
    private final JdbcTemplate jdbcTemplate;

    public DbQueryTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getId() {
        return "db-query-task";
    }

    @Override
    public String getName() {
        return "Database Query Task";
    }

    @Override
    public String getDescription() {
        return "Runs a read-only SQL query and logs the row count";
    }

    @Override
    public List<TaskParameterSchema> getParameterSchema() {
        return List.of(
            new TaskParameterSchema("query", "string", true, "The SQL SELECT query to execute")
        );
    }

    @Override
    public void execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            log.info("[DbQueryTask] Query returned {} rows: {}", results.size(), query);
        } catch (Exception e) {
            log.error("[DbQueryTask] Query failed: {} - Error: {}", query, e.getMessage());
        }
    }
}
