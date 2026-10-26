package com.corosus.watut.paper.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Prueft auf Modrinth, ob eine neuere Version veroeffentlicht wurde.
 * Haengt nur an der Bukkit-API und an Gson (liegt Spigot/Paper bei).
 */
public final class UpdateChecker {

    /** Rueckmeldung nach Abschluss -- laeuft im Main-Thread. */
    @FunctionalInterface
    public interface Callback {
        void finished(UpdateChecker checker);
    }

    private static final String API = "https://api.modrinth.com/v2/project/%s/version";
    private static final int TIMEOUT_MS = 10_000;

    private final JavaPlugin plugin;
    private final String projectId;
    private final String currentVersion;
    private final String contact;
    private final boolean stableOnly;

    private volatile String latestVersion;
    private volatile boolean updateAvailable;
    private volatile boolean checked;
    private volatile long lastCheckTime;

    /**
     * @param projectId  Modrinth-Projekt-ID oder Slug
     * @param contact    Kontakt fuer den User-Agent -- Modrinth verlangt das
     * @param stableOnly true = Vorabversionen (beta/alpha) ignorieren
     */
    public UpdateChecker(JavaPlugin plugin, String projectId, String contact, boolean stableOnly) {
        this.plugin = plugin;
        this.projectId = projectId;
        this.contact = contact;
        this.stableOnly = stableOnly;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    // ---------------------------------------------------------------- API

    /** Sofortiger Abruf mit Rueckmeldung. */
    public void check(Callback callback) {
        check(0L, callback);
    }

    /**
     * @param delayTicks Verzoegerung vor dem Request
     * @param callback   darf null sein
     */
    public void check(long delayTicks, Callback callback) {
        long delayMs = Math.max(0L, delayTicks) * 50L;
        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            try {
                performCheck();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Update-Pruefung fehlgeschlagen: " + e.getMessage(), e);
            } finally {
                if (callback != null && plugin.isEnabled()) {
                    Bukkit.getGlobalRegionScheduler().run(plugin, regionTask -> callback.finished(this));
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /** true nur, wenn wirklich geprueft wurde UND eine neuere Version existiert. */
    public boolean isUpdateAvailable() { return checked && updateAvailable; }

    /** false = noch kein Ergebnis (nie gelaufen oder fehlgeschlagen). */
    public boolean hasChecked()        { return checked; }

    public String getLatestVersion()   { return latestVersion; }
    public String getCurrentVersion()  { return currentVersion; }
    public long   getLastCheckTime()   { return lastCheckTime; }
    public String getDownloadUrl()     { return "https://modrinth.com/plugin/" + projectId; }

    // ------------------------------------------------------------- intern

    private void performCheck() throws Exception {
        URL url = URI.create(String.format(API, projectId)).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                    plugin.getName() + "/" + currentVersion + " (" + contact + ")");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            int status = connection.getResponseCode();
            if (status != 200) {
                plugin.getLogger().warning("Update-Pruefung: HTTP " + status + " von Modrinth");
                return;
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            JsonArray versions = JsonParser.parseString(body.toString()).getAsJsonArray();

            String highest = null;
            for (JsonElement element : versions) {
                JsonObject version = element.getAsJsonObject();
                if (stableOnly && version.has("version_type")
                        && !"release".equals(version.get("version_type").getAsString())) {
                    continue;
                }
                if (!version.has("version_number")) {
                    continue;
                }
                String number = version.get("version_number").getAsString();
                if (highest == null || isNewer(highest, number)) {
                    highest = number;
                }
            }
            if (highest == null) {
                return;
            }

            latestVersion = highest;
            updateAvailable = isNewer(currentVersion, highest);
            checked = true;
            lastCheckTime = System.currentTimeMillis();

            if (updateAvailable) {
                plugin.getLogger().info("Update verfuegbar: " + currentVersion
                        + " -> " + latestVersion + " (" + getDownloadUrl() + ")");
            } else {
                plugin.getLogger().info("Plugin ist aktuell (" + currentVersion + ")");
            }
        } finally {
            connection.disconnect();
        }
    }

    static boolean isNewer(String current, String candidate) {
        int[] a = numericParts(current);
        int[] b = numericParts(candidate);
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int left  = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (right != left) {
                return right > left;
            }
        }
        return isPreRelease(current) && !isPreRelease(candidate);
    }

    private static int[] numericParts(String version) {
        if (version == null) {
            return new int[0];
        }
        String core = version.split("[-+]", 2)[0];
        List<Integer> parts = new ArrayList<>();
        for (String segment : core.split("\\.")) {
            String digits = segment.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                parts.add(0);
                continue;
            }
            try {
                parts.add(Integer.parseInt(digits));
            } catch (NumberFormatException e) {
                parts.add(Integer.MAX_VALUE);
            }
        }
        int[] out = new int[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }

    private static boolean isPreRelease(String version) {
        if (version == null) {
            return false;
        }
        String lower = version.toLowerCase(Locale.ROOT);
        return lower.contains("-rc") || lower.contains("-beta")
                || lower.contains("-alpha") || lower.contains("-pre")
                || lower.contains("-snapshot");
    }
}
