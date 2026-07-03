package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class BaritoneQueue extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> tasks = sgGeneral.add(new StringListSetting.Builder()
        .name("tasks")
        .description("A list of commands or chat messages to execute sequentially (e.g. #mine diamond_ore, #goto 0 64 0, /home).")
        .defaultValue(List.of("#mine diamond_ore", "#goto 0 64 0"))
        .build()
    );

    private final Setting<Integer> delaySeconds = sgGeneral.add(new IntSetting.Builder()
        .name("delay-seconds")
        .description("The duration in seconds to wait before executing the next command or task in the queue.")
        .defaultValue(10)
        .min(1)
        .max(600)
        .build()
    );

    private final Setting<Boolean> waitForBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("wait-for-baritone")
        .description("If enabled and the command starts with '#', the queue will wait for Baritone's completion/termination message in the chat before moving on.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> loop = sgGeneral.add(new BoolSetting.Builder()
        .name("loop")
        .description("Whether to restart the task queue from the first item once all tasks have been completed.")
        .defaultValue(false)
        .build()
    );

    private int currentIndex = 0;
    private int timerTicks = 0;
    private boolean taskStarted = false;

    public BaritoneQueue() {
        super(AddonTemplate.CATEGORY, "baritone-queue", "Executes a sequential list of Baritone tasks and standard chat commands.");
    }

    @Override
    public void onActivate() {
        currentIndex = 0;
        timerTicks = 20; // 1 segundo de espera antes de iniciar a primeira tarefa
        taskStarted = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        List<String> list = tasks.get();
        if (list == null || list.isEmpty()) {
            warning("The task queue list is empty.");
            toggle();
            return;
        }

        if (currentIndex >= list.size()) {
            if (loop.get()) {
                info("Queue finished. Restarting in loop...");
                currentIndex = 0;
            } else {
                info("Baritone task queue successfully completed!");
                toggle();
                return;
            }
        }

        if (timerTicks > 0) {
            timerTicks--;
            return;
        }

        String currentTask = list.get(currentIndex);
        if (currentTask == null || currentTask.trim().isEmpty()) {
            advance();
            return;
        }

        if (!taskStarted) {
            info("Starting task [" + (currentIndex + 1) + "/" + list.size() + "]: " + currentTask);
            ChatUtils.sendPlayerMsg(currentTask.trim());
            taskStarted = true;

            // Se não for comando do Baritone ou se não devemos aguardar o Baritone no chat, usamos o temporizador
            if (!currentTask.trim().startsWith("#") || !waitForBaritone.get()) {
                timerTicks = delaySeconds.get() * 20;
            } else {
                // Se for comando Baritone, definimos um tempo máximo de segurança (ex: 5 minutos ou delaySeconds * 10)
                timerTicks = Math.max(delaySeconds.get() * 20, 1200);
            }
        } else {
            // Se já começou e o temporizador chegou a zero, avançamos
            advance();
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!taskStarted || mc.player == null) return;

        List<String> list = tasks.get();
        if (list == null || currentIndex >= list.size()) return;

        String currentTask = list.get(currentIndex);
        if (currentTask != null && currentTask.trim().startsWith("#") && waitForBaritone.get()) {
            String msg = event.getMessage().getString();
            if (msg.contains("[Baritone]") && (
                msg.contains("Done") || msg.contains("Pathing complete") ||
                msg.contains("Canceled") || msg.contains("Unable to find")
            )) {
                info("Baritone completion detected. Advancing to the next task...");
                advance();
            }
        }
    }

    private void advance() {
        currentIndex++;
        taskStarted = false;
        timerTicks = delaySeconds.get() * 20;
    }
}
