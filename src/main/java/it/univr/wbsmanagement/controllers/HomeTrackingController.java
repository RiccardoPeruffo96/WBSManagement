package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;
import it.univr.wbsmanagement.models.User;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/house-tracking")
public class HouseTrackingController {

    /**
     * Display the tracking page for a given week.
     *
     * @param weekStart optional start of week (ISO date, Monday); defaults to this week’s Monday.
     * @param model     Thymeleaf model.
     * @return the main layout view with content set to "house-tracking".
     */
    @GetMapping
    public String showTrackingPage(
            @RequestParam(name = "weekStart", required = false)
            @DateTimeFormat(iso = ISO.DATE) LocalDate weekStart,
            Model model
    ) {
        // 1) Determine week range: Monday → Sunday
        LocalDate monday = (weekStart != null)
                ? weekStart
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        // 2) Current user & ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = DatabaseManager.getUser();
        // se non è già stato fatto, si potrebbe inizializzare in login:
        // int uid = DatabaseManager.getUserIdByEmail(email);
        // currentUser = new User(uid, email, DatabaseManager.getUserRole(email));
        int userId = currentUser.getUserId();

        // 3) Fetch hours per day (map LocalDate→Double)
        Map<LocalDate, Double> dailyMap =
                DatabaseManager.getWeeklyHours(userId, monday, sunday);

        // 4) Build ordered lists for Thymeleaf
        List<LocalDate> weekDates = new ArrayList<>();
        List<Double>   weekHours = new ArrayList<>();
        for (LocalDate d = monday; !d.isAfter(sunday); d = d.plusDays(1)) {
            weekDates.add(d);
            weekHours.add(dailyMap.getOrDefault(d, 0.0));
        }

        // 5) Totale ore e ore contrattuali
        double weeklyTotal = weekHours.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        int contractedHours = DatabaseManager.getWorkingHoursWeekly(userId);

        // 6) Task assegnati per il form
        List<String> tasks = DatabaseManager.getTasksByUser(userId);

        // 7) Popola il modello
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("weekHours", weekHours);
        model.addAttribute("weeklyTotal", weeklyTotal);
        model.addAttribute("contractedHours", contractedHours);
        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedWeekStart", monday);
        model.addAttribute("content", "house-tracking");

        return "layout";
    }

    /**
     * Handle the submission of new hours for a given task & day.
     *
     * @param taskId    the chosen task ID.
     * @param date      the date of work (ISO format).
     * @param hours     the number of hours to log.
     * @param weekStart optional weekStart to preserve selection.
     * @return redirect back to the same week view.
     */
    @PostMapping("/record")
    public String recordHours(
            @RequestParam("taskId")   int taskId,
            @RequestParam("date")
            @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam("hours")    double hours,
            @RequestParam(name = "weekStart", required = false)
            @DateTimeFormat(iso = ISO.DATE) LocalDate weekStart
    ) {
        // Log hours via DatabaseManager
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = DatabaseManager.getUser();
        int userId = currentUser.getUserId();

        DatabaseManager.insertTimeEntry(userId, taskId, date, hours);

        // Redirect mantenendo la selezione della settimana
        String param = (weekStart != null) ? "?weekStart=" + weekStart : "";
        return "redirect:/house-tracking" + param;
    }
}
