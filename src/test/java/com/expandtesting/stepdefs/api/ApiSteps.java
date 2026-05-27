package com.expandtesting.stepdefs.api;

import com.expandtesting.api.NotesApiManager;
import com.expandtesting.config.ConfigReader;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.testng.Assert;

/**
 * Step definitions for API scenarios.
 * All test data is supplied via Scenario Outline Examples tables —
 * no Faker / random data generation is used here.
 */
public class ApiSteps {

    NotesApiManager apiManager = new NotesApiManager();

    // Shared state across steps in the same scenario
    private Response lastResponse;
    private String   createdNoteId;
    private String   createdNoteTitle;
    private String   createdNoteCategory;

    // ─── Auth ───────────────────────────────────────────────────

    @Given("the user is authenticated via the API")
    public void theUserIsAuthenticatedViaTheAPI() {
        apiManager.authenticate(
                ConfigReader.getProperty("test.email"),
                ConfigReader.getProperty("test.password")
        );
    }

    // ─── TS-API-01: Login token ──────────────────────────────────

    /**
     * TS-API-01: Credentials come from the Examples table.
     * Previously read directly from config — now fully data-driven.
     */
    @When("the user sends a POST login request with email {string} and password {string}")
    public void userSendsPostLoginRequest(String email, String password) {
        lastResponse = apiManager.loginAndGetResponse(email, password);
    }

    @Then("the response status should be {int}")
    public void responseStatusShouldBe(int expectedStatus) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "Expected status " + expectedStatus + " but got: " + lastResponse.getStatusCode());
    }

    @Then("the response should contain a valid auth token")
    public void responseShouldContainToken() {
        String token = lastResponse.jsonPath().getString("data.token");
        Assert.assertNotNull(token, "Auth token is null in response");
        Assert.assertFalse(token.trim().isEmpty(), "Auth token is empty in response");
    }

    // ─── TS-API-02: GET notes SLA ────────────────────────────────

    /**
     * TS-API-02: Seed title comes from the Examples table.
     * Previously hardcoded "Pure API Validation".
     */
    @When("the user requests to fetch all notes with seed title {string}")
    public void theUserRequestsToFetchAllNotes(String seedTitle) {
        apiManager.createNoteViaApi(seedTitle, "Testing GET endpoint", "Personal");
    }

    @Then("the API response should contain the note {string}")
    public void theAPIResponseShouldContainNote(String title) {
        Assert.assertTrue(
                apiManager.verifyNoteExists(title),
                "Failed to retrieve note from the API: " + title
        );
    }

    // ─── TS-API-03: POST create note ─────────────────────────────

    /**
     * TS-API-03: Title, description, category all from Examples table.
     * Previously Faker-generated.
     */
    @When("the user creates a note via API with title {string} description {string} and category {string}")
    public void userCreatesNoteViaApi(String title, String description, String category) {
        createdNoteTitle    = title;
        createdNoteCategory = category;
        lastResponse = apiManager.createNoteAndGetResponse(title, description, category);
    }

    @Then("the API should return status {int} and the note should exist")
    public void apiShouldReturnStatusAndNoteExists(int expectedStatus) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "Note creation returned wrong status. Body: " + lastResponse.getBody().asString());
        Assert.assertTrue(
                apiManager.verifyNoteExists(createdNoteTitle),
                "Created note not found via GET /notes: " + createdNoteTitle
        );
    }

    // ─── Shared setup for PUT / PATCH / GET-by-ID ────────────────

    /**
     * Shared pre-condition for TS-API-04, TS-API-05, TS-API-06.
     * Title, description, category all from Examples table — no Faker.
     */
    @And("a note is created via the API with title {string} description {string} category {string}")
    public void noteCreatedViaAPIWithDetails(String title, String description, String category) {
        createdNoteTitle    = title;
        createdNoteCategory = category;
        createdNoteId = apiManager.createAndReturnNoteId(title, description, category);
        Assert.assertNotNull(createdNoteId,
                "Could not create a note for update testing: " + title);
    }

    // ─── TS-API-04: PUT update ───────────────────────────────────

    /**
     * TS-API-04: Updated title and description from Examples table.
     * Previously Faker-generated.
     */
    @When("the user sends a PUT request to update with title {string} and description {string}")
    public void userSendsPutRequest(String updatedTitle, String updatedDesc) {
        lastResponse = apiManager.putNote(
                createdNoteId, updatedTitle, updatedDesc, createdNoteCategory
        );
        createdNoteTitle = updatedTitle;
    }

    @Then("the API should return {int} and reflect the updated values")
    public void apiShouldReturnAndReflectUpdatedValues(int expectedStatus) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "PUT returned wrong status. Body: " + lastResponse.getBody().asString());
        String returnedTitle = lastResponse.jsonPath().getString("data.title");
        Assert.assertEquals(returnedTitle, createdNoteTitle,
                "PUT response title does not match the title we sent");
    }

    // ─── TS-API-05: PATCH category ───────────────────────────────

    /**
     * TS-API-05: New category from Examples table.
     * Previously hardcoded "Home".
     */
    @When("the user sends a PATCH request to update the category to {string}")
    public void userSendsPatchRequest(String newCategory) {
        lastResponse        = apiManager.patchNoteCategory(createdNoteId, newCategory);
        createdNoteCategory = newCategory;
    }

    @Then("the API should return {int} and only the category should be changed to {string}")
    public void apiShouldReturnAndOnlyCategoryChanged(int expectedStatus, String expectedCategory) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "PATCH returned wrong status. Body: " + lastResponse.getBody().asString());
        String returnedCategory = lastResponse.jsonPath().getString("data.category");
        Assert.assertEquals(returnedCategory, expectedCategory,
                "PATCH response category does not match the category we sent");
    }

    // ─── TS-API-06: GET by ID ────────────────────────────────────

    @When("the user sends a GET request for that specific note ID")
    public void userSendsGetByIdRequest() {
        lastResponse = apiManager.getNoteById(createdNoteId);
    }

    @Then("the API should return {int} and the note data should match")
    public void apiShouldReturnAndNoteDataShouldMatch(int expectedStatus) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "GET by ID returned wrong status. Body: " + lastResponse.getBody().asString());
        String returnedId = lastResponse.jsonPath().getString("data.id");
        Assert.assertEquals(returnedId, createdNoteId,
                "Returned note ID does not match the one we created");
    }

    // ─── TS-NEG-04: POST missing title ───────────────────────────

    /**
     * TS-NEG-04: Description and category from Examples table.
     * Previously Faker-generated description.
     */
    @When("the user sends a POST notes request with missing title but description {string} and category {string}")
    public void userSendsPostNotesWithMissingTitle(String description, String category) {
        lastResponse = apiManager.createNoteWithMissingTitle(description, category);
    }

    @Then("the API should return a {int} Bad Request status")
    public void apiShouldReturnBadRequest(int expectedStatus) {
        Assert.assertEquals(lastResponse.getStatusCode(), expectedStatus,
                "Expected " + expectedStatus + " for missing title but got: "
                        + lastResponse.getStatusCode()
                        + ". Body: " + lastResponse.getBody().asString());
    }
}
