package com.simpledeathbans.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The Soul Link Totem item used for manual soul linking.
 * 
 * Usage:
 * - Hold this item and shift+right-click another player to request a soul link
 * - The target must also be holding a Soul Link Totem and shift+right-click you back
 * - Once both players consent, their souls are bound together
 * 
 * Recipe (3x3 shaped):
 * - Row 1: Amethyst Shard | Ender Pearl | Amethyst Shard
 * - Row 2: Ender Pearl | Totem of Undying | Ender Pearl
 * - Row 3: Amethyst Shard | Ender Pearl | Amethyst Shard
 */
public class SoulLinkTotemItem extends Item {
    
    public SoulLinkTotemItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Give it the enchantment glint
    }
}
