package com.tripu1404.enchantcontrol;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;

public class EnchantControl extends PluginBase implements Listener {

    // IDs de encantamientos
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

    // --- NBT Utilities ---
    private CompoundTag getOrCreateTag(Item item) {
        CompoundTag tag = item.getNamedTag();
        if (tag == null) {
            tag = new CompoundTag();
            item.setNamedTag(tag);
        }
        return tag;
    }

    private void removeEnchantmentById(Item item, int id) {
        CompoundTag tag = getOrCreateTag(item);
        if (!tag.contains("ench")) return;

        ListTag<CompoundTag> enchList = tag.getList("ench", CompoundTag.class);
        ListTag<CompoundTag> newList = new ListTag<>("ench");
        for (int i = 0; i < enchList.size(); i++) {
            CompoundTag e = enchList.get(i);
            if (e.getShort("id") != id) newList.add(e);
        }
        tag.putList(newList);
        item.setNamedTag(tag);
    }

    private boolean hasEnchantment(Item item, int id) {
        CompoundTag tag = getOrCreateTag(item);
        if (!tag.contains("ench")) return false;
        ListTag<CompoundTag> enchList = tag.getList("ench", CompoundTag.class);
        for (int i = 0; i < enchList.size(); i++) {
            if (enchList.get(i).getShort("id") == id) return true;
        }
        return false;
    }

    // --- Item Fixers ---
    private boolean fixItem(Item item) {
        if (item == null || item.isNull()) return false;
        boolean changed = false;
        if (!isArmor(item)) changed = fixNonArmor(item);
        return changed;
    }

    private boolean isArmor(Item item) {
        int id = item.getId();
        return (id >= 298 && id <= 317);
    }

    private boolean fixArmor(Item item, Player player, int armorSlot) {
        if (item == null || item.isNull()) return false;
        boolean changed = false;
        int id = item.getId();

        int[] allowed = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION,
                PROJECTILE_PROTECTION, THORNS, UNBREAKING, CURSE_OF_VANISHING, MENDING};

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

        CompoundTag tag = getOrCreateTag(item);
        if (tag.contains("ench")) {
            ListTag<CompoundTag> enchList = tag.getList("ench", CompoundTag.class);
            ListTag<CompoundTag> newList = new ListTag<>("ench");
            for (int i = 0; i < enchList.size(); i++) {
                CompoundTag e = enchList.get(i);
                int eid = e.getShort("id");
                boolean allowedEnchant = false;
                for (int a : allowed) if (eid == a) allowedEnchant = true;
                if (allowedEnchant) newList.add(e); else changed = true;
            }
            tag.putList(newList);
            item.setNamedTag(tag);
        }

        // incompatibilidades
        if (hasEnchantment(item, FROST_WALKER) && hasEnchantment(item, DEPTH_STRIDER)) {
            removeEnchantmentById(item, FROST_WALKER);
            changed = true;
        }

        int[] protGroup = {PROTECTION, BLAST_PROTECTION, FIRE_PROTECTION, PROJECTILE_PROTECTION};
        int keep = -1;
        for (int pid : protGroup) if (hasEnchantment(item, pid)) { keep = pid; break; }
        if (keep != -1) for (int pid : protGroup) if (pid != keep && hasEnchantment(item, pid)) {
            removeEnchantmentById(item, pid);
            changed = true;
        }

        if (player != null && changed) {
            PlayerInventory inv = player.getInventory();
            inv.setArmorItem(armorSlot, item);       // Actualiza solo la pieza de armadura
            inv.sendArmorContents(player);           // Envia solo armadura, no todo el inventario
        }

        return changed;
    }

    private boolean fixNonArmor(Item item) {
        boolean changed = false;
        int id = item.getId();

        switch (id) {
            case Item.BOW:
                if (hasEnchantment(item, MENDING) && hasEnchantment(item, INFINITY)) {
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
                if (hasEnchantment(item, SILK_TOUCH) && hasEnchantment(item, FORTUNE)) {
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
                if (hasEnchantment(item, FIRE_ASPECT)) {
                    removeEnchantmentById(item, FIRE_ASPECT);
                    changed = true;
                }
                break;
            case Item.TRIDENT:
                if (hasEnchantment(item, FIRE_ASPECT)) { removeEnchantmentById(item, FIRE_ASPECT); changed = true; }
                if (hasEnchantment(item, SHARPNESS)) { removeEnchantmentById(item, SHARPNESS); changed = true; }
                break;
            case Item.ELYTRA:
                int[] invalid = {PROTECTION, FIRE_PROTECTION, FEATHER_FALLING, BLAST_PROTECTION, PROJECTILE_PROTECTION};
                for (int pid : invalid) if (hasEnchantment(item, pid)) {
                    removeEnchantmentById(item, pid);
                    changed = true;
                }
                break;
        }

        return changed;
    }

    // --- EVENTOS ---
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (Item i : player.getInventory().getContents().values()) fixItem(i);
        for (int slot = 0; slot < 4; slot++) {
            Item armor = player.getInventory().getArmorItem(slot);
            if (armor != null && !armor.isNull()) fixArmor(armor, player, slot);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        for (Item i : inv.getContents().values()) fixItem(i);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player p = event.getPlayer();
        Inventory inv = event.getInventory();
        int slot = event.getSlot();
        Item i = inv.getItem(slot);
        if (i != null && !i.isNull()) fixItem(i);

        // Revisar solo armaduras equipadas
        if (p != null) {
            PlayerInventory pinv = p.getInventory();
            for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
                Item armor = pinv.getArmorItem(armorSlot);
                if (armor != null && !armor.isNull()) fixArmor(armor, p, armorSlot);
            }
        }
    }
}
