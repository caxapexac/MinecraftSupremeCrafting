package com.supremecrafting.client;

import com.supremecrafting.table.SupremeTableBlockEntity;
import com.supremecrafting.table.SupremeTableInventory;
import com.supremecrafting.table.SupremeTableMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import yalter.mousetweaks.api.MouseTweaksDisableWheelTweak;

/**
 * Google-Maps-style canvas over the full 81×81 grid. Every grid cell is a real
 * vanilla {@link Slot} (full 6561 of them); we just position them per-frame
 * and park invisible ones off-canvas. MMB-drag pans, mouse wheel zooms
 * (centered on cursor). All visible cells are clickable.
 *
 * <p>{@link MouseTweaksDisableWheelTweak} disables MouseTweaks's wheel-scroll
 * tweak when this screen is open — the wheel is reserved for our zoom UX.
 * RMB-drag and other MouseTweaks features still work in the player-inventory
 * portion. The annotation is class-retention RUNTIME and read by MouseTweaks
 * via reflection; if MouseTweaks isn't installed, the JVM silently ignores
 * the missing annotation class — no NoClassDefFoundError on screen open.
 */
@MouseTweaksDisableWheelTweak
@Environment(EnvType.CLIENT)
public class SupremeTableScreen extends AbstractContainerScreen<SupremeTableMenu> {
    private static final int PANEL_BG = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int CANVAS_BG = 0xFF373737;
    private static final int GRID_BORDER = 0xFFA0A0A0;
    private static final int GRID_BORDER_PX = 2;
    /** 1px recessed bevel around the canvas — dark on top/left, light on bottom/right. */
    private static final int CANVAS_BEVEL_DARK = 0xFF373737;
    private static final int CANVAS_BEVEL_LIGHT = 0xFFFFFFFF;

    private static final int ARROW_SIZE = 16;
    private static final int ARROW_RIGHT_INSET = 6;
    private static final int ARROW_TOP_INSET = 4;
    private static final int ARROW_BG = 0xFF8B8B8B;
    private static final int ARROW_BG_HOVER = 0xFFFFD040;
    private static final int ARROW_FG = 0xFFFFFFFF;

    /** Vanilla 18×18 recessed slot sprite — same as inventory / chest slots. */
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.withDefaultNamespace("container/slot");

    private static final int CANVAS_PAD = 4;
    private static final int TITLE_HEIGHT = 17;
    private static final int CANVAS_HEIGHT = 220;
    private static final int PLAYER_INV_GAP = 14;
    private static final int HOTBAR_GAP = 4;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;

    private static final double DEFAULT_CELL = 18.0;
    // Min cellSize chosen so the whole 81x81 fits in the canvas vertically
    // (canvas height ÷ 81 ≈ 2.7); 2.0 leaves a touch of headroom.
    private static final double MIN_CELL = 2.0;
    private static final double MAX_CELL = 36.0;
    private static final double ZOOM_STEP = 1.15;
    private static final double EDGE_PAD = 60.0;

    /** Sentinel screen position used to park slots that aren't currently visible. */
    private static final int OFFSCREEN = -9999;

    private double panOffsetX;
    private double panOffsetY;
    private double cellSize = DEFAULT_CELL;

    private boolean panning;
    private double dragLastX;
    private double dragLastY;

    public SupremeTableScreen(SupremeTableMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 356;
        int playerInvHeight = PLAYER_INV_ROWS * SupremeTableMenu.SLOT_PX
                + HOTBAR_GAP + SupremeTableMenu.SLOT_PX;
        this.imageHeight = TITLE_HEIGHT + CANVAS_HEIGHT + PLAYER_INV_GAP + playerInvHeight + 6;
        this.inventoryLabelY = TITLE_HEIGHT + CANVAS_HEIGHT + PLAYER_INV_GAP - 11;
        this.inventoryLabelX = (imageWidth - PLAYER_INV_COLS * SupremeTableMenu.SLOT_PX) / 2;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        panOffsetX = CanvasMath.clampPan(
                canvasWidth() / 2.0 - SupremeTableInventory.WIDTH * cellSize / 2.0,
                canvasWidth(), cellSize, SupremeTableInventory.WIDTH, EDGE_PAD);
        panOffsetY = CanvasMath.clampPan(
                canvasHeight() / 2.0 - SupremeTableInventory.HEIGHT * cellSize / 2.0,
                canvasHeight(), cellSize, SupremeTableInventory.HEIGHT, EDGE_PAD);
    }

    private static final int SIDEBAR_WIDTH = 36;
    private static final int SIDEBAR_LEFT_REL = 320; // canvas (4..316) + 4px gap

