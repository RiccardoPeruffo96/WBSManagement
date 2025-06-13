package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;
import it.univr.wbsmanagement.models.User;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Controller for the weekly “house tracking” view:
 * shows a read‐only table of hours per day (Mon–Sun),
 * il totale della settimana e le ore contrattuali,
 * più il form per inserire nuove ore.
 */
@Controller
public class HomeTrackingController {

    /**
     * Display the hours charged by the user for a specific day.
     *
     * @param model Thymeleaf model.
     * @return the main layout view with content set to "home-tracking".
     */
    @GetMapping("/home-tracking/{targetDay}/add-home-tracking")
    public String showTrackingAddPage(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDay,
            Model model
    ) {
        // 1) Current user & ID
        User currentUser = DatabaseManager.getUser();
        int userId = currentUser.getUserId();

        // 2) Fetch hours per day (map LocalDate->Double)
        HashMap<LocalDate, HashMap<Integer, HashMap<Integer, Double>>> userDay = DatabaseManager.getUsersAndAssignmentsHoursByRangeDay(userId, targetDay, targetDay);

        // 3) Totale ore caricate giornaliere
        if(userDay.containsKey(targetDay)) {

            HashMap<Integer, HashMap<Integer, Double>> dayHours = userDay.get(targetDay);

            HashMap<String, Double> hoursMap = new HashMap<>();
            HashMap<Integer, String> projectTrack = new HashMap<>();

            for (Map.Entry<Integer, HashMap<Integer, Double>> hoursByProjectId : dayHours.entrySet()) {

                // Retrieve project ID
                int projectId = hoursByProjectId.getKey();

                for (Map.Entry<Integer, Double> hourEntry : hoursByProjectId.getValue().entrySet()) {
                    // Retrieve task ID and hours
                    int taskId = hourEntry.getKey();
                    double hours = hourEntry.getValue();

                    // Process or display the hours as needed
                    projectTrack.computeIfAbsent(projectId, k -> DatabaseManager.getProjectTitleById(projectId, true));
                    String taskName = DatabaseManager.getTaskTitleNameById(taskId, true);

                    hoursMap.put("Project: " + projectTrack.get(projectId) + "; Task: " + taskName, hours);
                }
            }
            model.addAttribute("hourCommission", hoursMap);
        }

        // 4) Add list of tasks available for the user
        HashMap<String, String> tasks_available = DatabaseManager.getRetrieveTimeEntriesAvaibilityByUserAndDay(userId, targetDay, true);
        HashMap<String, String> tasks_not_working = DatabaseManager.getNonWorkingTasks(true);
        for (Map.Entry<String, String> entry : tasks_not_working.entrySet()) {
            // Add the task to the available tasks with a note that it is not working
            tasks_available.put(entry.getKey(), entry.getValue());
        }

        model.addAttribute("targetDay", targetDay);
        model.addAttribute("tasks_available", tasks_available);

        model.addAttribute("content", "add-home-tracking");

        return "layout";
    }

    /**
     * Handles adding a researcher to the task_assignments table.
     */
    @PostMapping("/home-tracking/{targetDay}/add-home-tracking")
    public String handleAddAssignment(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDay,
            @RequestParam int taskId,
            @RequestParam double hours,
            Model model
    ) {
        // 1) Current user & ID
        User currentUser = DatabaseManager.getUser();
        int userId = currentUser.getUserId();

        if ((int) hours <= 0) {
            model.addAttribute("addTimeEntryMessage", "Please enter a valid number of hours");
            // reload everything
            return showTrackingAddPage(targetDay, model);
        }

        boolean time_insert_insert_status = DatabaseManager.insertTimeEntry(userId, taskId, targetDay, hours);
        boolean task_assignments_insert_status = DatabaseManager.updateEffortConsumedInTaskAssignments(userId, taskId, (int) hours, 1);

        if (time_insert_insert_status && task_assignments_insert_status) {
            model.addAttribute("addTimeEntryMessage", "Hours entry added successfully");
        }
        else {
            model.addAttribute("addTimeEntryMessage", "Failed to add hour entry");

            // Rollback operation if insertion failed
            if(!(time_insert_insert_status && task_assignments_insert_status)) {

                DatabaseManager.removeTimeEntryAndTaskAssignmentByUserIdAndTaskId(userId, taskId, targetDay);

            }
        }

        // reload everything
        return showTrackingAddPage(targetDay, model);
    }

    /**
     * Handles adding a researcher to the task_assignments table.
     */
    @PostMapping("/home-tracking/{targetDay}/add-home-tracking/remove/{taskId}")
    public String handleRemoveAssignment(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDay,
            @PathVariable int taskId,
            Model model
    ) {
        // 1) Current user & ID
        User currentUser = DatabaseManager.getUser();
        int userId = currentUser.getUserId();

        // 2) Remove time entry and task assignment
        boolean remove_TimeEntry_and_TaskAssignment_status = DatabaseManager.removeTimeEntryAndTaskAssignmentByUserIdAndTaskId(userId, taskId, targetDay);
        model.addAttribute("removeTimeEntryMessage", remove_TimeEntry_and_TaskAssignment_status ? "Entity removed successfully" : "Failed to remove entity");

        // reload everything
        return showTrackingAddPage(targetDay, model);
    }
}
