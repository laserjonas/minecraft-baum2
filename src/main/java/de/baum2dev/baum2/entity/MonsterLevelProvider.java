package de.baum2dev.baum2.entity;

/**
 * Implemented by custom mobs that have a real, fixed monster level (as opposed to the
 * placeholder "Lvl. 1" every other entity shows - see MobNameplateHud.getMonsterLevelText()).
 */
public interface MonsterLevelProvider {
    int getMonsterLevel();
}
