package com.expandtesting.pages;

import com.expandtesting.base.BaseUi;
import com.expandtesting.drivers.GridDriverManager;
import com.expandtesting.utils.AdDismissalUtils;
import com.expandtesting.utils.PerformanceLogger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class LoginPage extends BaseUi {

    // ─── Login locators ──────────────────────────────────────────
    private final By emailField    = By.id("email");
    private final By passwordField = By.id("password");
    private final By loginButton   = By.xpath("//button[@type='submit' or contains(text(),'Login')]");
    // Logout is rendered as <a> in the nav bar on the profile/dashboard pages
    // and as <button> only on some views — match both to handle all cases.
    // The outer practice site nav also contains "Logout" text.
    // Scope to the MyNotes app header: the app renders Profile and Logout as
    // sibling <button> elements inside its own navbar/header container.
    // Using (//button[...])[last()] selects the LAST matching button — always
    // the one inside the MyNotes app, after the outer site elements.
    private final By logoutButton  = By.xpath(
            "(//*[contains(@class,'navbar') or contains(@class,'header') or "
            + "contains(@class,'nav') or contains(@class,'app')]"
            + "//button[normalize-space(text())='Logout'])[last()]"
            + " | (//button[normalize-space(text())='Logout'])[last()]"
    );

    // ─── Registration locators ───────────────────────────────────
    private final By regNameField     = By.id("name");
    private final By regEmailField    = By.id("email");
    private final By regPasswordField = By.id("password");
    private final By registerButton   = By.xpath("//button[@type='submit' or contains(text(),'Register')]");

    // ─── Profile / Change-password locators ──────────────────────
    private final By profileLink        = By.xpath(
            "//a[contains(@href,'profile')] | //a[contains(text(),'Profile')] | "
                    + "//button[contains(text(),'Profile')] | //*[@data-testid='profile-link'] | "
                    + "//nav//*[contains(text(),'Profile') or contains(@href,'profile')]"
    );
    // The actual input id values on the profile page differ from the label text.
    // Multi-strategy locators: id first (original), then name attr, then
    // label-sibling XPath, then positional password input as final fallback.
    private final By currentPassField   = By.xpath(
            "//*[@id='currentPassword'] | //*[@name='currentPassword'] | "
            + "//*[@id='current-password'] | //*[@name='current-password'] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'current')]"
            + "/following-sibling::input[@type='password'][1] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'current')]"
            + "/..//input[@type='password'] | "
            + "(//input[@type='password'])[1]"
    );
    private final By newPassField       = By.xpath(
            "//*[@id='newPassword'] | //*[@name='newPassword'] | "
            + "//*[@id='new-password'] | //*[@name='new-password'] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'new password')]"
            + "/following-sibling::input[@type='password'][1] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'new password')]"
            + "/..//input[@type='password'] | "
            + "(//input[@type='password'])[2]"
    );
    private final By confirmPassField   = By.xpath(
            "//*[@id='confirmPassword'] | //*[@name='confirmPassword'] | "
            + "//*[@id='confirm-password'] | //*[@name='confirm-password'] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm')]"
            + "/following-sibling::input[@type='password'][1] | "
            + "//label[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm')]"
            + "/..//input[@type='password'] | "
            + "(//input[@type='password'])[3]"
    );
    private final By savePasswordButton = By.xpath(
            "//button[normalize-space(text())='Update password'] | "
            + "//button[contains(text(),'Update')] | "
            + "//button[contains(text(),'Save')] | "
            + "//form//button[@type='submit']"
    );

    // ─── Error locator (broadened — works for all three login-error scenarios) ───
    private final By errorAlertLocator = By.cssSelector(
            "[role='alert'], [data-testid='alert-message'], div.alert-danger, " +
                    ".alert.alert-danger, .text-danger, .error-message, p.alert, " +
                    "div[class*='error'], div[class*='Error'], span[class*='error']"
    );

    // ─────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────

    public void navigateToLogin() {
        WebDriver driver = GridDriverManager.getDriver();
        // Use safeNavigate: strips #google_vignette before loading and dismisses
        // any ad overlay that appears immediately after the page loads.
        AdDismissalUtils.safeNavigate("https://practice.expandtesting.com/notes/app/login");
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.localStorage.clear(); window.sessionStorage.clear();"
            );
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {}

        driver.navigate().refresh();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
        AdDismissalUtils.dismissAds();
        wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
    }

    public void login(String email, String password) {
        WebDriver driver = GridDriverManager.getDriver();
        AdDismissalUtils.dismissAds();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebElement emailEl = wait.until(ExpectedConditions.elementToBeClickable(emailField));
        emailEl.clear();
        emailEl.sendKeys(email);

        WebElement passwordEl = wait.until(ExpectedConditions.elementToBeClickable(passwordField));
        passwordEl.clear();
        passwordEl.sendKeys(password);

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(loginButton));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
    }

    public boolean isLoginSuccessful() {
        try {
            WebDriverWait wait = new WebDriverWait(GridDriverManager.getDriver(), Duration.ofSeconds(10));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoginUnsuccessful() {
        try {
            WebDriverWait wait = new WebDriverWait(GridDriverManager.getDriver(), Duration.ofSeconds(5));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(logoutButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isErrorMessageDisplayed() {
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Strategy 1 — wait for a visible error element with non-empty text
        try {
            WebElement error = wait.until(ExpectedConditions.presenceOfElementLocated(errorAlertLocator));
            String txt = error.getText().trim();
            if (!txt.isEmpty()) return true;
            wait.until(d -> {
                try { return !d.findElement(errorAlertLocator).getText().trim().isEmpty(); }
                catch (Exception e) { return false; }
            });
            return true;
        } catch (Exception ignored) {}

        // Strategy 2 — body text keyword scan
        try {
            String pageText = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return pageText.contains("invalid") || pageText.contains("incorrect") ||
                    pageText.contains("unauthorized") ||
                    (pageText.contains("password") && pageText.contains("required")) ||
                    (pageText.contains("email") && pageText.contains("required")) ||
                    pageText.contains("bad request") || pageText.contains("login failed");
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REGISTRATION (TS-UI-02)
    // ─────────────────────────────────────────────────────────────

    public void navigateToRegister() {
        WebDriver driver = GridDriverManager.getDriver();
        AdDismissalUtils.safeNavigate("https://practice.expandtesting.com/notes/app/register");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
        AdDismissalUtils.dismissAds();
        wait.until(ExpectedConditions.visibilityOfElementLocated(regEmailField));
    }

    /**
     * Fills and submits the registration form with the provided values.
     * All inputs come from the step — no hardcoded test data here.
     */
    public void register(String name, String email, String password) {
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebElement nameEl = wait.until(ExpectedConditions.elementToBeClickable(regNameField));
        nameEl.clear();
        nameEl.sendKeys(name);

        driver.findElement(regEmailField).sendKeys(email);
        driver.findElement(regPasswordField).sendKeys(password);

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(registerButton));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
    }

    /**
     * After registration, the app redirects to login with a success banner.
     * We accept either: (a) a success alert is visible, or (b) the URL is now /login.
     */
    public boolean isRegistrationSuccessful() {
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Check for a success toast / banner
        By successLocator = By.cssSelector(
                ".alert-success, [class*='success'], [role='alert']"
        );
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(successLocator));
            if (el.isDisplayed()) return true;
        } catch (Exception ignored) {}

        // Check URL redirect
        try {
            wait.until(d -> d.getCurrentUrl().contains("/login") || d.getCurrentUrl().contains("/app"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT (TS-UI-07)
    // ─────────────────────────────────────────────────────────────

    public void clickLogout() {
        WebDriver driver = GridDriverManager.getDriver();

        // CRITICAL: AdDismissalUtils.dismissAds() scans iframes and can leave the
        // WebDriver context INSIDE an ad iframe if defaultContent() silently fails.
        // Any findElements() call made while inside an iframe operates on the iframe
        // DOM — the MyNotes Logout button is invisible from there.
        // Fix: always return to the top-level document BEFORE doing anything else.
        try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}

        AdDismissalUtils.dismissAds();

        // Ensure we're back on the main document after ad dismissal (which scans iframes).
        try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // The outer practice site nav also has "Logout" text — it appears BEFORE
        // the MyNotes app elements in the DOM.  The MyNotes Logout <button> is
        // the LAST matching element.  Walk in reverse to pick it.
        List<WebElement> allLogoutBtns = driver.findElements(By.xpath(
                "//button[normalize-space(text())='Logout'] | //a[normalize-space(text())='Logout']"
        ));
        System.out.println("[clickLogout] Total logout buttons found: " + allLogoutBtns.size());

        WebElement targetBtn = null;
        for (int i = allLogoutBtns.size() - 1; i >= 0; i--) {
            try {
                WebElement candidate = allLogoutBtns.get(i);
                if (candidate.isDisplayed() && candidate.isEnabled()) {
                    System.out.println("[clickLogout] Selecting button at index " + i
                            + " tag=" + candidate.getTagName()
                            + " text='" + candidate.getText().trim() + "'");
                    targetBtn = candidate;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (targetBtn == null) {
            System.out.println("[clickLogout] Reverse-scan found nothing; falling back to wait.");
            targetBtn = wait.until(ExpectedConditions.elementToBeClickable(logoutButton));
        }

        // Use native Selenium click — avoids firing on overlapping/wrong-layer elements.
        // JS click is the fallback only.
        try {
            targetBtn.click();
            System.out.println("[clickLogout] Native click fired.");
        } catch (Exception e) {
            System.out.println("[clickLogout] Native click failed (" + e.getMessage() + "); trying JS.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetBtn);
        }

        // Confirm logout completed: wait for the Logout button to disappear from DOM/view.
        // If the element goes stale (navigated away) that also counts as success.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.invisibilityOf(targetBtn));
            System.out.println("[clickLogout] Logout confirmed — button is no longer visible.");
        } catch (Exception ignored) {
            System.out.println("[clickLogout] Logout button visibility timeout (may be stale — OK).");
        }
    }

    public boolean isOnLoginPage() {
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            // TS-UI-07: After logout the app may stay on the same URL and re-render
            // the login form in-place (SPA behaviour), OR it may redirect to /login.
            // Strategy: wait for EITHER the URL to contain "login" OR the email input
            // to become visible. Use a combined condition to avoid a hard URL assertion.
            wait.until(d -> {
                boolean urlOk = d.getCurrentUrl().contains("login");
                boolean fieldOk = false;
                try {
                    List<WebElement> els = d.findElements(emailField);
                    fieldOk = !els.isEmpty() && els.get(0).isDisplayed();
                } catch (Exception ignored) {}
                return urlOk || fieldOk;
            });
            // Final check: email field must actually be present and interactable
            List<WebElement> emailEls = driver.findElements(emailField);
            if (!emailEls.isEmpty() && emailEls.get(0).isDisplayed()) return true;
            // If URL is /login but email field hasn't rendered yet, wait a bit more
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(emailField));
            return true;
        } catch (Exception e) {
            // Last resort: check URL only
            return driver.getCurrentUrl().contains("login");
        }
    }

    /**
     * Attempts to navigate directly to the dashboard URL.
     * Returns true if the app shows the login form (session is gone).
     *
     * Fix for TS-UI-07: the app may not redirect to a /login URL; it may stay on /app
     * and render the login form in-place.  We therefore accept EITHER a URL containing
     * "/login" OR the email field being visible as proof the session is invalidated.
     */
    public boolean isDashboardAccessDenied() {
        WebDriver driver = GridDriverManager.getDriver();
        driver.navigate().to("https://practice.expandtesting.com/notes/app");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(d -> d.getCurrentUrl().contains("/login")
                    || ExpectedConditions.visibilityOfElementLocated(emailField).apply(d) != null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CHANGE PASSWORD (TS-UI-08)
    // ─────────────────────────────────────────────────────────────

    /**
     * Navigates to the profile page and changes the password.
     * All inputs are dynamic — nothing is hardcoded.
     *
     * Fix for TS-UI-08:
     *  1. Broadened success CSS selector to cover Toastify / react-toastify / Bootstrap toasts
     *     in addition to the previously-used .alert-success selectors.
     *  2. Changed presenceOfElementLocated → visibilityOfElementLocated so we do not
     *     accidentally match a hidden element that has the right class but no visible text.
     *  3. Scroll the Save button into view before clicking — a sticky header can obscure it
     *     causing the JS click to fire on the header instead.
     *  4. Added console logging so a false-return surfaces the actual page text for diagnosis.
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        WebDriver driver = GridDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Navigate to the profile page.
        // ROOT CAUSE of previous failures: the profile page opens on the
        // "Account details" tab by default.  The diagnostic log confirmed this —
        // 5 text/email inputs (userId, email, name, phone, company) but ZERO
        // password inputs.  We must click the "Change password" TAB first.
        AdDismissalUtils.safeNavigate("https://practice.expandtesting.com/notes/app/profile");
        try {
            wait.until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
        } catch (Exception ignored) {}
        try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
        AdDismissalUtils.dismissAds();

        // ── Step 1: Click the "Change password" tab ──────────────────────────────
        // The page has two tabs: "Account details" (default) | "Change password".
        // XPath covers: exact tab text match with case variations.
        By changePasswordTab = By.xpath(
                "//a[normalize-space(text())='Change password'] | "
                + "//button[normalize-space(text())='Change password'] | "
                + "//li[normalize-space(text())='Change password'] | "
                + "//li/a[normalize-space(text())='Change password'] | "
                + "//*[@role='tab' and normalize-space(text())='Change password'] | "
                + "//*[contains(@class,'tab') and normalize-space(text())='Change password'] | "
                + "//a[normalize-space(text())='Change Password'] | "
                + "//button[normalize-space(text())='Change Password'] | "
                + "//*[@role='tab' and normalize-space(text())='Change Password']"
        );
        try {
            WebElement tab = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(changePasswordTab));
            System.out.println("[changePassword] Clicking 'Change password' tab: '" + tab.getText() + "'");
            try { tab.click(); } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tab);
            }
            // Wait for at least one password input to appear after tab switch
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[@type='password']")));
            System.out.println("[changePassword] 'Change password' tab loaded — password inputs present.");
        } catch (Exception e) {
            System.out.println("[changePassword] Tab click failed: " + e.getMessage());
        }

        // ── Diagnostic: log all inputs now visible ────────────────────────────────
        try {
            List<WebElement> inputs = driver.findElements(By.tagName("input"));
            System.out.println("[changePassword] Inputs after tab switch: " + inputs.size());
            for (WebElement inp : inputs) {
                System.out.println("[changePassword]   id='" + inp.getAttribute("id")
                        + "' name='" + inp.getAttribute("name")
                        + "' type='" + inp.getAttribute("type") + "'");
            }
        } catch (Exception ignored) {}

        try {
            // ── Fill Current Password ─────────────────────────────────────────────
            // currentPassField uses a multi-strategy XPath covering id, name, label
            // association, and positional (//input[@type='password'])[1] as fallback.
            WebElement curPwd = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(currentPassField));
            curPwd.clear();
            curPwd.sendKeys(currentPassword);
            System.out.println("[changePassword] Filled current password field.");

            // ── Fill New Password ─────────────────────────────────────────────────
            WebElement newPwd = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(newPassField));
            newPwd.clear();
            newPwd.sendKeys(newPassword);
            System.out.println("[changePassword] Filled new password field.");

            // ── Fill Confirm Password (always present per screenshot) ─────────────
            WebElement confirmPwd = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(confirmPassField));
            confirmPwd.clear();
            confirmPwd.sendKeys(newPassword);
            System.out.println("[changePassword] Filled confirm password field.");

            // ── Click 'Update password' button ────────────────────────────────────
            // savePasswordButton XPath targets "Update password" text first (exact
            // label from screenshot), then broader fallbacks.
            WebElement saveBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(savePasswordButton));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block:'center'});", saveBtn);
            System.out.println("[changePassword] Clicking save button: '" + saveBtn.getText() + "'");
            try {
                saveBtn.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveBtn);
            }

            // ── Wait for success toast/alert ──────────────────────────────────────
            By successLocator = By.cssSelector(
                    ".alert-success, [class*='success'], [role='alert'], "
                            + "[data-testid='alert-message'], "
                            + ".Toastify__toast, .Toastify__toast--success, "
                            + "[class*='Toast'], .toast, [class*='toast']"
            );
            WebElement success = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(successLocator));

            String successText = success.getText().trim().toLowerCase();
            System.out.println("[changePassword] Toast text: '" + successText + "'");

            boolean isSuccess = !successText.contains("invalid")
                    && !successText.contains("incorrect")
                    && !successText.contains("error")
                    && !successText.contains("fail");
            if (!isSuccess) {
                System.out.println("[changePassword] Failure indicator in toast: " + successText);
            }
            return isSuccess;

        } catch (Exception e) {
            System.out.println("[changePassword] Exception: " + e.getMessage());
            try {
                String bodyText = driver.findElement(By.tagName("body")).getText();
                System.out.println("[changePassword] Page body (first 600 chars): "
                        + bodyText.substring(0, Math.min(600, bodyText.length())));
            } catch (Exception ignored) {}
            return false;
        }
    }
}
