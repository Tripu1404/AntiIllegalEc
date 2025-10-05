package com.tripu1404.enchantcontrol;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.plugin.PluginBase;

import java.util.*;

public class EnchantControl extends PluginBase implements Listener {

    // IDs de encantamientos definitivos según tu versión
    private static final int PROTECTION = 0;
    private static final int FIRE_PROTECTION = 1;
    private static final int FEATHER_FALLING = 2;
    private static final int BLAST_PROTECTION = 3;
    private static final int PROJECTILE_PROTECTION = 4;
    private static final int THORNS = 5;
    private static final int UNBREAKING = 34;
    private static final int DEPTH_STRIDER = 7;
    private static final int FROST_WALKER = 25;
    private static final int SOUL_SPEED = 36;
    private static final int RESPIRATION = 6;
    private static final int AQUA_AFFINITY = 8;
    private static final int SWIFT_SNEAK = 37;
    private static final int MENDING = 26;
    private static final int CURSE_OF_VANISHING = 28;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantControl habilitado!");
    }

    // Revisa y corrige encantamientos de un item
    private void fixItem(Item item) {
        if (item == null || item.isNull()) return;

        // Armadura
        if (isArmor(item)) {
            fixArmor(item);
        } else {
            fixNonArmor(item);
        }
    }

    private boolean isArmor(Item item) {
        switch (item.getId()) {
            case Item.LEATHER_HELMET:
            case Item.CHAIN_HELMET:
            case Item.IRON_HELMET:
            case Item.GOLD_HELMET:
            case Item.DIAMOND_HELMET:
            case Item.NETHERITE_HELMET:
            case Item.LEATHER_CHESTPLATE:
            case Item.CHAIN_CHESTPLATE:
            case Item.IRON_CHESTPLATE:
            case Item.GOLD_CHESTPLATE:
            case Item.DIAMOND_CHESTPLATE:
            case Item.NETHERITE_CHESTPLATE:
            case Item.LEATHER_LEGGINGS:
            case Item.CHAIN_LEGGINGS:
            case Item.IRON_LEGGINGS:
            case Item.GOLD_LEGGINGS:
            case Item.DIAMOND_LEGGINGS:
            case Item.NETHERITE_LEGGINGS:
            case Item.LEATHER_BOOTS:
            case Item.CHAIN_BOOTS:
            case Item.IRON_BOOTS:
            case Item.GOLD_BOOTS:
            case Item.DIAMOND_BOOTS:
            case Item.NETHERITE_BOOTS:
                return true;
        }
        return false;
    }

    private void fixArmor(Item item) {
        Map<Integer,Integer> enchants = item.getEnchantments();
        Map<Integer,Integer> toKeep = new HashMap<>();

        // Lista blanca básica para todas las piezas
        int[] allowed = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION, PROJECTILE_PROTECTION,
                THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING};

        // Piezas específicas
        switch (item.getId()) {
            case Item.LEATHER_HELMET:
            case Item.CHAIN_HELMET:
            case Item.IRON_HELMET:
            case Item.GOLD_HELMET:
            case Item.DIAMOND_HELMET:
            case Item.NETHERITE_HELMET:
                allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                        PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                        RESPIRATION, AQUA_AFFINITY};
                break;
            case Item.LEATHER_LEGGINGS:
            case Item.CHAIN_LEGGINGS:
            case Item.IRON_LEGGINGS:
            case Item.GOLD_LEGGINGS:
            case Item.DIAMOND_LEGGINGS:
            case Item.NETHERITE_LEGGINGS:
                allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                        PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                        SWIFT_SNEAK};
                break;
            case Item.LEATHER_BOOTS:
            case Item.CHAIN_BOOTS:
            case Item.IRON_BOOTS:
            case Item.GOLD_BOOTS:
            case Item.DIAMOND_BOOTS:
            case Item.NETHERITE_BOOTS:
                allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                        PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                        DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED, FEATHER_FALLING};
                break;
        }

        // Mantener solo encantamientos permitidos
        for (Map.Entry<Integer,Integer> e : enchants.entrySet()) {
            for (int id : allowed) {
                if (e.getKey() == id) {
                    toKeep.put(id, e.getValue());
                    break;
                }
            }
        }

        // Aplicar prioridades
        applyArmorPriorities(toKeep);

        // Limpiar y re-aplicar
        item.removeAllEnchantments();
        for (Map.Entry<Integer,Integer> e : toKeep.entrySet()) {
            item.addEnchantment(Enchantment.getEnchantment(e.getKey(), e.getValue()));
        }
    }

    private void applyArmorPriorities(Map<Integer,Integer> enchants) {
        // Protecciones
        int[] priority = {PROTECTION, BLAST_PROTECTION, FIRE_PROTECTION, PROJECTILE_PROTECTION};
        boolean found = false;
        for (int id : priority) {
            if (enchants.containsKey(id)) {
                found = true;
                // eliminar las demás
                for (int other : priority) {
                    if (other != id) enchants.remove(other);
                }
                break;
            }
        }
        // Frost Walker vs Depth Strider
        if (enchants.containsKey(FROST_WALKER) && enchants.containsKey(DEPTH_STRIDER)) {
            enchants.remove(FROST_WALKER); // prioridad a Depth Strider
        }
    }

    private void fixNonArmor(Item item) {
        if (item == null || item.isNull()) return;

        Map<Integer,Integer> enchants = item.getEnchantments();

        // Reglas específicas
        switch (item.getId()) {
            // Arco
            case Item.BOW:
                if (enchants.containsKey(MENDING) && enchants.containsKey(Enchantment.ID_INFINITY)) {
                    enchants.remove(MENDING); // prioridad a Infinity
                }
                break;
            // Pico
            case Item.DIAMOND_PICKAXE:
            case Item.NETHERITE_PICKAXE:
            case Item.IRON_PICKAXE:
            case Item.GOLD_PICKAXE:
            case Item.STONE_PICKAXE:
            case Item.WOODEN_PICKAXE:
                if (enchants.containsKey(Enchantment.ID_SILK_TOUCH) && enchants.containsKey(Enchantment.ID_FORTUNE)) {
                    enchants.remove(Enchantment.ID_FORTUNE); // prioridad a Silk Touch
                }
                break;
            // Hacha
            case Item.DIAMOND_AXE:
            case Item.NETHERITE_AXE:
            case Item.IRON_AXE:
            case Item.GOLD_AXE:
            case Item.STONE_AXE:
            case Item.WOODEN_AXE:
                enchants.remove(Enchantment.ID_FIRE_ASPECT); // eliminar Fire Aspect
                break;
            // Tridente
            case Item.TRIDENT:
                enchants.remove(Enchantment.ID_FIRE_ASPECT);
                enchants.remove(Enchantment.ID_SHARPNESS);
                break;
            // Elytra
            case Item.ELYTRA:
                // eliminar cualquier protección
                enchants.remove(PROTECTION);
                enchants.remove(FIRE_PROTECTION);
                enchants.remove(FEATHER_FALLING);
                enchants.remove(BLAST_PROTECTION);
                enchants.remove(PROJECTILE_PROTECTION);
                break;
        }
    }

    // Eventos
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (Item armor : player.getInventory().getArmorContents()) {
            fixItem(armor);
        }
        for (Item item : player.getInventory().getContents().values()) {
            fixItem(item);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        event.getInventory().getContents().values().forEach(this::fixItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        fixItem(event.getCursor());
        fixItem(event.getClickedItem());
    }
}
