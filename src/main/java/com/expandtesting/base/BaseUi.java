package com.expandtesting.base;

import com.expandtesting.drivers.GridDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * 2.1 — Framework Architecture: Base UI class.
 *
 * Provides shared WebDriver access and Navigation Timing API helpers
 * used by all Page Object classes.  Section 3.5 performance measurements
 * are triggered from here so page objects don't duplicate the logic.
 */
public class BaseUi {

    /**
     * Returns the shared WebDriver instance for the current thread.
     */
    protected WebDriver getDriver() {
        return GridDriverManager.getDriver();
    }

    /**
     * 3.5 — Performance Engineering: Navigation Timing API.
     *
     * Reads window.performance.timing from the browser after a page load
     * and returns the total page-load duration in milliseconds:
     *   loadEventEnd − navigationStart
     *
     * Returns -1 if the timing data is not yet available.
     */
    public long getPageLoadTimeMs() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            Object loadEnd   = js.executeScript("return window.performance.timing.loadEventEnd");
            Object navStart  = js.executeScript("return window.performance.timing.navigationStart");
            if (loadEnd == null || navStart == null) return -1;
            long end   = Long.parseLong(loadEnd.toString());
            long start = Long.parseLong(navStart.toString());
            return (end > 0 && start > 0) ? end - start : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 3.5 — DOM readiness timing.
     *
     * Returns the time between navigationStart and domContentLoadedEventEnd
     * (i.e. how long until the DOM was fully parsed and ready).
     */
    public long getDomReadyTimeMs() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            Object domReady  = js.executeScript("return window.performance.timing.domContentLoadedEventEnd");
            Object navStart  = js.executeScript("return window.performance.timing.navigationStart");
            if (domReady == null || navStart == null) return -1;
            long end   = Long.parseLong(domReady.toString());
            long start = Long.parseLong(navStart.toString());
            return (end > 0 && start > 0) ? end - start : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
