package it.univr.wbsmanagement.controllers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import it.univr.wbsmanagement.database.DatabaseManager;

/**
 * Adds the current count of pending recover-credentials requests
 * to every model, so the sidebar can display it.
 */
@ControllerAdvice
public class AdminModelAdvice {

    @ModelAttribute
    public void addPendingCount(Model model) {
        int pending = DatabaseManager.countRecoverCredentialsRequestsActive();
        model.addAttribute("pendingCount", pending);
    }
}
