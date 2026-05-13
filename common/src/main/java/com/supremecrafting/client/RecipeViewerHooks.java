package com.supremecrafting.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge between the in-game arrow button on {@link SupremeTableScreen} and
 * whichever recipe-viewer plugin is loaded (EMI, JEI, …). Per-loader plugins
 * register a {@link Runnable} here that, when invoked, opens that viewer's
 * recipe panel filtered to the Supreme Crafting category.
 *
 * <p>The list is a registry-holder static per the project's static-field rule —
 * loader-side integrations need a place to register themselves at plugin
 * load time, before any screen exists.
 */
public final class RecipeViewerHooks {
    /** Plugins append here at startup. */
    public static final List<Runnable> openSupremeRecipes = new ArrayList<>();

    private RecipeViewerHooks() {}

    /**
     * Run the first hook that doesn't throw. EMI and JEI both being installed
     * is normal in modpacks; we only want one viewer to open per click. Plugins
     * are encouraged to register in preferred order (EMI added first by
     * convention since it tends to ship later in load order).
     */
    public static boolean invokeFirst() {
        for (Runnable r : openSupremeRecipes) {
            try {
                r.run();
                return true;
            } catch (Throwable t) {
                // Try the next hook — viewer may not be ready yet (no world joined, etc.).
            }
        }
        return false;
    }
}
