package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.gui.SpectatorGuardDisconnectScreen;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;


/**
 * Disconnects the client when another player on the server is
 * detected in Creative or Spectator mode, based on the tab-list
 * data the server already sends.
 *
 * NOTE: some servers hide/fake tab-list entries for vanished or
 * spectating staff, in which case this module won't see them.
 */
public class SpectatorGuard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onCreative = sgGeneral.add(new BoolSetting.Builder()
        .name("on-creative")
        .description("Disconnect if another player is seen in Creative mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onSpectator = sgGeneral.add(new BoolSetting.Builder()
        .name("on-spectator")
        .description("Disconnect if another player is seen in Spectator mode.")
        .defaultValue(true)
        .build()
    );

    public SpectatorGuard() {
        super(AddonTemplate.CATEGORY, "spectator-guard",
            "Disconnects when another player on the server is Creative or Spectator.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getConnection() == null) return;

        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            // Skip yourself
            if (entry.getProfile().id().equals(mc.player.getUUID())) continue;

            GameType gm = entry.getGameMode();
            if (gm == null) continue;

            boolean shouldDisconnect =
                (gm == GameType.CREATIVE && onCreative.get()) ||
                (gm == GameType.SPECTATOR && onSpectator.get());

            if (shouldDisconnect) {
                warning("Detected " + entry.getProfile().name() + " in " + gm + " — disconnecting.");
                disconnect(entry, gm);
                return;
            }
        }
    }

    private void disconnect(PlayerInfo entry, GameType gm) {
        ServerData serverData = mc.getCurrentServer();
        if (serverData == null && mc.getConnection() != null) {
            serverData = mc.getConnection().getServerData();
        }

        String playerName = entry != null && entry.getProfile() != null ? entry.getProfile().name() : "Unknown";
        String modeName = gm != null ? gm.getName() : "Unknown";

        Component title = Component.literal("§cDisconnected by SpectatorGuard");
        Component reason = Component.literal("§eReason:\n§fDetected player §b" + playerName + " §fin §c" + modeName + "§f mode.");

        if (mc.level != null) {
            mc.level.disconnect(title);
        }
        mc.disconnect(new SpectatorGuardDisconnectScreen(new TitleScreen(), title, reason, serverData), false);
        toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        // Nothing needed here, but handy if you want to reset state on disconnect
    }
}