    private int canvasLeftRel() { return CANVAS_PAD; }
    private int canvasTopRel() { return TITLE_HEIGHT; }
    private int canvasWidth() { return imageWidth - 2 * CANVAS_PAD - SIDEBAR_WIDTH; }
    private int canvasHeight() { return CANVAS_HEIGHT; }

    private int arrowXRel() { return imageWidth - ARROW_SIZE - ARROW_RIGHT_INSET; }
    private int arrowYRel() { return ARROW_TOP_INSET; }

    private boolean isMouseOverArrow(double mouseX, double mouseY) {
        int ax = leftPos + arrowXRel();
        int ay = topPos + arrowYRel();
        return mouseX >= ax && mouseX < ax + ARROW_SIZE
                && mouseY >= ay && mouseY < ay + ARROW_SIZE;
    }

    /** Canvas-relative (Slot.x-space) screen X of grid line {@code gx}. */
    private int gridLineX(int gx) {
        return canvasLeftRel() + (int) Math.round(panOffsetX + gx * cellSize);
    }

    private int gridLineY(int gy) {
        return canvasTopRel() + (int) Math.round(panOffsetY + gy * cellSize);
    }

    private int cellWidth(int gx) { return gridLineX(gx + 1) - gridLineX(gx); }
    private int cellHeight(int gy) { return gridLineY(gy + 1) - gridLineY(gy); }

    private SupremeTableBlockEntity tableBE() {
        if (Minecraft.getInstance().level == null) return null;
        if (Minecraft.getInstance().level.getBlockEntity(menu.tablePos()) instanceof SupremeTableBlockEntity be) {
            return be;
        }
        return null;
    }

