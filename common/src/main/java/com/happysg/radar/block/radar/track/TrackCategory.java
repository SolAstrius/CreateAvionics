package com.happysg.radar.block.radar.track;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;


public enum TrackCategory {
    PLAYER,
    MOB,
    HOSTILE,
    ANIMAL,
    SUBLEVEL,
    PROJECTILE,
    CONTRAPTION,
    ITEM,
    MISC;


    public static TrackCategory get(Entity entity) {
        EntityType<?> type = entity.getType();
        if (type.is(RadarEntityTypeTags.RADAR_HOSTILE)) return HOSTILE;
        if (type.is(RadarEntityTypeTags.RADAR_ANIMAL)) return ANIMAL;
        if (type.is(RadarEntityTypeTags.RADAR_MOB)) return MOB;
        if (type.is(RadarEntityTypeTags.RADAR_PROJECTILE)) return PROJECTILE;
        if (type.is(RadarEntityTypeTags.RADAR_ITEM)) return ITEM;

        if (entity instanceof Player) return PLAYER;
        if (entity instanceof Enemy) return HOSTILE;
        if (entity instanceof Animal) return ANIMAL;
        if (entity instanceof Mob) return MOB;
        if (entity instanceof Projectile) return PROJECTILE;
        if (entity instanceof AbstractContraptionEntity) return CONTRAPTION;
        if (entity instanceof ItemEntity) return ITEM;
        return MISC;
    }
}
