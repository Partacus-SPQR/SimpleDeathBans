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
    
    public static final Identifier SOUL_LINK_TOTEM_ID = Identifier.of(SimpleDeathBans.MOD_ID, "soul_link_totem");
    public static final RegistryKey<Item> SOUL_LINK_TOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, SOUL_LINK_TOTEM_ID);
    
    public static final Identifier VOID_CRYSTAL_TOTEM_ID = Identifier.of(SimpleDeathBans.MOD_ID, "void_crystal_totem");
    public static final RegistryKey<Item> VOID_CRYSTAL_TOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, VOID_CRYSTAL_TOTEM_ID);
    
    public static Item RESURRECTION_TOTEM;
    public static Item SOUL_LINK_TOTEM;
    public static Item VOID_CRYSTAL_TOTEM;
    
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
        
        // Register Soul Link Totem
        SOUL_LINK_TOTEM = Registry.register(
            Registries.ITEM,
            SOUL_LINK_TOTEM_ID,
            new SoulLinkTotemItem(new Item.Settings()
                .registryKey(SOUL_LINK_TOTEM_KEY)
                .maxCount(1)
                .rarity(Rarity.EPIC))
        );
        
        // Register Void Crystal Totem
        VOID_CRYSTAL_TOTEM = Registry.register(
            Registries.ITEM,
            VOID_CRYSTAL_TOTEM_ID,
            new VoidCrystalTotemItem(new Item.Settings()
                .registryKey(VOID_CRYSTAL_TOTEM_KEY)
                .maxCount(1)
                .rarity(Rarity.EPIC))
        );
        
        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(RESURRECTION_TOTEM);
            content.add(SOUL_LINK_TOTEM);
            content.add(VOID_CRYSTAL_TOTEM);
        });
        
        // Register recipes
        ModRecipes.register();
        
        SimpleDeathBans.LOGGER.info("Registered items for Simple Death Bans");
    }
}
