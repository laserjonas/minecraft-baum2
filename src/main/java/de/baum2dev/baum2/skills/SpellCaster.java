package de.baum2dev.baum2.skills;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.classes.PlayerClass;

/**
 * Shared cast-attempt logic used by both the {@code /baum2 cast} command and the
 * keybind-driven {@code CastSpellPayload} path, so the two entry points can't drift apart.
 * Does not send any player-facing messages itself - callers format those, since a
 * {@code ServerCommandSource} (command) and a bare {@code ServerPlayerEntity} (keybind) report
 * feedback differently.
 */
public final class SpellCaster {

    public enum Result {
        SUCCESS,
        WRONG_CLASS,
        ON_COOLDOWN
    }

    public record CastAttempt(Result result, Spell spell, long remainingCooldownTicks) {
    }

    public static CastAttempt attemptCast(ServerPlayerEntity player, Spell spell) {
        Optional<PlayerClass> currentClass = ClassManager.getSelectedClass(player);
        if (currentClass.isEmpty() || currentClass.get() != spell.requiredClass()) {
            return new CastAttempt(Result.WRONG_CLASS, spell, 0);
        }

        var server = player.getEntityWorld().getServer();
        if (SkillCooldownManager.isOnCooldown(player, spell, server)) {
            return new CastAttempt(Result.ON_COOLDOWN, spell, SkillCooldownManager.remainingCooldownTicks(player, spell, server));
        }

        spell.cast(player);
        SkillCooldownManager.recordCast(player, spell, server);
        return new CastAttempt(Result.SUCCESS, spell, 0);
    }

    /**
     * The Nth spell (0-based) belonging to {@code playerClass}, in {@link Spell}'s own
     * declaration order (2 spells per class) - backs the "Cast Spell 1"/"Cast Spell 2"
     * keybinds, which cast whichever spell is slot 0/1 for the player's current class rather
     * than needing a separate keybind per spell.
     */
    public static Optional<Spell> spellForSlot(PlayerClass playerClass, int slot) {
        List<Spell> spells = Arrays.stream(Spell.values()).filter(s -> s.requiredClass() == playerClass).toList();
        return slot >= 0 && slot < spells.size() ? Optional.of(spells.get(slot)) : Optional.empty();
    }

    private SpellCaster() {
    }
}
