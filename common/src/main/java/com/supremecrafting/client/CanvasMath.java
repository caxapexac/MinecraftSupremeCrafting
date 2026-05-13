package com.supremecrafting.client;

/**
 * Pure-logic helpers for the Supreme Table screen's pan/zoom transforms.
 * Has no Minecraft / OpenGL deps so it's covered by unit tests in {@code
 * CanvasMathTest}.
 */
public final class CanvasMath {
    private CanvasMath() {}

    /** Clamp a value to {@code [min, max]}. */
    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Clamp a pan offset so the grid stays roughly inside the canvas. Allows
     * {@code edgePad} pixels of overshoot in either direction so the user can
     * see canvas-background (i.e. "the world ends here") past the grid edges.
     *
     * @param panOffset      current pan offset (pixels). 0 = grid (0,0) at canvas left.
     * @param canvasSize     canvas width or height in pixels.
     * @param cellSize       size of one grid cell in pixels.
     * @param gridSizeCells  number of cells along this axis.
     * @param edgePad        pixels of canvas-background visible past each grid edge.
     * @return clamped pan offset.
     */
    public static double clampPan(double panOffset, double canvasSize, double cellSize, int gridSizeCells, double edgePad) {
        double gridPx = gridSizeCells * cellSize;
        if (gridPx <= canvasSize) {
            return (canvasSize - gridPx) / 2.0;
        }
        double minPan = canvasSize - gridPx - edgePad; // grid right edge can sit edgePad px inside canvas right
        double maxPan = edgePad;                       // grid left edge can sit edgePad px right of canvas left
        return clamp(panOffset, minPan, maxPan);
    }

    /** Backwards-compatible overload (no edge padding). */
    public static double clampPan(double panOffset, double canvasSize, double cellSize, int gridSizeCells) {
        return clampPan(panOffset, canvasSize, cellSize, gridSizeCells, 0.0);
    }

    /**
     * Snap the active region's origin so the {@code viewSizeCells}-wide window
     * is centered on the canvas. Clamped to {@code [0, gridSizeCells - viewSizeCells]}.
     */
    public static int activeOriginFromCanvasCenter(double canvasSize, double panOffset, double cellSize,
                                                   int gridSizeCells, int viewSizeCells) {
        double canvasCenter = canvasSize / 2.0;
        double gridAtCenter = (canvasCenter - panOffset) / cellSize;
        int centerCell = (int) Math.round(gridAtCenter);
        int origin = centerCell - viewSizeCells / 2;
        return clamp(origin, 0, gridSizeCells - viewSizeCells);
    }

    /**
     * Compute the pan offset that keeps the grid coordinate under the cursor
     * fixed when {@code cellSize} changes from {@code oldCellSize} to {@code
     * newCellSize}.
     *
     * @param oldPan       previous pan offset.
     * @param oldCellSize  previous cell size.
     * @param newCellSize  new cell size.
     * @param cursorRelToCanvas  cursor position relative to canvas top-left.
     */
    public static double zoomCenteredPan(double oldPan, double oldCellSize, double newCellSize,
                                         double cursorRelToCanvas) {
        double gridUnderCursor = (cursorRelToCanvas - oldPan) / oldCellSize;
        return cursorRelToCanvas - gridUnderCursor * newCellSize;
    }
}
