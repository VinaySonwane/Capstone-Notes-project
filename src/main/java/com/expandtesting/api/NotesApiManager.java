package com.expandtesting.api;

import com.expandtesting.base.BaseApi;
import com.expandtesting.utils.PerformanceLogger;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

/**
 * Central API manager — extends BaseApi so all calls inherit the shared
 * request spec, response-time SLA spec, and logging filters.
 *
 * 2.3 additions:
 *   • Response time < 2 000 ms enforced on every SLA-bearing call via BaseApi.slaSpec
 *   • JSON schema validation on Login, Create Note and Get Notes responses
 *   • Retry mechanism via BaseApi.executeWithRetry() for transient failures (503, 429, 500)
 *
 * 2.5 additions:
 *   • Every public method attaches its response body to the current Allure scenario
 *     via BaseApi.attachResponseToAllure() — not just console-logged
 *
 * 3.5 additions:
 *   • Every timed call records its duration via PerformanceLogger
 */
public class NotesApiManager extends BaseApi {

    private String authToken;

    // ─────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────

    public void authenticate(String email, String password) {
        String payload = buildJson("email", email, "password", password);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("authenticate POST /users/login", () ->
                given(requestSpec)
                        .body(payload)
                        .when().post("/users/login")
                        .then()
                        .statusCode(200)
                        .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(
                                "schemas/login_response_schema.json"))
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("authenticate", "POST /users/login", elapsed, 2000);
        attachResponseToAllure("POST /users/login — authenticate", response);

        this.authToken = response.jsonPath().getString("data.token");
    }

