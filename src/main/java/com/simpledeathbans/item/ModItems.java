package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * Registers all custom items for the mod.
 */
public class ModItems {
    
    public static final Identifier RESURRECTION_TOTEM_ID = Identifier.of(SimpleDeathBans.MOD_ID, "resurrection_totem");
    public static final RegistryKey<Item> RESURRECTION_TOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, RESURRECTION_TOTEM_ID);
    
    public static Item RESURRECTION_TOTEM;
    
    public static void register() {
        // Register Resurrection Totem
        RESURRECTION_TOTEM = Registry.register(
            Registries.ITEM,
            RESURRECTION_TOTEM_ID,
            new ResurrectionTotemItem(new Item.Settings()
                .registryKey(RESURRECTION_TOTEM_KEY)
                .maxCount(1)
                .rarity(Rarity.EPIC))
        );
        
        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(RESURRECTION_TOTEM);
        });
        
        // Register recipes
        ModRecipes.register();
        
        SimpleDeathBans.LOGGER.info("Registered items for Simple Death Bans");
    }
}
