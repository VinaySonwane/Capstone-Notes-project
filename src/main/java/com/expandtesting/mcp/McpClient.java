package com.expandtesting.mcp;

import com.expandtesting.config.ConfigReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 3.4 — MCP (Model Context Protocol) Implementation.
 *
 * This client uses the Grok (xAI) API as the MCP backend.
 * It sends structured chat messages to the Grok API endpoint and returns
 * the model's text response.
 *
 * MCP in test automation context:
 *   The MCP client is used to query Grok for intelligent test-scenario
 *   suggestions, failure triage, and test-data generation. It complements
 *   the existing Examples-table data strategy by providing an AI layer that
 *   can reason about test failures and produce contextual hints.
 *
 * Configuration keys in config.properties:
 *   mcp.api.url    = https://api.x.ai/v1/chat/completions
 *   mcp.api.key    = gsk_...   (or set via env var GROK_API_KEY)
 *   mcp.model      = grok-3-mini
 *   mcp.max.tokens = 512
 *
 * Usage:
 *   McpClient mcp = new McpClient();
 *   String insight = mcp.triageFailure("TS-API-05", "Expected: Home, Actual: Personal");
 */
public class McpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int    maxTokens;

    public McpClient() {
        this.apiUrl    = getConfig("mcp.api.url",    "https://api.x.ai/v1/chat/completions");
        this.apiKey    = resolveApiKey();
        this.model     = getConfig("mcp.model",       "grok-3-mini");
        this.maxTokens = Integer.parseInt(getConfig("mcp.max.tokens", "512"));
    }

    // ── Public API ────────────────────────────────────────────────

    /**
     * Sends a failure description to Grok and returns a triage insight.
     *
     * @param testId      Cucumber scenario tag, e.g. "TS-API-05"
     * @param errorDetail The assertion error message from the test run
     * @return Grok's triage suggestion, or a fallback message on error
     */
    public String triageFailure(String testId, String errorDetail) {
        String prompt = String.format(
                "You are a senior QA automation engineer reviewing a test failure.\n\n" +
                "Test ID: %s\n" +
                "Error: %s\n\n" +
                "In 2-3 sentences explain the likely root cause and suggest the fix.",
                testId, errorDetail);
        return chat(prompt);
    }

    /**
     * Asks Grok to suggest additional test data rows for a Scenario Outline.
     *
     * @param scenarioTitle The scenario name
     * @param existingData  A description of the current Examples table rows
     * @return Suggested new Examples table rows as plain text
     */
    public String suggestTestData(String scenarioTitle, String existingData) {
        String prompt = String.format(
                "You are a QA engineer writing Cucumber Examples tables.\n\n" +
                "Scenario: %s\n" +
                "Existing rows: %s\n\n" +
                "Suggest 2 additional rows that improve boundary and equivalence coverage. " +
                "Reply with only the pipe-delimited rows, no explanations.",
                scenarioTitle, existingData);
        return chat(prompt);
    }

    /**
     * Asks Grok to generate a one-line summary of the test run for a report.
     *
     * @param passed  number of passed scenarios
     * @param failed  number of failed scenarios
     * @param skipped number of skipped scenarios
     * @return A one-sentence executive summary
     */
    public String generateRunSummary(int passed, int failed, int skipped) {
        String prompt = String.format(
                "Write a one-sentence executive summary of this test run for a QA report:\n" +
                "Passed: %d, Failed: %d, Skipped: %d.\n" +
                "Be concise and professional.",
                passed, failed, skipped);
        return chat(prompt);
    }

    // ── Core Grok chat message sending ───────────────────────────

    /**
     * Sends a user message to the Grok API (OpenAI-compatible chat/completions format)
     * and returns the first text choice from the response.
     */
    public String chat(String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[MCP/Grok] API key not configured — set mcp.api.key in config.properties " +
                   "or GROK_API_KEY environment variable.";
        }
        try {
            // Grok uses the OpenAI-compatible chat/completions format
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);

            ArrayNode messages = body.putArray("messages");
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are an expert QA automation assistant helping with test triage, " +
                    "test data generation, and test reporting.");

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String requestBody = MAPPER.writeValueAsString(body);

            HttpURLConnection conn = openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",  "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            InputStream stream = statusCode < 400
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                responseBody = sb.toString();
            }

            if (statusCode != 200) {
                return "[MCP/Grok] HTTP " + statusCode + ": " + responseBody;
            }

            // Parse OpenAI-compatible response: choices[0].message.content
            JsonNode root    = MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                String content = firstChoice.path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content;
                }
            }
            return "[MCP/Grok] Unexpected response structure: " + responseBody;

        } catch (Exception e) {
            return "[MCP/Grok] Error: " + e.getMessage();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private HttpURLConnection openConnection() throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        return conn;
    }

    private String resolveApiKey() {
        // Prefer environment variable (safer for CI), fall back to config file
        String envKey = System.getenv("GROK_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;
        return getConfig("mcp.api.key", "");
    }

    private String getConfig(String key, String defaultValue) {
        try {
            String value = ConfigReader.getProperty(key);
            return (value != null && !value.isBlank()) ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
