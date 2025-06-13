package it.univr.wbsmanagement.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import java.util.List;

public class ProjectPage extends BasePage {

    @FindBy(css = ".project-link-button")
    private List<WebElement> projectButtons;

    public ProjectPage(WebDriver driver) {
        super(driver);
    }

    public void goToProject(int index) {
        projectButtons.get(index).click();
    }
}

