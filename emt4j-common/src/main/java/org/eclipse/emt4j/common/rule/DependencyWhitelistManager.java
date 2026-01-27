/********************************************************************************
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.emt4j.common.rule;

import org.eclipse.emt4j.common.DependencySourceDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * White list manager for dependency checking.
 * Dependencies in whitelist will skip all rule analysis.
 * If current version is greater than whitelist version, it's also considered whitelisted.
 */
public class DependencyWhitelistManager {
    private static volatile boolean initialized = false;
    private static Map<String, String> whitelistCache = new ConcurrentHashMap<>();

    // Configuration file path
    private static final String WHITELIST_CONFIG_PATH = "emt4j-whitelist.properties";

    /**
     * Initialize whitelist from configuration file
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        loadWhitelist();
        initialized = true;
    }

    /**
     * Check if dependency should be whitelisted (skip analysis)
     *
     * @param dependency the dependency to check
     * @return true if should skip analysis, false otherwise
     */
    public static boolean isWhitelisted(DependencySourceDto dependency) {
        if (!initialized) {
            init();
        }

        if (!dependency.getInformation().isDependency()) {
            return false; // Only check actual dependencies
        }

        String dependencyKey = extractDependencyKey(dependency);
        if (dependencyKey == null) {
            return false;
        }
        String whitelistVersion = whitelistCache.get(dependencyKey);
        if (whitelistVersion == null) {
            return false; // Not in whitelist
        }

        String currentVersion = extractCurrentVersion(dependency);
        if (currentVersion == null) {
            return false;
        }
        // If current version >= whitelist version, consider it whitelisted
        boolean result = compareVersions(currentVersion, whitelistVersion) >= 0;
        System.out.println("当前依赖【"+ dependencyKey +"】位于白名单中，currentVersion为【{" + currentVersion + "}】，whitelistVersion为【{" + whitelistVersion + "}】，验证结果为：【{" + result + "}】");
        return result;
    }

    /**
     * Extract dependency key (groupId:artifactId) from dependency
     */
    private static String extractDependencyKey(DependencySourceDto dependency) {
        try {
            if (dependency.getInformation() != null &&
                    dependency.getInformation().getExtras() != null &&
                    dependency.getInformation().getExtras().length > 0) {

                String[] GAV = dependency.getInformation().getExtras()[0].split(":");
                if (GAV.length >= 2) {
                    return GAV[0] + ":" + GAV[1]; // groupId:artifactId
                }
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        return null;
    }

    /**
     * Extract current version from dependency
     */
    private static String extractCurrentVersion(DependencySourceDto dependency) {
        try {
            if (dependency.getInformation() != null &&
                    dependency.getInformation().getExtras() != null &&
                    dependency.getInformation().getExtras().length > 0) {

                String[] GAV = dependency.getInformation().getExtras()[0].split(":");
                if (GAV.length >= 3) {
                    return GAV[2]; // version
                }
            }
        } catch (Exception e) {
            // Ignore extraction errors
        }
        return null;
    }

    /**
     * Load whitelist from configuration file
     */
    private static void loadWhitelist() {
        whitelistCache.clear();

        // Try to load from classpath first
        InputStream is = DependencyWhitelistManager.class.getClassLoader().getResourceAsStream(WHITELIST_CONFIG_PATH);
        if (is == null) {
            // Try to load from file system
            try {
                URL configFile = new URL("file:" + WHITELIST_CONFIG_PATH);
                is = configFile.openStream();
            } catch (Exception e) {
                // No config file found, use empty whitelist
                return;
            }
        }

        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue; // Skip empty lines and comments
                    }

                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String dependencyKey = parts[0].trim();
                        String version = parts[1].trim();
                        whitelistCache.put(dependencyKey, version);
                    }
                }
            } catch (IOException e) {
                // Log error but continue with empty whitelist
                System.err.println("Failed to load whitelist config: " + e.getMessage());
            }
        }
    }

    /**
     * Compare two version strings
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        if (v1.equals(v2)) {
            return 0;
        }

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    /**
     * Parse version part to int, handling non-numeric suffixes
     */
    private static int parseInt(String part) {
        try {
            // Extract numeric part before any non-numeric characters
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Reload whitelist (for testing or dynamic updates)
     */
    public static synchronized void reload() {
        initialized = false;
        init();
    }

    /**
     * Get current whitelist entries (for debugging)
     */
    public static Map<String, String> getWhitelistEntries() {
        if (!initialized) {
            init();
        }
        return new HashMap<>(whitelistCache);
    }
}