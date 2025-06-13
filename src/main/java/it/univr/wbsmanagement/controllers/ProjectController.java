package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles project‐specific views such as the detail placeholder.
 */
@Controller
public class ProjectController {

    /**
     * Displays the “Add Project” form.
     *
     * <p>Retrieves a list of supervisors from the database,
     * formats each as "id - name", and makes it available
     * for the dropdown.</p>
     *
     * @param model the Spring Model for passing attributes to the view
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project/add")
    public String showAddProjectForm(Model model) {

        // fetch supervisors as an array of "id - email" strings
        String[] supervisorsArray = DatabaseManager.getSupervisors();  // returns String[] :contentReference[oaicite:0]{index=0}

        // convert to a List for Thymeleaf iteration
        List<String> supervisors = Arrays.asList(supervisorsArray);

        model.addAttribute("supervisors", supervisors);
        model.addAttribute("content", "project-add");
        return "layout";
    }

     /**
     * Handles the submission of the “Add Project” form.
     *
     * <p>Extracts the project name, description, and supervisor ID from the request,
     * retrieves the current admin’s user ID via Spring Security,
     * and attempts to insert a new project into the database.
     * On success, adds a message “Task added successfully” (in English);
     * on failure, adds “Failed to add task”.</p>
     *
     * @param name         the project title from the form
     * @param description  the project description from the form
     * @param supervisorId the selected supervisor’s user ID
     * @param model        the Spring Model for passing attributes to the view
     * @return the Thymeleaf layout template (same form, with a feedback message)
     */
    @PostMapping("/project/add")
    public String handleAddProjectForm(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("supervisorId") int supervisorId,
            Model model
    ) {
        // 1) Get current authenticated user’s email
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // 2) Look up full user row to get admin ID
        Map<String, String> row = DatabaseManager.getUserRowByEmail(email); //:contentReference[oaicite:1]{index=1}

        String roleName = row.get("role_name");
        int adminId = DatabaseManager.getRoleId(roleName);

        // 3) Insert the project
        boolean added = DatabaseManager.addProject(name, description, adminId, supervisorId); //:contentReference[oaicite:2]{index=2}

        // 4) Prepare feedback message
        if (added) {
            model.addAttribute("message", "Task added successfully");
        } else {
            model.addAttribute("message", "Failed to add task");
        }

        // 5) Re-load supervisors list for the form
        String[] supervisorsArray = DatabaseManager.getSupervisors();
        List<String> supervisors = Arrays.asList(supervisorsArray);
        model.addAttribute("supervisors", supervisors);

        model.addAttribute("content", "project-add");
        return "layout";
    }

    /**
     * Displays the “Search Project” form.
     *
     * <p>Fetches all active projects from the database (each as "id - title"),
     * and puts them in the model under "projects" so the dropdown can populate.</p>
     *
     * @param model the Spring Model for passing attributes to the view
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project/search")
    public String showSearchProjectForm(Model model) {
        // Load all active projects as an array of "id - title"
        String[] projectsArray = DatabaseManager.getProjectsActive(false);  // returns String[] of "id - title"
        model.addAttribute("projects", projectsArray);
        model.addAttribute("content", "project-search");
        return "layout";
    }

    /**
     * Handles the “Search Project” form submission.
     *
     * <p>Reads the selected projectId from the request, then redirects
     * the user to /project/{id} so they see that project's detail page.</p>
     *
     * @param projectId the numeric project ID chosen in the dropdown
     * @return a redirect to /project/{projectId}
     */
    @PostMapping("/project/search")
    public String handleSearchProject(@RequestParam("projectId") int projectId) {
        // Redirect to the chosen project's detail page
        return "redirect:/project/" + projectId;
    }

    /**
     * Displays the “Search Archived Project” form.
     *
     * <p>Fetches all archived projects from the database (each as "id - title"),
     * and puts them in the model under "archivedProjects" so the dropdown can populate.</p>
     *
     * @param model the Spring Model for passing attributes to the view
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project/archived")
    public String showArchivedProjectForm(Model model) {
        // Load all archived projects as an array of "id - title"
        String[] archivedArray = DatabaseManager.getProjectsArchived(false);  // returns String[] of "id - title"
        model.addAttribute("archivedProjects", archivedArray);
        model.addAttribute("content", "project-archived");
        return "layout";
    }

    /**
     * Handles the “Search Archived Project” form submission.
     *
     * <p>Reads the selected projectId from the request, then redirects
     * the user to /project/{id} so they see that archived project's detail page.</p>
     *
     * @param projectId the numeric project ID chosen in the dropdown
     * @return a redirect to /project/{projectId}
     */
    @PostMapping("/project/archived")
    public String handleArchivedSearch(@RequestParam("projectId") int projectId) {
        // Redirect to the chosen project's detail page
        return "redirect:/project/" + projectId;
    }

    /**
     * Shows a placeholder details page for any given project key.
     *
     * <p>This will accept either a numeric ID ("1", "9") or a unique project name
     * in the path. We simply pass it through to the view for display.</p>
     *
     * @param projectKey the project identifier or name from the URL
     * @param model      Spring Model to pass attributes into the Thymeleaf template
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project/{projectKey}")
    public String showProjectDetails(@PathVariable("projectKey") String projectKey, Model model) {
        // Pass the raw key (could be "1" or "MyProjName") into the view as-is.
        model.addAttribute("projectKey", projectKey);
        model.addAttribute("content", "project-details");
        return "layout";
    }
}
