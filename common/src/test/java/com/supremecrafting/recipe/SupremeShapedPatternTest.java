package com.supremecrafting.recipe;

import com.supremecrafting.table.SupremeTableInventory;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SupremeShapedPatternTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SupremeShapedPattern pattern(Map<Character, Ingredient> key, String... rows) {
        return SupremeShapedPattern.MAP_CODEC.codec().parse(
                com.mojang.serialization.JsonOps.INSTANCE,
                buildJson(key, rows)
        ).getOrThrow();
    }

    private static com.google.gson.JsonObject buildJson(Map<Character, Ingredient> key, String... rows) {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        com.google.gson.JsonObject keyJson = new com.google.gson.JsonObject();
        for (var e : key.entrySet()) {
            com.google.gson.JsonElement encoded = Ingredient.CODEC_NONEMPTY
                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, e.getValue())
                    .getOrThrow();
            keyJson.add(String.valueOf(e.getKey()), encoded);
        }
        obj.add("key", keyJson);
        com.google.gson.JsonArray patternJson = new com.google.gson.JsonArray();
        for (String r : rows) patternJson.add(r);
        obj.add("pattern", patternJson);
        return obj;
    }

    private static CraftingInput inputFromGrid(String[] rows) {
        int height = rows.length;
        int width = rows[0].length();
        NonNullList<ItemStack> stacks = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                char ch = rows[r].charAt(c);
                stacks.set(c + r * width, switch (ch) {
                    case '.' -> ItemStack.EMPTY;
                    case 'C' -> new ItemStack(Items.COBBLESTONE);
                    case 'G' -> new ItemStack(Items.GOLD_INGOT);
                    case 'D' -> new ItemStack(Items.DIAMOND);
                    default -> throw new IllegalArgumentException("unknown char " + ch);
                });
            }
        }
        return CraftingInput.of(width, height, stacks);
    }

    @Test
    void emptyPatternRejected() {
        assertThrows(Exception.class, () -> pattern(Map.of()));
    }

    @Test
    void simple3x3MatchesExact() {
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE), 'G', Ingredient.of(Items.GOLD_INGOT)),
                "CCC",
                "CGC",
                "CCC"
        );
        CraftingInput input = inputFromGrid(new String[]{"CCC", "CGC", "CCC"});
        assertTrue(p.matches(input));
    }

    @Test
    void wrongCenterDoesNotMatch() {
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE), 'G', Ingredient.of(Items.GOLD_INGOT)),
                "CCC",
                "CGC",
                "CCC"
        );
        CraftingInput input = inputFromGrid(new String[]{"CCC", "CDC", "CCC"});
        assertFalse(p.matches(input));
    }

    @Test
    void differentSizeDoesNotMatch() {
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE)),
                "CCC",
                "CCC",
                "CCC"
        );
        CraftingInput input = inputFromGrid(new String[]{"CC", "CC"});
        assertFalse(p.matches(input));
    }

    @Test
    void nineByNineFrameMatches() {
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE), 'G', Ingredient.of(Items.GOLD_INGOT)),
                "CCCCCCCCC",
                "C.......C",
                "C.......C",
                "C.......C",
                "C...G...C",
                "C.......C",
                "C.......C",
                "C.......C",
                "CCCCCCCCC"
        );
        CraftingInput input = inputFromGrid(new String[]{
                "CCCCCCCCC",
                "C.......C",
                "C.......C",
                "C.......C",
                "C...G...C",
                "C.......C",
                "C.......C",
                "C.......C",
                "CCCCCCCCC"
        });
        assertEquals(9, p.width());
        assertEquals(9, p.height());
        assertTrue(p.matches(input));
    }

    @Test
    void emptyInteriorIsRequired() {
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE), 'G', Ingredient.of(Items.GOLD_INGOT)),
                "CCCCCCCCC",
                "C.......C",
                "C.......C",
                "C.......C",
                "C...G...C",
                "C.......C",
                "C.......C",
                "C.......C",
                "CCCCCCCCC"
        );
        // Stray diamond inside the frame — bounding box still 9x9 but interior cell mismatches.
        CraftingInput input = inputFromGrid(new String[]{
                "CCCCCCCCC",
                "C.......C",
                "C.......C",
                "C.......C",
                "C...G...C",
                "C..D....C",
                "C.......C",
                "C.......C",
                "CCCCCCCCC"
        });
        assertFalse(p.matches(input));
    }

    @Test
    void shrinkStripsEmptyPerimeter() {
        // Pattern padded with empty rows/cols — should shrink to the actual content.
        SupremeShapedPattern p = pattern(
                Map.of('C', Ingredient.of(Items.COBBLESTONE)),
                "     ",
                " CCC ",
                " C C ",
                " CCC ",
                "     "
        );
        assertEquals(3, p.width());
        assertEquals(3, p.height());
    }

    @Test
    void buildInputCropsTo3x3WhenPlacedSmall() {
        SupremeTableInventory inv = new SupremeTableInventory();
        // Place a 3x3 of cobblestone at offset (5,5) in the 81x81 grid.
        for (int dr = 0; dr < 3; dr++) {
            for (int dc = 0; dc < 3; dc++) {
                inv.set(5 + dc, 5 + dr, new ItemStack(Items.COBBLESTONE));
            }
        }
        CraftingInput input = SupremeCraftingMatcher.buildInput(inv);
        assertEquals(3, input.width());
        assertEquals(3, input.height());
        assertEquals(9, input.ingredientCount());
    }
}
