package com.expandtesting.stepdefs.e2e;

import com.expandtesting.api.NotesApiManager;
import com.expandtesting.config.ConfigReader;
import com.expandtesting.pages.LoginPage;
import com.expandtesting.pages.NotesPage;
import com.expandtesting.drivers.GridDriverManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/**
 * Step definitions for Hybrid (UI + API) synchronization scenarios.
 * All test data is supplied via Scenario Outline Examples tables —
 * no Faker / random data generation is used here.
 */
public class HybridSyncSteps {

    NotesPage       notesPage  = new NotesPage();
    LoginPage       loginPage  = new LoginPage();
    NotesApiManager apiManager = new NotesApiManager();

    // Shared state
    private String noteTitle;
    private String apiCreatedTitle;

    // ─── Shared login step ───────────────────────────────────────

    @Given("the user is logged into the Notes UI with valid credentials")
    public void userIsLoggedIntoUI() {
        loginPage.navigateToLogin();
        loginPage.login(
                ConfigReader.getProperty("test.email"),
                ConfigReader.getProperty("test.password")
        );
        Assert.assertTrue(
                loginPage.isLoginSuccessful(),
                "Pre-condition login failed — check test.email / test.password in config.properties"
        );
        apiManager.authenticate(
                ConfigReader.getProperty("test.email"),
                ConfigReader.getProperty("test.password")
        );
    }

    // ─── TS-E2E-01: UI → API note sync ──────────────────────────

    /**
     * TS-E2E-01: Title, description, category all from Examples table.
     * Previously Faker-generated inside createDynamicNoteViaUI().
     */
    @When("the user creates a note via UI with title {string} description {string} and category {string}")
    public void createNoteViaUI(String title, String description, String category) {
        this.noteTitle = title;
        System.out.println("Creating note: " + title + " in category: " + category);
        notesPage.createNewNote(category, title, description);
        Assert.assertTrue(
                notesPage.isNoteVisible(title),
                "Note did not appear in UI list after creation: " + title
        );
    }

    @Then("the GET {string} API should return the note with title {string}")
    public void verifyNoteInAPI(String endpoint, String title) {
        Assert.assertTrue(
                apiManager.verifyNoteExists(title),
                "Note not found via API GET /notes: " + title
        );
    }

    // ─── TS-E2E-02: UI edit → API sync ──────────────────────────

    /**
     * TS-E2E-02: All values from Examples table.
     * Previously Faker-generated titles.
     */
    @And("a note exists via UI with title {string} description {string} category {string}")
    public void aNoteExistsViaUI(String title, String description, String category) {
        this.noteTitle = title;
        notesPage.createNewNote(category, title, description);
        Assert.assertTrue(
                notesPage.isNoteVisible(title),
                "Pre-condition note not visible before edit test: " + title
        );
    }

    @When("the user edits that note with new title {string} and description {string}")
    public void editNoteWithNewTitle(String updatedTitle, String updatedDesc) {
        notesPage.editNote(this.noteTitle, updatedTitle, updatedDesc);
        this.noteTitle = updatedTitle;
    }

    @Then("the GET {string} API should return the updated title")
    public void verifyUpdatedTitleInApi(String endpoint) {
        Assert.assertTrue(
                apiManager.verifyNoteExists(this.noteTitle),
                "Updated note title not found via API GET /notes: " + this.noteTitle
        );
    }

    // ─── TS-DEF-01 / TS-14: API → UI sync ───────────────────────

    // Stores the note ID so we can delete by ID in TS-14
    private String apiCreatedNoteId;

    /**
     * TS-DEF-01 and TS-14: Title from Examples table.
     * Uses createAndReturnNoteId() so the ID is available for deletion in TS-14.
     */
    @When("a note is created via the API with title {string} description {string} and category {string}")
    public void noteIsCreatedViaAPI(String title, String description, String category) {
        this.apiCreatedTitle = title;
        this.apiCreatedNoteId = apiManager.createAndReturnNoteId(title, description, category);
        System.out.println("Created via API: " + title + " (id=" + apiCreatedNoteId + ")");
    }


    @Then("the API created note with title {string} should be visible on the UI dashboard")
    public void apiCreatedNoteShouldBeVisibleOnUi(String title) {
        Assert.assertTrue(
                notesPage.isNoteVisible(title),
                "API-created note not visible on UI dashboard: " + title
        );
    }

    // ─── TS-14: Delete via API → UI sync ─────────────────────────

    @When("the user deletes that specific note via the API")
    public void userDeletesNoteViaApi() {
        // deleteNoteById uses the ID captured during noteIsCreatedViaAPI()
        apiManager.deleteNoteById(this.apiCreatedNoteId);
    }

    @Then("the deleted note should no longer be visible on the screen")
    public void deletedNoteNotVisible() {
        Assert.assertTrue(
                notesPage.isNoteAbsent(this.apiCreatedTitle),
                "Deleted note is still visible on UI: " + this.apiCreatedTitle
        );
    }
}
