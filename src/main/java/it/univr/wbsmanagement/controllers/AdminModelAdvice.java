package it.univr.wbsmanagement.controllers;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import it.univr.wbsmanagement.database.DatabaseManager;

/**
 * AdminModelAdvice is a controller advice that adds common attributes to the model
 * for all admin-related views.
 * It specifically adds the count of pending recover-credentials requests.
 */
@ControllerAdvice
public class AdminModelAdvice {

    /**
     * Adds the count of pending recover-credentials requests to the model.
     *
     * @param model the model to which the pending count will be added
     */
    @ModelAttribute
    public void addPendingCount(Model model) {
        int pending = DatabaseManager.countRecoverCredentialsRequestsActive();
        model.addAttribute("pendingCount", pending);
    }
}