    /**
     * TS-API-01 — Returns raw login response for step-level assertions.
     */
    public Response loginAndGetResponse(String email, String password) {
        String payload = buildJson("email", email, "password", password);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("loginAndGetResponse POST /users/login", () ->
                given(requestSpec)
                        .body(payload)
                        .when().post("/users/login")
                        .then().extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        if (response.getStatusCode() == 200) {
            response.then()
                    .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(
                            "schemas/login_response_schema.json"));
        }

        PerformanceLogger.logApiTiming("TS-API-01", "POST /users/login", elapsed, 2000);
        // 2.5 — attach to Allure per scenario
        attachResponseToAllure("POST /users/login — TS-API-01 Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────

    public void createNoteViaApi(String title, String description, String category) {
        String payload = buildNoteJson(title, description, category);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("createNoteViaApi POST /notes", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().post("/notes")
                        .then()
                        .statusCode(200)
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("createNoteViaApi", "POST /notes", elapsed, 2000);
        attachResponseToAllure("POST /notes — createNoteViaApi Response", response);
    }

    public String createAndReturnNoteId(String title, String description, String category) {
        String payload = buildNoteJson(title, description, category);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("createAndReturnNoteId POST /notes", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().post("/notes")
                        .then()
                        .statusCode(200)
                        .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(
                                "schemas/create_note_response_schema.json"))
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("createAndReturnNoteId", "POST /notes", elapsed, 2000);
        attachResponseToAllure("POST /notes — createAndReturnNoteId Response", response);
        return response.path("data.id");
    }

    /**
     * TS-API-03 — Creates a note, validates schema, and returns the full response.
     */
    public Response createNoteAndGetResponse(String title, String description, String category) {
        String payload = buildNoteJson(title, description, category);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("createNoteAndGetResponse POST /notes", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().post("/notes")
                        .then().extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        if (response.getStatusCode() == 200) {
            response.then()
                    .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(
                            "schemas/create_note_response_schema.json"))
                    .time(lessThan(2000L));
        }

        PerformanceLogger.logApiTiming("TS-API-03", "POST /notes", elapsed, 2000);
        attachResponseToAllure("POST /notes — TS-API-03 Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────

    public boolean verifyNoteExists(String expectedTitle) {
        long start = System.currentTimeMillis();
        Response response = executeWithRetry("verifyNoteExists GET /notes", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .when().get("/notes")
                        .then()
                        .statusCode(200)
                        .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(
                                "schemas/get_notes_response_schema.json"))
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("verifyNoteExists", "GET /notes", elapsed, 2000);
        attachResponseToAllure("GET /notes — verifyNoteExists Response", response);

        List<String> titles = response.jsonPath().getList("data.title");
        return titles != null && titles.contains(expectedTitle);
    }

    /** TS-API-06 — Fetches a single note by ID with SLA check. */
    public Response getNoteById(String noteId) {
        long start = System.currentTimeMillis();
        Response response = executeWithRetry("getNoteById GET /notes/" + noteId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .when().get("/notes/" + noteId)
                        .then()
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("TS-API-06", "GET /notes/:id", elapsed, 2000);
        attachResponseToAllure("GET /notes/" + noteId + " — TS-API-06 Response", response);
        return response;
    }

    public Response getNotesWithoutToken() {
        Response response = executeWithRetry("getNotesWithoutToken GET /notes", () ->
                given(requestSpec)
                        .when().get("/notes")
                        .then().extract().response()
        );
        attachResponseToAllure("GET /notes — No-Auth Security Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────

    /** TS-API-04 — Full replace via PUT with SLA check and retry. */
    public Response putNote(String noteId, String title, String description, String category) {
        String payload = buildNoteJson(title, description, category);

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("putNote PUT /notes/" + noteId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().put("/notes/" + noteId)
                        .then()
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("TS-API-04", "PUT /notes/:id", elapsed, 2000);
        attachResponseToAllure("PUT /notes/" + noteId + " — TS-API-04 Response", response);
        return response;
    }

    /**
     * TS-API-05 — Partial update via PATCH.
     * Fetches the existing note first to preserve title/description,
     * then sends a full body with only the category changed.
     */
    public Response patchNoteCategory(String noteId, String newCategory) {
        Response existing = executeWithRetry("patchNoteCategory GET /notes/" + noteId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .when().get("/notes/" + noteId)
                        .then().extract().response()
        );

        String currentTitle       = existing.jsonPath().getString("data.title");
        String currentDescription = existing.jsonPath().getString("data.description");

        String payload = "{ \"title\": \"" + currentTitle
                + "\", \"description\": \"" + currentDescription
                + "\", \"category\": \"" + newCategory
                + "\", \"completed\": false }";

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("patchNoteCategory PATCH /notes/" + noteId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().patch("/notes/" + noteId)
                        .then()
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("TS-API-05", "PATCH /notes/:id", elapsed, 2000);
        attachResponseToAllure("PATCH /notes/" + noteId + " — TS-API-05 Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────

    public void deleteNoteById(String noteId) {
        long start = System.currentTimeMillis();
        Response response = executeWithRetry("deleteNoteById DELETE /notes/" + noteId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .when().delete("/notes/" + noteId)
                        .then()
                        .statusCode(200)
                        .spec(slaSpec)
                        .extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("deleteNoteById", "DELETE /notes/:id", elapsed, 2000);
        attachResponseToAllure("DELETE /notes/" + noteId + " Response", response);
    }

    public Response deleteInvalidNote(String invalidId) {
        Response response = executeWithRetry("deleteInvalidNote DELETE /notes/" + invalidId, () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .when().delete("/notes/" + invalidId)
                        .then().extract().response()
        );
        attachResponseToAllure("DELETE /notes/" + invalidId + " — Invalid ID Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // NEGATIVE
    // ─────────────────────────────────────────────────────────────

    public Response createNoteWithMissingTitle(String description, String category) {
        String payload = "{ \"description\": \"" + description + "\", \"category\": \"" + category + "\" }";

        long start = System.currentTimeMillis();
        Response response = executeWithRetry("createNoteWithMissingTitle POST /notes", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().post("/notes")
                        .then().extract().response()
        );
        long elapsed = System.currentTimeMillis() - start;

        PerformanceLogger.logApiTiming("TS-NEG-04", "POST /notes (missing title)", elapsed, 2000);
        attachResponseToAllure("POST /notes — TS-NEG-04 Missing Title Response", response);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // USER PROFILE
    // ─────────────────────────────────────────────────────────────

    public Response changePassword(String currentPassword, String newPassword) {
        String payload = "{ \"currentPassword\": \"" + currentPassword
                + "\", \"newPassword\": \"" + newPassword + "\" }";
        Response response = executeWithRetry("changePassword PATCH /users/change-password", () ->
                given(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(payload)
                        .when().patch("/users/change-password")
                        .then().extract().response()
        );
        attachResponseToAllure("PATCH /users/change-password Response", response);
        return response;
    }

    public String getAuthToken() { return authToken; }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private String buildNoteJson(String title, String description, String category) {
        return "{ \"title\": \"" + title
                + "\", \"description\": \"" + description
                + "\", \"category\": \"" + category
                + "\", \"completed\": false }";
    }

    private String buildJson(String k1, String v1, String k2, String v2) {
        return "{ \"" + k1 + "\": \"" + v1 + "\", \"" + k2 + "\": \"" + v2 + "\" }";
    }
}
