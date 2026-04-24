package net.arsija.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RaysMenuScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_PADDING = 20;

    private EditBox searchField;
    private double scroll = 0;
    private List<RaysItem> filtered = List.of();

    public RaysMenuScreen() {
        super(Component.literal("Rays Menu"));
    }

    @Override
    protected void init() {
        super.init();

        int reloadW = 80;
        int gap = 10;
        int searchY = 30;
        int searchH = 20;
        int searchW = Math.min(420, this.width - 2 * LIST_PADDING - reloadW - gap);
        int searchX = (this.width - (searchW + gap + reloadW)) / 2;

        this.searchField = new EditBox(this.font, searchX, searchY, searchW, searchH,
                Component.literal("Search"));
        this.searchField.setMaxLength(64);
        this.searchField.setHint(Component.literal("Search items..."));
        this.searchField.setResponder(s -> applyFilter());
        this.addRenderableWidget(this.searchField);
        this.setInitialFocus(this.searchField);

        Button reloadButton = Button.builder(Component.literal("Reload"),
                        b -> RaysDataLoader.reload().thenRun(() -> {
                            if (this.minecraft != null) {
                                this.minecraft.execute(this::applyFilter);
                            }
                        }))
                .bounds(searchX + searchW + gap, searchY, reloadW, searchH)
                .build();
        this.addRenderableWidget(reloadButton);

        Button closeButton = Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(this.width - LIST_PADDING - 80, this.height - 30, 80, 20)
                .build();
        this.addRenderableWidget(closeButton);

        applyFilter();
    }

    private void applyFilter() {
        String q = this.searchField == null
                ? ""
                : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<RaysItem> all = RaysDataLoader.getItems();
        if (q.isEmpty()) {
            this.filtered = all;
        } else {
            List<RaysItem> out = new ArrayList<>();
            for (RaysItem it : all) {
                if (it.displayName.toLowerCase(Locale.ROOT).contains(q)
                        || it.namespacedId.toLowerCase(Locale.ROOT).contains(q)
                        || it.category.toLowerCase(Locale.ROOT).contains(q)
                        || it.farmingMethod.toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(it);
                }
            }
            this.filtered = out;
        }
        scroll = 0;
    }

    private int listX() { return LIST_PADDING; }
    private int listY() { return 60; }
    private int listW() { return this.width - 2 * LIST_PADDING; }
    private int listH() { return this.height - listY() - 40; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        ctx.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        String status;
        if (RaysDataLoader.isLoading()) {
            status = "Loading...";
        } else if (RaysDataLoader.getLastError() != null && filtered.isEmpty()) {
            status = "Error: " + RaysDataLoader.getLastError();
        } else {
            status = filtered.size() + " / " + RaysDataLoader.getItems().size() + " items";
        }
        ctx.text(this.font, Component.literal(status), LIST_PADDING, 50, 0xFFAAAAAA);

        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        ctx.fill(lx, ly, lx + lw, ly + lh, 0x80000000);
        ctx.outline(lx, ly, lw, lh, 0xFF333333);

        int totalHeight = filtered.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - lh);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        ctx.enableScissor(lx + 1, ly + 1, lx + lw - 1, ly + lh - 1);
        int yStart = ly - (int) scroll;
        for (int i = 0; i < filtered.size(); i++) {
            int rowY = yStart + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < ly || rowY > ly + lh) continue;
            RaysItem item = filtered.get(i);
            boolean hovered = mouseX >= lx && mouseX < lx + lw
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) ctx.fill(lx + 1, rowY, lx + lw - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);

            String main = item.displayName;
            if (!item.namespacedId.isEmpty()) {
                main += " §8(" + item.namespacedId + ")";
            }
            ctx.text(this.font, Component.literal(main), lx + 6, rowY + 3, 0xFFFFFFFF);

            String sub = "";
            if (!item.category.isEmpty()) sub += item.category;
            if (!item.farmingMethod.isEmpty()) {
                if (!sub.isEmpty()) sub += " · ";
                sub += item.farmingMethod;
            }
            if (!sub.isEmpty()) {
                ctx.text(this.font, Component.literal("§7" + sub),
                        lx + 6, rowY + 13, 0xFFAAAAAA);
            }

            if (item.hasVideo()) {
                String yt = "▶ Video";
                int ytWidth = this.font.width(yt);
                ctx.text(this.font, Component.literal("§c" + yt),
                        lx + lw - ytWidth - 8, rowY + 7, 0xFFFF5555);
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(20, (int) ((double) lh * lh / totalHeight));
            int barY = ly + (int) ((scroll / maxScroll) * (lh - barH));
            ctx.fill(lx + lw - 4, barY, lx + lw - 1, barY + barH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int lx = listX(), ly = listY(), lw = listW(), lh = listH();
            if (mouseX >= lx && mouseX < lx + lw && mouseY >= ly && mouseY < ly + lh) {
                int rel = (int) (mouseY - ly + scroll);
                int idx = rel / ROW_HEIGHT;
                if (idx >= 0 && idx < filtered.size()) {
                    RaysItem item = filtered.get(idx);
                    if (item.hasVideo()) {
                        try {
                            Util.getPlatform().openUri(URI.create(item.youtubeUrl));
                        } catch (Exception ignored) {
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * ROW_HEIGHT;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
