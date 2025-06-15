package it.univr.wbsmanagement.tests;

import it.univr.wbsmanagement.pages.LoginPage;
import it.univr.wbsmanagement.pages.ProjectPage;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.junit.jupiter.api.*;

/**
 * Test class that verifies login functionality and access to the first project.
 */
public class LoginTest {

    private static WebDriver driver;
    private static LoginPage loginPage;
    private ProjectPage projectPage;

    /**
     * Setup method executed once before all tests.
     * Initializes the WebDriver and opens the login page.
     */
    @BeforeAll
    public static void setUp() {
        // note: set path to msedgedriver.exe
        System.setProperty("webdriver.edge.driver", "C:\\Users\\Public\\edgedriver_win64\\msedgedriver.exe");
        driver = new EdgeDriver();
        driver.get("http://localhost:8080/login");
        loginPage = new LoginPage(driver);
    }

    /**
     * Test that performs login as admin and navigates to the first project.
     * Verifies that the resulting page URL contains the string "project".
     */
    @Test
    public void testLoginAndOpenFirstProject() {
        loginPage.loginAs("admin", "admin");
        projectPage = new ProjectPage(driver);
        projectPage.goToProject(0);

        // Basic check to confirm we're on a project page
        Assertions.assertTrue(driver.getCurrentUrl().contains("project"));
    }

    /**
     * Cleanup method executed once after all tests.
     * Quits the WebDriver and closes the browser.
     */
    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
