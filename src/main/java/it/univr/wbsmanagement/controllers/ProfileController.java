package it.univr.wbsmanagement.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.univr.wbsmanagement.database.DatabaseManager;

/**
 * ProfileController handles password change requests when the user
 * clicks on Options/Profile.
 */
@Controller
public class ProfileController {

    /**
     * Displays the change password form.
     *
     * @param model the Spring model to inject attributes into the view.
     * @return the Thymeleaf layout page showing the profile fragment.
     */
    @GetMapping("/profile")
    public String showProfile(Model model) {
        model.addAttribute("content", "profile");
        return "layout";
    }

    /**
     * Processes the password change request.
     *
     * @param currentPassword the user's current password.
     * @param newPassword the new password to set.
     * @param confirmPassword the confirmation of the new password.
     * @param model the Spring model to inject attributes into the view.
     * @return the Thymeleaf layout page with a success or error message.
     */
    @PostMapping("/profile")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        String message;
        if (!newPassword.equals(confirmPassword)) {
            message = "New password and confirmation do not match.";
        } else {
            // Verify that the current password matches the stored one
            var userRow = DatabaseManager.getUserRowByEmail(email);
            if (userRow == null || !userRow.get("password").equals(currentPassword)) {
                message = "Current password is incorrect.";
            } else {
                boolean success = DatabaseManager.updateUserPassword(email, newPassword);
                message = success
                        ? "Password changed successfully."
                        : "An error occurred while updating the password.";
            }
        }

        model.addAttribute("passwordChangeMessage", message);
        model.addAttribute("content", "profile");
        return "layout";
    }
}
