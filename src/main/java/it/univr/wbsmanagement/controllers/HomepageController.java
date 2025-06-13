package it.univr.wbsmanagement.controllers;
import it.univr.wbsmanagement.database.DatabaseManager;
import it.univr.wbsmanagement.models.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * HomepageController handles requests for the homepage view.
 * When users are redirected here after login, they will see the homepage template.
 */
@Controller
public class HomepageController {

    /**
     * Displays the list of projects assigned to the authenticated user.
     *
     * <p>Before rendering, this method:
     * <ul>
     *   <li>Retrieves the current user's email from Spring Security.</li>
     *   <li>Loads the user's full record (including id and role) from the database.</li>
     *   <li>Calls DatabaseManager.getAssignedProjects(userId) to fetch only those projects.</li>
     * </ul>
     * The view fragment "project" will then render the header buttons, a static
     * message, and the list of project titles.</p>
     *
     * @param model the Spring Model for passing attributes to the view
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project")
    public String showProject(Model model) {

        // 1) Get current authentication and email
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // 2) Load full user row and set as current user
        Map<String,String> row = DatabaseManager.getUserRowByEmail(email);

        String roleName = row.get("role_name");
        int userId = DatabaseManager.getRoleId(roleName);

        User currentUser = new User(userId, email, roleName);
        DatabaseManager.setUser(currentUser);

        // 3) Fetch the projects assigned to this user
        String[] assignedProjects = DatabaseManager.getAssignedProjects(userId, false);
        model.addAttribute("assignedProjects", assignedProjects);

        // Make role available to the template
        model.addAttribute("role", roleName);

        model.addAttribute("content", "project");
        return "layout";
    }

    /**
     * Loads the home tracking fragment inside the layout.
     *
     * @param model the Spring model to inject attributes into the view.
     * @return the Thymeleaf layout page.
     */
    @GetMapping("/home-tracking")
    public String showHomeTracking(Model model) {
        model.addAttribute("content", "home-tracking");
        return "layout";
    }

    /**
     * Loads the save report fragment inside the layout.
     *
     * @param model the Spring model to inject attributes into the view.
     * @return the Thymeleaf layout page.
     */
    @GetMapping("/save-report")
    public String showSaveReport(Model model) {
        model.addAttribute("content", "save-report");
        return "layout";
    }

    @GetMapping("/homepage")
    public String showHomepage(Model model) {
        // Fetch Spring Security principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Load from DB
        Map<String, String> row = DatabaseManager.getUserRowByEmail(email);
        int roleId = DatabaseManager.getRoleId(row.get("role_name"));

        // Create our model.User (user_id not used for role checks)
        User currentUser = new User(-1, email, row.get("role_name"), roleId);
        DatabaseManager.setUser(currentUser);

        model.addAttribute("content", "homepage");
        return "layout";
    }
}
