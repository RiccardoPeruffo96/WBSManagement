package it.univr.wbsmanagement.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import java.util.List;

/**
 * Page Object that models the main project list page.
 * Allows navigation to a specific project by index.
 */
public class ProjectPage extends BasePage {

    // List of buttons, each representing a project link in the UI
    @FindBy(css = ".project-link-button")
    private List<WebElement> projectButtons;

    /**
     * Constructor that initializes the page and web elements.
     * @param driver WebDriver instance used for this page
     */
    public ProjectPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Clicks on a project button by its position in the list.
     * @param index the zero-based index of the project to open
     */
    public void goToProject(int index) {
        projectButtons.get(index).click();
    }
}
