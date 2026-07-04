package de.baum2dev.baum2.classes;

/**
 * The stable identity of a class sub-specialization. Each value belongs to exactly one
 * {@link PlayerClass} (see {@link #parentClass()}) and is only selectable once that class is
 * the player's current class. {@code name()} doubles as the ASCII id used in commands and
 * {@link net.minecraft.util.Identifier}s, same convention as {@link PlayerClass}.
 */
public enum ClassSubspec {
    BOLLWERK(PlayerClass.EISENWAECHTER),
    STAHLFAUST(PlayerClass.EISENWAECHTER),
    SCHATTENPIRSCHER(PlayerClass.SCHATTENLAEUFER),
    STURMKLINGE(PlayerClass.SCHATTENLAEUFER),
    SPLITTERRUNE(PlayerClass.RUNENWIRKER),
    GLUECKSRUNE(PlayerClass.RUNENWIRKER),
    WURZELWALL(PlayerClass.WESENSWAHRER),
    WESENSFUELLE(PlayerClass.WESENSWAHRER);

    private final PlayerClass parentClass;

    ClassSubspec(PlayerClass parentClass) {
        this.parentClass = parentClass;
    }

    public PlayerClass parentClass() {
        return parentClass;
    }
}
