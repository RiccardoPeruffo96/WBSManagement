package it.univr.wbsmanagement.tests;

import it.univr.wbsmanagement.pages.LoginPage;
import it.univr.wbsmanagement.pages.ProjectPage;

import org.junit.AfterClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;
import com.univr.tests.pages.LoginPage;
import com.univr.tests.pages.ProjectPage;

public class LoginTest {

    private WebDriver driver;
    private LoginPage loginPage;
    private ProjectPage projectPage;

    @BeforeClass
    public void setUp() {
        // note: set path to chromedriver
        System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");
        driver = new ChromeDriver();
        driver.get("http://localhost:8080/login");
        loginPage = new LoginPage(driver);
    }

    @Test
    public void testLoginAndOpenFirstProject() {
        loginPage.loginAs("admin","admin");
        projectPage = new ProjectPage(driver);
        projectPage.goToProject(0);
        // add assertion here
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
