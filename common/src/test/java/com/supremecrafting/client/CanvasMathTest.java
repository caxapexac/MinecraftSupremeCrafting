package com.supremecrafting.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanvasMathTest {

    @Test
    void clampPan_centersGridSmallerThanCanvas() {
        // 50 cells × 4 px = 200 px grid; canvas 300 px → centered.
        double pan = CanvasMath.clampPan(0, 300, 4, 50);
        assertEquals((300 - 200) / 2.0, pan, 1e-9);
        // Even if user pushes the offset, clamp returns to centered.
        assertEquals((300 - 200) / 2.0, CanvasMath.clampPan(999, 300, 4, 50), 1e-9);
    }

    @Test
    void clampPan_pinsLargerGridToCanvasEdges() {
        // 81 cells × 18 px = 1458 px grid; canvas 200 px.
        // Allowed range: [200 - 1458, 0] = [-1258, 0].
        assertEquals(0, CanvasMath.clampPan(50, 200, 18, 81), 1e-9);
        assertEquals(-1258, CanvasMath.clampPan(-9999, 200, 18, 81), 1e-9);
        assertEquals(-100, CanvasMath.clampPan(-100, 200, 18, 81), 1e-9);
    }

    @Test
    void activeOrigin_centersWindowOnCanvasCenter() {
        // pan=0, cellSize=18, canvas 300; canvas center = 150 → grid coord 150/18 ≈ 8.33 → round 8.
        // origin = 8 - 9/2 = 8 - 4 = 4. Clamp to [0, 81-9=72] → 4.
        int origin = CanvasMath.activeOriginFromCanvasCenter(300, 0, 18, 81, 9);
        assertEquals(4, origin);
    }

    @Test
    void activeOrigin_clampsAtEdges() {
        // Pan such that canvas center is at grid coord 0 → origin would be -4 → clamp 0.
        int oLeft = CanvasMath.activeOriginFromCanvasCenter(300, 150, 18, 81, 9);
        assertEquals(0, oLeft);
        // Pan such that canvas center is at grid coord 81 → origin 81-4=77 → clamp 72.
        double panForRight = 150 - 81 * 18;
        int oRight = CanvasMath.activeOriginFromCanvasCenter(300, panForRight, 18, 81, 9);
        assertEquals(72, oRight);
    }

    @Test
    void zoomCenteredPan_keepsGridCoordUnderCursorFixed() {
        // Cursor at relX=120, oldPan=-50, oldCell=10 → grid = (120 - (-50))/10 = 17.
        double oldPan = -50;
        double oldCell = 10;
        double cursor = 120;
        double newCell = 20;
        double newPan = CanvasMath.zoomCenteredPan(oldPan, oldCell, newCell, cursor);
        // Grid coord under cursor after zoom: (cursor - newPan) / newCell.
        double gridAfter = (cursor - newPan) / newCell;
        double gridBefore = (cursor - oldPan) / oldCell;
        assertEquals(gridBefore, gridAfter, 1e-9);
    }

    @Test
    void zoomCenteredPan_invariantUnderRoundTrip() {
        double pan = -300;
        double cell = 18;
        double cursor = 200;
        double zoomedIn = CanvasMath.zoomCenteredPan(pan, cell, 24, cursor);
        double zoomedOut = CanvasMath.zoomCenteredPan(zoomedIn, 24, 18, cursor);
        assertEquals(pan, zoomedOut, 1e-9);
    }
}
