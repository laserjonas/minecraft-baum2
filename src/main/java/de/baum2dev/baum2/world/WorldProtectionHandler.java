package de.baum2dev.baum2.world;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Heimgrund is a fixed, authored map: no block may be broken or placed anywhere in the
 * dimension by survival players - that rule is what makes the boundary mountains
 * genuinely impassable. Three player-action hooks cover it (non-player griefing -
 * explosions, fire, mob griefing - is handled by gamerules in {@link WorldSetupHandler}):
 *
 * <ul>
 *   <li>{@code PlayerBlockBreakEvents.BEFORE} - cancels every survival block break.</li>
 *   <li>{@code BlockEvents.USE_ITEM_ON} - cancels using items ON blocks (block placement,
 *       flint and steel, bone meal, ...). Plain block interactions (doors, future village
 *       blocks) go through a different stage and stay usable.</li>
 *   <li>{@code UseItemCallback} - buckets place fluids via a raycast in {@code use()},
 *       not {@code useOnBlock()}, so they need their own cancel.</li>
 * </ul>
 *
 * <p>Creative players bypass everything - that is how the village gets hand-built and
 * how ops can fix the world. Survival players cannot reach creative without op rights.
 */
public final class WorldProtectionHandler {

    public static void registerEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !isLocked(world, player));

        BlockEvents.USE_ITEM_ON.register((stack, state, world, pos, player, hand, hitResult) ->
                isLocked(world, player) ? ActionResult.FAIL : ActionResult.PASS);

        UseItemCallback.EVENT.register((player, world, hand) ->
                isLocked(world, player) && player.getStackInHand(hand).getItem() instanceof BucketItem
                        ? ActionResult.FAIL
                        : ActionResult.PASS);
    }

    private static boolean isLocked(World world, PlayerEntity player) {
        return Baum2WorldKeys.isHeimgrund(world) && !player.isCreative();
    }

    private WorldProtectionHandler() {
    }
}
