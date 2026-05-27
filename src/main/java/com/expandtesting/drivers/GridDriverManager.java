package com.expandtesting.drivers;

import com.expandtesting.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GridDriverManager {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static void initDriver() {
        try {
            ChromeOptions options = new ChromeOptions();

            // ── Block Google ad vignettes and interstitials at the browser level ──────
            // The practice site hosts Google ads that render as a full-screen overlay
            // with the URL fragment #google_vignette, completely blocking UI interaction.
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");

            // Block the Google ad domains at the network level so vignettes never load.
            options.addArguments("--host-rules=MAP googleads.g.doubleclick.net 127.0.0.1,"
                    + "MAP pagead2.googlesyndication.com 127.0.0.1,"
                    + "MAP adservice.google.com 127.0.0.1,"
                    + "MAP tpc.googlesyndication.com 127.0.0.1");

            // Suppress the Chrome automation info-bar and any first-run dialogs.
            options.setExperimentalOption("excludeSwitches",
                    Arrays.asList("enable-automation", "disable-popup-blocking"));
            options.setExperimentalOption("useAutomationExtension", false);

            // Disable image loading for ad iframes (keeps test speed up too).
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.ads", 2);       // block ads
            prefs.put("profile.default_content_setting_values.popups", 2);    // block popups
            prefs.put("profile.default_content_setting_values.notifications", 2);
            options.setExperimentalOption("prefs", prefs);

            URL gridUrl = new URL(ConfigReader.getProperty("grid.url"));
            driver.set(new RemoteWebDriver(gridUrl, options));
            driver.get().manage().window().maximize();
            // Implicit wait safety-net (explicit waits are still used in page objects).
            driver.get().manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(30));
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to Grid: " + e.getMessage());
        }
    }

    public static WebDriver getDriver() {
        return driver.get();
    }

    public static void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
}