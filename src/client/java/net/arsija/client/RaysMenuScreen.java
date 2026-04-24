package net.arsija.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public class RaysMenuScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_PADDING = 20;

    private TextFieldWidget searchField;
    private double scroll = 0;
    private List<RaysItem> filtered = List.of();

    public RaysMenuScreen() {
        super(Text.literal("Rays Menu"));
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

        this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, searchW, searchH,
                Text.literal("Search"));
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.literal("Search items..."));
        this.searchField.setChangedListener(s -> applyFilter());
        this.addSelectableChild(this.searchField);
        this.setInitialFocus(this.searchField);

        ButtonWidget reloadButton = ButtonWidget.builder(Text.literal("Reload"),
                        b -> RaysDataLoader.reload().thenRun(() ->
                                this.client.execute(this::applyFilter)))
                .dimensions(searchX + searchW + gap, searchY, reloadW, searchH)
                .build();
        this.addDrawableChild(reloadButton);

        ButtonWidget closeButton = ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(this.width - LIST_PADDING - 80, this.height - 30, 80, 20)
                .build();
        this.addDrawableChild(closeButton);

        applyFilter();
    }

    private void applyFilter() {
        String q = this.searchField == null
                ? ""
                : this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<RaysItem> all = RaysDataLoader.getItems();
        if (q.isEmpty()) {
            this.filtered = all;
        } else {
            List<RaysItem> out = new java.util.ArrayList<>();
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        String status;
        if (RaysDataLoader.isLoading()) {
            status = "Loading...";
        } else if (RaysDataLoader.getLastError() != null && filtered.isEmpty()) {
            status = "Error: " + RaysDataLoader.getLastError();
        } else {
            status = filtered.size() + " / " + RaysDataLoader.getItems().size() + " items";
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(status), LIST_PADDING, 50, 0xAAAAAA);

        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        ctx.fill(lx, ly, lx + lw, ly + lh, 0x80000000);
        ctx.drawBorder(lx, ly, lw, lh, 0xFF333333);

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
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(main), lx + 6, rowY + 3, 0xFFFFFF);

            String sub = "";
            if (!item.category.isEmpty()) sub += item.category;
            if (!item.farmingMethod.isEmpty()) {
                if (!sub.isEmpty()) sub += " · ";
                sub += item.farmingMethod;
            }
            if (!sub.isEmpty()) {
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7" + sub),
                        lx + 6, rowY + 13, 0xAAAAAA);
            }

            if (item.hasVideo()) {
                String yt = "▶ Video";
                int ytWidth = this.textRenderer.getWidth(yt);
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("§c" + yt),
                        lx + lw - ytWidth - 8, rowY + 7, 0xFF5555);
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(20, (int) ((double) lh * lh / totalHeight));
            int barY = ly + (int) ((scroll / maxScroll) * (lh - barH));
            ctx.fill(lx + lw - 4, barY, lx + lw - 1, barY + barH, 0xFFAAAAAA);
        }

        this.searchField.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int lx = listX(), ly = listY(), lw = listW(), lh = listH();
            if (mouseX >= lx && mouseX < lx + lw && mouseY >= ly && mouseY < ly + lh) {
                int rel = (int) (mouseY - ly + scroll);
                int idx = rel / ROW_HEIGHT;
                if (idx >= 0 && idx < filtered.size()) {
                    RaysItem item = filtered.get(idx);
                    if (item.hasVideo()) {
                        try {
                            Util.getOperatingSystem().open(URI.create(item.youtubeUrl));
                        } catch (Exception ignored) {
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= vertical * ROW_HEIGHT;
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
