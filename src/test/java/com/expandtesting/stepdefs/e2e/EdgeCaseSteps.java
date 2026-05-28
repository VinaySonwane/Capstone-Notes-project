package com.expandtesting.stepdefs.e2e;

import com.expandtesting.api.NotesApiManager;
import com.expandtesting.config.ConfigReader;
import com.expandtesting.drivers.GridDriverManager;
import com.expandtesting.pages.LoginPage;
import com.expandtesting.pages.NotesPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Given;
import io.restassured.response.Response;
import org.testng.Assert;

/**
 * Step definitions for edge-case and security scenarios.
 * All test data is supplied via Scenario Outline Examples tables —
 * no hard-coded values used here.
 */
public class EdgeCaseSteps {

    LoginPage loginPage = new LoginPage();
    NotesPage notesPage = new NotesPage();
    NotesApiManager apiManager = new NotesApiManager();

    private Response apiResponse;

    // ─── UI Negative ─────────────────────────────────────────────

    @Then("an error message should be displayed on the screen")
    public void errorMessageShouldBeDisplayed() {
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Login error message did not appear!");
    }

    @When("the user tries to create a note with an empty title")
    public void createNoteEmptyTitle() {
        notesPage.createNewNote("Work", "", "This note has no title");
    }

    @Then("a validation error should appear on the screen preventing submission")
    public void validationErrorShouldAppear() {
        Assert.assertTrue(notesPage.isTitleValidationErrorDisplayed(),
                "Form submitted even though title was empty!");
    }

    // ─── API Security — endpoint and invalid ID come from Examples table ─────

    @When("a GET request is sent to {string} without an auth token")
    public void getRequestWithoutToken(String endpoint) {
        apiResponse = apiManager.getNotesWithoutToken();
    }

    @Then("the API should return a {int} Unauthorized status")
    public void apiShouldReturnUnauthorized(int expectedStatus) {
        Assert.assertEquals(apiResponse.getStatusCode(), expectedStatus,
                "API did not block unauthorized access!");
    }

    @When("a DELETE request is sent for an invalid note ID {string}")
    public void deleteRequestInvalidId(String invalidId) {
        apiManager.authenticate(
                ConfigReader.getProperty("test.email"),
                ConfigReader.getProperty("test.password")
        );
        apiResponse = apiManager.deleteInvalidNote(invalidId);
    }

    @Then("the API should return a {int} or {int} error status code")
    public void apiShouldReturnErrorStatus(int status1, int status2) {
        int actualStatus = apiResponse.getStatusCode();
        Assert.assertTrue(actualStatus == status1 || actualStatus == status2,
                "Expected " + status1 + " or " + status2 + ", but got: " + actualStatus);
    }

    // TS-NEG-05: DELETE invalid ID must return exactly 404 (per requirement sheet)
    @Then("the API should return a {int} Not Found status")
    public void apiShouldReturnNotFound(int expectedStatus) {
        Assert.assertEquals(apiResponse.getStatusCode(), expectedStatus,
                "Expected " + expectedStatus + " for invalid note ID but got: "
                        + apiResponse.getStatusCode()
                        + ". Body: " + apiResponse.getBody().asString());
    }

    // ─── UI Dashboard refresh ─────────────────────────────────────

    @When("the user refreshes the UI dashboard")
    public void userRefreshesDashboard() {
        GridDriverManager.getDriver().navigate().refresh();
        com.expandtesting.utils.AdDismissalUtils.dismissAds();
    }
}
