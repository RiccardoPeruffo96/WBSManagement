package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the ProjectController using MockMvc.
 * Static calls to DatabaseManager are mocked using Mockito.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(ProjectController.class)
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockedStatic<DatabaseManager> dbMock;

    /**
     * Set up before each test. Mocks the static class DatabaseManager.
     */
    @BeforeEach
    void setUp() {
        dbMock = Mockito.mockStatic(DatabaseManager.class);
    }

    /**
     * Clean up after each test. Closes the static mock.
     */
    @AfterEach
    void tearDown() {
        dbMock.close();
    }

    /**
     * Test GET /project/add.
     * Verifies that the form is rendered correctly with supervisor list.
     */
    @Test
    void testShowAddProjectForm() throws Exception {
        String[] supervisors = {"1 - s@s.s", "2 - d@d.d"};
        dbMock.when(DatabaseManager::getSupervisors).thenReturn(supervisors);

        mockMvc.perform(get("/project/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout"))
                .andExpect(model().attribute("supervisors", Arrays.asList(supervisors)))
                .andExpect(model().attribute("content", "project-add"));
    }

    /**
     * Test POST /project/add with valid input.
     * Simulates a successful project creation and verifies the result.
     */
    @Test
    void testHandleAddProjectFormSuccess() throws Exception {
        String name = "New Project";
        String description = "Desc";
        int supId = 2;
        String email = "s@s.s";

        // mock user role as Supervisor
        Map<String, String> userRow = new HashMap<>();
        userRow.put("role_name", "Supervisor");

        dbMock.when(() -> DatabaseManager.getSupervisors()).thenReturn(new String[]{});
        dbMock.when(() -> DatabaseManager.getUserRowByEmail(email)).thenReturn(userRow);
        dbMock.when(() -> DatabaseManager.getRoleId("Supervisor")).thenReturn(10);
        dbMock.when(() -> DatabaseManager.addProject(name, description, 10, supId)).thenReturn(true);

        mockMvc.perform(post("/project/add")
                        .param("name", name)
                        .param("description", description)
                        .param("supervisorId", String.valueOf(supId))
                        .principal(() -> email)
                )
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "Task added successfully"))
                .andExpect(view().name("layout"));
    }
}
