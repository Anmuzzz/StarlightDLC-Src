package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.network.SendPacketEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.modules.modules.combat.KillAura;
import com.isusdlc.systems.setting.settings.BindSetting;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.rotations.MoveCorrection;
import com.isusdlc.utility.rotations.Rotation;
import com.isusdlc.utility.rotations.RotationPriority;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
   name = "Elytra Target",
   category = ModuleCategory.MOVEMENT,
   desc = "Target players to elytra!"
)
public class ElytraTarget extends BaseModule {

    public final ModeSetting mode = new ModeSetting(this, "Mode");
    public final ModeSetting.Value normal = new ModeSetting.Value(this.mode, "Обычный").select();
    public final ModeSetting.Value advanced = new ModeSetting.Value(this.mode, "Продвинутый");

    private final SliderSetting sila = new SliderSetting(this, "Отлёт", () -> !this.mode.is(this.advanced))
       .min(5.0F).max(40.0F).step(1.0F).currentValue(8.0F);
    private final SliderSetting silaTime = new SliderSetting(this, "Время отлёта", () -> !this.mode.is(this.advanced))
       .min(200.0F).max(1000.0F).step(10.0F).currentValue(400.0F);

    private final BooleanSetting perelet = new BooleanSetting(this, "Перелёт").enable();
    private final SliderSetting predict = new SliderSetting(this, "Предикт", () -> !this.perelet.isEnabled())
       .min(0.1F).max(6.0F).step(0.1F).currentValue(2.6F);

