package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.time.LocalDate;

/**
 * Controller for handling project-related requests.
 *
 * <p>This class provides endpoints for adding new projects,
 * searching existing projects, and viewing archived projects.</p>
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
        int adminId = Integer.parseInt(row.get("role_id"));

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
        String[] projectsArray = DatabaseManager.getProjectsActive(true);  // returns String[] of "id - title"
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
        String[] archivedArray = DatabaseManager.getProjectsArchived(true);  // returns String[] of "id - title"
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
     * Shows the detail page for a given project key (ID or name). In particular,
     * this method now loads all work packages for that project, and for each work package
     * loads its tasks, assembling a nested list for Thymeleaf rendering.
     *
     * @param projectKey the project identifier (e.g. "9" or a unique project name)
     * @param model      Spring Model to pass attributes to the Thymeleaf template
     * @return the Thymeleaf layout template
     */
    @GetMapping("/project/{projectKey}")
    public String showProjectDetails(
            @PathVariable("projectKey") String projectKey,
            Model model
    ) {
        // 1) Parse projectKey into an integer ID
        int projectId;
        try {
            projectId = Integer.parseInt(projectKey);
        } catch (NumberFormatException e) {
            // If you ever allow non-numeric keys, you could look up the ID by name here.
            // For now, assume projectKey is always numeric.
            return "redirect:/project";
        }

        // 2) Recupera l’utente corrente (id e ruolo) dal SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Map<String, String> row = DatabaseManager.getUserRowByEmail(email);

        String currentUserRole = row.get("role_name");
        int currentUserId = Integer.parseInt(row.get("user_id"));

        // 3) Verifica se l’utente corrente è il supervisore di questo progetto
        //    (supponendo che tu abbia un metodo DB per ottenere l’id del supervisore del progetto)
        int supervisorId = DatabaseManager.getSupervisorIdByProject(projectId);
        boolean isSupervisor = (currentUserId == supervisorId);
        boolean isResearcher = currentUserRole.equalsIgnoreCase("RESEARCHER");

        // 4) Aggiungi i tre attributi al model
        model.addAttribute("role", currentUserRole);
        model.addAttribute("isSupervisor", isSupervisor);;
        model.addAttribute("isResearcher", isResearcher);
        model.addAttribute("projectKey", projectKey);

        // 2) Retrieve the project title (optional display elsewhere):
        String projectTitle = DatabaseManager.getProjectTitleById(projectId, false);
        model.addAttribute("projectTitle", projectTitle);

        // 3) Load all work packages as List<String> of "id - title"
        List<String> wpStrings = DatabaseManager.getWorkPackagesByProject(projectId, true); //:contentReference[oaicite:2]{index=2}

        // 4) For each work package, load its tasks (List<String> of "id - title")
        List<Map<String, Object>> workPackages = new ArrayList<>();
        for (String wp : wpStrings) {
            // wp is like "3 - Design phase"
            String[] wpParts = wp.split(" - ", 2);
            int wpId = Integer.parseInt(wpParts[0]);
            String wpTitle = wpParts[1];

            // Fetch tasks for this work package
            List<String> tasks = DatabaseManager.getTasksByWorkPackages(wpId, true); //:contentReference[oaicite:3]{index=3}

            // Build a map: {"id": 3, "title": "Design phase", "tasks": List<String> }
            Map<String, Object> wpEntry = new HashMap<>();
            wpEntry.put("id", wpId);
            wpEntry.put("title", wpTitle);
            wpEntry.put("tasks", tasks);

            workPackages.add(wpEntry);
        }

        // 5) Add the nested list to the model
        model.addAttribute("workPackages", workPackages);

        // 6) Project archived status
        String messageArchived = (DatabaseManager.getIsProjectsArchivedById(projectId)) ? "Archived" : "Active";
        model.addAttribute("message", messageArchived);

        model.addAttribute("content", "project-details");

        return "layout";
    }

    /**
     * Populates common attributes for project-related views.
     * This method is used to avoid code duplication in multiple endpoints.
     *
     * @param projectKey the project identifier (ID or name)
     * @param model      the Spring Model to add attributes to
     */
    private void populateCommonAttributes(String projectKey, Model model) {
        // 1) current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Map<String, String> userRow = DatabaseManager.getUserRowByEmail(email);
        String currentUserRole = userRow.get("role_name");
        int currentUserId = Integer.parseInt(userRow.get("user_id"));
        model.addAttribute("role", currentUserRole);

        // 2) isSupervisor?
        int projectId = Integer.parseInt(projectKey);
        int supervisorId = DatabaseManager.getSupervisorIdByProject(projectId);
        model.addAttribute("isSupervisor", currentUserId == supervisorId);

        // 3) projectKey for URL building
        model.addAttribute("projectKey", projectKey);

        // 4) optional: project title
        String title = DatabaseManager.getProjectTitleById(projectId, true);
        model.addAttribute("projectTitle", title);
    }

    /**
     * GET: Show Manage Researchers page for a project.
     * This page allows adding/removing researchers to/from project_visibility.
     *
     * @param projectKey the project identifier (ID or name)
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for managing researchers
     */
    @GetMapping("/project/{projectKey}/manage-researchers")
    public String showManageResearchers(@PathVariable String projectKey, Model model) {
        // common attributes: role, isSupervisor, projectKey, etc.
        populateCommonAttributes(projectKey, model);

        int projectId = Integer.parseInt(projectKey);

        // Researchers not yet in project_visibility
        String[] availArr = DatabaseManager.getResearchersExcludingProjectId(projectId, true);
        model.addAttribute("availableResearchers", Arrays.asList(availArr));

        // Researchers already in project_visibility
        String[] assignedArr = DatabaseManager.getResearchersByProjectId(projectId, true);
        model.addAttribute("projectResearchers", Arrays.asList(assignedArr));

        model.addAttribute("content", "manage-researcher");
        return "layout";
    }

    /**
     * Adds a researcher to project_visibility.
     * Then reloads both lists and shows a feedback message.
     *
     * @param projectKey the project identifier (ID or name)
     * @param researcherId the ID of the researcher to add
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for managing researchers
     */
    @PostMapping("/project/{projectKey}/manage-researchers/add")
    public String handleAddResearcher(
            @PathVariable String projectKey,
            @RequestParam("researcherId") int researcherId,
            Model model
    ) {
        populateCommonAttributes(projectKey, model);
        int projectId = Integer.parseInt(projectKey);

        boolean success = DatabaseManager.addReasearchersToProject(projectId, researcherId);
        model.addAttribute("addMessage", success ? "Researcher added successfully" : "Failed to add researcher");

        // refresh lists
        model.addAttribute("availableResearchers", Arrays.asList(DatabaseManager.getResearchersExcludingProjectId(projectId, true)));
        model.addAttribute("projectResearchers", Arrays.asList(DatabaseManager.getResearchersByProjectId(projectId, true)));

        model.addAttribute("content", "manage-researcher");
        return "layout";
    }

    /**
     * Adds a researcher to project_visibility.
     * Then reloads both lists and shows a feedback message.
     *
     * @param projectKey the project identifier (ID or name)
     * @param researcherId the ID of the researcher to add
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for managing researchers
     */
    @PostMapping("/project/{projectKey}/manage-researchers/remove")
    public String handleRemoveResearcher(
            @PathVariable String projectKey,
            @RequestParam("researcherId") int researcherId,
            Model model
    ) {
        populateCommonAttributes(projectKey, model);
        int projectId = Integer.parseInt(projectKey);

        // You may need to implement this in DatabaseManager similarly to addReasearchersToProject
        boolean success = DatabaseManager.removeResearcherFromProject(projectId, researcherId);
        model.addAttribute("removeMessage", success ? "Researcher removed successfully" : "Failed to remove researcher");

        // refresh lists
        model.addAttribute("availableResearchers", Arrays.asList(DatabaseManager.getResearchersExcludingProjectId(projectId, true)));
        model.addAttribute("projectResearchers", Arrays.asList(DatabaseManager.getResearchersByProjectId(projectId, true)));

        model.addAttribute("content", "manage-researcher");
        return "layout";
    }

    /**
     * GET: Show Add Work Package form.
     *
     * <p>This method populates the model with common attributes
     * and sets the content to "add-workpackage" for rendering.</p>
     *
     * @param projectKey the project identifier (ID or name)
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for adding a work package
     */
    @GetMapping("/project/{projectKey}/add-workpackage")
    public String showAddWorkPackageForm(@PathVariable String projectKey, Model model) {
        populateCommonAttributes(projectKey, model);
        model.addAttribute("content", "add-workpackage");
        return "layout";
    }

    /**
     * POST: Handle Add Work Package submission.
     *
     * <p>This method processes the form submission for adding a work package,
     * validates the input, and attempts to insert it into the database.
     * It then populates the model with a success or failure message.</p>
     *
     * @param projectKey   the project identifier (ID or name)
     * @param name         the name of the work package
     * @param description  the description of the work package
     * @param startDate    the start date of the work package
     * @param endDate      the end date of the work package
     * @param model        the Spring Model to add attributes to
     * @return the Thymeleaf layout template with feedback message
     */
    @PostMapping("/project/{projectKey}/add-workpackage")
    public String handleAddWorkPackage(
            @PathVariable String projectKey,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        int projectId = Integer.parseInt(projectKey);
        boolean success = DatabaseManager.addWorkPackage(
                projectId, name, description, startDate, endDate
        );
        populateCommonAttributes(projectKey, model);
        model.addAttribute("content", "add-workpackage");
        model.addAttribute("message",
                success ? "Work package added successfully"
                        : "Failed to add work package");
        return "layout";
    }

    /**
     * GET: Show Add Task form for a specific project.
     *
     * <p>This method populates the model with common attributes,
     * retrieves work packages for the dropdown, and sets the content
     * to "add-task" for rendering.</p>
     *
     * @param projectKey the project identifier (ID or name)
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for adding a task
     */
    @GetMapping("/project/{projectKey}/add-task")
    public String showAddTaskForm(@PathVariable String projectKey, Model model) {
        populateCommonAttributes(projectKey, model);

        // load work packages for dropdown
        int projectId = Integer.parseInt(projectKey);
        List<String> wpStrings = DatabaseManager.getWorkPackagesByProject(projectId, true);

        // build id/title maps
        List<Map<String, Object>> workPackages = new ArrayList<>();
        for (String wp : wpStrings) {
            String[] parts = wp.split(" - ", 2);
            Map<String,Object> m = new HashMap<>();
            m.put("id", Integer.parseInt(parts[0]));
            m.put("title", parts[1]);
            // add to the list
            workPackages.add(m);
        }

        String[] taskPriority = DatabaseManager.getAllPriority(true);
        String[] taskStatus = DatabaseManager.getAllStatus(true);

        model.addAttribute("workPackages", workPackages);
        model.addAttribute("taskPriority", taskPriority);
        model.addAttribute("taskStatus", taskStatus);
        model.addAttribute("content", "add-task");
        return "layout";
    }

    /**
     * POST: Handle Add Task submission.
     *
     * <p>This method processes the form submission for adding a task,
     * validates the deadline, and attempts to insert it into the database.
     * It then populates the model with a success or failure message.</p>
     *
     * @param projectKey       the project identifier (ID or name)
     * @param workPackageId    the ID of the work package to which the task belongs
     * @param title            the title of the task
     * @param description      the description of the task
     * @param effort           the effort estimate for the task
     * @param deadline         the deadline for the task
     * @param taskPriority     the priority of the task
     * @param taskStatus       the status of the task
     * @param model            the Spring Model to add attributes to
     * @return the Thymeleaf layout template with feedback message
     */
    @PostMapping("/project/{projectKey}/add-task")
    public String handleAddTask(
            @PathVariable String projectKey,
            @RequestParam int workPackageId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int effort,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @RequestParam int taskPriority,
            @RequestParam int taskStatus,
            Model model
    ) {
        boolean taskDeadlineValidity = DatabaseManager.checkTaskDeadlineValidity(workPackageId, deadline);

        if( !taskDeadlineValidity ) {
            model.addAttribute("message", "Invalid deadline for the task");
            showAddTaskForm(projectKey, model);
            return "layout";
        }

        // reload form data
        int taskId = DatabaseManager.addTask(workPackageId, title, description, effort, 0, deadline, taskPriority, taskStatus);
        showAddTaskForm(projectKey, model);
        model.addAttribute("message", taskId == -1 ? "Failed to add task" : "Task added successfully" );
        return "layout";
    }

    /**
     * GET: Show Manage Milestone placeholder.
     *
     * <p>This method populates the model with common attributes
     * and sets the content to "manage-milestone" for rendering.</p>
     *
     * @param projectKey the project identifier (ID or name)
     * @param model      the Spring Model to add attributes to
     * @return the Thymeleaf layout template for managing milestones
     */ 
    @GetMapping("/project/{projectKey}/manage-milestone")
    public String showManageMilestone(@PathVariable String projectKey, Model model) {
        populateCommonAttributes(projectKey, model);
        model.addAttribute("content", "manage-milestone");
        return "layout";
    }

    /**
     * POST: Handle Archive Project request.
     *
     * <p>This method attempts to archive the project with the given ID,
     * and redirects back to the project list.</p>
     *
     * @param projectKey the project identifier (ID or name)
     * @return a redirect to the project list
     */
    @GetMapping("/project/{projectKey}/archive")
    public String handleArchiveProject(@PathVariable String projectKey) {
        boolean archive = DatabaseManager.archiveProject(Integer.parseInt(projectKey));
        return "redirect:/project";
    }

    /**
     * GET: Show the Work Package Details page, with current dates.
     *
     * @param projectKey       project ID as path variable
     * @param workPackageId    work package ID as path variable
     * @param model            Spring MVC model
     * @return layout with content="workpackage-details"
     */
    @GetMapping("/project/{projectKey}/workpackage/{workPackageId}")
    public String showWorkPackageDetails(
            @PathVariable String projectKey,
            @PathVariable int workPackageId,
            Model model
    ) {
        // common attributes (role, isSupervisor, projectKey, projectTitle, etc.)
        populateCommonAttributes(projectKey, model);

        // 1) Load WP details
        Map<String, String> wpMeta = DatabaseManager.getWorkPackageFromId(workPackageId);
        String wpTitle   = wpMeta.get("wp_title");
        String startDate = wpMeta.get("wp_sdate");
        String endDate   = wpMeta.get("wp_edate");

        model.addAttribute("workPackageId", workPackageId);
        model.addAttribute("workPackageTitle", wpTitle);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        // 2) Render fragment
        model.addAttribute("content", "workpackage-details");
        return "layout";
    }

    /**
     * POST: Update the start/end dates of a work package.
     *
     * @param projectKey       project ID as path variable
     * @param workPackageId    work package ID as path variable
     * @param startDate        new start date from form
     * @param endDate          new end date from form
     * @param model            Spring MVC model
     * @return forwards back to the same details page with a message
     */
    @PostMapping("/project/{projectKey}/workpackage/{workPackageId}/update-dates")
    public String handleUpdateWorkPackageDates(
            @PathVariable String projectKey,
            @PathVariable int workPackageId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            Model model
    ) {
        // common attributes
        populateCommonAttributes(projectKey, model);

        boolean ok = DatabaseManager.editWorkPackage(workPackageId, startDate, endDate);
        model.addAttribute("updateMessage", ok ? "Dates updated successfully" : "Failed to update dates");

        // re‐load the WP details so the form shows the new values
        Map<String, String> wpMeta = DatabaseManager.getWorkPackageFromId(workPackageId);
        model.addAttribute("workPackageId", workPackageId);
        model.addAttribute("workPackageTitle", wpMeta.get("wp_title"));
        model.addAttribute("startDate", wpMeta.get("wp_sdate"));
        model.addAttribute("endDate", wpMeta.get("wp_edate"));

        model.addAttribute("content", "workpackage-details");
        return "layout";
    }

    /**
     * POST: Delete the given work package and redirect back to the project.
     *
     * @param projectKey       project ID as path variable
     * @param workPackageId    work package ID to delete
     * @return redirect to /project/{projectKey}
     */
    @PostMapping("/project/{projectKey}/workpackage/{workPackageId}/delete")
    public String handleDeleteWorkPackage(
            @PathVariable String projectKey,
            @PathVariable int workPackageId
    ) {
        DatabaseManager.deleteWorkPackageById(workPackageId);
        return "redirect:/project/" + projectKey;
    }
}
