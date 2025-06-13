package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class TaskController {

    /**
     * Shows the Task Details page, including:
     * - project/work package/task names
     * - dropdowns for adding/removing researchers
     * - current assignments table
     */
    @GetMapping("/task/{taskId}")
    public String showTaskDetails(@PathVariable int taskId, Model model) {
        // 1. Retrieve task related project And work package data
        Map<String, String> metadata = DatabaseManager.getProjectAndWorkPackageFromTaskId(taskId);
        String taskTitle = metadata.get("t_title");
        int workPackageId = Integer.parseInt(metadata.get("wp_id"));
        String workPackageTitle = metadata.get("wp_title");
        int projectId = Integer.parseInt(metadata.get("proj_id"));
        String projectTitle = metadata.get("proj_title");

        String wpStartDate = metadata.get("wp_sdate");
        String wpEndDate = metadata.get("wp_edate");
        String tDeadline = metadata.get("t_deadline");
        String tPriorityId = metadata.get("t_priorityId");
        String tStatusId = metadata.get("t_statusId");

        model.addAttribute("taskId", taskId);
        model.addAttribute("taskTitle", taskTitle);
        model.addAttribute("workPackageId", workPackageId);
        model.addAttribute("workPackageTitle", workPackageTitle);
        model.addAttribute("projectId", projectId);
        model.addAttribute("projectTitle", projectTitle);

        String priority_name = DatabaseManager.getPriorityById(Integer.parseInt(tPriorityId), true);
        String status_name = DatabaseManager.getStatusById(Integer.parseInt(tStatusId), true);

        model.addAttribute("wpStartDate", wpStartDate);
        model.addAttribute("wpEndDate", wpEndDate);
        model.addAttribute("tDeadline", tDeadline.substring(0, 10)); // format YYYY-MM-DD
        model.addAttribute("priority_name", priority_name);
        model.addAttribute("status_name", status_name);

        // 2. Researchers available to assign (from project_visibility)
        String[] avail = DatabaseManager.getResearchersByProjectIdAndExcludedByTaskId(projectId, taskId, true);
        model.addAttribute("availableResearchers", Arrays.asList(avail));

        // 3. Use a single call to getUsersAndAssignmentsHoursByTasks:
        //    drives both the remove-dropdown and the assignments table
        List<HashMap<String,String>> raw = DatabaseManager.getUsersAndAssignmentsHoursByTasks(taskId);

        List<Map<String,Object>> assignments = new ArrayList<>();
        List<String> assignedResearchers = new ArrayList<>();

        for (HashMap<String,String> row : raw) {
            // each map has one entry: key="id - email", value="consumed - hypothetic"
            for (Map.Entry<String,String> e : row.entrySet()) {
                String userKey   = e.getKey();   // e.g. "7 - alice@example.com"
                String hoursVal  = e.getValue(); // e.g. "5 - 8"

                String[] userParts  = userKey.split(" - ", 2);
                String[] effortParts= hoursVal.split(" - ", 2);

                // build table row
                Map<String,Object> entry = new HashMap<>();
                entry.put("userName", userParts[1]);
                entry.put("effortConsumed", Integer.parseInt(effortParts[0]));
                entry.put("effortHypothetic", Integer.parseInt(effortParts[1]));
                assignments.add(entry);

                // deposit for the "remove" dropdown
                assignedResearchers.add(userKey);
            }
        }

        model.addAttribute("assignments", assignments);
        model.addAttribute("assignedResearchers", assignedResearchers);

        String[] allPriority = DatabaseManager.getAllPriority(true);
        String[] allStatus = DatabaseManager.getAllStatus(true);
        model.addAttribute("allPriority", allPriority);
        model.addAttribute("allStatus", allStatus);

        // 4. Render task-details fragment
        model.addAttribute("content", "task-details");
        return "layout";
    }


    /**
     * Handles adding a researcher to the task_assignments table.
     */
    @PostMapping("/task/{taskId}/assignments/add")
    public String handleAddAssignment(
            @PathVariable int taskId,
            @RequestParam int userId,
            @RequestParam int effortHypothetic,
            Model model
    ) {
        boolean ok = DatabaseManager.addTaskAssignment(taskId, userId, effortHypothetic);
        model.addAttribute("addAssignmentMessage",
                ok ? "Assignment added successfully"
                        : "Failed to add assignment");
        // reload everything
        return showTaskDetails(taskId, model);
    }

    /**
     * Handles removing a researcher from the task_assignments table.
     */
    @PostMapping("/task/{taskId}/assignments/remove")
    public String handleRemoveAssignment(
            @PathVariable int taskId,
            @RequestParam int userId,
            Model model
    ) {
        boolean ok = DatabaseManager.removeResearcherFromTaskAssignments(taskId, userId);
        model.addAttribute("removeAssignmentMessage",
                ok ? "Assignment removed successfully"
                        : "Failed to remove assignment");
        // reload everything
        return showTaskDetails(taskId, model);
    }

    /**
     * Deletes the given task and redirects back to its parent project page.
     *
     * @param taskId the ID of the task to delete
     * @return a redirect to /project/{projectId}
     */
    @GetMapping("/task/{taskId}/delete")
    public String deleteTask(@PathVariable int taskId) {

        Map<String, String> metadata = DatabaseManager.getProjectAndWorkPackageFromTaskId(taskId);
        int projectId = Integer.parseInt(metadata.get("proj_id"));

        DatabaseManager.deleteTaskById(taskId);

        return "redirect:/project/" + projectId;
    }

    /**
     * Deletes the given task and redirects back to its parent project page.
     *
     * @param taskId the ID of the task to delete
     * @return a redirect to /project/{projectId}
     */
    @PostMapping("/task/{taskId}/assignments/updateStatusAndPriority")
    public String updateStatusAndPriority(@PathVariable int taskId,
                                          @RequestParam int priorityId,
                                          @RequestParam int statusId,
                                          Model model) {
        // Update the task's status and priority
        boolean ok = DatabaseManager.updateStatusAndPriority(taskId, priorityId, statusId);

        model.addAttribute("updateStatusAndPriority",
                ok ? "Status and priority updated successfully"
                        : "Failed to update status and priority");

        // reload everything
        return showTaskDetails(taskId, model);
    }
}
