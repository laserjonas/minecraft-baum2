package de.baum2dev.baum2.mixin;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Vanilla registers EntityAttributes.MAX_HEALTH with a hardcoded max clamp of 1024, which
 * VitalsCurve.getMaxLife (500 + 10/level, up to 1500 at level 100) would silently exceed
 * from level ~53 onward - every level past that would compute a higher value but have zero
 * actual effect, since ClampedEntityAttribute#clamp() caps it back down. Used by
 * VitalsManager to widen the ceiling once during mod init.
 */
@Mixin(ClampedEntityAttribute.class)
public interface ClampedEntityAttributeAccessor {
    @Mutable
    @Accessor("maxValue")
    void setMaxValue(double maxValue);
}
