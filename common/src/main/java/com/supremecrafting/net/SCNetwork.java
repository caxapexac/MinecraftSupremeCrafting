package com.supremecrafting.net;

import com.mojang.logging.LogUtils;
import com.supremecrafting.furnace.CasingCascade;
import com.supremecrafting.furnace.FurnaceTick;
import com.supremecrafting.furnace.MultiblockRegions;
import com.supremecrafting.furnace.Region;
import com.supremecrafting.recipe.integration.SupremeRecipeTransfer;
import com.supremecrafting.table.SupremeTableBlockEntity;
import com.supremecrafting.table.SupremeTableMenu;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Network plumbing for the mod: JEI recipe transfer (C2S) and multiblock
 * region sync (S2C). EMI uses its own fill protocol so doesn't go through here.
 */
public final class SCNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SCNetwork() {}

    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                C2STransferRecipePacket.TYPE,
                C2STransferRecipePacket.STREAM_CODEC,
                SCNetwork::handleTransferRecipe);

        // S2C receiver only meaningful on a client; the registration is safe
        // server-side because Architectury filters by Side, but the handler
        // class itself is client-only — so route through EnvExecutor to avoid
        // loading ClientMultiblockRegions on a dedicated server.
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                S2CMultiblockSyncPacket.TYPE,
                S2CMultiblockSyncPacket.STREAM_CODEC,
                (msg, ctx) -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientNetHandlers.handleSync(msg, ctx)));

        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                S2CMultiblockLitPacket.TYPE,
                S2CMultiblockLitPacket.STREAM_CODEC,
                (msg, ctx) -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientNetHandlers.handleLit(msg, ctx)));

        PlayerEvent.PLAYER_JOIN.register(SCNetwork::onPlayerJoin);
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> sendResetTo(player));
        TickEvent.SERVER_LEVEL_POST.register(FurnaceTick::tickAll);
        TickEvent.SERVER_LEVEL_POST.register(CasingCascade::tick);
    }

    private static void onPlayerJoin(ServerPlayer player) {
        sendResetTo(player);
    }

    private static void sendResetTo(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        List<Region> regions = new ArrayList<>(MultiblockRegions.get(sl).all());
        S2CMultiblockSyncPacket packet = S2CMultiblockSyncPacket.reset(regions);
        NetworkManager.sendToPlayer(player, packet);
        LOGGER.debug("multiblock_sync reset → {} ({} regions)", player.getGameProfile().getName(), regions.size());
    }

    private static void handleTransferRecipe(C2STransferRecipePacket msg, NetworkManager.PacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return;
        ctx.queue(() -> {
            if (!(sp.containerMenu instanceof SupremeTableMenu menu)) {
                LOGGER.debug("transfer_recipe: player's open menu is not SupremeTableMenu");
                return;
            }
            if (!menu.tablePos().equals(msg.tablePos())) {
                LOGGER.debug("transfer_recipe: pos mismatch");
                return;
            }
            var holderOpt = sp.level().getRecipeManager().byKey(msg.recipeId());
            if (holderOpt.isEmpty()) {
                LOGGER.debug("transfer_recipe: unknown recipe {}", msg.recipeId());
                return;
            }
            RecipeHolder<?> holder = holderOpt.get();
            if (!(holder.value() instanceof Recipe<?> raw)) return;
            // Only support recipes whose input type is CraftingInput (our supreme + vanilla 3x3).
            @SuppressWarnings("unchecked")
            Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) raw;

            // Partial-fill: server takes only what the player actually has.
            // No pre-check needed — executeTransfer is naturally anti-cheat.
            if (sp.level().getBlockEntity(msg.tablePos()) instanceof SupremeTableBlockEntity be) {
                SupremeRecipeTransfer.executeTransfer(be, sp, recipe);
            }
        });
    }
}
