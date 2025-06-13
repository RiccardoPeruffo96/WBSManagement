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

import it.univr.wbsmanagement.controllers.ProjectController;
import it.univr.wbsmanagement.database.DatabaseManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProjectController.class)
public class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private MockedStatic<DatabaseManager> dbMock;

    @BeforeEach
    void setUp() {
        // mock static DatabaseManager
        dbMock = Mockito.mockStatic(DatabaseManager.class);
    }

    @AfterEach
    void tearDown() {
        dbMock.close();
    }

    @Test
    void testShowAddProjectForm() throws Exception {
        String[] supervisors = {"1 - sup1@example.com", "2 - sup2@example.com"};
        dbMock.when(DatabaseManager::getSupervisors).thenReturn(supervisors);

        mockMvc.perform(get("/project/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout"))
                .andExpect(model().attribute("supervisors", Arrays.asList(supervisors)))
                .andExpect(model().attribute("content", "project-add"));
    }

    @Test
    void testHandleAddProjectFormSuccess() throws Exception {
        String name = "New Project";
        String description = "Desc";
        int supId = 2;
        String email = "user@example.com";

        // stub security principal
        Map<String, String> userRow = new HashMap<>();
        userRow.put("role_name", "Supervisor");

        dbMock.when(() -> DatabaseManager.getSupervisors()).thenReturn(new String[]{});
        dbMock.when(DatabaseManager::getUserRowByEmail).thenReturn(userRow);
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
