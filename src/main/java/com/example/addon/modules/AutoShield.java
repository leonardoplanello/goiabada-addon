package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoShield extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyWhenEquipped = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-equipped")
        .description("Segura o clique direito apenas se houver um escudo na mão principal ou secundária.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pausa a defesa com escudo quando precisar comer ou quando o AutoEat estiver ativo.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseInScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-in-screens")
        .description("Pausa a defesa em telas abertas (inventário, baús, chat) para evitar conflitos de clique.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseLookingAtContainers = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-looking-at-containers")
        .description("Pausa a defesa se a visão (mira) estiver apontada para um contêiner (baú, fornalha, etc.).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> strictHandCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("strict-hand-check")
        .description("Funciona somente se a mão principal segurar Picareta, Espada, Machado, Escudo ou estiver vazia.")
        .defaultValue(true)
        .build()
    );

    private boolean wasHolding = false;

    public AutoShield() {
        super(AddonTemplate.CATEGORY, "auto-shield", "Segura o escudo (Hold Right Click) de forma contínua sem conflitar com AutoEat ou baús.");
    }

    @Override
    public void onDeactivate() {
        if (mc.options != null && wasHolding) {
            mc.options.keyUse.setDown(false);
            wasHolding = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        // 1. Verificar telas abertas (Baús, Inventário, Chat)
        if (pauseInScreens.get() && mc.screen != null) {
            releaseKey();
            return;
        }

        // 1.1 Verificar se a mira (visão) do jogador está em um contêiner
        if (pauseLookingAtContainers.get() && mc.hitResult instanceof BlockHitResult blockHit && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            if (state != null) {
                if (state.getMenuProvider(mc.level, blockHit.getBlockPos()) != null ||
                    state.getBlock() instanceof BaseEntityBlock ||
                    state.getBlock() instanceof CraftingTableBlock ||
                    state.getBlock() instanceof AnvilBlock) {
                    releaseKey();
                    return;
                }
            }
        }

        // 1.2 Verificar se a mão principal tem apenas picareta, espada, machado, escudo ou vazia
        if (strictHandCheck.get()) {
            ItemStack main = mc.player.getMainHandItem();
            Item item = main.getItem();
            String name = item.getClass().getSimpleName();
            boolean validMain = main.isEmpty() ||
                                main.is(Items.SHIELD) ||
                                item instanceof AxeItem ||
                                name.contains("Sword") ||
                                name.contains("Pickaxe") ||
                                name.contains("Axe");
            if (!validMain) {
                releaseKey();
                return;
            }
        }

        // 2. Verificar se o jogador está comendo ou se o AutoEat precisa comer
        if (pauseOnEat.get()) {
            if (mc.player.isUsingItem() && !mc.player.getUseItem().is(Items.SHIELD)) {
                releaseKey();
                return;
            }

            Module autoEat = Modules.get().get("auto-eat");
            if (autoEat != null && autoEat.isActive()) {
                // Se o AutoEat está ativo e o jogador está com fome ou usando comida, deixar o AutoEat controlar
                if (mc.player.getFoodData().getFoodLevel() < 20 || (mc.player.getHealth() < mc.player.getMaxHealth() && mc.player.getFoodData().getFoodLevel() >= 18)) {
                    ItemStack main = mc.player.getMainHandItem();
                    ItemStack off = mc.player.getOffhandItem();
                    if ((main != null && main.get(DataComponents.FOOD) != null) || (off != null && off.get(DataComponents.FOOD) != null)) {
                        releaseKey();
                        return;
                    }
                }
            }
        }

        // 3. Verificar se tem escudo equipado se a configuração exigir
        if (onlyWhenEquipped.get()) {
            boolean hasShield = mc.player.getMainHandItem().is(Items.SHIELD) || mc.player.getOffhandItem().is(Items.SHIELD);
            if (!hasShield) {
                releaseKey();
                return;
            }
        }

        // Ativar clique direito (Segurar escudo)
        mc.options.keyUse.setDown(true);
        wasHolding = true;
    }

    private void releaseKey() {
        if (wasHolding) {
            mc.options.keyUse.setDown(false);
            wasHolding = false;
        }
    }
}
