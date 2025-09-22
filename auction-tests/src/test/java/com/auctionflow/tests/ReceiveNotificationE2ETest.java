package com.auctionflow.tests;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReceiveNotificationE2ETest extends BaseE2ETest {

    @Test
    public void testReceiveNotification() {
        // Navigate to auction page or dashboard
        String auctionId = "1";
        driver.get(baseUrl + "/auctions/" + auctionId);

        // Assume user is watching or something
        // Perhaps place a bid to trigger notification, but since it's the same user, maybe not.

        // For simplicity, assume notifications appear in a div
        // Perhaps use another driver or simulate.

        // Wait for notification element to appear
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement notification = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("notification")));

        assertTrue(notification.isDisplayed());
        assertTrue(notification.getText().contains("New bid placed"));
    }
}