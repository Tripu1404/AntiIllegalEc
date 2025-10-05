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

import java.util.ArrayList;
import java.util.List;

public class EnchantControl extends PluginBase implements Listener {

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
        getLogger().info("EnchantControl habilitado correctamente.");
    }

    private boolean fixItem(Item item) {
        if (item == null || item.isNull()) return false;
        boolean changed = false;

        if (isArmor(item)) {
            changed = fixArmor(item);
        } else {
            changed = fixNonArmor(item);
        }

        return changed;
    }

    private boolean isArmor(Item item) {
        int id = item.getId();
        return (id >= 298 && id <= 317);
    }

    private boolean fixArmor(Item item) {
        boolean changed = false;
        Enchantment[] enchants = item.getEnchantments();

        if (enchants == null || enchants.length == 0) return false;

        int id = item.getId();
        int[] allowed = {
                PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING
        };

        if (id == 298 || id == 302 || id == 306 || id == 310 || id == 314) {
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    RESPIRATION, AQUA_AFFINITY};
        } else if (id == 300 || id == 304 || id == 308 || id == 312 || id == 316) {
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    SWIFT_SNEAK};
        } else if (id == 301 || id == 305 || id == 309 || id == 313 || id == 317) {
            allowed = new int[]{PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                    PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING,
                    DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED};
        }

        // eliminar los no permitidos
        for (Enchantment e : enchants) {
            boolean allowedEnchant = false;
            for (int a : allowed) {
                if (e.getId() == a) {
                    allowedEnchant = true;
                    break;
                }
            }
            if (!allowedEnchant) {
                removeEnchantmentById(item, e.getId());
                changed = true;
            }
        }

        // FROST_WALKER + DEPTH_STRIDER incompatibles
        if (item.hasEnchantment(FROST_WALKER) && item.hasEnchantment(DEPTH_STRIDER)) {
            removeEnchantmentById(item, FROST_WALKER);
            changed = true;
        }

        // Solo una protección
        int[] protGroup = {PROTECTION, BLAST_PROTECTION, FIRE_PROTECTION, PROJECTILE_PROTECTION};
        int keep = -1;
        for (int pid : protGroup) {
            if (item.hasEnchantment(pid)) {
                keep = pid;
                break;
            }
        }
        if (keep != -1) {
            for (int pid : protGroup) {
                if (pid != keep && item.hasEnchantment(pid)) {
                    removeEnchantmentById(item, pid);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private boolean fixNonArmor(Item item) {
        boolean changed = false;
        int id = item.getId();

        switch (id) {
            case Item.BOW:
                if (item.hasEnchantment(MENDING) && item.hasEnchantment(INFINITY)) {
                    removeEnchantmentById(item, MENDING);
                    changed = true;
                }
                break;
            case Item.DIAMOND_PICKAXE:
            case Item.NETHERITE_PICKAXE:
            case Item.IRON_PICKAXE:
            case Item.GOLD_PICKAXE:
            case Item.STONE_PICKAXE:
            case Item.WOODEN_PICKAXE:
                if (item.hasEnchantment(SILK_TOUCH) && item.hasEnchantment(FORTUNE)) {
                    removeEnchantmentById(item, FORTUNE);
                    changed = true;
                }
                break;
            case Item.DIAMOND_AXE:
            case Item.NETHERITE_AXE:
            case Item.IRON_AXE:
            case Item.GOLD_AXE:
            case Item.STONE_AXE:
            case Item.WOODEN_AXE:
                if (item.hasEnchantment(FIRE_ASPECT)) {
                    removeEnchantmentById(item, FIRE_ASPECT);
                    changed = true;
                }
                break;
            case Item.TRIDENT:
                if (item.hasEnchantment(FIRE_ASPECT)) {
                    removeEnchantmentById(item, FIRE_ASPECT);
                    changed = true;
                }
                if (item.hasEnchantment(SHARPNESS)) {
                    removeEnchantmentById(item, SHARPNESS);
                    changed = true;
                }
                break;
            case Item.ELYTRA:
                int[] invalid = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION, PROJECTILE_PROTECTION};
                for (int pid : invalid) {
                    if (item.hasEnchantment(pid)) {
                        removeEnchantmentById(item, pid);
                        changed = true;
                    }
                }
                break;
        }

        return changed;
    }

    private void removeEnchantmentById(Item item, int id) {
        Enchantment[] current = item.getEnchantments();
        List<Enchantment> keep = new ArrayList<>();
        for (Enchantment e : current) {
            if (e.getId() != id) keep.add(e);
        }
        item.clearEnchantments();
        for (Enchantment e : keep) item.addEnchantment(e);
    }

    // --- EVENTOS ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getServer().getScheduler().scheduleDelayedTask(this, () -> {
            boolean changed = false;
            for (Item i : player.getInventory().getContents().values()) if (fixItem(i)) changed = true;
            for (Item i : player.getInventory().getArmorContents()) if (fixItem(i)) changed = true;
            if (changed) player.getInventory().sendContents(player);
        }, 1);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        getServer().getScheduler().scheduleDelayedTask(this, () -> {
            for (Item i : inv.getContents().values()) fixItem(i);
        }, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player p = event.getPlayer();
        Inventory inv = event.getInventory();
        int slot = event.getSlot();
        Item i = inv.getItem(slot);
        if (i == null || i.isNull()) return;

        getServer().getScheduler().scheduleDelayedTask(this, () -> {
            if (fixItem(i)) {
                inv.setItem(slot, i);
                if (p != null && p.isOnline()) {
                    p.getInventory().sendContents(p);
                }
            }
        }, 1);
    }
}
