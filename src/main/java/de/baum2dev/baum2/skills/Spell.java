package de.baum2dev.baum2.skills;

import java.util.function.Consumer;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.classes.PlayerClass;

/**
 * The 8 active spells, 2 per class. Names are this project's own original placeholders from
 * MASTERPROMPT.md's "Skill-System" section, now implemented. Not final balance (cooldowns,
 * damage/heal amounts) - see HANDOFF.md for the pending balance-review step.
 */
public enum Spell {
    SCHILDSTOSS(PlayerClass.EISENWAECHTER, "Schildstoß", 160, 25, SpellEffects::schildstoss),
    STANDHAFTE_AURA(PlayerClass.EISENWAECHTER, "Standhafte Aura", 400, 30, SpellEffects::standhafteAura),
    NEBELSCHRITT(PlayerClass.SCHATTENLAEUFER, "Nebelschritt", 200, 15, SpellEffects::nebelschritt),
    KLINGENWIRBEL(PlayerClass.SCHATTENLAEUFER, "Klingenwirbel", 160, 25, SpellEffects::klingenwirbel),
    RUNENFUNKE(PlayerClass.RUNENWIRKER, "Runenfunke", 120, 20, SpellEffects::runenfunke),
    ARKANER_KREIS(PlayerClass.RUNENWIRKER, "Arkaner Kreis", 500, 40, SpellEffects::arkanerKreis),
    LEBENSBAND(PlayerClass.WESENSWAHRER, "Lebensband", 300, 30, SpellEffects::lebensband),
    GEISTERWOGE(PlayerClass.WESENSWAHRER, "Geisterwoge", 240, 20, SpellEffects::geisterwoge);

    private final PlayerClass requiredClass;
    private final String displayName;
    private final int cooldownTicks;
    private final int manaCost;
    private final Consumer<ServerPlayerEntity> effect;

    Spell(PlayerClass requiredClass, String displayName, int cooldownTicks, int manaCost, Consumer<ServerPlayerEntity> effect) {
        this.requiredClass = requiredClass;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.manaCost = manaCost;
        this.effect = effect;
    }

    public PlayerClass requiredClass() {
        return requiredClass;
    }

    public String displayName() {
        return displayName;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int manaCost() {
        return manaCost;
    }

    public void cast(ServerPlayerEntity player) {
        effect.accept(player);
    }
}
