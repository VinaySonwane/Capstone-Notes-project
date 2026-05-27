package com.expandtesting.utils;

import com.expandtesting.drivers.GridDriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;

public class ScreenshotUtils {

    /**
     * Captures a screenshot and returns raw bytes.
     * Called from Hooks.java which passes the bytes to:
     *   1. scenario.attach()        — via Cucumber event bus (primary)
     *   2. Allure.addAttachment()   — direct Allure lifecycle (secondary)
     *
     * No AspectJ / @Attachment needed. Both paths above work on Jenkins
     * without any javaagent argument.
     */
    public static byte[] captureScreenshot() {
        WebDriver driver = GridDriverManager.getDriver();
        if (driver == null) return new byte[0];
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.out.println("[ScreenshotUtils] Could not capture screenshot: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Attaches screenshot bytes directly to the Allure lifecycle.
     * Used as secondary path from Hooks.java alongside scenario.attach().
     */
    public static void attachToAllure(String name, byte[] screenshotBytes) {
        if (screenshotBytes == null || screenshotBytes.length == 0) return;
        try {
            Allure.addAttachment(name, "image/png",
                    new ByteArrayInputStream(screenshotBytes), "png");
        } catch (Exception e) {
            System.out.println("[ScreenshotUtils] Allure.addAttachment failed: " + e.getMessage());
        }
    }
}


//package com.expandtesting.utils;
//
//import com.expandtesting.drivers.GridDriverManager;
//import io.qameta.allure.Attachment;
//import org.openqa.selenium.OutputType;
//import org.openqa.selenium.TakesScreenshot;
//
//public class ScreenshotUtils {
//
//    /**
//     * Captures a screenshot and automatically attaches it to the Allure Report.
//     */
//    @Attachment(value = "Test Failure Screenshot", type = "image/png")
//    public static byte[] takeScreenshotOnFailure() {
//        if (GridDriverManager.getDriver() != null) {
//            return ((TakesScreenshot) GridDriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
//        }
//        return new byte[0];
//    }
//}