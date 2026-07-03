# Goiabada Addon for Meteor Client

![Meteor Client Addon](https://img.shields.io/badge/Meteor%20Client-Addon-purple?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-CC0-blue?style=for-the-badge)

**Goiabada Addon** is a feature-packed utility, survival automation, and security addon for [Meteor Client](https://meteorclient.com/). Designed to enhance survival gameplay, automate tasks, and protect players against unexpected threats or server checks.

---

## ✨ Features & Modules

All modules can be found inside the **Goiabada** category in the Meteor Client ClickGUI (Right Shift).

### 🛡️ 1. SpectatorGuard (`spectator-guard`)
Monitors the server tab-list and automatically disconnects your client if another player or server admin switches to **Creative** or **Spectator** mode.
- **Custom Disconnect Screen:** Displays the exact detection reason and includes convenient quick-reconnect buttons.
- **HUD Element (`spectator-guard-hud`):** Displays real-time status (ON/OFF) overlay directly on your screen.
- **Settings:**
  - `on-creative`: Disconnect when another player is detected in Creative mode.
  - `on-spectator`: Disconnect when another player is detected in Spectator mode.

---

### ⚡ 2. SmartEventAction (`smart-event-action`)
An advanced event-triggered automation system that performs defensive actions when critical health or damage conditions are met.
- **Triggers:**
  - `mob-damage`: Triggers upon taking damage from monsters/mobs.
  - `player-damage`: Triggers upon taking damage from other players.
  - `any-damage`: Triggers upon taking any kind of damage.
  - `low-health`: Triggers when health falls below a configurable threshold (`health-threshold`).
  - `low-hunger`: Triggers when hunger falls below a configurable threshold (`hunger-threshold`).
- **Actions:**
  - `pause-baritone`: Automatically pauses any active Baritone pathing or mining (`#pause`).
  - `toggle-module`: Toggles any specified Meteor module on/off by name (e.g., `AutoEscape`, `killaura`).
  - `custom-command`: Executes custom chat commands or messages (e.g., `/home`, `#goto 0 64 0`).
  - `cooldown-ticks`: Anti-spam timer to prevent consecutive command floods.

---

### 🗡️ 3. AutoShield (`auto-shield`)
Continuously keeps your shield raised (holds Right Click) while intelligently managing inventory and player interactions.
- **Settings:**
  - `only-when-equipped`: Only holds right-click when a shield is held in main hand or offhand.
  - `pause-on-eat`: Automatically releases shield when eating or when `AutoEat` becomes active.
  - `pause-in-screens`: Automatically pauses inside open GUI screens (Chests, Inventory, Chat) to prevent input conflicts.

---

### 🚫 4. RestrictedArea (`restricted-area`)
Automatically deactivates selected modules when stepping into restricted zones (such as spawn areas, PvP-free zones, or anti-cheat inspection zones).
- **Settings:**
  - `mode`: Choose between **Radius** (circular area from a center point) or **Bounding Box** (box coordinates).
  - `radius` / `center-x` / `center-z`: Define circular restricted zones.
  - `exceptions`: List of modules exempt from deactivation when entering the area.

---

### 🔄 5. BaritoneQueue (`baritone-queue`)
Executes a customized, sequential list of Baritone instructions and chat commands automatically.
- **Settings:**
  - `tasks`: Ordered list of commands (e.g., `["#mine diamond_ore", "#goto 0 64 0", "/home"]`).
  - `wait-for-baritone`: Waits for Baritone chat completion messages before proceeding to the next `#` command.
  - `delay-seconds`: Custom delay between standard commands or task transitions.
  - `loop`: Optionally restarts the queue from the beginning once finished.

---

### 📦 6. SmartDrop (`smart-drop`)
Intelligent inventory management module that drops items gradually to avoid getting kicked by anti-cheat rate limiters.
- **Settings:**
  - `mode`: Choose between **Whitelist** (drops everything except permitted items) or **Blacklist** (drops only selected items).
  - `items`: Configurable item list.
  - `delay-ticks`: Tick delay between drops to bypass inventory anti-cheat limits.
  - `ignore-hotbar`: Protects hotbar items from being dropped.

---

## 🖥️ HUD Elements

Goiabada registers custom HUD elements under the **Goiabada** group in the HUD editor:
- **`spectator-guard-hud`**: A clean visual overlay indicating whether `SpectatorGuard` is currently active (`ON` in green or `OFF` in red).

---

## 📥 Installation

1. Download and install **[Fabric Loader](https://fabricmc.net/)** for your Minecraft version.
2. Download and place the **[Meteor Client](https://meteorclient.com/)** mod inside your `.minecraft/mods` folder.
3. Download the compiled **Goiabada Addon `.jar`** file from Releases or build it locally.
4. Place the Goiabada Addon `.jar` file inside your `.minecraft/mods` folder.
5. Launch Minecraft! Open the ClickGUI (default: `Right Shift`) and look for the **Goiabada** category.

---

## 🛠️ Building from Source

To compile the addon yourself using Gradle:

### Prerequisites
- **Java JDK 21** or newer.
- Git.

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/goiabada-addon.git
   cd goiabada-addon
   ```

2. **Build with Gradle:**
   - **Windows:**
     ```cmd
     gradlew.bat build
     ```
   - **Linux / macOS:**
     ```bash
     ./gradlew build
     ```

3. Locate the built JAR file inside the `build/libs/` directory and copy it to your `mods` folder.

---

## 📜 License

This project is available under the **CC0 License**. Feel free to use, modify, and distribute for your own projects.
