package com.auctionflow.tests;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateAuctionE2ETest extends BaseE2ETest {

    @Test
    public void testCreateAuction() {
        driver.get(baseUrl + "/create-auction");

        // Assume login is handled or page has login
        // For simplicity, assume user is logged in or handle login here
        // WebElement loginButton = driver.findElement(By.id("login"));
        // etc.

        WebElement titleField = driver.findElement(By.id("auction-title"));
        titleField.sendKeys("Test Auction");

        WebElement descriptionField = driver.findElement(By.id("auction-description"));
        descriptionField.sendKeys("Description for test auction");

        WebElement startPriceField = driver.findElement(By.id("start-price"));
        startPriceField.sendKeys("100");

        WebElement submitButton = driver.findElement(By.id("submit-auction"));
        submitButton.click();

        // Wait for success message or redirect
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement successMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("auction-created")));

        assertTrue(successMessage.isDisplayed());
    }
}