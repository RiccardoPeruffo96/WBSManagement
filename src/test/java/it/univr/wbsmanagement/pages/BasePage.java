package it.univr.wbsmanagement.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * Base class for all page objects in the Selenium Page Object Model.
 * Initializes the WebDriver instance and sets up web elements via PageFactory.
 */
public class BasePage {
    // Protected reference to the WebDriver used by child page objects
    protected WebDriver driver;

    /**
     * Constructor that assigns the driver and initializes web elements.
     * @param driver the WebDriver used to interact with the browser
     */
    public BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this); // binds web elements to this page class
    }
}
