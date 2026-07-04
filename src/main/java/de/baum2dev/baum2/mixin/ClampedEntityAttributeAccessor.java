package de.baum2dev.baum2.mixin;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Vanilla registers EntityAttributes.MAX_HEALTH with a hardcoded max clamp of 1024, which
 * VitalsCurve.getMaxLife (Endurance-driven: 500 + 20/point above the starting 5, up to 2480
 * at the real max attainable Endurance of 104) would silently exceed once a player invests
 * enough level-up points - every point past that would compute a higher value but have zero
 * actual effect, since ClampedEntityAttribute#clamp() caps it back down. Used by
 * VitalsManager to widen the ceiling once during mod init (currently to 4096, comfortable
 * headroom above the real 2480 max - see VitalsManager.widenMaxHealthCeiling()).
 */
@Mixin(ClampedEntityAttribute.class)
public interface ClampedEntityAttributeAccessor {
    @Mutable
    @Accessor("maxValue")
    void setMaxValue(double maxValue);
}
