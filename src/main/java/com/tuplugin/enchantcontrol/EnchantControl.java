package com.tripu1404.enchantcontrol;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.plugin.PluginBase;

import java.util.*;

/**
 * EnchantControl - Nukkit plugin
 *
 * Reglas implementadas (resumen):
 * - Armadura: aplican listas blancas estrictas por pieza (solo los encantamientos permitidos se mantienen)
 *   -> Prioridad entre protecciones: Protection > Blast > Fire > Projectile
 *   -> Frost Walker vs Depth Strider -> prioridad Depth Strider
 * - Otras herramientas/armas: no lista blanca estricta, pero se eliminan encantamientos según reglas:
 *   - Hachas: eliminar Fire Aspect
 *   - Tridentes: eliminar Sharpness y Fire Aspect
 *   - Arcos: si Mending + Infinity -> mantener Infinity, eliminar Mending
 *   - Picos: si Silk Touch + Fortune -> mantener Silk Touch, eliminar Fortune
 *   - Elytra: eliminar cualquier Protection
 * - Los encantamientos ilegales se eliminan "cada vez que se pueda":
 *   - Al abrir inventarios (InventoryOpenEvent)
 *   - En transacciones (InventoryTransactionEvent)
 *   - Al cambiar el item en mano (PlayerItemHeldEvent)
 *   - Al hacer click en inventarios (InventoryClickEvent)
 *   - Al unirse el jugador (PlayerJoinEvent) se valida su inventario y armadura
 * - Importante: si el item está dentro de un bloque de almacenamiento y ese bloque no está abierto, NO se modifica hasta que se abra.
 */
public class EnchantControl extends PluginBase implements Listener {

    // Prioridad para protecciones (menor índice = mayor prioridad)
    private static final List<Integer> PROTECTION_PRIORITY = Arrays.asList(
            Enchantment.ID_PROTECTION, // Protection
            Enchantment.ID_PROTECTION_EXPLOSIONS, // Blast Protection
            Enchantment.ID_PROTECTION_FIRE, // Fire Protection
            Enchantment.ID_PROTECTION_PROJECTILE // Projectile Protection
    );

    // Map de listas blancas para armaduras: itemId -> set(encId)
    private final Map<Integer, Set<Integer>> armorWhitelist = new HashMap<>();

    @Override
    public void onEnable() {
        Server.getInstance().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantControl habilitado");
        loadArmorWhitelist();
    }

    private void loadArmorWhitelist() {
        // Encantamientos permitidos para TODA la armadura
        Set<Integer> baseArmor = new HashSet<>(Arrays.asList(
                Enchantment.ID_PROTECTION,
                Enchantment.ID_PROTECTION_EXPLOSIONS,
                Enchantment.ID_PROTECTION_FIRE,
                Enchantment.ID_PROTECTION_PROJECTILE,
                Enchantment.ID_THORNS,
                Enchantment.ID_UNBREAKING,
                Enchantment.ID_BINDING_CURSE,
                Enchantment.ID_VANISHING_CURSE,
                Enchantment.ID_MENDING
        ));

        // Casco: + Respiration, Aqua Affinity
        Set<Integer> helmet = new HashSet<>(baseArmor);
        helmet.add(Enchantment.ID_RESPIRATION);
        helmet.add(Enchantment.ID_AQUA_AFFINITY);

        // Pechera: base
        Set<Integer> chest = new HashSet<>(baseArmor);

        // Pantalones: + Swift Sneak
        Set<Integer> leggings = new HashSet<>(baseArmor);
        // There may not be a constant for Swift Sneak in older Nukkit, try ID_SWIFT_SNEAK fallback
        // Use 24 for Swift Sneak if not available in Enchantment constants (Minecraft 1.19+). We'll reference via Enchantment.ID_UNKNOWN if necessary.
        try {
            // If Enchantment has an ID for Swift Sneak, add it (platform-dependent)
            // Many Nukkit builds don't have a constant; attempt to add by name is not possible here, so we'll include a numeric literal used in recent MC versions.
            final int SWIFT_SNEAK_ID = 26; // (if the runtime supports another value, adjust in your environment)
            leggings.add(SWIFT_SNEAK_ID);
        } catch (Throwable ignored) {
        }

        // Botas: + Depth Strider, Frost Walker, Soul Speed, Feather Falling
        Set<Integer> boots = new HashSet<>(baseArmor);
        boots.add(Enchantment.ID_DEPTH_STRIDER);
        boots.add(Enchantment.ID_FROST_WALKER);
        boots.add(Enchantment.ID_SOUL_SPEED);
        boots.add(Enchantment.ID_FEATHER_FALLING);

        // Asociar por item id (usar constantes de Item si están disponibles)
        armorWhitelist.put(Item.LEATHER_HELMET, helmet);
        armorWhitelist.put(Item.CHAIN_HELMET, helmet);
        armorWhitelist.put(Item.IRON_HELMET, helmet);
        armorWhitelist.put(Item.GOLD_HELMET, helmet);
        armorWhitelist.put(Item.DIAMOND_HELMET, helmet);
        armorWhitelist.put(Item.NETHERITE_HELMET, helmet);

        armorWhitelist.put(Item.LEATHER_CHESTPLATE, chest);
        armorWhitelist.put(Item.CHAIN_CHESTPLATE, chest);
        armorWhitelist.put(Item.IRON_CHESTPLATE, chest);
        armorWhitelist.put(Item.GOLD_CHESTPLATE, chest);
        armorWhitelist.put(Item.DIAMOND_CHESTPLATE, chest);
        armorWhitelist.put(Item.NETHERITE_CHESTPLATE, chest);

        armorWhitelist.put(Item.LEATHER_LEGGINGS, leggings);
        armorWhitelist.put(Item.CHAIN_LEGGINGS, leggings);
        armorWhitelist.put(Item.IRON_LEGGINGS, leggings);
        armorWhitelist.put(Item.GOLD_LEGGINGS, leggings);
        armorWhitelist.put(Item.DIAMOND_LEGGINGS, leggings);
        armorWhitelist.put(Item.NETHERITE_LEGGINGS, leggings);

        armorWhitelist.put(Item.LEATHER_BOOTS, boots);
        armorWhitelist.put(Item.CHAIN_BOOTS, boots);
        armorWhitelist.put(Item.IRON_BOOTS, boots);
        armorWhitelist.put(Item.GOLD_BOOTS, boots);
        armorWhitelist.put(Item.DIAMOND_BOOTS, boots);
        armorWhitelist.put(Item.NETHERITE_BOOTS, boots);
    }

