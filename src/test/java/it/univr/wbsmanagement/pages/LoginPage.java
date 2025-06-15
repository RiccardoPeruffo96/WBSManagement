package it.univr.wbsmanagement.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object class that models the login page.
 * Provides methods to interact with the login form fields and button.
 * 
 * Note: the 'username' field actually refers to the user's email address.
 */
public class LoginPage extends BasePage {

    // Input field for email (labelled as 'username' in HTML)
    @FindBy(id = "username")
    private WebElement usernameInput;

    // Input field for the password
    @FindBy(id = "password")
    private WebElement passwordInput;

    // Login button element
    @FindBy(css = ".login-button")
    private WebElement loginButton;

    /**
     * Constructor that invokes BasePage and initializes the elements.
     * @param driver WebDriver used by the test
     */
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Fills in the email (username) input field.
     * @param user the user's email address
     */
    public void enterUsername(String user) {
        usernameInput.clear();
        usernameInput.sendKeys(user);
    }

    /**
     * Fills in the password input field.
     * @param pass the user's password
     */
    public void enterPassword(String pass) {
        passwordInput.clear();
        passwordInput.sendKeys(pass);
    }

    /**
     * Clicks the login button.
     */
    public void clickLogin() {
        loginButton.click();
    }

    /**
     * Fills the login form with credentials and submits it.
     * @param user the user's email address
     * @param pass the user's password
     */
    public void loginAs(String user, String pass) {
        enterUsername(user);
        enterPassword(pass);
        clickLogin();
    }
}
