package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class RestrictedArea extends Module {
    public enum AreaMode {
        Radius("Raio"),
        BoundingBox("Caixa Delimitadora");

        private final String name;
        AreaMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgZone = settings.createGroup("Zona");
    private final SettingGroup sgExceptions = settings.createGroup("Exceções");

    private final Setting<AreaMode> mode = sgGeneral.add(new EnumSetting.Builder<AreaMode>()
        .name("mode")
        .description("Modo de definição da zona restrita.")
        .defaultValue(AreaMode.Radius)
        .build()
    );

    // Modo Raio
    private final Setting<Double> centerX = sgZone.add(new DoubleSetting.Builder()
        .name("center-x")
        .description("Coordenada X do centro da zona.")
        .defaultValue(0.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    private final Setting<Double> centerZ = sgZone.add(new DoubleSetting.Builder()
        .name("center-z")
        .description("Coordenada Z do centro da zona.")
        .defaultValue(0.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    private final Setting<Double> radius = sgZone.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Raio da zona restrita em blocos.")
        .defaultValue(50.0)
        .min(1.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    // Modo BoundingBox
    private final Setting<Double> minX = sgZone.add(new DoubleSetting.Builder()
        .name("min-x")
        .description("Coordenada mínima X da zona.")
        .defaultValue(-50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> maxX = sgZone.add(new DoubleSetting.Builder()
        .name("max-x")
        .description("Coordenada máxima X da zona.")
        .defaultValue(50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> minZ = sgZone.add(new DoubleSetting.Builder()
        .name("min-z")
        .description("Coordenada mínima Z da zona.")
        .defaultValue(-50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> maxZ = sgZone.add(new DoubleSetting.Builder()
        .name("max-z")
        .description("Coordenada máxima Z da zona.")
        .defaultValue(50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<List<String>> exceptions = sgExceptions.add(new StringListSetting.Builder()
        .name("exceptions")
        .description("Lista de nomes (ou títulos) dos módulos que NÃO serão desativados ao entrar na área.")
        .defaultValue(List.of("click-gui", "hud", "restricted-area", "spectator-guard"))
        .build()
    );

    private boolean wasInside = false;

    public RestrictedArea() {
        super(AddonTemplate.CATEGORY, "restricted-area", "Desativa automaticamente todos os módulos ao entrar em uma zona restrita.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        boolean inside = false;
        if (mode.get() == AreaMode.Radius) {
            double dist = Math.hypot(px - centerX.get(), pz - centerZ.get());
            inside = (dist <= radius.get());
        } else {
            inside = (px >= minX.get() && px <= maxX.get() && pz >= minZ.get() && pz <= maxZ.get());
        }

        if (inside) {
            if (!wasInside) {
                warning("Você entrou na Área Restrita! Desativando módulos não permitidos.");
            }
            disableModules();
        }

        wasInside = inside;
    }

    private void disableModules() {
        List<String> exemptList = exceptions.get();

        for (Module mod : Modules.get().getAll()) {
            if (!mod.isActive() || mod == this) continue;

            boolean isExempt = false;
            if (exemptList != null) {
                for (String ex : exemptList) {
                    if (ex == null) continue;
                    String cleanEx = ex.trim();
                    if (mod.name.equalsIgnoreCase(cleanEx) || mod.title.equalsIgnoreCase(cleanEx)) {
                        isExempt = true;
                        break;
                    }
                }
            }

            if (!isExempt) {
                mod.toggle();
                info("Módulo " + mod.title + " desativado pela Área Restrita.");
            }
        }
    }
}
