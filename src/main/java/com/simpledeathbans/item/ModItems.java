package com.simpledeathbans.item;

import com.simpledeathbans.SimpleDeathBans;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;*/
//?}
import net.minecraft.world.item.Item;
//? if <26.1
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.world.item.Rarity;

/**
 * Registers all custom items for the mod.
 */
public class ModItems {
    
    //? if >=1.21.11 {
    public static final Identifier RESURRECTION_TOTEM_ID = Identifier.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "resurrection_totem");
    public static final ResourceKey<Item> RESURRECTION_TOTEM_KEY = ResourceKey.create(Registries.ITEM, RESURRECTION_TOTEM_ID);
    
    public static final Identifier SOUL_LINK_TOTEM_ID = Identifier.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "soul_link_totem");
    public static final ResourceKey<Item> SOUL_LINK_TOTEM_KEY = ResourceKey.create(Registries.ITEM, SOUL_LINK_TOTEM_ID);
    
    public static final Identifier VOID_CRYSTAL_TOTEM_ID = Identifier.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "void_crystal_totem");
    public static final ResourceKey<Item> VOID_CRYSTAL_TOTEM_KEY = ResourceKey.create(Registries.ITEM, VOID_CRYSTAL_TOTEM_ID);
    //?} else {
    /*public static final ResourceLocation RESURRECTION_TOTEM_ID = ResourceLocation.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "resurrection_totem");
    public static final ResourceKey<Item> RESURRECTION_TOTEM_KEY = ResourceKey.create(Registries.ITEM, RESURRECTION_TOTEM_ID);
    
    public static final ResourceLocation SOUL_LINK_TOTEM_ID = ResourceLocation.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "soul_link_totem");
    public static final ResourceKey<Item> SOUL_LINK_TOTEM_KEY = ResourceKey.create(Registries.ITEM, SOUL_LINK_TOTEM_ID);
    
    public static final ResourceLocation VOID_CRYSTAL_TOTEM_ID = ResourceLocation.fromNamespaceAndPath(SimpleDeathBans.MOD_ID, "void_crystal_totem");
    public static final ResourceKey<Item> VOID_CRYSTAL_TOTEM_KEY = ResourceKey.create(Registries.ITEM, VOID_CRYSTAL_TOTEM_ID);*/
    //?}
    
    public static Item RESURRECTION_TOTEM;
    public static Item SOUL_LINK_TOTEM;
    public static Item VOID_CRYSTAL_TOTEM;
    
    public static void register() {
        // Register Resurrection Totem
        RESURRECTION_TOTEM = Registry.register(
            BuiltInRegistries.ITEM,
            RESURRECTION_TOTEM_ID,
            new ResurrectionTotemItem(new Item.Properties()
                .setId(RESURRECTION_TOTEM_KEY)
                .stacksTo(1)
                .rarity(Rarity.EPIC))
        );
        
        // Register Soul Link Totem
        SOUL_LINK_TOTEM = Registry.register(
            BuiltInRegistries.ITEM,
            SOUL_LINK_TOTEM_ID,
            new SoulLinkTotemItem(new Item.Properties()
                .setId(SOUL_LINK_TOTEM_KEY)
                .stacksTo(1)
                .rarity(Rarity.EPIC))
        );
        
        // Register Void Crystal Totem
        VOID_CRYSTAL_TOTEM = Registry.register(
            BuiltInRegistries.ITEM,
            VOID_CRYSTAL_TOTEM_ID,
            new VoidCrystalTotemItem(new Item.Properties()
                .setId(VOID_CRYSTAL_TOTEM_KEY)
                .stacksTo(1)
                .rarity(Rarity.EPIC))
        );
        
        // Add to creative tab
        //? if >=26.1 {
        ResourceKey<CreativeModeTab> toolsKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"));
        CreativeModeTabEvents.modifyOutputEvent(toolsKey).register(content -> {
        //?} else {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
        *///?}
            content.accept(RESURRECTION_TOTEM);
            content.accept(SOUL_LINK_TOTEM);
            content.accept(VOID_CRYSTAL_TOTEM);
        });
        
        // Register recipes
        ModRecipes.register();
        
        SimpleDeathBans.LOGGER.info("Registered items for Simple Death Bans");
    }
}
