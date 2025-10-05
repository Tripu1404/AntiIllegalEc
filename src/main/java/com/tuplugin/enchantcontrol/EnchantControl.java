package com.tripu1404.enchantcontrol;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.plugin.PluginBase;

import java.util.*;

public class EnchantControl extends PluginBase implements Listener {

    // IDs definitivos
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

    // Fix general de items
    private void fixItem(Item item) {
        if (item == null || item.isNull()) return;

        if (isArmor(item)) {
            fixArmor(item);
        } else {
            fixNonArmor(item);
        }
    }

    private boolean isArmor(Item item) {
        int id = item.getId();
        return (id >= 298 && id <= 317) || (id >= 300 && id <= 319); // Helm, Chest, Legs, Boots
    }

    private void fixArmor(Item item) {
        Enchantment[] enchants = item.getEnchantments();
        Map<Integer, Enchantment> toKeep = new HashMap<>();

        int[] allowed = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING};

        int id = item.getId();
        if (id == 298 || id == 302 || id == 306 || id == 310 || id == 314) { // Helmet
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    RESPIRATION, AQUA_AFFINITY};
        } else if (id == 300 || id == 304 || id == 308 || id == 312 || id == 316) { // Leggings
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    SWIFT_SNEAK};
        } else if (id == 301 || id == 305 || id == 309 || id == 313 || id == 317) { // Boots
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED};
        }

        // Mantener solo encantamientos permitidos
        for (Enchantment e : enchants) {
            for (int a : allowed) {
                if (e.getId() == a) {
                    toKeep.put(a, e);
                    break;
                }
            }
        }

        // Aplicar prioridades
        applyArmorPriorities(toKeep);

        // Limpiar y reaplicar
        for (Enchantment e : enchants) {
            item.removeEnchantment(e);
        }
        for (Enchantment e : toKeep.values()) {
            item.addEnchantment(e);
        }
    }

    private void applyArmorPriorities(Map<Integer, Enchantment> enchants) {
        int[] priority = {PROTECTION, BLAST_PROTECTION, FIRE_PROTECTION, PROJECTILE_PROTECTION};
        for (int id : priority) {
            if (enchants.containsKey(id)) {
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
        Enchantment[] enchants = item.getEnchantments();
        if (enchants == null) return;

        switch (item.getId()) {
            case Item.BOW:
                handleMendingInfinity(item);
                break;
            case Item.DIAMOND_PICKAXE:
            case Item.NETHERITE_PICKAXE:
            case Item.IRON_PICKAXE:
            case Item.GOLD_PICKAXE:
            case Item.STONE_PICKAXE:
            case Item.WOODEN_PICKAXE:
                handleSilkFortune(item);
                break;
            case Item.DIAMOND_AXE:
            case Item.NETHERITE_AXE:
            case Item.IRON_AXE:
            case Item.GOLD_AXE:
            case Item.STONE_AXE:
            case Item.WOODEN_AXE:
                removeEnchantment(item, Enchantment.FIRE_ASPECT);
                break;
            case Item.TRIDENT:
                removeEnchantment(item, Enchantment.FIRE_ASPECT);
                removeEnchantment(item, Enchantment.SHARPNESS);
                break;
            case Item.ELYTRA:
                removeEnchantment(item, PROTECTION);
                removeEnchantment(item, FIRE_PROTECTION);
                removeEnchantment(item, FEATHER_FALLING);
                removeEnchantment(item, BLAST_PROTECTION);
                removeEnchantment(item, PROJECTILE_PROTECTION);
                break;
        }
    }

    private void handleMendingInfinity(Item item) {
        boolean hasMending = false;
        boolean hasInfinity = false;
        for (Enchantment e : item.getEnchantments()) {
            if (e.getId() == MENDING) hasMending = true;
            if (e.getId() == Enchantment.INFINITY) hasInfinity = true;
        }
        if (hasMending && hasInfinity) removeEnchantment(item, MENDING);
    }

    private void handleSilkFortune(Item item) {
        boolean hasSilk = false;
        boolean hasFortune = false;
        for (Enchantment e : item.getEnchantments()) {
            if (e.getId() == Enchantment.SILK_TOUCH) hasSilk = true;
            if (e.getId() == Enchantment.FORTUNE) hasFortune = true;
        }
        if (hasSilk && hasFortune) removeEnchantment(item, Enchantment.FORTUNE);
    }

    private void removeEnchantment(Item item, int id) {
        for (Enchantment e : item.getEnchantments()) {
            if (e.getId() == id) {
                item.removeEnchantment(e);
                break;
            }
        }
    }

    // Eventos
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (Item armor : player.getInventory().getArmorContents()) {
            fixItem(armor);
        }
        for (Item i : player.getInventory().getContents().values()) {
            fixItem(i);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        for (Item i : inv.getContents().values()) {
            fixItem(i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        int slot = event.getSlot();
        Item i = inv.getItem(slot);
        fixItem(i);
        inv.setItem(slot, i);
    }
}