    private final BooleanSetting leaveHP = new BooleanSetting(this, "Улетать", () -> !this.mode.is(this.advanced)).enable();
    private final BooleanSetting leaveLowHP = new BooleanSetting(this, "Улетать при мало здоровья", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));
    private final BooleanSetting leaveUsingItem = new BooleanSetting(this, "Улетать при исп. предмета", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));
    private final BooleanSetting leaveShieldPush = new BooleanSetting(this, "Улетать при отжиме щита", () -> !(this.leaveHP.isEnabled() && this.mode.is(this.advanced)));

    private final BooleanSetting resolver = new BooleanSetting(this, "Resolver уклонение", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting resolverStrength = new SliderSetting(this, "Сила резольвера", () -> !(this.resolver.isEnabled() && this.mode.is(this.advanced)))
       .min(4.0F).max(30.0F).step(1.0F).currentValue(12.0F);

    public final BooleanSetting prefer = new BooleanSetting(this, "Менять направление", () -> !this.mode.is(this.advanced)).enable();
    public final BindSetting preferBind = new BindSetting(this, "Prefer bind");

    private final BooleanSetting preferNorth = new BooleanSetting(this, "Pref Север", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferSouth = new BooleanSetting(this, "Pref Юг", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferWest = new BooleanSetting(this, "Pref Запад", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferEast = new BooleanSetting(this, "Pref Восток", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferUp = new BooleanSetting(this, "Pref Верх", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced))).enable();
    private final BooleanSetting preferDown = new BooleanSetting(this, "Pref Вниз", () -> !(this.prefer.isEnabled() && this.mode.is(this.advanced)));

    private final BooleanSetting avoidWalls = new BooleanSetting(this, "Избегать стены", () -> !this.mode.is(this.advanced)).enable();
    private final BooleanSetting firework = new BooleanSetting(this, "Отправка фейерверков", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting fireworkTime = new SliderSetting(this, "Время отправки", () -> !(this.firework.isEnabled() && this.mode.is(this.advanced)))
       .min(100.0F).max(2000.0F).step(10.0F).currentValue(800.0F);

    private final BooleanSetting fakelags = new BooleanSetting(this, "Fake Lags", () -> !this.mode.is(this.advanced)).enable();
    private final SliderSetting fakelagsDistance = new SliderSetting(this, "Дистанция флагера", () -> !(this.fakelags.isEnabled() && this.mode.is(this.advanced)))
       .min(5.0F).max(20.0F).step(0.5F).currentValue(10.0F);
    private final SliderSetting fakelagsMaxDistance = new SliderSetting(this, "Макс дистанция флагера", () -> !(this.fakelags.isEnabled() && this.mode.is(this.advanced)))
       .min(15.0F).max(50.0F).step(1.0F).currentValue(30.0F);

    private final CopyOnWriteArrayList<Packet<?>> packetBuffer = new CopyOnWriteArrayList<>();
    private boolean isAccumulatingPackets = false;
    private boolean wasInFarRange = false;
    private LivingEntity currentTarget = null;
    private Vec3d defensivePos = null;
    private long defensiveTimer = 0L;

    private final BooleanSetting visual = new BooleanSetting(this, "Рендерить позицию");

    public boolean defensiveActive, lastDefensive;

    private int rotationPhase = 0;
    private int directionIndex = 0;
    private long lastHitTime = 0;
    private LivingEntity lastTarget = null;
    private long targetLastAttackTime = 0;

    private final Map<String, Vec3d> namedDirections = Map.of(
            "Север", new Vec3d(0, 0, -1),
            "Юг", new Vec3d(0, 0, 1),
            "Запад", new Vec3d(-1, 0, 0),
            "Восток", new Vec3d(1, 0, 0),
            "Верх", new Vec3d(0, 1, 0),
            "Вниз", new Vec3d(0, -1, 0)
    );

    private List<Vec3d> airDirections = new ArrayList<>();
    private long fireworkTimer = 0L;
    public boolean trueFireWork = false;
    private int x, y, z;

    private long lastPacketSend = 0L;
    private long lastFireworkCheck = 0L;

    private final EventListener<SendPacketEvent> onPacket = event -> {
        if (mc.player == null || mc.world == null) return;
        if (!this.mode.is(this.advanced) || !mc.player.isGliding()) return;
        if (!fakelags.isEnabled()) return;

        if (isAccumulatingPackets && !event.isCancelled() && !(event.getPacket() instanceof KeepAliveC2SPacket)) {
            packetBuffer.add(event.getPacket());
            event.cancel();
        }
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (!this.mode.is(this.advanced) || !mc.player.isGliding()) return;

        updateFakeLagsState();

        if (firework.isEnabled()) {
            long now = System.currentTimeMillis();
            if (now - lastFireworkCheck > fireworkTime.getCurrentValue()) {
                if (trueFireWork) {
                    inventorySwap(new ItemStack(Items.FIREWORK_ROCKET));
                }
                lastFireworkCheck = now;
            }
        }

        defensiveActive = shouldLeave();
        lastDefensive = defensiveActive;

        if (resolver.isEnabled() && rotationPhase != 0) {
            float evadePitch = (float) (Math.sin(System.currentTimeMillis() / 100.0) * resolverStrength.getCurrentValue());
            float evadeYaw = (float) (Math.cos(System.nanoTime() / 100.0) * resolverStrength.getCurrentValue());
            float cy = getCurrentYaw();
            float cp = getCurrentPitch();
            rotate(cy + evadeYaw, MathHelper.clamp(cp + evadePitch, -89.9f, 89.9f));
        }

        if (rotationPhase != 0 && System.currentTimeMillis() - lastHitTime > silaTime.getCurrentValue()) {
            rotationPhase = 0;
        }
    };

    private final EventListener<Render3DEvent> onRender = event -> {
        if (!this.mode.is(this.advanced)) return;

        if (defensivePos != null && fakelags.isEnabled() && isAccumulatingPackets) {
        }
    };

    @Override
    public void onEnable() {
        rebuildAirDirections();
        packetBuffer.clear();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
    }

    private void rebuildAirDirections() {
        airDirections.clear();
        boolean isFlying = mc.player != null && mc.player.isGliding();

        addIfEnabled(preferNorth, isFlying, "Север");
        addIfEnabled(preferSouth, isFlying, "Юг");
        addIfEnabled(preferWest, isFlying, "Запад");
        addIfEnabled(preferEast, isFlying, "Восток");
        addIfEnabled(preferUp, isFlying, "Верх");
        addIfEnabled(preferDown, isFlying, "Вниз");

        if (airDirections.isEmpty()) {
            for (Map.Entry<String, Vec3d> entry : namedDirections.entrySet()) {
                if (entry.getKey().equals("Вниз") && !isFlying) continue;
                airDirections.add(entry.getValue());
            }
        }
        if (directionIndex >= airDirections.size()) directionIndex = 0;
    }

    private void addIfEnabled(BooleanSetting setting, boolean isFlying, String name) {
        if (setting.isEnabled()) {
            if (name.equals("Вниз") && !isFlying) return;
            Vec3d dir = namedDirections.get(name);
            if (dir != null) airDirections.add(dir);
        }
    }

    private void updateFakeLagsState() {
        if (!fakelags.isEnabled()) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            return;
        }

        boolean isFlying = mc.player.isGliding();
        KillAura killAura = elegant.getInstance().getModuleManager().getModuleSafe(KillAura.class);
        LivingEntity target = killAura != null ? killAura.target : null;
        boolean hasTarget = target != null;

        if (target != null && !target.equals(currentTarget)) {
            currentTarget = target;
            if (isAccumulatingPackets) {
                sendBufferedPackets();
            }
        }

        if (!isFlying || !hasTarget || perelet.isEnabled() || defensiveActive) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }

        double distanceToTarget = mc.player.getPos().distanceTo(target.getPos());
        double triggerDistance = fakelagsDistance.getCurrentValue();
        double maxDistance = fakelagsMaxDistance.getCurrentValue();

        if (distanceToTarget > maxDistance) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }
        if (distanceToTarget > triggerDistance && distanceToTarget <= maxDistance) {
            if (!isAccumulatingPackets) {
                isAccumulatingPackets = true;
                defensivePos = mc.player.getPos();
                defensiveTimer = System.currentTimeMillis();
            }
            wasInFarRange = true;
        } else if (distanceToTarget <= triggerDistance && wasInFarRange) {
            sendBufferedPackets();
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        } else if (distanceToTarget <= triggerDistance) {
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        }

        if (isAccumulatingPackets && System.currentTimeMillis() - defensiveTimer > 1500) {
            sendBufferedPackets();
            isAccumulatingPackets = true;
            defensiveTimer = System.currentTimeMillis();
        }
    }

    private void sendBufferedPackets() {
        if (packetBuffer.isEmpty()) return;
        for (Packet<?> packet : packetBuffer) {
            mc.getNetworkHandler().sendPacket(packet);
        }
        packetBuffer.clear();
    }

    public void onTargetAttack(LivingEntity attacker) {
        if (attacker != null && attacker.equals(lastTarget)) {
            targetLastAttackTime = System.currentTimeMillis();
            if (fakelags.isEnabled()) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
                wasInFarRange = false;
                defensivePos = null;
            }
        }
    }

    @Override
    public void onDisable() {
        sendBufferedPackets();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
        rotationPhase = 0;
    }

    private boolean shouldLeave() {
        boolean lowHP = mc.player.getHealth() <= 4.0f && leaveLowHP.isEnabled();
        boolean usingItem = mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD) && leaveUsingItem.isEnabled();
        return leaveHP.isEnabled() && (lowHP || usingItem);
    }

    public void nextPhase(LivingEntity target) {
        lastTarget = target;
        lastHitTime = System.currentTimeMillis();
        rotationPhase = 1;

        if (fakelags.isEnabled()) {
            sendBufferedPackets();
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        }

        rebuildAirDirections();

        int oldIndexInNew = -1;
        Vec3d oldDir = null;
        if (!airDirections.isEmpty() && directionIndex >= 0 && directionIndex < airDirections.size()) {
            oldDir = airDirections.get(directionIndex);
        }
        if (oldDir != null) {
            for (int i = 0; i < airDirections.size(); i++) {
                Vec3d v = airDirections.get(i);
                if (Double.compare(v.x, oldDir.x) == 0 && Double.compare(v.y, oldDir.y) == 0 && Double.compare(v.z, oldDir.z) == 0) {
                    oldIndexInNew = i;
                    break;
                }
            }
        }

        int size = airDirections.size();
        int candidateIndex = (oldIndexInNew == -1) ? 0 : (oldIndexInNew + 1) % size;
        int attempts = 0;
        int chosenIndex = candidateIndex;

        while (attempts < size) {
            Vec3d dir = airDirections.get(candidateIndex);
            if (resolver.isEnabled() && dir.y < 0 && !mc.player.isGliding()) {
                candidateIndex = (candidateIndex + 1) % size;
                attempts++;
                continue;
            }

            Vec3d candidatePos = target.getPos().add(dir.normalize().multiply(sila.getCurrentValue()));
            Vec3d tryBetter = findClosestValidPosAround(candidatePos, 4.5);
            Vec3d checkPos = tryBetter != null ? tryBetter : new Vec3d(candidatePos.x, Math.max(candidatePos.y, mc.player.getY() + 6), candidatePos.z);

            if (!(avoidWalls.isEnabled() && isObstructed(mc.player.getEyePos(), checkPos)) && isValidFlyPosition(checkPos)) {
                chosenIndex = candidateIndex;
                break;
            }

            candidateIndex = (candidateIndex + 1) % size;
            attempts++;
        }

        directionIndex = chosenIndex;
    }

    public void overtakingElytra(LivingEntity base, boolean attack) {
        boolean leave = shouldLeave();
        boolean inEvasion = rotationPhase != 0 || leave;

        Vec3d targetPos = base.getPos().add(0, base.getHeight() / 2.0, 0);
        Vec3d modifiedPos;

        if (inEvasion) {
            Vec3d dir = airDirections.get(directionIndex);
            double strength = sila.getCurrentValue();
            Vec3d candidatePos = targetPos.add(dir.normalize().multiply(strength));
            Vec3d tryBetter = findClosestValidPosAround(candidatePos, 10);
            if (tryBetter != null) {
                trueFireWork = true;
                candidatePos = tryBetter;
            } else {
                candidatePos = new Vec3d(candidatePos.x, Math.max(candidatePos.y, mc.player.getY() + 2), candidatePos.z);
            }
            modifiedPos = candidatePos;

            if (mc.player.getPos().distanceTo(modifiedPos) < 1) {
                rotationPhase = 0;
            }
        } else {
            double bps = Math.hypot(base.getX() - base.prevX, base.getZ() - base.prevZ) * 20;

            boolean canPredict = perelet.isEnabled() && bps >= 20.0 && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater();

            if (canPredict) {
                Vec3d motion = new Vec3d(base.getX() - base.prevX, base.getY() - base.prevY, base.getZ() - base.prevZ);
                double predictFactor = MathHelper.clamp(predict.getCurrentValue(), 0.2F, 2.5F);
                Vec3d predicted = motion.multiply(predictFactor * 1.5);

                if (predicted.lengthSquared() < 1.0E-4) {
                    predicted = base.getRotationVecClient().normalize().multiply(predictFactor);
                }
                modifiedPos = base.getPos().add(predicted).add(0, base.getHeight() / 2.0, 0);
            } else {
                modifiedPos = base.getPos().add(0, base.getHeight() / 2.0, 0);
            }
        }

        double dx = modifiedPos.x - mc.player.getX();
        double dy = modifiedPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = modifiedPos.z - mc.player.getZ();

        x = (int) modifiedPos.x;
        y = (int) modifiedPos.y;
        z = (int) modifiedPos.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));

        rotateSmooth(targetYaw, targetPitch, 1.15f, 140f, 40f);
    }

    public void targetDefault(LivingEntity base, boolean attack) {
        Vec3d targetPos = base.getPos();
        double bps = Math.hypot(base.getX() - base.prevX, base.getZ() - base.prevZ) * 20;

        boolean canPredict = perelet.isEnabled() && bps >= 20.0 && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater();

        if (canPredict) {
            Vec3d motion = new Vec3d(base.getX() - base.prevX, base.getY() - base.prevY, base.getZ() - base.prevZ);
            double predictFactor = MathHelper.clamp(predict.getCurrentValue(), 0.2F, 2.5F);
            Vec3d predicted = motion.multiply(predictFactor * 1.5);

            if (predicted.lengthSquared() < 1.0E-4) {
                predicted = base.getRotationVecClient().normalize().multiply(predictFactor);
            }
            targetPos = targetPos.add(predicted);
        }

        double diffX = targetPos.x - mc.player.getX();
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = targetPos.z - mc.player.getZ();

        x = (int) targetPos.x;
        y = (int) targetPos.y;
        z = (int) targetPos.z;

        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))));

        rotateSmooth(yaw, pitch, 1.15f, 140f, 40f);
    }

    private Vec3d findClosestValidPosAround(Vec3d center, double radius) {
        List<Vec3d> offsets = new ArrayList<>();
        for (int y = 2; y >= -1; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && y == 0) continue;
                    offsets.add(new Vec3d(dx, y, dz));
                }
            }
        }

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (Vec3d offset : offsets) {
            Vec3d candidate = center.add(offset);
            if (!isValidFlyPosition(candidate)) continue;
            if (isObstructed(mc.player.getEyePos(), candidate)) continue;
            double dist = candidate.squaredDistanceTo(center);
            if (dist < bestDist) {
                best = candidate;
                bestDist = dist;
            }
        }
        return best;
    }

    private boolean isObstructed(Vec3d from, Vec3d to) {
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() != HitResult.Type.MISS;
    }

    private boolean isValidFlyPosition(Vec3d pos) {
        Vec3d head = new Vec3d(pos.x, pos.y + mc.player.getHeight(), pos.z);
        Vec3d feet = new Vec3d(pos.x, pos.y, pos.z);
        boolean obstructedVertically = mc.world.raycast(new net.minecraft.world.RaycastContext(
                head, feet,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() != HitResult.Type.MISS;

        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        boolean isBlockSolid = mc.world.getBlockState(new BlockPos(blockX, blockY, blockZ)).isOpaqueFullCube();
        int groundY = mc.world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, new BlockPos(blockX, 0, blockZ)).getY();

        boolean belowGround = pos.y < groundY + 1.0;

        return !obstructedVertically && !isBlockSolid && !belowGround;
    }

    private void inventorySwap(ItemStack item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item.getItem()) {
                mc.player.getInventory().selectedSlot = i < 9 ? i : mc.player.getInventory().selectedSlot;
                break;
            }
        }
    }

    public String getMode() {
        return mode.getValue().getName();
    }

    private void rotate(float yaw, float pitch) {
        Rotation rot = new Rotation(yaw, pitch);
        elegant.getInstance().getRotationHandler().rotate(rot, MoveCorrection.DIRECT, 180f, 180f, 180f, RotationPriority.OVERRIDE);
    }

    private void rotateSmooth(float yaw, float pitch, float speed, float yawSpeed, float pitchSpeed) {
        Rotation rot = new Rotation(yaw, pitch);
        elegant.getInstance().getRotationHandler().rotate(rot, MoveCorrection.DIRECT, yawSpeed, pitchSpeed, speed, RotationPriority.OVERRIDE);
    }

    private float getCurrentYaw() {
        return elegant.getInstance().getRotationHandler().getCurrentRotation().getYaw();
    }

    private float getCurrentPitch() {
        return elegant.getInstance().getRotationHandler().getCurrentRotation().getPitch();
    }
}
