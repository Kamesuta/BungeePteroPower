package com.kamesuta.bungeepteropower;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Checks for updates via SpigotMC API
 */
public class UpdateChecker {
    /**
     * The resource ID of this plugin on SpigotMC
     */
    private static final int ResourceId = 114883;
    /**
     * The current version
     */
    private final String runningVersion;
    /**
     * The new version
     */
    private String newVersion;

    /**
     * Create a new update checker
     *
     * @param runningVersion The current version
     */
    public UpdateChecker(String runningVersion) {
        this.runningVersion = runningVersion;
    }

    /**
     * Check for updates
     *
     * @return A future that completes when the update check is finished
     */
    public CompletableFuture<Void> checkForUpdates() {
        // Check for updates via SpigotMC API
        HttpClient client = HttpClient.newHttpClient();
        // Create a request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + ResourceId))
                .build();

        // Execute request and register a callback
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(version -> {
                    newVersion = version;
                });
    }

    /**
     * Returns whether an update is available
     *
     * @return true if an update is available
     */
    public boolean isUpdateAvailable() {
        if (newVersion == null) {
            // If the update check has not been completed, assume there is no update
            return false;
        }
        // Compare the versions
        return compareVersions(runningVersion, newVersion);
    }

    /**
     * Get download link
     *
     * @return the download link of the new version
     */
    public String getDownloadLink() {
        return "https://www.spigotmc.org/resources/" + ResourceId;
    }

    /**
     * Get the current version
     *
     * @return the current version
     */
    public String getRunningVersion() {
        return runningVersion;
    }

    /**
     * Get the new version
     *
     * @return the new version
     */
    public @Nullable String getNewVersion() {
        return newVersion;
    }

    /**
     * Compares two versions and returns whether the new version is newer than the current version
     *
     * @param runningVersion The current version
     * @param newVersion     The new version
     * @return true if the new version is newer than the current version
     */
    public static boolean compareVersions(String runningVersion, String newVersion) {
        // Split the running versions into integers
        List<Integer> current = Arrays.stream(runningVersion.split("\\."))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        // Split the new versions into integers
        List<Integer> latest = Arrays.stream(newVersion.split("\\."))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        // Compare the versions
        int comparison = Objects.compare(current, latest, Comparator.<List<Integer>>comparingInt(List::size).thenComparing(List::hashCode));
        return comparison < 0;
    }
}
