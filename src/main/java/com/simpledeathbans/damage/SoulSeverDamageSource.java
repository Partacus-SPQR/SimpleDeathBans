package com.simpledeathbans.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom damage source for Soul Sever deaths.
 */
public class SoulSeverDamageSource {
    
    public static final Identifier SOUL_SEVER_ID = Identifier.of("simpledeathbans", "soul_sever");
    public static final RegistryKey<DamageType> SOUL_SEVER_KEY = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, SOUL_SEVER_ID);
    
    // Track players currently being damaged by soul sever
    private static final Set<UUID> soulSeverTargets = ConcurrentHashMap.newKeySet();
    
    /**
     * Mark a player as receiving soul sever damage
     */
    public static void markSoulSeverTarget(UUID playerId) {
        soulSeverTargets.add(playerId);
    }
    
    /**
     * Clear the soul sever target flag
     */
    public static void clearSoulSeverTarget(UUID playerId) {
        soulSeverTargets.remove(playerId);
    }
    
    /**
     * Check if a player is currently a soul sever target
     */
    public static boolean isSoulSeverTarget(UUID playerId) {
        return soulSeverTargets.contains(playerId);
    }
    
    /**
     * Create a soul sever damage source
     * @param world The server world
     * @param causedBy The player whose death caused the soul sever (can be null)
     */
    public static DamageSource create(ServerWorld world, ServerPlayerEntity causedBy) {
        // Use magic damage as a fallback since we can't register custom damage types easily
        // The death message will be handled separately
        if (causedBy != null) {
            return world.getDamageSources().indirectMagic(causedBy, causedBy);
        }
        return world.getDamageSources().magic();
    }
    
    /**
     * Check if a damage source is soul sever damage
     */
    public static boolean isSoulSever(DamageSource source) {
        // Check by name pattern since we're using magic damage
        // indirectMagic creates damage with name like "indirectMagic" or similar
        // This prevents infinite damage sharing loops
        String name = source.getName();
        return (name.contains("magic") || name.contains("Magic") || name.equals("indirectMagic")) 
               && source.getAttacker() instanceof ServerPlayerEntity;
    }
}
