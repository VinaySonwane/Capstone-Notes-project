package com.expandtesting.stepdefs.ui;

import com.expandtesting.config.ConfigReader;
import com.expandtesting.pages.LoginPage;
import com.expandtesting.pages.NotesPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.testng.Assert;

/**
 * Step definitions for UI scenarios.
 * All test data is now supplied via Scenario Outline Examples tables —
 * no Faker / random data generation is used here.
 */
public class UiSteps {

    LoginPage loginPage = new LoginPage();
    NotesPage notesPage = new NotesPage();

    // Shared state within a scenario
    private String currentNoteTitle;

    // ─── Login ───────────────────────────────────────────────────

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        loginPage.navigateToLogin();
    }

    @When("the user attempts to login with email {string} and password {string}")
    public void theUserAttemptsToLogin(String email, String password) {
        loginPage.login(email, password);
    }

    @Then("the login should be {string}")
    public void theLoginShouldBe(String status) {
        if (status.equals("successful")) {
            Assert.assertTrue(
                    loginPage.isLoginSuccessful(),
                    "Login failed for valid credentials — Logout button not visible."
            );
        } else if (status.equals("unsuccessful")) {
            Assert.assertTrue(
                    loginPage.isLoginUnsuccessful(),
                    "Login succeeded unexpectedly — user appears logged in with invalid credentials."
            );
        }
    }

    // ─── Registration (TS-UI-02) ─────────────────────────────────

    @Given("the user is on the registration page")
    public void theUserIsOnTheRegistrationPage() {
        loginPage.navigateToRegister();
    }

    /**
     * TS-UI-02: All registration data comes from the Examples table.
     * Previously used Faker — now fully data-driven via Scenario Outline.
     */
    @When("the user registers with name {string} email {string} and password {string}")
    public void theUserRegistersWithCredentials(String name, String email, String password) {
        loginPage.register(name, email, password);
    }

    @Then("the account should be created successfully")
    public void theAccountShouldBeCreatedSuccessfully() {
        Assert.assertTrue(
                loginPage.isRegistrationSuccessful(),
                "Registration did not succeed — no success banner or redirect to login."
        );
    }

    // ─── Note pre-condition helpers ──────────────────────────────

    /**
     * Creates a note with explicit title/description/category from the Examples table.
     * Replaces the old Faker-based aNoteExistsOnTheDashboard() and aNoteExistsForCategory().
     */
    @And("a note exists with title {string} description {string} category {string}")
    public void aNoteExistsWithDetails(String title, String description, String category) {
        currentNoteTitle = title;
        notesPage.createNewNote(category, currentNoteTitle, description);
        Assert.assertTrue(
                notesPage.isNoteVisible(currentNoteTitle),
                "Pre-condition failed: note did not appear after creation: " + currentNoteTitle
        );
    }

    // ─── Edit (TS-UI-04) ─────────────────────────────────────────

    /**
     * TS-UI-04: Edit values come from the Examples table.
     * Previously Faker-generated title and description.
     */
    @When("the user edits the note with new title {string} and description {string}")
    public void theUserEditsTheNote(String updatedTitle, String updatedDesc) {
        notesPage.editNote(currentNoteTitle, updatedTitle, updatedDesc);
        currentNoteTitle = updatedTitle; // track new title for the Then step
    }

    @Then("the updated note should be visible on the dashboard")
    public void theUpdatedNoteShouldBeVisible() {
        Assert.assertTrue(
                notesPage.isNoteVisible(currentNoteTitle),
                "Edited note title not visible on dashboard: " + currentNoteTitle
        );
    }

    // ─── Delete (TS-UI-05) ───────────────────────────────────────

    @When("the user deletes the note via the UI")
    public void theUserDeletesTheNoteViaUi() {
        notesPage.deleteNoteViaUi(currentNoteTitle);
    }

    @Then("the note should no longer appear on the dashboard")
    public void theNoteShouldNoLongerAppear() {
        Assert.assertTrue(
                notesPage.isNoteAbsent(currentNoteTitle),
                "Deleted note is still visible: " + currentNoteTitle
        );
    }

    // ─── Category Filter (TS-UI-06) ──────────────────────────────

    @When("the user filters the dashboard by category {string}")
    public void theUserFiltersByCategory(String category) {
        notesPage.filterByCategory(category);
    }

    @Then("only notes from category {string} should be visible")
    public void onlyNotesShouldBeVisible(String category) {
        Assert.assertTrue(
                notesPage.areAllVisibleNotesInCategory(category),
                "Dashboard shows notes outside the selected category: " + category
        );
    }

    // ─── Logout (TS-UI-07) ───────────────────────────────────────

    @When("the user clicks logout")
    public void theUserClicksLogout() {
        loginPage.clickLogout();
    }

    @Then("the user should be redirected to the login page")
    public void theUserShouldBeRedirectedToLogin() {
        Assert.assertTrue(
                loginPage.isOnLoginPage(),
                "After logout, the login form (email field) was not visible — user may still be logged in."
        );
    }

    @And("accessing the dashboard directly should redirect back to login")
    public void accessingDashboardShouldRedirectToLogin() {
        Assert.assertTrue(
                loginPage.isDashboardAccessDenied(),
                "Dashboard is still accessible after logout."
        );
    }
}
