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
        Radius("Radius"),
        BoundingBox("Bounding Box");

        private final String name;
        AreaMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgZone = settings.createGroup("Zone");
    private final SettingGroup sgExceptions = settings.createGroup("Exceptions");

    private final Setting<AreaMode> mode = sgGeneral.add(new EnumSetting.Builder<AreaMode>()
        .name("mode")
        .description("Selects how the restricted area zone is defined: Radius (circle around center) or Bounding Box (cuboid coordinates).")
        .defaultValue(AreaMode.Radius)
        .build()
    );

    // Radius Mode
    private final Setting<Double> centerX = sgZone.add(new DoubleSetting.Builder()
        .name("center-x")
        .description("X coordinate for the center of the circular restricted zone.")
        .defaultValue(0.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    private final Setting<Double> centerZ = sgZone.add(new DoubleSetting.Builder()
        .name("center-z")
        .description("Z coordinate for the center of the circular restricted zone.")
        .defaultValue(0.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    private final Setting<Double> radius = sgZone.add(new DoubleSetting.Builder()
        .name("radius")
        .description("The radius of the restricted zone in blocks.")
        .defaultValue(50.0)
        .min(1.0)
        .visible(() -> mode.get() == AreaMode.Radius)
        .build()
    );

    // BoundingBox Mode
    private final Setting<Double> minX = sgZone.add(new DoubleSetting.Builder()
        .name("min-x")
        .description("Minimum X coordinate of the bounding box restricted zone.")
        .defaultValue(-50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> maxX = sgZone.add(new DoubleSetting.Builder()
        .name("max-x")
        .description("Maximum X coordinate of the bounding box restricted zone.")
        .defaultValue(50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> minZ = sgZone.add(new DoubleSetting.Builder()
        .name("min-z")
        .description("Minimum Z coordinate of the bounding box restricted zone.")
        .defaultValue(-50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<Double> maxZ = sgZone.add(new DoubleSetting.Builder()
        .name("max-z")
        .description("Maximum Z coordinate of the bounding box restricted zone.")
        .defaultValue(50.0)
        .visible(() -> mode.get() == AreaMode.BoundingBox)
        .build()
    );

    private final Setting<List<String>> exceptions = sgExceptions.add(new StringListSetting.Builder()
        .name("exceptions")
        .description("List of module names or titles that will NOT be disabled when inside the restricted area.")
        .defaultValue(List.of("click-gui", "hud", "restricted-area", "spectator-guard"))
        .build()
    );

    private boolean wasInside = false;

    public RestrictedArea() {
        super(AddonTemplate.CATEGORY, "restricted-area", "Automatically disables all active modules (except specified exclusions) when inside a defined restricted zone.");
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
                warning("You entered the Restricted Area! Disabling unauthorized modules.");
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
                info("Module " + mod.title + " disabled by the Restricted Area.");
            }
        }
    }
}