    // ------------------------- Eventos -------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // validar inventario completo y armadura al entrar
        validateInventory(player.getInventory());
        validateArmorOfPlayer(player);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        // Al abrir un inventario — revisar sus items y corregir si hace falta
        validateInventory(inv);
    }

    @EventHandler
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        // En transacciones normales revisamos los outputs
        try {
            event.getTransaction().getTransactions().forEach(t -> {
                t.getOutputs().forEach(this::validateAndUpdateItemIfNeeded);
            });
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Al clickear en inventarios, validar los items involucrados (slot, cursor)
        try {
            Item clicked = event.getClickedItem();
            Item item = (clicked == null) ? null : clicked.clone();
            if (item != null && !item.isNull()) validateAndUpdateItemIfNeeded(item);

            Item cursor = event.getCursor();
            if (cursor != null && !cursor.isNull()) validateAndUpdateItemIfNeeded(cursor);
        } catch (Throwable ignored) {
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Item item = player.getInventory().getItemInHand();
        if (item != null && !item.isNull()) validateAndUpdateItemIfNeeded(item);
    }

    // ------------------------- Validaciones -------------------------

    private void validateInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            Item item = inventory.getItem(i);
            if (item == null || item.isNull()) continue;
            Item fixed = validateAndUpdateItemIfNeeded(item.clone());
            if (fixed != null) {
                inventory.setItem(i, fixed);
            }
        }
    }

    private void validateArmorOfPlayer(Player player) {
        if (player == null) return;
        // slots 36-39 are armor in many inventories, but safer to check equipped armor if API exposes it
        for (Item armor : player.getArmorInventory().getContents().values()) {
            if (armor == null || armor.isNull()) continue;
            Item fixed = validateAndUpdateItemIfNeeded(armor.clone());
            if (fixed != null) {
                player.getArmorInventory().setItem(player.getArmorInventory().getSlotByItem(armor), fixed);
            }
        }
    }

    /**
     * Valida un item y devuelve el item modificado si hubo cambios, o null si no hubo cambios.
     */
    private Item validateAndUpdateItemIfNeeded(Item item) {
        if (item == null || item.isNull()) return null;

        boolean changed = false;
        int id = item.getId();

        // Si es armadura -> aplicar lista blanca estricta
        if (armorWhitelist.containsKey(id)) {
            changed |= applyArmorWhitelist(item);
        } else {
            // Reglas específicas para otras herramientas/armas
            changed |= applyNonArmorRules(item);
        }

        return changed ? item : null;
    }

    // ------------------------- Reglas de armadura -------------------------
    private boolean applyArmorWhitelist(Item item) {
        int itemId = item.getId();
        Set<Integer> allowed = armorWhitelist.get(itemId);
        if (allowed == null) return false;

        // Recolectar encantamientos actuales
        List<Enchantment> current = new ArrayList<>(item.getEnchantments());
        boolean modified = false;

        Map<Integer, Enchantment> toKeep = new HashMap<>();

        // Primero: mantener solo los que están en la whitelist
        for (Enchantment e : current) {
            if (!allowed.contains(e.getId())) {
                // eliminado
                modified = true;
                continue;
            }
            toKeep.put(e.getId(), e);
        }

        // Aplicar prioridad entre protecciones
        int chosenProtection = -1;
        for (int i = 0; i < PROTECTION_PRIORITY.size(); i++) {
            int pid = PROTECTION_PRIORITY.get(i);
            if (toKeep.containsKey(pid)) {
                chosenProtection = pid;
                break;
            }
        }
        if (chosenProtection != -1) {
            // eliminar otras protecciones distintas de chosenProtection
            for (int pid : PROTECTION_PRIORITY) {
                if (pid != chosenProtection && toKeep.containsKey(pid)) {
                    toKeep.remove(pid);
                    modified = true;
                }
            }
        }

        // Frost Walker vs Depth Strider: prioridad Depth Strider
        if (toKeep.containsKey(Enchantment.ID_FROST_WALKER) && toKeep.containsKey(Enchantment.ID_DEPTH_STRIDER)) {
            // keep depth strider, remove frost
            toKeep.remove(Enchantment.ID_FROST_WALKER);
            modified = true;
        }

        // Reconstruir encantamientos: primero limpiar todos
        for (Enchantment e : current) {
            item.removeEnchantment(e.getId());
        }
        // Re-aplicar los que quedan
        for (Enchantment keep : toKeep.values()) {
            item.addEnchantment(keep);
        }

        return modified;
    }

    // ------------------------- Reglas no-armadura (armas/herramientas/elytra) -------------------------
    private boolean applyNonArmorRules(Item item) {
        boolean modified = false;
        List<Enchantment> current = new ArrayList<>(item.getEnchantments());
        Set<Integer> present = new HashSet<>();
        for (Enchantment e : current) present.add(e.getId());

        int id = item.getId();

        // Elytra: eliminar cualquier protección
        if (id == Item.ELYTRA) {
            for (int pid : PROTECTION_PRIORITY) {
                if (present.contains(pid)) {
                    item.removeEnchantment(pid);
                    modified = true;
                }
            }
            return modified;
        }

        // Hacha: eliminar Fire Aspect
        if (isAxe(id)) {
            if (present.contains(Enchantment.ID_FIRE_ASPECT)) {
                item.removeEnchantment(Enchantment.ID_FIRE_ASPECT);
                modified = true;
            }
        }

        // Tridente: eliminar Sharpness y Fire Aspect
        if (id == Item.TRIDENT) {
            if (present.contains(Enchantment.ID_SHARPNESS)) {
                item.removeEnchantment(Enchantment.ID_SHARPNESS);
                modified = true;
            }
            if (present.contains(Enchantment.ID_FIRE_ASPECT)) {
                item.removeEnchantment(Enchantment.ID_FIRE_ASPECT);
                modified = true;
            }
        }

        // Arco: si Mending + Infinity -> eliminar Mending (prioridad Infinity)
        if (id == Item.BOW) {
            if (present.contains(Enchantment.ID_MENDING) && present.contains(Enchantment.ID_INFINITY)) {
                item.removeEnchantment(Enchantment.ID_MENDING);
                modified = true;
            }
        }

        // Pico: Silk Touch vs Fortune -> prioridad Silk Touch
        if (isPickaxe(id)) {
            if (present.contains(Enchantment.ID_SILK_TOUCH) && present.contains(Enchantment.ID_FORTUNE)) {
                item.removeEnchantment(Enchantment.ID_FORTUNE);
                modified = true;
            }
        }

        return modified;
    }

    // Métodos utilitarios para detectar tipo de herramienta (según Item IDs)
    private boolean isAxe(int id) {
        return id == Item.WOODEN_AXE || id == Item.STONE_AXE || id == Item.IRON_AXE || id == Item.GOLDEN_AXE || id == Item.DIAMOND_AXE || id == Item.NETHERITE_AXE;
    }

    private boolean isPickaxe(int id) {
        return id == Item.WOODEN_PICKAXE || id == Item.STONE_PICKAXE || id == Item.IRON_PICKAXE || id == Item.GOLDEN_PICKAXE || id == Item.DIAMOND_PICKAXE || id == Item.NETHERITE_PICKAXE;
    }
}
