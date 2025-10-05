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

public class EnchantControl extends PluginBase implements Listener {

    // IDs definitivos
    private static final int PROTECTION = 0;
    private static final int FIRE_PROTECTION = 1;
    private static final int FEATHER_FALLING = 2;
    private static final int BLAST_PROTECTION = 3;
    private static final int PROJECTILE_PROTECTION = 4;
    private static final int THORNS = 5;
    private static final int RESPIRATION = 6;
    private static final int DEPTH_STRIDER = 7;
    private static final int AQUA_AFFINITY = 8;
    private static final int SHARPNESS = 9;
    private static final int FIRE_ASPECT = 13;
    private static final int SILK_TOUCH = 16;
    private static final int FORTUNE = 18;
    private static final int INFINITY = 22;
    private static final int FROST_WALKER = 25;
    private static final int MENDING = 26;
    private static final int CURSE_OF_VANISHING = 28;
    private static final int UNBREAKING = 34;
    private static final int SOUL_SPEED = 36;
    private static final int SWIFT_SNEAK = 37;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantControl habilitado!");
    }

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
        return (id >= 298 && id <= 317); // casco, pechera, pantalón, botas
    }

    private void fixArmor(Item item) {
        Enchantment[] enchants = item.getEnchantments();
        if (enchants != null) {
            for (Enchantment e : enchants) {
                e.setLevel(0); // eliminar todos
            }
        }

        int[] allowed = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING};

        int id = item.getId();
        if (id == 298 || id == 302 || id == 306 || id == 310 || id == 314) { // casco
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    RESPIRATION, AQUA_AFFINITY};
        } else if (id == 300 || id == 304 || id == 308 || id == 312 || id == 316) { // pantalón
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    SWIFT_SNEAK};
        } else if (id == 301 || id == 305 || id == 309 || id == 313 || id == 317) { // botas
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED};
        }

        // reaplicar solo permitidos
        if (enchants != null) {
            for (Enchantment e : enchants) {
                for (int a : allowed) {
                    if (e.getId() == a) {
                        Enchantment ne = Enchantment.getEnchantment(a);
                        ne.setLevel(e.getLevel());
                        item.addEnchantment(ne);
                    }
                }
            }
        }

        applyArmorPriorities(item);
    }

    private void applyArmorPriorities(Item item) {
        int[] priority = {PROTECTION, BLAST_PROTECTION, FIRE_PROTECTION, PROJECTILE_PROTECTION};
        int keep = -1;
        for (int id : priority) {
            if (item.hasEnchantment(id)) {
                keep = id;
                break;
            }
        }
        if (keep != -1) {
            for (int id : priority) {
                if (id != keep && item.hasEnchantment(id)) {
                    Enchantment e = Enchantment.getEnchantment(id);
                    e.setLevel(0);
                }
            }
        }

        if (item.hasEnchantment(FROST_WALKER) && item.hasEnchantment(DEPTH_STRIDER)) {
            Enchantment e = Enchantment.getEnchantment(FROST_WALKER);
            e.setLevel(0);
        }
    }

    private void fixNonArmor(Item item) {
        if (item == null || item.isNull()) return;

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
                removeEnchantment(item, FIRE_ASPECT);
                break;
            case Item.TRIDENT:
                removeEnchantment(item, FIRE_ASPECT);
                removeEnchantment(item, SHARPNESS);
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
        boolean hasMending = item.hasEnchantment(MENDING);
        boolean hasInfinity = item.hasEnchantment(INFINITY);
        if (hasMending && hasInfinity) removeEnchantment(item, MENDING);
    }

    private void handleSilkFortune(Item item) {
        boolean hasSilk = item.hasEnchantment(SILK_TOUCH);
        boolean hasFortune = item.hasEnchantment(FORTUNE);
        if (hasSilk && hasFortune) removeEnchantment(item, FORTUNE);
    }

    private void removeEnchantment(Item item, int id) {
        if (item.hasEnchantment(id)) {
            Enchantment e = Enchantment.getEnchantment(id);
            e.setLevel(0);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (Item i : player.getInventory().getContents().values()) fixItem(i);
        for (Item i : player.getInventory().getArmorContents()) fixItem(i);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        for (Item i : inv.getContents().values()) fixItem(i);
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
