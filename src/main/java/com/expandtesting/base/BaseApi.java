package com.expandtesting.base;

import com.expandtesting.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.lessThan;

/**
 * 2.1 — Framework Architecture: Base API class.
 *
 * Centralises RestAssured configuration so every API test class inherits
 * a consistent base URI, content-type, logging filters, and the 2-second
 * SLA response-time specification required by FR-08 / section 2.3.
 *
 * 2.3 (advanced) — Retry mechanism:
 *   executeWithRetry() wraps any API call in a retry loop (default 3 attempts,
 *   500 ms back-off) that retries on transient HTTP status codes (503, 429, 500)
 *   and on IOException / SocketTimeoutException.
 *
 * 2.5 — Reporting:
 *   attachResponseToAllure() attaches the full API response body to the
 *   current Allure step as a named text/json attachment.
 */
public class BaseApi {

    // ─── Retry configuration ──────────────────────────────────────
    /** Maximum number of attempts for transient-failure retries. */
    protected static final int    RETRY_MAX_ATTEMPTS  = 3;
    /** Milliseconds to wait between retry attempts (exponential back-off base). */
    protected static final long   RETRY_BACKOFF_MS    = 500L;
    /** HTTP status codes that are considered transient and worth retrying. */
    protected static final int[]  RETRY_ON_STATUS     = {500, 503, 429};

    /** Pre-built request spec: base URI + JSON content type + logging. */
    protected static final RequestSpecification requestSpec;

    /**
     * 2.3 / 3.5 — SLA response spec: every API response must arrive in < 2 000 ms.
     * Attach with .then().spec(slaSpec) on any API call that must meet the SLA.
     */
    protected static final ResponseSpecification slaSpec;

    static {
        RestAssured.baseURI = ConfigReader.getProperty("api.base.url");

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.getProperty("api.base.url"))
                .setContentType(ContentType.JSON)
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        // 2 000 ms hard ceiling as per section 2.3 / FR-08
        slaSpec = new ResponseSpecBuilder()
                .expectResponseTime(lessThan(2000L))
                .build();
    }

    // ─── 2.3 Advanced: Retry mechanism ───────────────────────────

    /**
     * Executes the given API call with automatic retry on transient failures.
     *
     * <p>Retry is triggered when:
     * <ul>
     *   <li>The HTTP response status code is in {@link #RETRY_ON_STATUS} (500, 503, 429)</li>
     *   <li>An exception is thrown (e.g. {@code SocketTimeoutException}, connection reset)</li>
     * </ul>
     *
     * <p>Back-off: attempt N waits {@code RETRY_BACKOFF_MS * N} ms before the next attempt.
     *
     * @param operationName Human-readable label used in logs and Allure step names
     * @param apiCall       A lambda / method reference that performs the actual API call
     * @return              The last successful {@link Response}, or the last failed response
     *                      after all attempts are exhausted
     */
    protected Response executeWithRetry(String operationName, Supplier<Response> apiCall) {
        Response lastResponse = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= RETRY_MAX_ATTEMPTS; attempt++) {
            try {
                lastException = null;
                lastResponse  = apiCall.get();

                int status = lastResponse.getStatusCode();
                if (!isTransientStatus(status)) {
                    // Success or a non-transient failure — stop retrying
                    if (attempt > 1) {
                        System.out.printf("[Retry] %s succeeded on attempt %d (status %d)%n",
                                operationName, attempt, status);
                    }
                    return lastResponse;
                }

                System.out.printf("[Retry] %s got transient status %d on attempt %d/%d%n",
                        operationName, status, attempt, RETRY_MAX_ATTEMPTS);

            } catch (Exception ex) {
                lastException = ex;
                System.out.printf("[Retry] %s threw %s on attempt %d/%d: %s%n",
                        operationName, ex.getClass().getSimpleName(),
                        attempt, RETRY_MAX_ATTEMPTS, ex.getMessage());
            }

            if (attempt < RETRY_MAX_ATTEMPTS) {
                long waitMs = RETRY_BACKOFF_MS * attempt; // exponential: 500, 1000, 1500 …
                System.out.printf("[Retry] Waiting %d ms before attempt %d/%d …%n",
                        waitMs, attempt + 1, RETRY_MAX_ATTEMPTS);
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (lastException != null) {
            throw new RuntimeException(
                    "[Retry] " + operationName + " failed after " + RETRY_MAX_ATTEMPTS
                    + " attempts: " + lastException.getMessage(), lastException);
        }
        return lastResponse; // return the last transient-failure response
    }

    /**
     * Returns {@code true} when the status code is one we should retry.
     */
    private boolean isTransientStatus(int status) {
        for (int s : RETRY_ON_STATUS) {
            if (s == status) return true;
        }
        return false;
    }

    // ─── 2.5 Advanced: Allure response body attachment ───────────

    /**
     * Attaches the full API response body to the current Allure scenario
     * as a named {@code application/json} attachment.
     *
     * <p>Call this immediately after any API call whose response body
     * should appear in the Allure report (not just in the console log).
     *
     * @param attachmentName Label shown in the Allure report, e.g. "POST /users/login Response"
     * @param response       The RestAssured {@link Response} to attach
     */
    protected void attachResponseToAllure(String attachmentName, Response response) {
        if (response == null) return;
        try {
            String body = response.getBody().asPrettyString();
            if (body == null || body.isBlank()) {
                body = "<empty response body>";
            }
            // Prefix with status line for quick triage
            String fullContent = "HTTP " + response.getStatusCode()
                    + " " + response.getStatusLine() + "\n\n" + body;

            byte[] bytes = fullContent.getBytes(StandardCharsets.UTF_8);
            Allure.addAttachment(
                    attachmentName,
                    "application/json",
                    new ByteArrayInputStream(bytes),
                    ".json"
            );
        } catch (Exception e) {
            System.out.println("[BaseApi] Could not attach response to Allure: " + e.getMessage());
        }
    }
}
