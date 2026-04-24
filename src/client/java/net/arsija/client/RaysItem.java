package net.arsija.client;

public final class RaysItem {
    public final String displayName;
    public final String namespacedId;
    public final String category;
    public final String farmingMethod;
    public final String youtubeUrl;

    public RaysItem(String displayName, String namespacedId, String category,
                    String farmingMethod, String youtubeUrl) {
        this.displayName = displayName == null ? "" : displayName;
        this.namespacedId = namespacedId == null ? "" : namespacedId;
        this.category = category == null ? "" : category;
        this.farmingMethod = farmingMethod == null ? "" : farmingMethod;
        this.youtubeUrl = youtubeUrl == null ? "" : youtubeUrl;
    }

    public boolean hasVideo() {
        return !youtubeUrl.isBlank();
    }
}
