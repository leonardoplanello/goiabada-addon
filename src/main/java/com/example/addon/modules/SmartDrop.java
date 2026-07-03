package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SmartDrop extends Module {
    public enum DropMode {
        Whitelist("Permitidos (Dropa o resto)"),
        Blacklist("Bloqueados (Dropa selecionados)");

        private final String name;
        DropMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<DropMode> mode = sgGeneral.add(new EnumSetting.Builder<DropMode>()
        .name("mode")
        .description("Modo de verificação dos itens selecionados.")
        .defaultValue(DropMode.Whitelist)
        .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Lista de itens selecionados para Whitelist ou Blacklist.")
        .defaultValue(List.of())
        .build()
    );

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("Intervalo em ticks entre cada drop para evitar kicks por anti-cheat.")
        .defaultValue(3)
        .min(0)
        .max(40)
        .build()
    );

    private final Setting<Boolean> ignoreHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-hotbar")
        .description("Ignorar os itens na barra de atalhos (hotbar) durante o drop.")
        .defaultValue(true)
        .build()
    );

    private int timer = 0;

    public SmartDrop() {
        super(AddonTemplate.CATEGORY, "smart-drop", "Descarte automático inteligente baseado em Whitelist ou Blacklist.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.gameMode == null || mc.player.inventoryMenu == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        List<Item> selectedItems = items.get();
        if (selectedItems == null) return;

        int startSlot = ignoreHotbar.get() ? 9 : 0;

        for (int i = startSlot; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            boolean isSelected = selectedItems.contains(item);
            boolean shouldDrop = false;

            if (mode.get() == DropMode.Whitelist) {
                // Em Whitelist, dropamos se NÃO estiver na lista de permitidos
                shouldDrop = !isSelected;
            } else {
                // Em Blacklist, dropamos se ESTIVER na lista de bloqueados
                shouldDrop = isSelected;
            }

            if (shouldDrop) {
                InvUtils.drop().slotId(i);
                timer = delayTicks.get();
                return;
            }
        }
    }
}
