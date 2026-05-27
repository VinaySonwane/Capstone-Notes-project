package com.expandtesting.utils;

import com.expandtesting.drivers.GridDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * AdDismissalUtils
 *
 * The practice.expandtesting.com site injects Google ad vignettes that render
 * as a full-screen overlay (URL fragment: #google_vignette).  These block ALL
 * Selenium interactions with the underlying page — every click, every field
 * sendKeys, every element lookup lands on the ad instead of the app.
 *
 * Call dismissAds() at the START of every public page-object method that
 * interacts with the UI (navigate, create, edit, delete, filter, logout …).
 * It is cheap: the fast path is a single URL-fragment check + one JS call,
 * so there is no meaningful time cost when no ad is present.
 *
 * Three-layer defence:
 *  1. Strip the #google_vignette fragment from the URL via JS (removes the
 *     overlay without a full page reload in most cases).
 *  2. Click the "Close" button if one is visible in the main document.
 *  3. Switch into any ad iframes and dismiss them from the inside.
 */
public class AdDismissalUtils {

    /**
     * Master entry point — call this before any UI interaction.
     * Safe to call when no ad is present (exits immediately).
     */
    public static void dismissAds() {
        WebDriver driver = GridDriverManager.getDriver();
        try {
            // ── Layer 1: strip the #google_vignette hash fragment ─────────────────
            // This is the fastest defence: remove the fragment that activates the
            // vignette overlay without triggering a navigation/reload.
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl != null && currentUrl.contains("google_vignette")) {
                ((JavascriptExecutor) driver).executeScript(
                        "if (window.location.hash && window.location.hash.includes('google_vignette')) {"
                                + "  history.replaceState(null, '', window.location.pathname + window.location.search);"
                                + "}"
                );
                // Brief pause for the overlay to collapse after the hash is removed.
                Thread.sleep(600);
            }

            // ── Layer 2: click a visible "Close" button in the main document ──────
            // Google vignettes render a "Close" text link or an ✕ button.
            By closeButton = By.xpath(
                    "//*[normalize-space(text())='Close' or normalize-space(text())='✕' "
                            + "or normalize-space(text())='×' or @aria-label='Close' "
                            + "or @title='Close' or contains(@class,'close-button') "
                            + "or contains(@id,'dismiss') or contains(@id,'close')]"
            );
            List<WebElement> closeBtns = driver.findElements(closeButton);
            for (WebElement btn : closeBtns) {
                try {
                    if (btn.isDisplayed()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                        Thread.sleep(400);
                        break;
                    }
                } catch (Exception ignored) {}
            }

            // ── Layer 3: look inside iframes for ad close buttons ─────────────────
            // Some vignette variants render inside an iframe.
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            for (WebElement iframe : iframes) {
                try {
                    if (!iframe.isDisplayed()) continue;
                    driver.switchTo().frame(iframe);
                    List<WebElement> innerClose = driver.findElements(closeButton);
                    for (WebElement btn : innerClose) {
                        try {
                            if (btn.isDisplayed()) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                                Thread.sleep(300);
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                    driver.switchTo().defaultContent();
                } catch (Exception ignored) {
                    // Always return to main frame even if iframe access fails.
                    try { driver.switchTo().defaultContent(); } catch (Exception e2) {}
                }
            }

            // ── Final safety net: JS-nuke any remaining ad overlay divs ──────────
            // Removes position:fixed full-screen divs that belong to ad networks.
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll("
                            + "  '[id*=\"google_vignette\"], [id*=\"aswift\"], "
                            + "  [id*=\"google-ads\"], [class*=\"ad-overlay\"], "
                            + "  [class*=\"adsbygoogle\"], ins.adsbygoogle'"
                            + ").forEach(function(el) {"
                            + "  var s = window.getComputedStyle(el);"
                            + "  if (s.position === 'fixed' && parseInt(s.zIndex) > 100) {"
                            + "    el.remove();"
                            + "  }"
                            + "});"
            );

        } catch (Exception ignored) {
            // dismissAds is best-effort — never let it break the test.
            try { GridDriverManager.getDriver().switchTo().defaultContent(); } catch (Exception e2) {}
        }
    }

    /**
     * Waits for a target element to be interactable after ad dismissal.
     * Use this instead of a bare explicit wait when the element might be
     * occluded by an ad overlay.
     */
    public static WebElement waitForClickable(By locator, int timeoutSeconds) {
        dismissAds();
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Navigates to the given URL, strips any #google_vignette fragment that
     * appears after page load, and waits for the document to be ready.
     */
    public static void safeNavigate(String url) {
        WebDriver driver = GridDriverManager.getDriver();
        // Strip fragment from the target URL before navigating — prevents the
        // browser from re-triggering the vignette on load.
        String cleanUrl = url.replaceAll("#.*$", "");
        driver.get(cleanUrl);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));
        } catch (Exception ignored) {}
        // Dismiss any ad that appeared immediately after page load.
        dismissAds();
    }
}
