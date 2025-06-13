package it.univr.wbsmanagement.controllers;

import it.univr.wbsmanagement.database.DatabaseManager;
import it.univr.wbsmanagement.models.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

/**
 */
@Controller
public class SaveReportController {

    /**
     * Handles the request to display the report saving page.
     *
     * @param targetDay The target day for which the report is being saved.
     * @param model     The model to add attributes to the view.
     * @return The name of the view to render.
     */
    @PostMapping("/save-report/{targetDay}/print-report")
    public ResponseEntity<InputStreamResource> handlePrintReport(
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate targetDay
    ) throws IOException {
        /*
        // 1) Current user & ID
        User currentUser = DatabaseManager.getUser();
        int userId = currentUser.getUserId();

        // 1) Determine month range
        LocalDate firstDayOfMonth = LocalDate.from(targetDay.getMonth());
        LocalDate prevFirstDayOfMonth = firstDayOfMonth.plusMonths(1);
        LocalDate nextFirstDayOfMonth = firstDayOfMonth.minusMonths(1);
        LocalDate lastDayOfMonth = nextFirstDayOfMonth.minusDays(1);
        */

        String monthName = targetDay.getYear() + "_" + targetDay.getMonth().toString();

        // 1) Load the file from classpath:/static/reports/placeholder.pdf
        ClassPathResource pdf = new ClassPathResource("static/reports/reportSample.pdf");

        // 2) Prepare headers: attachment + suggested filename
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report_" + monthName + ".pdf\"");

        // 3) Build the response entity
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pdf.contentLength())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf.getInputStream()));
    }
}
