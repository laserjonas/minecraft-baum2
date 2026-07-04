package de.baum2dev.baum2.classes;

/**
 * The stable identity of a playable class. {@code name()} doubles as the ASCII id used in
 * commands, {@link net.minecraft.util.Identifier}s, and the persistence codec — renaming or
 * reordering these constants after release would break already-saved player data.
 */
public enum PlayerClass {
    EISENWAECHTER,
    SCHATTENLAEUFER,
    RUNENWIRKER,
    WESENSWAHRER
}
