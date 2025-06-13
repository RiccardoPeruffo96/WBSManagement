package it.univr.wbsmanagement.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.univr.wbsmanagement.database.DatabaseManager;

import java.util.List;
import java.util.Map;

/**
 * AdminController handles administrator actions such as creating new users.
 */
@Controller
@PreAuthorize("hasRole('Administrator')")
public class AdminController {

    /**
     * Displays the form for creating a new user.
     *
     * @param model the Spring model to inject attributes into the view
     * @return the Thymeleaf layout with the new-user fragment
     */
    @GetMapping("/admin/new-user")
    public String showNewUserForm(Model model) {
        // Fetch available role names for dropdown
        model.addAttribute("roles", DatabaseManager.getAllRoles());
        // Fragment key for Thymeleaf layout
        model.addAttribute("content", "new-user");
        return "layout";
    }

    /**
     * Processes the new user creation form submission.
     *
     * @param email        the email address of the new user
     * @param password     the password for the new user
     * @param role         the role to assign (must match one of the dropdown values)
     * @param weeklyHours  the weekly working hours for the new user
     * @param model        the Spring model for passing messages back to the view
     * @return the layout view showing success or error message
     */
    @PostMapping("/admin/new-user")
    public String createNewUser(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            @RequestParam("weeklyHours") String weeklyHours,
            Model model) {

        boolean success = DatabaseManager.addUser(email, password, role, weeklyHours);
        String message = success
                ? "User created successfully."
                : "Failed to create user. Email may already exist or inputs are invalid.";

        model.addAttribute("message", message);
        model.addAttribute("roles", DatabaseManager.getAllRoles());
        model.addAttribute("content", "new-user");
        return "layout";
    }

    /**
     * Display the change-role form.
     *
     * @param model Spring Model to inject attributes into the view.
     * @return the Thymeleaf layout page with the change-role fragment.
     */
    @GetMapping("/admin/change-role")
    public String showChangeRoleForm(Model model) {
        model.addAttribute("roles", DatabaseManager.getAllRoles());
        model.addAttribute("content", "change-role");
        return "layout";
    }

    /**
     * Process the change-role form submission.
     *
     * <p>Applies these rules:
     * <ul>
     *   <li>Cannot change your own role (throws error).</li>
     *   <li>If target user not found: show error.</li>
     *   <li>If target user already in requested role: show “already at target role”.</li>
     *   <li>Otherwise update and confirm success.</li>
     * </ul>
     * </p>
     *
     * @param email  the email address of the user to update.
     * @param role   the new role to assign.
     * @param model  Spring Model for passing attributes to the view.
     * @return the Thymeleaf layout page with success or error message.
     */
    @PostMapping("/admin/change-role")
    public String handleChangeRole(
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            Model model
    ) {
        model.addAttribute("roles", DatabaseManager.getAllRoles());
        model.addAttribute("content", "change-role");

        // 1) Prevent self-change
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();
        if (email.equalsIgnoreCase(currentEmail)) {
            model.addAttribute("message",
                    "You cannot change your own role. Due security constrain, auto change rule is not allowed"
            );
            return "layout";
        }

        // 2) Lookup target user
        Map<String, String> userRow = DatabaseManager.getUserRowByEmail(email);
        if (userRow == null) {
            model.addAttribute("message",
                    "No user found with that email. Due security constrain, auto change rule is not allowed"
            );
            return "layout";
        }

        // 3) Already at target role?
        String currentRole = userRow.get("role_name");
        if (currentRole.equalsIgnoreCase(role)) {
            model.addAttribute("message",
                    "The user already has the specified role. No changes were made."
            );
            return "layout";
        }

        // 4) Perform update
        boolean success = DatabaseManager.updateUserRole(email, role);
        if (success) {
            model.addAttribute("message", "User role updated successfully.");
        } else {
            model.addAttribute("message",
                    "Failed to update user role. Please try again."
            );
        }
        return "layout";
    }

    /**
     * Displays the Recover Credentials form, with a FIFO-ordered list
     * of all active recovery requests.
     *
     * @param model Spring model for passing data to the view
     * @return the Thymeleaf layout with the recover-credentials fragment
     */
    @GetMapping("/admin/recover-credentials")
    public String showRecoverCredentialsForm(Model model) {
        List<String[]> requests = DatabaseManager.getRecoveryCredentialsRequestsActive();
        model.addAttribute("requests", requests);
        model.addAttribute("pendingCount", DatabaseManager.countRecoverCredentialsRequestsActive());
        model.addAttribute("content", "recover-credentials");
        return "layout";
    }

    /**
     * Processes a password-update for the selected user and closes the request.
     *
     * @param email           the selected user’s email
     * @param newPassword     the new password to set
     * @param confirmPassword confirmation of the new password
     * @param model           Spring model for passing messages and refreshed data
     * @return the same layout showing success or error message and updated list
     */
    @PostMapping("/admin/recover-credentials")
    public String updateUserPassword(
            @RequestParam("email") String email,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "New password and confirmation do not match.");
        } else {
            boolean pwdOk = DatabaseManager.updateUserPassword(email, newPassword);
            boolean reqOk = DatabaseManager.updateRecoverCredentialsRequests(email);

            if (pwdOk && reqOk) {
                model.addAttribute("message", "Password updated successfully for " + email + ".");
            } else {
                model.addAttribute("errorMessage", "Failed to update password. Please try again.");
            }
        }

        // Refresh the list and count
        model.addAttribute("requests", DatabaseManager.getRecoveryCredentialsRequestsActive());
        model.addAttribute("pendingCount", DatabaseManager.countRecoverCredentialsRequestsActive());
        model.addAttribute("content", "recover-credentials");
        return "layout";
    }
}
