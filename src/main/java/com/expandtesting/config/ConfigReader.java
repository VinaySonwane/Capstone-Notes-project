package com.expandtesting.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration in a two-layer approach so the project runs
 * identically on a developer's machine and in Jenkins:
 *
 *  Layer 1  — src/test/resources/config.properties   (committed to git)
 *             Contains all non-secret, environment-agnostic settings.
 *
 *  Layer 2  — src/test/resources/environment.properties  (git-ignored)
 *             Contains secrets and local overrides (e.g. mcp.api.key).
 *             Values here OVERRIDE Layer 1.
 *             On Jenkins this file is absent; secrets arrive via the
 *             GROQ_API_KEY environment variable injected by withCredentials.
 *
 * Key resolution order for any property:
 *   1. environment.properties  (if the file exists and the key is present)
 *   2. config.properties
 */
public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        // ── Layer 1: base config (always present, committed to git) ──────────
        try (FileInputStream base =
                     new FileInputStream("src/test/resources/config.properties")) {
            properties.load(base);
        } catch (IOException e) {
            throw new RuntimeException(
                    "config.properties not found — expected at src/test/resources/config.properties", e);
        }

        // ── Layer 2: environment overrides (optional, git-ignored) ───────────
        // Present on developer machines; absent in Jenkins (secrets come via env-vars instead).
        try (InputStream env =
                     new FileInputStream("src/test/resources/environment.properties")) {
            Properties envProps = new Properties();
            envProps.load(env);
            // putAll makes Layer 2 win over Layer 1 for any overlapping key
            properties.putAll(envProps);
        } catch (IOException ignored) {
            // environment.properties is optional — Jenkins runs without it
        }
    }

    /**
     * Returns the property value for the given key.
     * Returns {@code null} if the key is absent in both files.
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
