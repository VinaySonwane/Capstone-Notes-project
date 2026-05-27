package com.expandtesting.hooks;

import com.expandtesting.base.BaseUi;
import com.expandtesting.drivers.GridDriverManager;
import com.expandtesting.utils.PerformanceLogger;
import com.expandtesting.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Hooks {

    private final BaseUi baseUi = new BaseUi() {};  // anonymous subclass to access protected methods

    @Before
    public void setup() {
        GridDriverManager.initDriver();
    }

    @After
    public void tearDown(Scenario scenario) {

        // ── 3.5 — UI timing: capture Navigation Timing API data after each scenario ──
        try {
            long pageLoadMs  = baseUi.getPageLoadTimeMs();
            long domReadyMs  = baseUi.getDomReadyTimeMs();
            String scenName  = scenario.getName();

            if (pageLoadMs > 0) {
                PerformanceLogger.logUiTiming(scenName, "pageLoad",  pageLoadMs, 3000);
            }
            if (domReadyMs > 0) {
                PerformanceLogger.logUiTiming(scenName, "domReady",  domReadyMs, 2000);
            }
        } catch (Exception ignored) {
            // Driver may already be closed for API-only scenarios — safe to ignore
        }

        // ── Screenshot on failure ────────────────────────────────────────────────────
        if (scenario.isFailed()) {
            byte[] screenshot = ScreenshotUtils.captureScreenshot();
            if (screenshot.length > 0) {
                try {
                    scenario.attach(screenshot, "image/png",
                            "Failure Screenshot — " + scenario.getName());
                } catch (Exception e) {
                    System.out.println("[Hooks] scenario.attach failed: " + e.getMessage());
                }
                ScreenshotUtils.attachToAllure(
                        "Failure Screenshot — " + scenario.getName(), screenshot);
            } else {
                System.out.println("[Hooks] Screenshot empty for: " + scenario.getName());
            }
        }

        // ── 3.5 — Attach performance-log.csv to Allure at end of each scenario ─────
        try {
            File perfLog = new File("target/performance-log.csv");
            if (perfLog.exists()) {
                Allure.addAttachment(
                        "Performance Trend Log",
                        "text/csv",
                        new FileInputStream(perfLog),
                        ".csv"
                );
            }
        } catch (Exception ignored) {}

        // ── Driver quit AFTER all data collection ────────────────────────────────────
        GridDriverManager.quitDriver();
    }
}