    private void updateSlotPositions() {
        // Compute visible cell range so we only spend cycles on cells in canvas.
        int firstX = Math.max(0, (int) Math.floor(-panOffsetX / cellSize));
        int lastX = Math.min(SupremeTableInventory.WIDTH - 1,
                (int) Math.ceil((canvasWidth() - panOffsetX) / cellSize));
        int firstY = Math.max(0, (int) Math.floor(-panOffsetY / cellSize));
        int lastY = Math.min(SupremeTableInventory.HEIGHT - 1,
                (int) Math.ceil((canvasHeight() - panOffsetY) / cellSize));

        // Park every grid slot off-canvas; visible ones get real positions below.
        for (int i = 0; i < SupremeTableInventory.SIZE; i++) {
            Slot s = menu.slots.get(i);
            s.x = OFFSCREEN;
            s.y = OFFSCREEN;
        }
        for (int gy = firstY; gy <= lastY; gy++) {
            int sy = gridLineY(gy);
            for (int gx = firstX; gx <= lastX; gx++) {
                int idx = SupremeTableInventory.indexOf(gx, gy);
                Slot s = menu.slots.get(idx);
                s.x = gridLineX(gx);
                s.y = sy;
            }
        }

        // Player inventory + hotbar — anchored at bottom, centered horizontally.
        int invLeft = (imageWidth - PLAYER_INV_COLS * SupremeTableMenu.SLOT_PX) / 2;
        int invTop = TITLE_HEIGHT + CANVAS_HEIGHT + PLAYER_INV_GAP;
        int hotbarTop = invTop + PLAYER_INV_ROWS * SupremeTableMenu.SLOT_PX + HOTBAR_GAP;
        int slotIndex = SupremeTableInventory.SIZE;
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                Slot s = menu.slots.get(slotIndex++);
                s.x = invLeft + col * SupremeTableMenu.SLOT_PX;
                s.y = invTop + row * SupremeTableMenu.SLOT_PX;
            }
        }
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            Slot s = menu.slots.get(slotIndex++);
            s.x = invLeft + col * SupremeTableMenu.SLOT_PX;
            s.y = hotbarTop;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateSlotPositions();
        super.render(g, mouseX, mouseY, partialTick);
        drawHoverHighlight(g);
        renderTooltip(g, mouseX, mouseY);
        if (isMouseOverArrow(mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("supreme_crafting.tooltip.show_recipes"),
                    mouseX, mouseY);
        }
    }

    private void drawHoverHighlight(GuiGraphics g) {
        if (this.hoveredSlot == null || this.hoveredSlot.index >= SupremeTableInventory.SIZE) return;
        int gx = SupremeTableInventory.xOf(this.hoveredSlot.index);
        int gy = SupremeTableInventory.yOf(this.hoveredSlot.index);
        int width = cellWidth(gx);
        int height = cellHeight(gy);
        int x = leftPos + this.hoveredSlot.x;
        int y = topPos + this.hoveredSlot.y;
        int cl = leftPos + canvasLeftRel();
        int ct = topPos + canvasTopRel();
        g.enableScissor(cl, ct, cl + canvasWidth(), ct + canvasHeight());
        g.fillGradient(x, y, x + width, y + height, 0x80FFFFFF, 0x80FFFFFF);
        g.disableScissor();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL_BG);
        g.fill(x, y, x + imageWidth, y + 1, PANEL_LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, PANEL_LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_DARK);

        int cl = x + canvasLeftRel();
        int ct = y + canvasTopRel();
        int cr = cl + canvasWidth();
        int cb = ct + canvasHeight();
        g.fill(cl, ct, cr, cb, CANVAS_BG);
        // 1px recessed bevel: dark top+left, light bottom+right.
        g.fill(cl - 1, ct - 1, cr + 1, ct, CANVAS_BEVEL_DARK);
        g.fill(cl - 1, ct, cl, cb, CANVAS_BEVEL_DARK);
        g.fill(cl - 1, cb, cr + 1, cb + 1, CANVAS_BEVEL_LIGHT);
        g.fill(cr, ct, cr + 1, cb, CANVAS_BEVEL_LIGHT);

        // Recipe-viewer arrow in top-right corner. Hover changes background;
        // click opens whichever recipe viewer is loaded (RecipeViewerHooks).
        int ax = x + arrowXRel();
        int ay = y + arrowYRel();
        boolean hover = isMouseOverArrow(mouseX, mouseY);
        g.fill(ax, ay, ax + ARROW_SIZE, ay + ARROW_SIZE, hover ? ARROW_BG_HOVER : ARROW_BG);
        g.fill(ax, ay, ax + ARROW_SIZE, ay + 1, PANEL_LIGHT);
        g.fill(ax, ay, ax + 1, ay + ARROW_SIZE, PANEL_LIGHT);
        g.fill(ax, ay + ARROW_SIZE - 1, ax + ARROW_SIZE, ay + ARROW_SIZE, PANEL_DARK);
        g.fill(ax + ARROW_SIZE - 1, ay, ax + ARROW_SIZE, ay + ARROW_SIZE, PANEL_DARK);
        String label = "?";
        int tw = font.width(label);
        g.drawString(font, label, ax + (ARROW_SIZE - tw) / 2 + 1, ay + 4, ARROW_FG, false);

        int invLeft = x + (imageWidth - PLAYER_INV_COLS * SupremeTableMenu.SLOT_PX) / 2;
        int invTop = y + TITLE_HEIGHT + CANVAS_HEIGHT + PLAYER_INV_GAP;
        drawSlotSprites(g, invLeft, invTop, PLAYER_INV_COLS, PLAYER_INV_ROWS, SupremeTableMenu.SLOT_PX);
        drawSlotSprites(g, invLeft, invTop + PLAYER_INV_ROWS * SupremeTableMenu.SLOT_PX + HOTBAR_GAP,
                PLAYER_INV_COLS, 1, SupremeTableMenu.SLOT_PX);

        // Result slot in sidebar.
        g.blitSprite(SLOT_SPRITE,
                x + SupremeTableMenu.RESULT_SLOT_X,
                y + SupremeTableMenu.RESULT_SLOT_Y,
                SupremeTableMenu.SLOT_PX, SupremeTableMenu.SLOT_PX);

        renderGridChrome(g, cl, ct, cr, cb);
        // Item rendering is handled by vanilla's slot loop calling our renderSlot
        // override; no separate full-grid pass needed.
    }

    private void renderGridChrome(GuiGraphics g, int cl, int ct, int cr, int cb) {
        g.enableScissor(cl, ct, cr, cb);

        int gridLeft = leftPos + gridLineX(0);
        int gridTop = topPos + gridLineY(0);
        int gridRight = leftPos + gridLineX(SupremeTableInventory.WIDTH);
        int gridBottom = topPos + gridLineY(SupremeTableInventory.HEIGHT);

        // Outer table-edge border so the player sees where the grid ends.
        int b = GRID_BORDER_PX;
        g.fill(gridLeft - b, gridTop - b, gridRight + b, gridTop, GRID_BORDER);
        g.fill(gridLeft - b, gridBottom, gridRight + b, gridBottom + b, GRID_BORDER);
        g.fill(gridLeft - b, gridTop, gridLeft, gridBottom, GRID_BORDER);
        g.fill(gridRight, gridTop, gridRight + b, gridBottom, GRID_BORDER);

        // Vanilla slot sprite for every visible cell — provides the recessed
        // look the player expects from inventory GUIs. blitSprite scales the
        // 18px sprite to whatever cellSize we're at.
        int firstX = Math.max(0, (int) Math.floor(-panOffsetX / cellSize));
        int lastX = Math.min(SupremeTableInventory.WIDTH - 1,
                (int) Math.ceil((canvasWidth() - panOffsetX) / cellSize));
        int firstY = Math.max(0, (int) Math.floor(-panOffsetY / cellSize));
        int lastY = Math.min(SupremeTableInventory.HEIGHT - 1,
                (int) Math.ceil((canvasHeight() - panOffsetY) / cellSize));
        for (int gy = firstY; gy <= lastY; gy++) {
            int sy = topPos + gridLineY(gy);
            int height = cellHeight(gy);
            for (int gx = firstX; gx <= lastX; gx++) {
                int sx = leftPos + gridLineX(gx);
                int width = cellWidth(gx);
                g.blitSprite(SLOT_SPRITE, sx, sy, width, height);
            }
        }

        g.disableScissor();
    }

    private static void drawSlotSprites(GuiGraphics g, int x, int y, int cols, int rows, int slotPx) {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                g.blitSprite(SLOT_SPRITE, x + c * slotPx, y + r * slotPx, slotPx, slotPx);
            }
        }
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics g, @NotNull Slot slot) {
        if (slot.index >= SupremeTableInventory.SIZE) {
            super.renderSlot(g, slot);
            return;
        }
        if (slot.x == OFFSCREEN) return;
        int gx = SupremeTableInventory.xOf(slot.index);
        int gy = SupremeTableInventory.yOf(slot.index);
        float sx = cellWidth(gx) / 16f;
        float sy = cellHeight(gy) / 16f;
        // Wrap super.renderSlot in a pose scaled around (slot.x, slot.y) so
        // vanilla's drag-place preview / split-stack visuals all render at the
        // correct cell size. Anchor math: T(slot.x*(1-sx)) * S(sx) maps point
        // (slot.x, slot.y) onto itself, scaling content around that point.
        int cl = leftPos + canvasLeftRel();
        int ct = topPos + canvasTopRel();
        g.enableScissor(cl, ct, cl + canvasWidth(), ct + canvasHeight());
        g.pose().pushPose();
        g.pose().translate(slot.x * (1.0f - sx), slot.y * (1.0f - sy), 0);
        g.pose().scale(sx, sy, 1);
        super.renderSlot(g, slot);
        g.pose().popPose();
        g.disableScissor();
    }

    @Override
    public boolean isHovering(@NotNull Slot slot, double mouseX, double mouseY) {
        if (slot.index >= SupremeTableInventory.SIZE) {
            return super.isHovering(slot, mouseX, mouseY);
        }
        if (slot.x == OFFSCREEN) return false;
        // Reject any cursor outside the canvas rect. Without this, slots whose
        // hit-box clips past the canvas edge (visually clipped, but logically
        // still hot) "win" over player inventory slots that visually overlap them.
        if (!inCanvas(mouseX, mouseY)) return false;
        int gx = SupremeTableInventory.xOf(slot.index);
        int gy = SupremeTableInventory.yOf(slot.index);
        int width = Math.max(1, cellWidth(gx));
        int height = Math.max(1, cellHeight(gy));
        return isHovering(slot.x, slot.y, width, height, mouseX, mouseY);
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        int cl = leftPos + canvasLeftRel();
        int ct = topPos + canvasTopRel();
        return mouseX >= cl && mouseX < cl + canvasWidth()
                && mouseY >= ct && mouseY < ct + canvasHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isMouseOverArrow(mouseX, mouseY)) {
            RecipeViewerHooks.invokeFirst();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && inCanvas(mouseX, mouseY)) {
            panning = true;
            dragLastX = mouseX;
            dragLastY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panning && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            panning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (panning && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            panOffsetX = CanvasMath.clampPan(
                    panOffsetX + (mouseX - dragLastX),
                    canvasWidth(), cellSize, SupremeTableInventory.WIDTH, EDGE_PAD);
            panOffsetY = CanvasMath.clampPan(
                    panOffsetY + (mouseY - dragLastY),
                    canvasHeight(), cellSize, SupremeTableInventory.HEIGHT, EDGE_PAD);
            dragLastX = mouseX;
            dragLastY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inCanvas(mouseX, mouseY)) {
            double oldCell = cellSize;
            double newCell = CanvasMath.clamp(
                    cellSize * Math.pow(ZOOM_STEP, scrollY),
                    MIN_CELL, MAX_CELL);
            if (newCell != oldCell) {
                int cl = leftPos + canvasLeftRel();
                int ct = topPos + canvasTopRel();
                panOffsetX = CanvasMath.zoomCenteredPan(panOffsetX, oldCell, newCell, mouseX - cl);
                panOffsetY = CanvasMath.zoomCenteredPan(panOffsetY, oldCell, newCell, mouseY - ct);
                cellSize = newCell;
                panOffsetX = CanvasMath.clampPan(panOffsetX, canvasWidth(), cellSize, SupremeTableInventory.WIDTH, EDGE_PAD);
                panOffsetY = CanvasMath.clampPan(panOffsetY, canvasHeight(), cellSize, SupremeTableInventory.HEIGHT, EDGE_PAD);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
