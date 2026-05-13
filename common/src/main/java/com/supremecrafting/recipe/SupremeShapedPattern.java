package com.supremecrafting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.supremecrafting.table.SupremeTableInventory;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Like vanilla {@link net.minecraft.world.item.crafting.ShapedRecipePattern} but
 * sized for the 81×81 Supreme Table. {@code MAX_SIZE = 81}; vanilla's class
 * is {@code final} with hardcoded {@code MAX_SIZE = 3}, so we can't just
 * extend it.
 *
 * <p>Strict-size matching: {@link #matches} returns true only when the input's
 * cropped bounding box dimensions equal {@link #width()} × {@link #height()}.
 * That means a 3×3 vanilla recipe placed inside an unrelated 81×81 layout
 * won't accidentally match — the layout's bbox would be larger than 3×3.
 *
 * <p>{@code ' '} in the pattern means "must be empty". Empty leading/trailing
 * rows and columns are stripped so authors don't have to align manually.
 */
public final class SupremeShapedPattern {
    public static final int MAX_SIZE = SupremeTableInventory.WIDTH;

    public static final MapCodec<SupremeShapedPattern> MAP_CODEC = Data.MAP_CODEC.flatXmap(
            SupremeShapedPattern::unpack,
            p -> p.data
                    .<DataResult<Data>>map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked Supreme shaped pattern")));

    public static final StreamCodec<RegistryFriendlyByteBuf, SupremeShapedPattern> STREAM_CODEC =
            StreamCodec.ofMember(SupremeShapedPattern::toNetwork, SupremeShapedPattern::fromNetwork);

    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final Optional<Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public SupremeShapedPattern(int width, int height, NonNullList<Ingredient> ingredients, Optional<Data> data) {
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.data = data;
        int count = 0;
        for (Ingredient i : ingredients) {
            if (!i.isEmpty()) count++;
        }
        this.ingredientCount = count;
        this.symmetrical = Util.isSymmetrical(width, height, ingredients);
    }

    public int width() { return width; }
    public int height() { return height; }
    public NonNullList<Ingredient> ingredients() { return ingredients; }

    public boolean matches(CraftingInput input) {
        if (input.ingredientCount() != ingredientCount) return false;
        if (input.width() != width || input.height() != height) return false;
        if (!symmetrical && matchOriented(input, true)) return true;
        return matchOriented(input, false);
    }

    private boolean matchOriented(CraftingInput input, boolean mirrored) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Ingredient ingredient = mirrored
                        ? ingredients.get((width - col - 1) + row * width)
                        : ingredients.get(col + row * width);
                if (!ingredient.test(input.getItem(col, row))) return false;
            }
        }
        return true;
    }

    private static DataResult<SupremeShapedPattern> unpack(Data data) {
        String[] rows = shrink(data.pattern);
        if (rows.length == 0) {
            return DataResult.error(() -> "Pattern is empty after trim");
        }
        int w = rows[0].length();
        int h = rows.length;
        if (w > MAX_SIZE || h > MAX_SIZE) {
            return DataResult.error(() -> "Pattern exceeds " + MAX_SIZE + "x" + MAX_SIZE);
        }
        NonNullList<Ingredient> ingredients = NonNullList.withSize(w * h, Ingredient.EMPTY);
        CharSet unused = new CharArraySet(data.key.keySet());

        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                Ingredient ing = (ch == ' ' || ch == '.') ? Ingredient.EMPTY : data.key.get(ch);
                if (ing == null) {
                    return DataResult.error(() -> "Pattern references symbol '" + ch + "' but it's not in the key");
                }
                unused.remove(ch);
                ingredients.set(c + r * w, ing);
            }
        }
        if (!unused.isEmpty()) {
            return DataResult.error(() -> "Key defines symbols not used in pattern: " + unused);
        }
        return DataResult.success(new SupremeShapedPattern(w, h, ingredients, Optional.of(data)));
    }

    /** Strip leading/trailing all-space rows and columns. Same algorithm as vanilla. */
    static String[] shrink(List<String> list) {
        int firstCol = Integer.MAX_VALUE;
        int lastCol = 0;
        int leadingEmpty = 0;
        int trailingEmpty = 0;

        for (int idx = 0; idx < list.size(); idx++) {
            String row = list.get(idx);
            firstCol = Math.min(firstCol, firstNonSpace(row));
            int last = lastNonSpace(row);
            lastCol = Math.max(lastCol, last);
            if (last < 0) {
                if (leadingEmpty == idx) leadingEmpty++;
                trailingEmpty++;
            } else {
                trailingEmpty = 0;
            }
        }

        if (list.size() == trailingEmpty) return new String[0];
        String[] out = new String[list.size() - trailingEmpty - leadingEmpty];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i + leadingEmpty).substring(firstCol, lastCol + 1);
        }
        return out;
    }

    private static int firstNonSpace(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static int lastNonSpace(String s) {
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == ' ') i--;
        return i;
    }

    private void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        for (Ingredient ing : ingredients) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
        }
    }

    private static SupremeShapedPattern fromNetwork(RegistryFriendlyByteBuf buf) {
        int w = buf.readVarInt();
        int h = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(w * h, Ingredient.EMPTY);
        ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
        return new SupremeShapedPattern(w, h, ingredients, Optional.empty());
    }

    public record Data(Map<Character, Ingredient> key, List<String> pattern) {
        private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(list -> {
            if (list.isEmpty()) return DataResult.error(() -> "Empty pattern");
            if (list.size() > MAX_SIZE) {
                return DataResult.error(() -> "Pattern has too many rows (max " + MAX_SIZE + ")");
            }
            int w = list.get(0).length();
            for (String s : list) {
                if (s.length() > MAX_SIZE) {
                    return DataResult.error(() -> "Pattern has too many columns (max " + MAX_SIZE + ")");
                }
                if (s.length() != w) {
                    return DataResult.error(() -> "Pattern rows must be the same width");
                }
            }
            return DataResult.success(list);
        }, Function.identity());

        private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(s -> {
            if (s.length() != 1) return DataResult.error(() -> "Key symbol must be 1 character: '" + s + "'");
            if (" ".equals(s) || ".".equals(s)) {
                return DataResult.error(() -> "Key symbol '" + s + "' is reserved (used for empty cells)");
            }
            return DataResult.success(s.charAt(0));
        }, String::valueOf);

        public static final MapCodec<Data> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(d -> d.key),
                PATTERN_CODEC.fieldOf("pattern").forGetter(d -> d.pattern)
        ).apply(instance, Data::new));
    }
}
