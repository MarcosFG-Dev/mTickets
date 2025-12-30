package com.marcosfgdev.mtickets.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SkullUtil {

    @SuppressWarnings("deprecation")
    public static ItemStack getPlayerSkull(String playerName) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(playerName);
        skull.setItemMeta(meta);
        return skull;
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getPlayerSkullByUUID(UUID uuid) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        try {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer != null && offlinePlayer.getName() != null) {
                meta.setOwner(offlinePlayer.getName());
            }
        } catch (Exception ignored) {
        }
        skull.setItemMeta(meta);
        return skull;
    }
}
