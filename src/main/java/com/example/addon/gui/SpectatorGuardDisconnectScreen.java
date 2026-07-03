package com.example.addon.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class SpectatorGuardDisconnectScreen extends DisconnectedScreen {
    private final ServerData serverData;

    public SpectatorGuardDisconnectScreen(Screen parent, Component title, Component reason, ServerData serverData) {
        super(parent, title, reason);
        this.serverData = serverData;
    }

    @Override
    protected void init() {
        super.init();

        for (net.minecraft.client.gui.components.events.GuiEventListener listener : new ArrayList<>(this.children())) {
            if (listener instanceof Button button) {
                this.removeWidget(button);
            }
        }

        int btnY = this.height / 2 + 40;
        boolean canReconnect = (serverData != null && serverData.ip != null && !serverData.ip.isEmpty());

        Button reconnectBtn = Button.builder(
            Component.literal(canReconnect ? "Reconectar / Reconnect" : "Reconectar (Sem Dados do Servidor)"),
            button -> {
                if (canReconnect && this.minecraft != null) {
                    ConnectScreen.startConnecting(
                        new TitleScreen(),
                        this.minecraft,
                        ServerAddress.parseString(serverData.ip),
                        serverData,
                        false,
                        null
                    );
                }
            }
        ).bounds(this.width / 2 - 100, btnY, 200, 20).build();

        reconnectBtn.active = canReconnect;
        this.addRenderableWidget(reconnectBtn);

        Button serverListBtn = Button.builder(
            Component.literal("Lista de Servidores / Go to Server List"),
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
                }
            }
        ).bounds(this.width / 2 - 100, btnY + 24, 200, 20).build();

        this.addRenderableWidget(serverListBtn);
    }
}
