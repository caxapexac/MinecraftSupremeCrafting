package com.supremecrafting.net;

import com.mojang.logging.LogUtils;
import com.supremecrafting.client.ClientMultiblockRegions;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

/**
 * Holds client-only S2C handlers in a separate class so the server JVM never
 * loads them (dispatched via {@link dev.architectury.utils.EnvExecutor} from
 * {@link SCNetwork}).
 */
@Environment(EnvType.CLIENT)
public final class ClientNetHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientNetHandlers() {}

    public static void handleSync(S2CMultiblockSyncPacket msg, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            ClientMultiblockRegions.apply(msg);
            LOGGER.debug("multiblock_sync recv op={} add={} remove={}",
                    msg.op(), msg.add().size(), msg.remove());
        });
    }

    public static void handleLit(S2CMultiblockLitPacket msg, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> ClientMultiblockRegions.applyLit(msg.id(), msg.lit()));
    }
}
