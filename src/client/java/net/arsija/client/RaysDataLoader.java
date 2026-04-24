package net.arsija.client;

import net.arsija.RaysMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class RaysDataLoader {
    private static final String SHEET_ID = "1S3jBzfy_PtJhQI_5jFIN3lXBiUEMebt_rT2x5os2MYw";
    private static final String CSV_URL =
            "https://docs.google.com/spreadsheets/d/" + SHEET_ID + "/export?format=csv";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicReference<List<RaysItem>> CACHE =
            new AtomicReference<>(Collections.emptyList());
    private static volatile boolean loading = false;
    private static volatile String lastError = null;
    private static volatile long lastLoadedAtMs = 0L;

    private RaysDataLoader() {}

    public static List<RaysItem> getItems() {
        return CACHE.get();
    }

    public static boolean isLoading() {
        return loading;
    }

    public static String getLastError() {
        return lastError;
    }

    public static long getLastLoadedAtMs() {
        return lastLoadedAtMs;
    }

    public static synchronized CompletableFuture<Void> reload() {
        if (loading) return CompletableFuture.completedFuture(null);
        loading = true;
        lastError = null;
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(CSV_URL))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "RaysMod/1.0 (+fabric)")
                        .GET()
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() / 100 != 2) {
                    throw new RuntimeException("HTTP " + resp.statusCode());
                }
                List<RaysItem> parsed = parseCsv(resp.body());
                CACHE.set(parsed);
                lastLoadedAtMs = System.currentTimeMillis();
                RaysMod.LOGGER.info("Rays Mod: loaded {} items from Google Sheet", parsed.size());
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                RaysMod.LOGGER.error("Rays Mod: failed to load sheet", e);
            } finally {
                loading = false;
            }
        });
    }

    private static List<RaysItem> parseCsv(String body) {
        List<List<String>> rows = parseRows(body);
        List<RaysItem> items = new ArrayList<>();
        if (rows.isEmpty()) return items;

        // Find header row by scanning the first few rows for an "Item" column.
        int headerRow = 0;
        outer:
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            for (String cell : rows.get(i)) {
                if (cell != null && cell.trim().toLowerCase().startsWith("item")) {
                    headerRow = i;
                    break outer;
                }
            }
        }

        List<String> header = rows.get(headerRow);
        int idxItem = -1, idxYoutube = -1, idxCategory = -1, idxFarming = -1,
                idxNamespaced = -1, idxFixedDisplay = -1, idxDisplay = -1;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i).trim().toLowerCase();
            if (h.startsWith("item") && idxItem < 0) idxItem = i;
            else if (h.contains("ray") && h.contains("video")) idxYoutube = i;
            else if (h.equals("category")) idxCategory = i;
            else if (h.contains("farming method")) idxFarming = i;
            else if (h.contains("namespaced")) idxNamespaced = i;
            else if (h.startsWith("fixed display name")) idxFixedDisplay = i;
            else if (h.equals("display name")) idxDisplay = i;
        }

        for (int r = headerRow + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String rawName = safe(row, idxItem);
            if (rawName.isBlank()) continue;

            String displayName = safe(row, idxFixedDisplay);
            if (displayName.isBlank()) displayName = safe(row, idxDisplay);
            if (displayName.isBlank()) {
                // Item column often has form "Display Name\n(minecraft:foo)"
                displayName = rawName.split("[\\r\\n(]")[0].trim();
            }

            String namespaced = safe(row, idxNamespaced);
            if (namespaced.isBlank()) {
                // Try to extract "minecraft:foo" from raw name
                int s = rawName.indexOf('(');
                int e = rawName.indexOf(')', s + 1);
                if (s >= 0 && e > s) {
                    namespaced = rawName.substring(s + 1, e).trim();
                }
            }

            items.add(new RaysItem(
                    displayName,
                    namespaced,
                    safe(row, idxCategory),
                    safe(row, idxFarming),
                    safe(row, idxYoutube)
            ));
        }
        return items;
    }

    private static String safe(List<String> row, int idx) {
        if (idx < 0 || idx >= row.size()) return "";
        String v = row.get(idx);
        return v == null ? "" : v.trim();
    }

    private static List<List<String>> parseRows(String body) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < body.length() && body.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    current.add(cell.toString());
                    cell.setLength(0);
                } else if (c == '\r') {
                    // ignore
                } else if (c == '\n') {
                    current.add(cell.toString());
                    cell.setLength(0);
                    rows.add(current);
                    current = new ArrayList<>();
                } else {
                    cell.append(c);
                }
            }
        }
        if (cell.length() > 0 || !current.isEmpty()) {
            current.add(cell.toString());
            rows.add(current);
        }
        return rows;
    }
}
