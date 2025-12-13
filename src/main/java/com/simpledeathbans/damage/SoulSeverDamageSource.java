package com.simpledeathbans.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Custom damage source for Soul Sever deaths.
 */
public class SoulSeverDamageSource {
    
    public static final Identifier SOUL_SEVER_ID = Identifier.of("simpledeathbans", "soul_sever");
    public static final RegistryKey<DamageType> SOUL_SEVER_KEY = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, SOUL_SEVER_ID);
    
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
        // This prevents infinite damage sharing loops
        return source.getName().contains("magic") && source.getAttacker() instanceof ServerPlayerEntity;
    }
}
