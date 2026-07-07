package de.baum2dev.baum2.items;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Drevathis's Cursed Blade: a plain sword-settings Item plus the boss blade's dark-smoke
 * wreath, scaled down to player proportions - a few slow smoke wisps curling off the blade
 * while it's held in the mainhand (the boss-side equivalent is
 * {@code DrevathisEntity.tickBladeSmoke()}). The Wave of Darkness on-hit proc lives in
 * {@code combat/DrevathisCursedBladeHandler}, unchanged by the boss rework.
 */
public class CursedBladeItem extends Item {

    public CursedBladeItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (slot != EquipmentSlot.MAINHAND || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (world.getTime() % 4 != 0) {
            return;
        }
        // approximate held-blade position: half a block toward the holder's right, hand height
        double yawRad = Math.toRadians(living.getBodyYaw());
        double x = living.getX() - Math.cos(yawRad) * 0.55;
        double z = living.getZ() - Math.sin(yawRad) * 0.55;
        double y = living.getY() + living.getHeight() * 0.55;
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.12, 0.3, 0.12, 0.003);
        if (world.getTime() % 16 == 0) {
            world.spawnParticles(ParticleTypes.SQUID_INK, x, y + 0.2, z, 1, 0.08, 0.25, 0.08, 0.006);
        }
    }
}
