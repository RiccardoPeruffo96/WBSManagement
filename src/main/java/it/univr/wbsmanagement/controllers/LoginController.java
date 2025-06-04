package it.univr.wbsmanagement.controllers;
import it.univr.wbsmanagement.database.DatabaseManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @GetMapping("/")
    public String homepageRedirect() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "homepage"; // Redirect to homepage after successful login
        }
        return "redirect:/login"; // If not authenticated, go to login page
    }

    /**
     * Displays the custom login page and handles error flag.
     *
     * @param error indicates whether authentication failed (optional).
     * @param model Spring Model to pass attributes to the view.
     * @return the login view name.
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    /**
     * Displays the forgot password form.
     *
     * @param model Spring Model to pass attributes to the view.
     * @return the forgot-password view name.
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "forgot-password";
    }

    /**
     * Processes the forgot password submission.
     * <p>
     * Uses DatabaseManager.addRecoverCredentialsRequests(), which
     * first checks for an existing active request and prevents duplicates :contentReference[oaicite:0]{index=0}.
     * </p>
     *
     * @param email the submitted email address.
     * @param redirectAttributes used to pass flash messages to the redirected login page.
     * @return redirect to the login page.
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        String createdAt = DatabaseManager.addRecoverCredentialsRequests(email);

        if (createdAt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to process recovery request. Please try again later.");
        } else {
            redirectAttributes.addFlashAttribute("logoutMessage",
                    "Your recovery request has been recorded. "
                            + "If one was already in progress, it dates from " + createdAt + ".");
        }

        return "redirect:/login";
    }
}
