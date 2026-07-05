package de.baum2dev.baum2.block;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * A stationary "world event" mini-boss made of stone: unbreakable by any vanilla means (mining,
 * explosions, creative instant-break - see {@link #registerEvents()}), damaged only by a player
 * left-clicking it directly. Spawns Silverfish waves at HP thresholds and drops Risssplitter +
 * grants XP on destruction - see {@link RissobeliskBlockEntity} for that logic. See
 * docs/fabric-modding.md "Custom Blocks and BlockEntitys" for the researched API this is built
 * on (MASTERPROMPT.md's "Welt-Events" spec explicitly allows a manually placeable block for a
 * first version).
 */
public class RissobeliskBlock extends Block implements BlockEntityProvider {

    public RissobeliskBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RissobeliskBlockEntity(pos, state);
    }

    /**
     * This single listener both damages the block and is what makes it unbreakable: it fires at
     * the HEAD of the server's block-break handling, before either survival mining or the
     * creative-mode instant-break branch runs, so returning non-PASS here cancels both
     * unconditionally (confirmed via decompiled source - see the doc referenced above).
     */
    public static void registerEvents() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(world.getBlockState(pos).getBlock() instanceof RissobeliskBlock)) {
                return ActionResult.PASS;
            }
            if (world instanceof ServerWorld serverWorld
                    && player instanceof ServerPlayerEntity serverPlayer
                    && serverWorld.getBlockEntity(pos) instanceof RissobeliskBlockEntity blockEntity) {
                float damage = (float) serverPlayer.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
                blockEntity.damage(serverWorld, serverPlayer, damage);
            }
            return ActionResult.SUCCESS;
        });
    }
}
