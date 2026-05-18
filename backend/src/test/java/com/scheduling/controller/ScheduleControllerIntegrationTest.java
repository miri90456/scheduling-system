package com.scheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scheduling.dto.ScheduleRequest;
import com.scheduling.model.ScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScheduleControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scheduling_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void getTasks_ShouldReturnPredefinedTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].parameterSchema").isArray());
    }

    @Test
    void createAndGetSchedule_ShouldWork() throws Exception {
        ScheduleRequest request = new ScheduleRequest();
        request.setTaskId("log-task");
        request.setScheduleType(ScheduleType.ONE_TIME);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setTaskParameters(Map.of("message", "integration test"));
        request.setEnabled(true);

        String responseBody = mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.taskId").value("log-task"))
                .andExpect(jsonPath("$.taskName").value("Log Task"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/schedules/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("log-task"));
    }

    @Test
    void createSchedule_WhenInvalidTask_ShouldReturn400() throws Exception {
        ScheduleRequest request = new ScheduleRequest();
        request.setTaskId("nonexistent");
        request.setScheduleType(ScheduleType.ONE_TIME);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setTaskParameters(Map.of());

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteSchedule_ShouldReturn204() throws Exception {
        ScheduleRequest request = new ScheduleRequest();
        request.setTaskId("log-task");
        request.setScheduleType(ScheduleType.ONE_TIME);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setTaskParameters(Map.of("message", "to be deleted"));
        request.setEnabled(true);

        String responseBody = mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/schedules/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/" + id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSchedule_ShouldModifyAndReturn200() throws Exception {
        ScheduleRequest createReq = new ScheduleRequest();
        createReq.setTaskId("log-task");
        createReq.setScheduleType(ScheduleType.ONE_TIME);
        createReq.setStartTime(LocalDateTime.now().plusHours(3));
        createReq.setTaskParameters(Map.of("message", "original"));
        createReq.setEnabled(true);

        String responseBody = mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asText();

        ScheduleRequest updateReq = new ScheduleRequest();
        updateReq.setTaskId("log-task");
        updateReq.setScheduleType(ScheduleType.ONE_TIME);
        updateReq.setStartTime(LocalDateTime.now().plusHours(5));
        updateReq.setTaskParameters(Map.of("message", "updated"));
        updateReq.setEnabled(true);

        mockMvc.perform(put("/api/schedules/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskParameters.message").value("updated"));
    }

    @Test
    void getAllSchedules_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
