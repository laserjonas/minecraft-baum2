package de.baum2dev.baum2.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.rule.GameRules;

/**
 * One-time-per-load setup of the Heimgrund world: the world border that hard-bounds the
 * finite map, and the gamerules that cover the non-player griefing Fabric has no events
 * for (explosions, fire) - see WorldProtectionHandler for the player-action half.
 *
 * <p>Gamerules are server-wide, not per-world; that is acceptable by design because
 * Heimgrund IS the game world (vanilla dimensions are unreachable).
 */
public final class WorldSetupHandler {

    /** 1000 = the mountain ring's outer edge (2 * ZoneLayout.WORLD_RADIUS). */
    private static final double BORDER_DIAMETER = 2.0 * ZoneLayout.WORLD_RADIUS;

    public static void registerEvents() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!Baum2WorldKeys.isHeimgrund(world)) {
                return;
            }

            // WorldBorder is per-world persistent state in 1.21.11; setting fixed values
            // on every load is idempotent.
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0.0, 0.0);
            border.setSize(BORDER_DIAMETER);

            GameRules rules = world.getGameRules();
            // No Fabric explosion/piston/fire events exist in 1.21.11, so non-player
            // griefing is shut off at the rules level instead (accepted tradeoff).
            rules.setValue(GameRules.DO_MOB_GRIEFING, false, server);
            rules.setValue(GameRules.TNT_EXPLODES, false, server);
            rules.setValue(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
            // Phantoms ignore the zone design entirely (insomnia-based, spawn anywhere
            // with skylight) - the authored monster map is biome-spawner-driven only.
            rules.setValue(GameRules.SPAWN_PHANTOMS, false, server);
        });
    }

    private WorldSetupHandler() {
    }
}
