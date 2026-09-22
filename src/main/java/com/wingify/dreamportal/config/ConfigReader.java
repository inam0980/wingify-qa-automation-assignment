package com.wingify.dreamportal.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Central access point for every tunable value in the suite.
 *
 * <p>Values are resolved with the following precedence, so CI and local runs can override the
 * committed defaults without editing a tracked file:
 *
 * <ol>
 *   <li>JVM system property — {@code -Dbrowser=edge}</li>
 *   <li>OS environment variable — {@code BROWSER=edge} (dots become underscores, upper-cased)</li>
 *   <li>{@code .env} file at the project root — untracked, holds API keys</li>
 *   <li>{@code src/test/resources/config.properties} — the committed defaults</li>
 * </ol>
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties DEFAULTS = new Properties();
    private static final Map<String, String> DOT_ENV = new HashMap<>();

    static {
        loadConfigProperties();
        loadDotEnv();
    }

    private ConfigReader() {
    }

    private static void loadConfigProperties() {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found on the test classpath");
            }
            DEFAULTS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + CONFIG_FILE, e);
        }
    }

    /** Reads an optional {@code .env} file so secrets never have to live in a tracked file. */
    private static void loadDotEnv() {
        Path envFile = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                DOT_ENV.put(
                        trimmed.substring(0, separator).trim(),
                        stripQuotes(trimmed.substring(separator + 1).trim()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read .env", e);
        }
    }

    private static String stripQuotes(String value) {
        boolean quoted = value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }

    /** @return the resolved value, or {@code null} when the key is defined nowhere. */
    public static String get(String key) {
        String fromSystem = System.getProperty(key);
        if (isPresent(fromSystem)) {
            return fromSystem.trim();
        }

        String envKey = toEnvKey(key);
        String fromEnv = System.getenv(envKey);
        if (isPresent(fromEnv)) {
            return fromEnv.trim();
        }

        String fromDotEnv = DOT_ENV.get(envKey);
        if (isPresent(fromDotEnv)) {
            return fromDotEnv.trim();
        }

        String fromFile = DEFAULTS.getProperty(key);
        return isPresent(fromFile) ? fromFile.trim() : null;
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return value == null ? fallback : value;
    }

    /** @throws IllegalStateException when the key is missing, so misconfiguration fails fast. */
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Required configuration '" + key + "' is not set. Add it to " + CONFIG_FILE
                            + ", pass -D" + key + "=<value>, or export " + toEnvKey(key) + ".");
        }
        return value;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Configuration '" + key + "' must be an integer but was '" + value + "'", e);
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static String toEnvKey(String key) {
        return key.replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
