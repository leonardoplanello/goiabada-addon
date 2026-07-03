package com.example.addon;

import com.example.addon.hud.SpectatorGuardHud;
import com.example.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    // Categoria que vai aparecer na ClickGUI (Right Shift no jogo)

    public static final Category CATEGORY = new Category("Goiabada");

    @Override
    public void onInitialize() {
        // Registra os módulos -> eles já aparecem automaticamente na ClickGUI
        Modules.get().add(new SpectatorGuard());
        Modules.get().add(new Defense());
        Modules.get().add(new AutoShield());
        Modules.get().add(new RestrictedArea());
        Modules.get().add(new BaritoneQueue());
        Modules.get().add(new SmartDrop());

        // Registra o elemento de HUD (overlay opcional mostrando ON/OFF)
        Hud.get().register(SpectatorGuardHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
