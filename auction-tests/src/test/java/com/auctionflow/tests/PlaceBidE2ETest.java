package com.auctionflow.tests;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlaceBidE2ETest extends BaseE2ETest {

    @Test
    public void testPlaceBid() {
        // Assume an auction ID, or create one first, but for simplicity, assume auction exists
        String auctionId = "1"; // Placeholder
        driver.get(baseUrl + "/auctions/" + auctionId);

        WebElement bidAmountField = driver.findElement(By.id("bid-amount"));
        bidAmountField.sendKeys("150");

        WebElement placeBidButton = driver.findElement(By.id("place-bid"));
        placeBidButton.click();

        // Wait for bid confirmation
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement bidAcceptedMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bid-accepted")));

        assertTrue(bidAcceptedMessage.isDisplayed());
    }
}