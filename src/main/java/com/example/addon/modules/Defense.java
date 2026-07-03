package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.AutoGap;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Defense extends Module {
    public enum State {
        IDLE,
        DEFENDING_MOB,
        RESCUING_LAVA,
        COOLDOWN_RESUME
    }

    public enum MoveType {
        Velocity,
        Packet
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgProtection = settings.createGroup("Protection");
    private final SettingGroup sgDodge = settings.createGroup("Dodge");

    // General
    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Automatically pauses active Baritone processes (#pause) when in danger and resumes them (#resume) when safety is restored.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> resumeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("resume-delay-seconds")
        .description("The safety cooldown duration in seconds with no threats present before resuming Baritone.")
        .defaultValue(2)
        .min(1)
        .max(15)
        .build()
    );

    // Protection
    private final Setting<Boolean> protectMobs = sgProtection.add(new BoolSetting.Builder()
        .name("protect-mobs")
        .description("Enables intelligent defense actions, such as tactical movement, shield blocking, and speed boosts, when hostile mobs are nearby.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> dangerRadius = sgProtection.add(new DoubleSetting.Builder()
        .name("danger-radius")
        .description("The radius in blocks within which hostile mobs are detected to trigger defense mode.")
        .defaultValue(6.0)
        .min(2.0)
        .max(15.0)
        .visible(protectMobs::get)
        .build()
    );

    private final Setting<Boolean> fleeMobs = sgProtection.add(new BoolSetting.Builder()
        .name("flee-mobs")
        .description("Commands Baritone to run away from nearby hostile mobs, navigating safely by avoiding holes, lava, and other environmental hazards.")
        .defaultValue(true)
        .visible(protectMobs::get)
        .build()
    );

    private final Setting<Double> fleeSpeedVal = sgProtection.add(new DoubleSetting.Builder()
        .name("flee-speed")
        .description("The speed multiplier temporarily applied to Meteor's Speed module when fleeing from threats.")
        .defaultValue(8.0)
        .min(1.0)
        .max(20.0)
        .visible(fleeMobs::get)
        .build()
    );

    private final Setting<Boolean> protectPlayers = sgProtection.add(new BoolSetting.Builder()
        .name("protect-players")
        .description("Enables tactical defense and evasion when attacked by other players.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> protectLava = sgProtection.add(new BoolSetting.Builder()
        .name("protect-lava")
        .description("Enables automatic water bucket rescue placement and emergency jumping when falling into lava or fire.")
        .defaultValue(true)
        .build()
    );

    // Dodge
    private final Setting<Boolean> dodgeMobs = sgDodge.add(new BoolSetting.Builder()
        .name("dodge-mobs")
        .description("Enables rapid dodge/evasion movement (Arrow Dodge style) to escape close-range melee monsters.")
        .defaultValue(true)
        .build()
    );

    private final Setting<MoveType> moveType = sgDodge.add(new EnumSetting.Builder<MoveType>()
        .name("move-type")
        .description("The movement implementation method used to dodge mobs: Velocity (directly modifies player velocity) or Packet (sends movement packets to the server).")
        .defaultValue(MoveType.Velocity)
        .visible(dodgeMobs::get)
        .build()
    );

    private final Setting<Double> moveSpeed = sgDodge.add(new DoubleSetting.Builder()
        .name("dodge-speed")
        .description("The speed/velocity step size applied when performing dodge evasion movements.")
        .defaultValue(1.0)
        .min(0.01)
        .sliderRange(0.01, 5.0)
        .visible(dodgeMobs::get)
        .build()
    );

    private final Setting<Double> distanceCheck = sgDodge.add(new DoubleSetting.Builder()
        .name("distance-check")
        .description("The safety threshold distance in blocks; triggers a dodge when a hostile mob is closer than this distance.")
        .defaultValue(4.0)
        .min(1.0)
        .sliderRange(1.0, 10.0)
        .visible(dodgeMobs::get)
        .build()
    );

    private final Setting<Boolean> groundCheck = sgDodge.add(new BoolSetting.Builder()
        .name("ground-check")
        .description("Checks if the destination ground is solid to prevent dodging off high cliffs or into holes.")
        .defaultValue(true)
        .visible(dodgeMobs::get)
        .build()
    );

    private State currentState = State.IDLE;
    private float lastHealth = -1f;
    private int safetyTimer = 0;
    private boolean pausedByUs = false;
    private final List<Vec3> possibleMoveDirections = Arrays.asList(
        new Vec3(1, 0, 1),
        new Vec3(0, 0, 1),
        new Vec3(-1, 0, 1),
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
        new Vec3(1, 0, -1),
        new Vec3(0, 0, -1),
        new Vec3(-1, 0, -1)
    );

    private int strafeTick = 0;
    private BlockPos lastDangerPos = null;
    private int fleeTicks = 0;
    private Object previousGoal = null;

    // Estados e configurações anteriores dos módulos Meteor
    private boolean wasSpeedActive = false;
    private boolean wasVelocityActive = false;
    private double originalSpeedVal = 1.0;

    private boolean holdingShield = false;
    private boolean holdingJump = false;
    private boolean holdingForward = false;
    private boolean holdingBack = false;
    private boolean holdingLeft = false;
    private boolean holdingRight = false;

    public Defense() {
        super(AddonTemplate.CATEGORY, "defense", "Protects the player against mobs, lava, and general hazards using a shield, lava rescue, and speed boosts.");
    }

    @Override
    public void onActivate() {
        currentState = State.IDLE;
        if (mc.player != null) {
            lastHealth = mc.player.getHealth();
        } else {
            lastHealth = -1f;
        }
        safetyTimer = 0;
        pausedByUs = false;
        strafeTick = 0;
        lastDangerPos = null;
        fleeTicks = 0;
        previousGoal = null;
        releaseControls();
    }

    @Override
    public void onDeactivate() {
        releaseControls();
        stopBaritoneFlee();
        restoreModules();
        if (pausedByUs && pauseBaritone.get()) {
            ChatUtils.sendPlayerMsg("#resume");
            pausedByUs = false;
        }
        currentState = State.IDLE;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        float currentHealth = mc.player.getHealth();
        boolean tookDamage = (lastHealth >= 0 && currentHealth < lastHealth);
        lastHealth = currentHealth;

        strafeTick++;

        // 1. Avaliar Ameaças Imediatas
        boolean inLavaOrFire = protectLava.get() && (mc.player.isInLava() || mc.player.isOnFire());
        LivingEntity closestMob = findClosestHostile(dangerRadius.get());
        boolean mobDanger = protectMobs.get() && (closestMob != null || (tookDamage && mc.player.getLastAttacker() instanceof Monster));
        boolean playerDanger = protectPlayers.get() && (tookDamage && mc.player.getLastAttacker() instanceof Player);

        // Máquina de Estados
        switch (currentState) {
            case IDLE -> {
                if (inLavaOrFire) {
                    enterDefenseState(State.RESCUING_LAVA);
                } else if (mobDanger || playerDanger) {
                    activateModules();
                    enterDefenseState(State.DEFENDING_MOB);
                    if (fleeMobs.get() && closestMob != null) {
                        lastDangerPos = closestMob.blockPosition();
                        fleeTicks = 0;
                        startBaritoneFlee(lastDangerPos);
                    }
                }
            }
            case RESCUING_LAVA -> {
                if (!mc.player.isInLava() && !mc.player.isOnFire()) {
                    releaseControls();
                    restoreModules();
                    currentState = State.COOLDOWN_RESUME;
                    safetyTimer = resumeDelay.get() * 20;
                    info("Lava/fire cleared! Waiting for safe conditions to resume.");
                } else {
                    handleLavaRescue();
                }
            }
            case DEFENDING_MOB -> {
                if (inLavaOrFire) {
                    releaseControls();
                    stopBaritoneFlee();
                    enterDefenseState(State.RESCUING_LAVA);
                    return;
                }
                LivingEntity target = closestMob;
                if (target == null && mc.player.getLastAttacker() instanceof LivingEntity le && le.isAlive()) {
                    if (mc.player.distanceTo(le) <= dangerRadius.get() * 1.5) {
                        target = le;
                    }
                }
                if (target == null || !target.isAlive()) {
                    releaseControls();
                    stopBaritoneFlee();
                    restoreModules();
                    currentState = State.COOLDOWN_RESUME;
                    safetyTimer = resumeDelay.get() * 20;
                    info("Threat cleared! Waiting for safety cooldown to resume.");
                } else {
                    handleMobDefense(target);
                    if (fleeMobs.get()) {
                        fleeTicks++;
                        if (fleeTicks % 20 == 0 || lastDangerPos == null || target.blockPosition().distSqr(lastDangerPos) > 4) {
                            lastDangerPos = target.blockPosition();
                            startBaritoneFlee(lastDangerPos);
                        }
                    }
                }
            }
            case COOLDOWN_RESUME -> {
                if (inLavaOrFire) {
                    enterDefenseState(State.RESCUING_LAVA);
                    return;
                }
                if (mobDanger || playerDanger) {
                    activateModules();
                    enterDefenseState(State.DEFENDING_MOB);
                    if (fleeMobs.get() && closestMob != null) {
                        lastDangerPos = closestMob.blockPosition();
                        fleeTicks = 0;
                        startBaritoneFlee(lastDangerPos);
                    }
                    return;
                }
                if (safetyTimer > 0) {
                    safetyTimer--;
                } else {
                    info("Safety restored.");
                    if (pausedByUs && pauseBaritone.get()) {
                        ChatUtils.sendPlayerMsg("#resume");
                        pausedByUs = false;
                    }
                    currentState = State.IDLE;
                }
            }
        }
    }

    private void enterDefenseState(State newState) {
        info("Entering defense state: " + newState);
        currentState = newState;
        if (!pausedByUs && pauseBaritone.get()) {
            if (newState == State.RESCUING_LAVA || (newState == State.DEFENDING_MOB && !fleeMobs.get())) {
                ChatUtils.sendPlayerMsg("#pause");
                pausedByUs = true;
            }
        }
    }

    private Object getBaritoneGoal() {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object customGoalProcess = primary.getClass().getMethod("getCustomGoalProcess").invoke(primary);
            return customGoalProcess.getClass().getMethod("getGoal").invoke(customGoalProcess);
        } catch (Exception e) {
            return null;
        }
    }

    private void startBaritoneFlee(BlockPos dangerPos) {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object customGoalProcess = primary.getClass().getMethod("getCustomGoalProcess").invoke(primary);

            Object currentGoal = getBaritoneGoal();
            if (currentGoal != null && !currentGoal.getClass().getName().contains("GoalRunAway")) {
                previousGoal = currentGoal;
                info("Saving previous Baritone goal: " + currentGoal.getClass().getSimpleName());
            }

            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.GoalRunAway");
            java.lang.reflect.Constructor<?> constructor = goalClass.getConstructor(double.class, BlockPos[].class);

            double distance = dangerRadius.get() * 1.5;
            Object goalInstance = constructor.newInstance(distance, new BlockPos[] { dangerPos });

            Class<?> goalInterface = Class.forName("baritone.api.pathing.goals.Goal");
            java.lang.reflect.Method setGoalAndPathMethod = customGoalProcess.getClass().getMethod("setGoalAndPath", goalInterface);

            setGoalAndPathMethod.invoke(customGoalProcess, goalInstance);
        } catch (NoClassDefFoundError | Exception e) {
            info("Error starting Baritone flee: " + e.getMessage());
        }
    }

    private void stopBaritoneFlee() {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object customGoalProcess = primary.getClass().getMethod("getCustomGoalProcess").invoke(primary);

            Class<?> goalInterface = Class.forName("baritone.api.pathing.goals.Goal");

            if (previousGoal != null) {
                info("Restoring previous Baritone goal...");
                java.lang.reflect.Method setGoalAndPathMethod = customGoalProcess.getClass().getMethod("setGoalAndPath", goalInterface);
                setGoalAndPathMethod.invoke(customGoalProcess, previousGoal);
                previousGoal = null;
            } else {
                java.lang.reflect.Method setGoalMethod = customGoalProcess.getClass().getMethod("setGoal", goalInterface);
                setGoalMethod.invoke(customGoalProcess, (Object) null);
            }
            
            lastDangerPos = null;
            fleeTicks = 0;
        } catch (NoClassDefFoundError | Exception ignored) {}
    }

    private void activateModules() {
        Module speedModule = Modules.get().get("speed");
        Module velocityModule = Modules.get().get("velocity");

        if (speedModule instanceof Speed sm) {
            wasSpeedActive = sm.isActive();
            if (sm.speedMode.get() == SpeedModes.Vanilla) {
                originalSpeedVal = sm.vanillaSpeed.get();
                sm.vanillaSpeed.set(fleeSpeedVal.get());
            } else if (sm.speedMode.get() == SpeedModes.Strafe) {
                originalSpeedVal = sm.ncpSpeed.get();
                sm.ncpSpeed.set(fleeSpeedVal.get());
            }
            if (!wasSpeedActive) {
                sm.toggle();
            }
        }

        if (velocityModule != null) {
            wasVelocityActive = velocityModule.isActive();
            if (!wasVelocityActive) {
                velocityModule.toggle();
            }
        }
    }

    private void restoreModules() {
        Module speedModule = Modules.get().get("speed");
        Module velocityModule = Modules.get().get("velocity");

        if (speedModule instanceof Speed sm) {
            if (!wasSpeedActive && sm.isActive()) {
                sm.toggle();
            }
            if (sm.speedMode.get() == SpeedModes.Vanilla) {
                sm.vanillaSpeed.set(originalSpeedVal);
            } else if (sm.speedMode.get() == SpeedModes.Strafe) {
                sm.ncpSpeed.set(originalSpeedVal);
            }
        }

        if (velocityModule != null) {
            if (!wasVelocityActive && velocityModule.isActive()) {
                velocityModule.toggle();
            }
        }
    }



    private void handleLavaRescue() {
        if (protectLava.get()) {
            int waterSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.WATER_BUCKET)) {
                    waterSlot = i;
                    break;
                }
            }
            if (waterSlot != -1) {
                InvUtils.swap(waterSlot, false);
                Rotations.rotate(mc.player.getYRot(), 90);
                if (mc.options != null) {
                    mc.options.keyUse.setDown(true);
                    holdingShield = true;
                }
            }
        }

        if (mc.options != null) {
            mc.options.keyJump.setDown(true);
            holdingJump = true;
            mc.options.keyUp.setDown(true);
            holdingForward = true;
        }
    }

    private void handleMobDefense(LivingEntity target) {
        if (mc.player == null || mc.options == null) return;

        // Combat (Aim and attack the mob if nearby)
        double cdx = target.getX() - mc.player.getX();
        double cdy = target.getEyeY() - mc.player.getEyeY();
        double cdz = target.getZ() - mc.player.getZ();
        double cdist = Math.hypot(cdx, cdz);
        float attackYaw = (float) (Math.toDegrees(Math.atan2(cdz, cdx)) - 90.0);
        float attackPitch = (float) -Math.toDegrees(Math.atan2(cdy, cdist));
        Rotations.rotate(attackYaw, attackPitch);

        float cooldown = mc.player.getAttackStrengthScale(0.5f);
        boolean canAttack = cooldown >= 0.9f && mc.player.distanceTo(target) <= 4.5f && !isEating();

        if (canAttack) {
            if (holdingShield) {
                mc.options.keyUse.setDown(false);
                holdingShield = false;
            }
            if (mc.gameMode != null) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            boolean hasShield = mc.player.getMainHandItem().is(Items.SHIELD) || mc.player.getOffhandItem().is(Items.SHIELD);
            if (hasShield && !isEating()) {
                mc.options.keyUse.setDown(true);
                holdingShield = true;
            } else if (holdingShield && !isEating()) {
                mc.options.keyUse.setDown(false);
                holdingShield = false;
            }
        }
    }

    private boolean isEating() {
        if (mc.player == null) return false;

        // Check if player is currently eating or drinking a consumable
        if (mc.player.isUsingItem()) {
            ItemStack useItem = mc.player.getUseItem();
            if (!useItem.isEmpty() && (useItem.get(DataComponents.FOOD) != null || useItem.is(Items.POTION) || useItem.is(Items.MILK_BUCKET))) {
                return true;
            }
        }

        // Check if the AutoEat module is active and eating
        try {
            AutoEat autoEat = Modules.get().get(AutoEat.class);
            if (autoEat != null && autoEat.isActive() && autoEat.eating) {
                return true;
            }
        } catch (NoClassDefFoundError | Exception ignored) {}

        // Check if the AutoGap module is active and eating
        try {
            AutoGap autoGap = Modules.get().get(AutoGap.class);
            if (autoGap != null && autoGap.isEating()) {
                return true;
            }
        } catch (NoClassDefFoundError | Exception ignored) {}

        return false;
    }

    private LivingEntity findClosestHostile(double radius) {
        if (mc.level == null || mc.player == null) return null;
        if (!protectMobs.get()) return null;
        LivingEntity closest = null;
        double minDist = radius;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof LivingEntity le) || !le.isAlive()) continue;

            boolean isHostile = (le instanceof Monster || le instanceof Slime || le instanceof Hoglin);
            if (!isHostile && protectPlayers.get() && le instanceof Player) {
                isHostile = true;
            }

            if (isHostile) {
                float dist = mc.player.distanceTo(le);
                if (dist <= minDist) {
                    minDist = dist;
                    closest = le;
                }
            }
        }
        return closest;
    }

    private void releaseControls() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            if (holdingShield) {
                mc.options.keyUse.setDown(false);
                holdingShield = false;
            }
            holdingForward = false;
            holdingBack = false;
            holdingLeft = false;
            holdingRight = false;
            holdingJump = false;
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || !isActive()) return;
        if (!dodgeMobs.get()) return;

        if (isDodgeValid(Vec3.ZERO, false)) return; // no need to move

        double speed = moveSpeed.get();
        for (int i = 0; i < 500; i++) { // it's not a while loop so it doesn't freeze if something is wrong
            boolean didMove = false;
            Collections.shuffle(possibleMoveDirections); // Make the direction unpredictable
            for (Vec3 direction : possibleMoveDirections) {
                Vec3 velocity = direction.scale(speed);
                if (isDodgeValid(velocity, true)) {
                    dodgeMove(velocity);
                    didMove = true;
                    break;
                }
            }
            if (didMove) break;
            speed += moveSpeed.get(); // move further
        }
    }

    private void dodgeMove(Vec3 vel) {
        dodgeMove(vel.x, vel.y, vel.z);
    }

    private void dodgeMove(double velX, double velY, double velZ) {
        switch (moveType.get()) {
            case Velocity -> mc.player.setDeltaMovement(velX, velY, velZ);
            case Packet -> {
                Vec3 newPos = mc.player.position().add(velX, velY, velZ);
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, false, mc.player.horizontalCollision));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y - 0.01, newPos.z, true, mc.player.horizontalCollision));
                mc.player.setPos(newPos.x, newPos.y, newPos.z); // Update client position to prevent rubberbanding/desync
            }
        }
    }

    private boolean isDodgeValid(Vec3 velocity, boolean checkGround) {
        Vec3 playerPos = mc.player.position().add(velocity);
        Vec3 headPos = playerPos.add(0, 1, 0);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isNonRangedMob(entity)) continue;

            Vec3 mobPos = entity.position();
            if (mobPos.closerThan(playerPos, distanceCheck.get())) return false;
            if (mobPos.closerThan(headPos, distanceCheck.get())) return false;
        }

        if (checkGround) {
            BlockPos blockPos = mc.player.blockPosition().offset(BlockPos.containing(velocity.x, velocity.y, velocity.z));

            // check if target pos is air
            if (!mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty()) return false;
            else if (!mc.level.getBlockState(blockPos.above()).getCollisionShape(mc.level, blockPos.above()).isEmpty())
                return false;

            if (groundCheck.get()) {
                // check if ground under target is solid
                return !mc.level.getBlockState(blockPos.below()).getCollisionShape(mc.level, blockPos.below()).isEmpty();
            }
        }

        return true;
    }

    private boolean isNonRangedMob(Entity entity) {
        if (!(entity instanceof LivingEntity le) || !le.isAlive()) return false;
        if (entity == mc.player) return false;

        boolean isHostile = (entity instanceof Monster || entity instanceof Slime || entity instanceof Hoglin);
        if (!isHostile && protectPlayers.get() && entity instanceof Player) {
            isHostile = true;
        }

        if (!isHostile) return false;

        // Exclude ranged/projectile shooting mobs
        if (entity instanceof RangedAttackMob) return false;
        if (entity instanceof Ghast) return false;
        if (entity instanceof Blaze) return false;
        if (entity instanceof Witch) return false;

        // Exclude Drowned holding tridents
        if (entity instanceof Drowned drowned) {
            if (drowned.getMainHandItem().is(Items.TRIDENT) || drowned.getOffhandItem().is(Items.TRIDENT)) {
                return false;
            }
        }

        return true;
    }
}
