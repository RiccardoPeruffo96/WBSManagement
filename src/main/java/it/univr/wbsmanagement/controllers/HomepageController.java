package it.univr.wbsmanagement.controllers;
import it.univr.wbsmanagement.database.DatabaseManager;
import it.univr.wbsmanagement.models.User;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        // 2) Load full user row and set as current user getUserIdByEmail
        Map<String,String> row = DatabaseManager.getUserRowByEmail(email);

        String roleName = row.get("role_name");
        int userId = Integer.parseInt(row.get("user_id"));

        User currentUser = new User(userId, email, roleName);
        DatabaseManager.setUser(currentUser);

        // 3) Fetch the projects assigned to this user
        //SELECT u.email, u.password, u.id as user_id, r.role_name, r.id as role_id
        String[] assignedProjects = DatabaseManager.getAssignedProjects(userId, true);

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
    @GetMapping("/home-tracking/{date}")
    public String showHomeTracking(@PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {

        LocalDate today = date;

        // 1) Determine week range: Monday -> Sunday
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate prevMonday = monday.minusDays(7);
        //LocalDate prevSunday = sunday.minusDays(7);
        LocalDate nextMonday = monday.plusDays(7);
        //LocalDate nextSunday = sunday.plusDays(7);

        // 2) Current user & ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //String email = auth.getName();
        User currentUser = DatabaseManager.getUser();

        // int uid = DatabaseManager.getUserIdByEmail(email);
        // currentUser = new User(uid, email, DatabaseManager.getUserRole(email));
        int userId = currentUser.getUserId();

        // 3) Fetch hours per day (map LocalDate->Double)
        HashMap<LocalDate, HashMap<Integer, HashMap<Integer, Double>>> userWeek = DatabaseManager.getUsersAndAssignmentsHoursByRangeDay(userId, monday, sunday);

        // 4) Totale ore e ore contrattuali
        int contractHours = DatabaseManager.getWorkingHoursWeekly(userId);
        double weeklyTotal = 0.0;

        // 5) Build ordered lists for Thymeleaf
        HashMap<LocalDate, Double> dailyMap = new HashMap<>();

        for(LocalDate actual_day = monday; actual_day.isBefore(sunday) || actual_day.isEqual(sunday); actual_day = actual_day.plusDays(1)) {
            // Add the single day map
            HashMap<Integer, HashMap<Integer, Double>> userDay = userWeek.get(actual_day);

            if (userWeek.get(actual_day) == null) {
                // If no data for this day, initialize to 0.0
                dailyMap.put(actual_day, 0.0);
                continue;
            }

            // Sum all hours for this day
            double totalHours = userDay.values().stream()
                .flatMap(hourMap -> hourMap.values().stream())
                .mapToDouble(Double::doubleValue)
                .sum();

            dailyMap.put(actual_day, totalHours);
            weeklyTotal += totalHours;
        }

        // 6) Task assegnati per il form
        List<String> tasks = DatabaseManager.getTasksByUser(userId, true);

        // 7) Popola il modello
        model.addAttribute("today", today);
        model.addAttribute("monday", monday);
        model.addAttribute("prevMonday", prevMonday);
        model.addAttribute("nextMonday", nextMonday);
        model.addAttribute("sunday", sunday);
        model.addAttribute("workweek", dailyMap);
        model.addAttribute("weeklyTotal", weeklyTotal);
        model.addAttribute("contractHours", contractHours);
        model.addAttribute("tasks", tasks);
        model.addAttribute("content", "home-tracking");

        return "layout";
    }

    /**
     * Loads the save report fragment inside the layout.
     *
     * @param model the Spring model to inject attributes into the view.
     * @return the Thymeleaf layout page.
     */
    @GetMapping("/save-report/{date}")
    public String showSaveReport(@PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        LocalDate today = date;

        // 1) Determine month range
        LocalDate actualFirstDayOfMonth = today.minusDays(today.getDayOfMonth() - 1);
        LocalDate prevFirstDayOfMonth = actualFirstDayOfMonth.minusMonths(1);
        LocalDate nextFirstDayOfMonth = actualFirstDayOfMonth.plusMonths(1);
        LocalDate actualLastDayOfMonth = nextFirstDayOfMonth.minusDays(1);

        //
        model.addAttribute("today", today);
        model.addAttribute("actualFirstDayOfMonth", actualFirstDayOfMonth);
        model.addAttribute("actualLastDayOfMonth", actualLastDayOfMonth);
        model.addAttribute("prevFirstDayOfMonth", prevFirstDayOfMonth);
        model.addAttribute("nextFirstDayOfMonth", nextFirstDayOfMonth);

        model.addAttribute("content", "save-report");
        return "layout";
    }

    /**
     * Displays the homepage view after successful login.
     * <p>
     * This method retrieves the current user's email from Spring Security,
     * loads their full user record from the database, and sets the user in
     * the DatabaseManager for further use.
     * </p>
     *
     * @param model the Spring Model to pass attributes to the view
     * @return the Thymeleaf layout template for the homepage
     */
    @GetMapping("/homepage")
    public String showHomepage(Model model) {
        // Fetch Spring Security principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Load from DB
        //SELECT u.email, u.password, u.id as user_id, r.role_name, r.id as role_id
        Map<String, String> row = DatabaseManager.getUserRowByEmail(email);
        int roleId = Integer.parseInt(row.get("role_id"));
        int userId = Integer.parseInt(row.get("user_id"));

        // Create our model.User (user_id not used for role checks)
        User currentUser = new User(userId, email, row.get("role_name"), roleId);
        DatabaseManager.setUser(currentUser);


        model.addAttribute("today", LocalDate.now());

        model.addAttribute("content", "homepage");
        return "layout";
    }
}
