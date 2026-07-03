package com.example.addon.hud;

import com.example.addon.modules.SpectatorGuard;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Elemento de HUD (overlay na tela) que mostra se o SpectatorGuard
 * está ativo ou não. Isso é só um indicador visual — para
 * ligar/desligar o módulo de verdade, use a ClickGUI (veja instruções).
 */
public class SpectatorGuardHud extends HudElement {
    public static final HudGroup GROUP = new HudGroup("Goiabada");

    public static final HudElementInfo<SpectatorGuardHud> INFO = new HudElementInfo<>(
        GROUP,
        "spectator-guard-hud",
        "Displays whether SpectatorGuard is active or not.",
        SpectatorGuardHud::new
    );

    private static final Color ON_COLOR = new Color(80, 220, 100);
    private static final Color OFF_COLOR = new Color(220, 80, 80);

    public SpectatorGuardHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        SpectatorGuard module = Modules.get().get(SpectatorGuard.class);
        boolean active = module != null && module.isActive();

        String text = "SpectatorGuard: " + (active ? "ON" : "OFF");
        Color color = active ? ON_COLOR : OFF_COLOR;

        double width = renderer.textWidth(text, true);
        double height = renderer.textHeight(true);
        setSize(width, height);

        if (isInEditor()) {
            renderer.quad(x, y, getWidth(), getHeight(), new Color(0, 0, 0, 120));
        }

        renderer.text(text, x, y, color, true);
    }
}
